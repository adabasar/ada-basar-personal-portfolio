import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;

/**
 * Stock class representing a stock in the trading system.
 */
public class Stock {
    private String ticker;
    private double price;
    private long lastFetchTime;
    private static final long CACHE_DURATION = 600000; // 10 minutes in milliseconds
    

    private static String API_KEY;

    static {
        try {
            java.util.Properties prop = new java.util.Properties();
            java.io.InputStream input = Stock.class.getClassLoader()
                .getResourceAsStream("config.properties");
            
            if (input == null) {
                System.err.println("Unable to find config.properties");
                API_KEY = "demo"; // fallback to demo key
            } else {
                prop.load(input);
                API_KEY = prop.getProperty("alpha.vantage.api.key");
                input.close();
            }
        } catch (Exception e) {
            System.err.println("Error loading API key: " + e.getMessage());
            API_KEY = "demo";
        }
    }
    
    /**
     * Constructor to create a stock with ticker symbol
     */
    public Stock(String ticker) {
        if (ticker == null || ticker.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticker cannot be null or empty");
        }
        this.ticker = ticker.toUpperCase().trim();
        this.price = 0.0;
        this.lastFetchTime = 0;
    }
    
    /**
     * Fetch current stock price from Alpha Vantage API
     * Uses GLOBAL_QUOTE endpoint for real-time price
     */
    public void fetchData(int time) {
        if (time <= 0) {
            throw new IllegalArgumentException("Time parameter must be positive");
        }
        
        long currentTime = System.currentTimeMillis();
        
        // Check if we have cached data within 10-minute window
        if ((currentTime - lastFetchTime) < CACHE_DURATION && price > 0) {
            System.out.println("Using cached price for " + ticker + ": $" + price);
            return;
        }
        
        try {
            // Build API URL
            String urlString = String.format(
                "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=%s&apikey=%s",
                ticker, API_KEY
            );
            
            // Make HTTP request
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("API request failed with response code: " + responseCode);
            }
            
            // Read response
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            
            // Parse price from JSON response (simple string parsing)
            String jsonResponse = response.toString();
            String priceKey = "\"05. price\":";
            int priceIndex = jsonResponse.indexOf(priceKey);
            
            if (priceIndex == -1) {
                throw new RuntimeException("Could not find price in API response for " + ticker);
            }
            
            // Extract price value
            int startIndex = priceIndex + priceKey.length();
            int endIndex = jsonResponse.indexOf(",", startIndex);
            if (endIndex == -1) {
                endIndex = jsonResponse.indexOf("}", startIndex);
            }
            
            String priceString = jsonResponse.substring(startIndex, endIndex)
                                            .trim()
                                            .replace("\"", "")
                                            .trim();
            
            this.price = Double.parseDouble(priceString);
            this.lastFetchTime = currentTime;
            
            System.out.println("Fetched fresh price for " + ticker + ": $" + price);
            
        } catch (Exception e) {
            System.err.println("Error fetching stock data for " + ticker + ": " + e.getMessage());
            throw new RuntimeException("Failed to fetch stock data", e);
        }
    }
    
    /**
     * Load historical stock data from Alpha Vantage into database
     * Uses TIME_SERIES_DAILY endpoint (compact = last 100 days, free tier)
     * 
     * @param ticker Stock symbol to load
     * @param apiKey Your Alpha Vantage API key
     * @param conn Database connection
     * @return Number of records loaded
     */
    public static int loadHistoricalData(String ticker, String apiKey, Connection conn) {
        int recordsLoaded = 0;
        
        try {
            // First, ensure the stock exists in Stocks table
            ensureStockExists(ticker, conn);
            
            // Build API URL for historical data
            String urlString = String.format(
                "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=%s&outputsize=compact&apikey=%s",
                ticker, apiKey
            );
            
            // Make HTTP request
            URL url = new URL(urlString);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("GET");
            httpConn.setConnectTimeout(10000);
            httpConn.setReadTimeout(10000);
            
            if (httpConn.getResponseCode() != 200) {
                throw new RuntimeException("API request failed");
            }
            
            // Read response
            BufferedReader in = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            
            String jsonResponse = response.toString();
            
            // Prepare SQL statement
            String sql = "INSERT INTO StockHistory (stock_id, date, open_price, high_price, low_price, close_price, volume) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "open_price=VALUES(open_price), high_price=VALUES(high_price), " +
                        "low_price=VALUES(low_price), close_price=VALUES(close_price), volume=VALUES(volume)";
            
            PreparedStatement pstmt = conn.prepareStatement(sql);
            
            // Parse each date entry from the JSON
            // Looking for pattern: "YYYY-MM-DD": { "1. open": "...", "2. high": "...", etc }
            String[] lines = jsonResponse.split("\"\\d{4}-\\d{2}-\\d{2}\":");
            
            for (int i = 1; i < lines.length; i++) {
                String entry = lines[i];
                
                // Extract date from previous segment
                int dateStart = lines[i-1].lastIndexOf("\"");
                if (dateStart == -1) {
                    // Get date from just before current entry
                    String prevPart = jsonResponse.substring(
                        Math.max(0, jsonResponse.indexOf(entry) - 15),
                        jsonResponse.indexOf(entry)
                    );
                    int quotePos = prevPart.lastIndexOf("\"");
                    if (quotePos != -1) {
                        String dateStr = prevPart.substring(quotePos + 1).trim();
                        if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            LocalDate date = LocalDate.parse(dateStr);
                            
                            // Extract OHLCV data
                            BigDecimal open = extractPrice(entry, "1. open");
                            BigDecimal high = extractPrice(entry, "2. high");
                            BigDecimal low = extractPrice(entry, "3. low");
                            BigDecimal close = extractPrice(entry, "4. close");
                            long volume = extractVolume(entry, "5. volume");
                            
                            // Insert into database
                            pstmt.setString(1, ticker);
                            pstmt.setDate(2, Date.valueOf(date));
                            pstmt.setBigDecimal(3, open);
                            pstmt.setBigDecimal(4, high);
                            pstmt.setBigDecimal(5, low);
                            pstmt.setBigDecimal(6, close);
                            pstmt.setLong(7, volume);
                            pstmt.addBatch();
                            
                            recordsLoaded++;
                            
                            // Execute batch every 20 records
                            if (recordsLoaded % 20 == 0) {
                                pstmt.executeBatch();
                            }
                        }
                    }
                }
            }
            
            // Execute remaining batch
            pstmt.executeBatch();
            pstmt.close();
            
            System.out.println("Successfully loaded " + recordsLoaded + " historical records for " + ticker);
            
        } catch (Exception e) {
            System.err.println("Error loading historical data for " + ticker + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return recordsLoaded;
    }
    
    /**
     * Ensure stock exists in Stocks table (create if doesn't exist)
     */
    private static void ensureStockExists(String ticker, Connection conn) {
        try {
            String sql = "INSERT IGNORE INTO Stocks (stock_id, company_name, description) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, ticker);
            pstmt.setString(2, ticker + " Corp"); // Placeholder
            pstmt.setString(3, "Stock ticker: " + ticker);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (Exception e) {
            System.err.println("Error ensuring stock exists: " + e.getMessage());
        }
    }
    
    /**
     * Extract price value from JSON string
     */
    private static BigDecimal extractPrice(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":";
            int index = json.indexOf(searchKey);
            if (index == -1) return BigDecimal.ZERO;
            
            int start = index + searchKey.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            
            String value = json.substring(start, end).trim().replace("\"", "").trim();
            return new BigDecimal(value);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Extract volume from JSON string
     */
    private static long extractVolume(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":";
            int index = json.indexOf(searchKey);
            if (index == -1) return 0;
            
            int start = index + searchKey.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            
            String value = json.substring(start, end).trim().replace("\"", "").trim();
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0;
        }
    }
    
    // Getters and Setters
    
    public String getTicker() {
        return ticker;
    }
    
    public void setTicker(String ticker) {
        this.ticker = ticker;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public long getLastFetchTime() {
        return lastFetchTime;
    }
    
    public void setLastFetchTime(long lastFetchTime) {
        this.lastFetchTime = lastFetchTime;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Stock stock = (Stock) obj;
        return ticker.equals(stock.ticker);
    }
    
    @Override
    public int hashCode() {
        return ticker.hashCode();
    }
}

