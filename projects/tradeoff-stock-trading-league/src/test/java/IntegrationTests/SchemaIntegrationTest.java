import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;

public class SchemaIntegrationTest {
    
    private Connection conn;
    
    @BeforeEach
    public void setUp() throws SQLException {
        conn = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/tradeoff_test",
            "testuser",
            "testpass"
        );
    }
    
    @AfterEach
    public void tearDown() throws SQLException {
        if (conn != null) conn.close();
    }
    
    @Test
    public void testUserCRUDOperations() throws SQLException {
        // Create
        String insertSQL = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
        pstmt.setString(1, "testuser");
        pstmt.setString(2, "hashedpassword123");
        pstmt.executeUpdate();
        
        ResultSet rs = pstmt.getGeneratedKeys();
        assertTrue(rs.next());
        int userId = rs.getInt(1);
        assertTrue(userId > 0);
        
        // Read
        String selectSQL = "SELECT * FROM users WHERE user_id = ?";
        pstmt = conn.prepareStatement(selectSQL);
        pstmt.setInt(1, userId);
        rs = pstmt.executeQuery();
        assertTrue(rs.next());
        assertEquals("testuser", rs.getString("username"));
        
        // Update
        String updateSQL = "UPDATE users SET username = ? WHERE user_id = ?";
        pstmt = conn.prepareStatement(updateSQL);
        pstmt.setString(1, "updateduser");
        pstmt.setInt(2, userId);
        pstmt.executeUpdate();
        
        // Delete
        String deleteSQL = "DELETE FROM users WHERE user_id = ?";
        pstmt = conn.prepareStatement(deleteSQL);
        pstmt.setInt(1, userId);
        pstmt.executeUpdate();
    }
    
    @Test
    public void testLedgerReflectsUserTransactions() throws SQLException {
        // Create user
        User user = new User("tradeuser", "pass123");
        user.saveToDatabase(conn);
        
        // Buy stock
        Stock stock = new Stock("AAPL");
        stock.setPrice(150.00);
        user.buy(stock, 10);
        
        // Verify ledger entry
        String query = "SELECT * FROM ledger WHERE user_id = ? AND ticker = ?";
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setInt(1, user.getUserId());
        pstmt.setString(2, "AAPL");
        ResultSet rs = pstmt.executeQuery();
        
        assertTrue(rs.next());
        assertEquals("BUY", rs.getString("transaction_type"));
        assertEquals(10, rs.getInt("quantity"));
    }
    
    @Test
    public void testForeignKeyPreventsOrphanedData() throws SQLException {
        assertThrows(SQLException.class, () -> {
            String insertSQL = "INSERT INTO ledger (user_id, ticker, quantity) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertSQL);
            pstmt.setInt(1, 99999); // Non-existent user
            pstmt.setString(2, "AAPL");
            pstmt.setInt(3, 10);
            pstmt.executeUpdate();
        });
    }
    
    @Test
    public void testUsernameUniqueConstraint() throws SQLException {
        String insertSQL = "INSERT INTO users (username, password_hash) VALUES (?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(insertSQL);
        pstmt.setString(1, "uniqueuser");
        pstmt.setString(2, "hash1");
        pstmt.executeUpdate();
        
        assertThrows(SQLException.class, () -> {
            PreparedStatement pstmt2 = conn.prepareStatement(insertSQL);
            pstmt2.setString(1, "uniqueuser");
            pstmt2.setString(2, "hash2");
            pstmt2.executeUpdate();
        });
    }
    
    @Test
    public void testDecimalPrecisionForCapitalAndPrice() throws SQLException {
        String insertSQL = "INSERT INTO users (username, password_hash, available_capital) VALUES (?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(insertSQL);
        pstmt.setString(1, "precisionuser");
        pstmt.setString(2, "hash");
        pstmt.setDouble(3, 12345.67);
        pstmt.executeUpdate();
        
        String selectSQL = "SELECT available_capital FROM users WHERE username = ?";
        pstmt = conn.prepareStatement(selectSQL);
        pstmt.setString(1, "precisionuser");
        ResultSet rs = pstmt.executeQuery();
        
        assertTrue(rs.next());
        assertEquals(12345.67, rs.getDouble("available_capital"), 0.01);
    }
    
    @Test
    public void testTimestampFunctionality() throws SQLException {
        String insertSQL = "INSERT INTO ledger (user_id, ticker, quantity, transaction_type) VALUES (?, ?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
        pstmt.setInt(1, 1);
        pstmt.setString(2, "AAPL");
        pstmt.setInt(3, 10);
        pstmt.setString(4, "BUY");
        pstmt.executeUpdate();
        
        ResultSet rs = pstmt.getGeneratedKeys();
        rs.next();
        int transactionId = rs.getInt(1);
        
        String selectSQL = "SELECT timestamp FROM ledger WHERE transaction_id = ?";
        pstmt = conn.prepareStatement(selectSQL);
        pstmt.setInt(1, transactionId);
        rs = pstmt.executeQuery();
        
        assertTrue(rs.next());
        assertNotNull(rs.getTimestamp("timestamp"));
    }

    @Test
    void testStockHistoryImport() throws Exception {
        StockHistoryImporter.importFromCSV(
            "IBM",
            "/Users/dhruvywuvy/Desktop/Github/201-Final/daily_IBM.csv",
            conn
        );

        PreparedStatement pstmt = conn.prepareStatement(
            "SELECT COUNT(*) FROM StockHistory WHERE stock_id = ?"
        );
        pstmt.setString(1, "IBM");
        ResultSet rs = pstmt.executeQuery();

        assertTrue(rs.next());
        assertTrue(rs.getInt(1) > 0);
    }
}