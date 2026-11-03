package app.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * JDBC Connector for database connections
 * School project MVP - portable configuration for testing on different machines
 * No encryption or security to ensure easy setup
 */
public class JDBCConnector {
    // Use environment variables with fallback defaults for portability
    private static final String DB_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT = System.getenv().getOrDefault("DB_PORT", "3306");
    private static final String DB_NAME = System.getenv().getOrDefault("DB_NAME", "stock_trader_db");
    private static final String DB_USER = System.getenv().getOrDefault("DB_USER", "root");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "");
    
    // Build connection URL with no SSL and allow public key retrieval for testing
    private static final String DB_URL = String.format(
        "jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
        DB_HOST, DB_PORT, DB_NAME
    );

    static {
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC Driver not found", e);
        }
    }

    /**
     * Get a database connection
     * Configuration can be overridden using environment variables:
     * - DB_HOST (default: localhost)
     * - DB_PORT (default: 3306)
     * - DB_NAME (default: stock_trader_db)
     * - DB_USER (default: root)
     * - DB_PASSWORD (default: empty string)
     * 
     * @return Database connection
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
    
    /**
     * Get the current database configuration (for debugging)
     * @return Configuration string
     */
    public static String getConfiguration() {
        return String.format("Database: %s@%s:%s/%s (Password: %s)", 
            DB_USER, DB_HOST, DB_PORT, DB_NAME, 
            DB_PASSWORD.isEmpty() ? "empty" : "set");
    }
}
