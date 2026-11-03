// StockHistoryImporter.java
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.*;
import java.time.LocalDate;

public class StockHistoryImporter {
    public static void importFromCSV(String ticker, String csvPath, Connection conn) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(csvPath));
        String line = reader.readLine(); // Skip header
        
        String sql = "INSERT INTO StockHistory (stock_id, date, open_price, high_price, " +
                    "low_price, close_price, volume) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE open_price=VALUES(open_price), " +
                    "high_price=VALUES(high_price), low_price=VALUES(low_price), " +
                    "close_price=VALUES(close_price), volume=VALUES(volume)";
        
        PreparedStatement pstmt = conn.prepareStatement(sql);
        
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            // Expected: stock_id,date,open,high,low,close,volume
            pstmt.setString(1, ticker);
            pstmt.setDate(2, Date.valueOf(LocalDate.parse(parts[0].trim())));
            pstmt.setBigDecimal(3, new java.math.BigDecimal(parts[1].trim()));
            pstmt.setBigDecimal(4, new java.math.BigDecimal(parts[2].trim()));
            pstmt.setBigDecimal(5, new java.math.BigDecimal(parts[3].trim()));
            pstmt.setBigDecimal(6, new java.math.BigDecimal(parts[4].trim()));
            pstmt.setLong(7, Long.parseLong(parts[5].trim()));
            pstmt.executeUpdate();
        }
        
        reader.close();
        pstmt.close();
    }
}