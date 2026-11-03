import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import com.tradeoff.util.DatabaseManager;

/**
 * User class representing a user in the trading system.
 * This is a template for developers to implement.
 */
public class User {
    private String username;
    private String password;
    private double availableCapital;
    private double initialCapital;
    private double portfolioValue;
    private Map<String, Stock> portfolio; // ticker -> Stock
    private Map<String, Integer> stockCounts; // ticker -> quantity
    private Map<String, Stock> watchlist; // ticker -> Stock
    private List<User> friends;
    private League league;
    private int userId;

    private static List<User> allUsers = new ArrayList<>();

    /**
     * Constructor to create a new user with username and password
     * // User initialization logic
     */
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.availableCapital = 100000.00; // Default starting capital
        this.initialCapital = 100000.00;
        this.portfolioValue = 0.0;
        this.portfolio = new HashMap<>();
        this.stockCounts = new HashMap<>();
        this.watchlist = new HashMap<>();
        this.friends = new ArrayList<>();
        this.league = null;
        allUsers.add(this);
        // TODO: Add password hashing
        // TODO: Add validation
    }

    /**
     * Constructor to load user from database
     * 
     * @param userId the ID of the user to load
     */
    public User(int userId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            loadFromDatabase(conn, userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructor to load user from database with existing connection
     * 
     * @param conn   existing database connection
     * @param userId the ID of the user to load
     */
    public User(Connection conn, int userId) {
        loadFromDatabase(conn, userId);
    }

    /**
     * Constructor for manually hydrating a user object (e.g. from Leaderboard)
     */
    public User(int userId, String username, double availableCapital, double initialCapital, double portfolioValue) {
        this.userId = userId;
        this.username = username;
        this.availableCapital = availableCapital;
        this.initialCapital = initialCapital;
        this.portfolioValue = portfolioValue;

        // Initialize collections to empty to avoid nulls
        this.portfolio = new HashMap<>();
        this.stockCounts = new HashMap<>();
        this.watchlist = new HashMap<>();
        this.friends = new ArrayList<>();
    }

    private void loadFromDatabase(Connection conn, int userId) {
        this.userId = userId;
        this.portfolio = new HashMap<>();
        this.stockCounts = new HashMap<>();
        this.watchlist = new HashMap<>();
        this.friends = new ArrayList<>();

        String sql = "SELECT username, available_capital, initial_capital FROM Users WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    this.username = rs.getString("username");
                    this.availableCapital = rs.getDouble("available_capital");
                    this.initialCapital = rs.getDouble("initial_capital");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Load portfolio value from Portfolios table if exists
        String portSql = "SELECT total_portfolio_value FROM Portfolios WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(portSql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    this.portfolioValue = rs.getDouble("total_portfolio_value");
                }
            }
        } catch (SQLException e) {
            // Ignore if portfolio record doesn't exist yet
        }

        allUsers.add(this);
    }

    /**
     * Calculate the percentage change in portfolio value
     * // Calculates percentage change including stock holdings
     * 
     * @return percentage change (positive for profit, negative for loss)
     * @throws ArithmeticException if initial capital is zero
     */
    public double getPercentChange() {
        if (initialCapital == 0) {
            throw new ArithmeticException("Initial capital cannot be zero");
        }
        // Calculates total value from available capital and stock portfolio
        // For now, use availableCapital as the current value - fixed by computing
        // portfolio value
        double currentValue = this.getAvailableCapital() + this.getPortfolioValue();
        return ((currentValue - this.getInitialCapital()) / this.getInitialCapital()) * 100.0;
    }

    /**
     * Add a friend to this user's friend list
     * TODO: Implement friend request/acceptance logic
     * 
     * @param friend the user to add as friend
     * @return true if friend was added successfully
     */
    public boolean addFriend(User friend) {
        if (friend == null || friends.contains(friend) || friend == this) {
            return false;
        }
        // Establishes bidirectional friendship
        friends.add(friend);
        friend.getFriends().add(this);
        return true;
    }

    /**
     * Buy stocks for this user
     * TODO: Implement buy logic with API integration
     * 
     * @param stock    the stock to buy
     * @param quantity number of shares to buy
     * @return true if purchase was successful
     */
    public boolean buy(Stock stock, int quantity) {
        // Checks if market is open
        if (!MarketStatus.isOpen()) {
            return false;
        }

        if (quantity <= 0) {
            return false;
        }

        double cost = stock.getPrice() * quantity;

        // Validates sufficient capital
        if (availableCapital < cost) {
            return false;
        }

        // TODO: Send buy request to API, update portfolio and stockCounts
        portfolio.put(stock.getTicker(), stock);
        stockCounts.put(stock.getTicker(), stockCounts.getOrDefault(stock.getTicker(), 0) + quantity);
        availableCapital -= cost;

        // Persist purchase to database
        try (Connection conn = DatabaseManager.getConnection()) {
            // 1. Update available capital
            String updateCapSql = "UPDATE Users SET available_capital = ? WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateCapSql)) {
                stmt.setDouble(1, availableCapital);
                stmt.setInt(2, userId);
                stmt.executeUpdate();
            }

            // 2. Update stock holdings (UPSERT)
            String upsertStockSql = "INSERT INTO UserStocks (user_id, stock_id, quantity) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE quantity = quantity + ?";
            try (PreparedStatement stmt = conn.prepareStatement(upsertStockSql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, stock.getTicker());
                stmt.setInt(3, quantity);
                stmt.setInt(4, quantity);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            // In a real app, we should rollback local state here or throw exception
            return false;
        }

        // Update ledger in database (TODO)
        return true;
    }

    /**
     * Sell stocks for this user
     * TODO: Implement sell logic with API integration
     * 
     * @param stock    the stock to sell
     * @param quantity number of shares to sell
     * @return true if sale was successful
     */
    public boolean sell(Stock stock, int quantity) {
        // Verifies stock ownership
        if (!portfolio.containsKey(stock.getTicker())) {
            return false;
        }

        if (quantity <= 0) {
            return false;
        }

        Integer currentQuantity = stockCounts.get(stock.getTicker());
        if (currentQuantity == null || currentQuantity < quantity) {
            return false;
        }

        // TODO: Send sell request to API
        double proceeds = stock.getPrice() * quantity;
        availableCapital += proceeds;

        int newQuantity = currentQuantity - quantity;
        if (newQuantity == 0) {
            portfolio.remove(stock.getTicker());
            stockCounts.remove(stock.getTicker());
        } else {
            stockCounts.put(stock.getTicker(), newQuantity);
        }

        // Persist sale to database
        try (Connection conn = DatabaseManager.getConnection()) {
            // 1. Update available capital
            String updateCapSql = "UPDATE Users SET available_capital = ? WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateCapSql)) {
                stmt.setDouble(1, availableCapital);
                stmt.setInt(2, userId);
                stmt.executeUpdate();
            }

            // 2. Update stock holdings
            if (newQuantity == 0) {
                String deleteStockSql = "DELETE FROM UserStocks WHERE user_id = ? AND stock_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(deleteStockSql)) {
                    stmt.setInt(1, userId);
                    stmt.setString(2, stock.getTicker());
                    stmt.executeUpdate();
                }
            } else {
                String updateStockSql = "UPDATE UserStocks SET quantity = ? WHERE user_id = ? AND stock_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateStockSql)) {
                    stmt.setInt(1, newQuantity);
                    stmt.setInt(2, userId);
                    stmt.setString(3, stock.getTicker());
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        // Update ledger in database (TODO)
        return true;
    }

    /**
     * Add a stock to user's watchlist
     * Adds a stock to the user's watchlist.
     * 
     * @param stock the stock to watch
     * @return true if added successfully
     */
    public boolean addToWatchlist(Stock stock) {
        if (watchlist.containsKey(stock.getTicker())) {
            return false;
        }
        // TODO: Persist to database
        watchlist.put(stock.getTicker(), stock);
        return true;
    }

    /**
     * Remove a stock from user's portfolio
     * // Removes stock from portfolio
     * 
     * @param stock the stock to remove
     * @return true if removed successfully
     */
    public boolean removeStock(Stock stock) {
        if (!portfolio.containsKey(stock.getTicker())) {
            return false;
        }
        // Removes stock from internal maps
        portfolio.remove(stock.getTicker());
        stockCounts.remove(stock.getTicker());
        return true;
    }

    /**
     * Join a league
     * // Joins the specified league
     * 
     * @param league the league to join
     * @return true if joined successfully
     */
    public boolean joinLeague(League league) {
        if (this.league != null) {
            return false;
        }
        // TODO: Add validation and league approval logic
        this.league = league;
        try {
            league.addMember(this.userId);
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    /**
     * Leave current league
     * // Leaves the current league
     * 
     * @return true if left successfully
     */
    public boolean leaveLeague() {
        if (this.league == null) {
            return false;
        }

        // TODO: Handle admin leaving
        if (this.league.getAdminUserId() == this.userId) {
            try {
                league.quitLeague(this.userId);
            } catch (SQLException e) {
                return false;
            }
            return true;
        }

        // Removes user from league members
        League currentLeague = this.league;
        this.league = null;
        try {
            currentLeague.removeMember(this.userId);
        } catch (SQLException e) {
            return false;
        }
        return true;
    }

    /**
     * Get user's ranking within their league
     * // Retrieves ranking within the league
     * 
     * @return ranking (1-indexed), or -1 if not in a league
     */
    public int getLeagueRanking() {
        if (league == null) {
            return -1;
        }
        // TODO: Calculate ranking based on league's sort criteria
        try {
            league.sortRankings();
        } catch (SQLException e) {
            return -1;
        }
        List<User> rankings = league.getRankings();
        return rankings.indexOf(this) + 1;
    }

    /**
     * Get user's global ranking
     * Get user's global ranking from the leaderboard.
     * 
     * @return global ranking (1-indexed)
     */
    public int getGlobalRanking() {
        return Leaderboard.getUserRanking(this);
    }

    /**
     * Register this user in the system
     * Register this user in the system.
     */
    public void register() {
        String sql = "INSERT INTO Users (username, password) VALUES (?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, username);
            stmt.setString(2, password); // TODO: Add hashing in future

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    this.userId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save user data to database
     * Save user data to database.
     * 
     * @param conn database connection
     */
    public void saveToDatabase(Connection conn) {
        // Update or Insert into Portfolios
        // Assuming Portfolios table exists with columns: user_id,
        // total_portfolio_value, percent_change
        String sql = "INSERT INTO Portfolios (user_id, total_portfolio_value, percent_change) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE total_portfolio_value = ?, percent_change = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, this.userId);
            double currentVal = getPortfolioValue();
            double pctChange = 0.0;
            try {
                pctChange = getPercentChange();
            } catch (ArithmeticException e) {
                // Ignore if initial capital is 0
            }

            stmt.setDouble(2, currentVal);
            stmt.setDouble(3, pctChange);

            // Update values
            stmt.setDouble(4, currentVal);
            stmt.setDouble(5, pctChange);

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Getters and Setters

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public double getAvailableCapital() {
        return availableCapital;
    }

    public void setAvailableCapital(double availableCapital) {
        this.availableCapital = availableCapital;
    }

    public double getInitialCapital() {
        return initialCapital;
    }

    public void setInitialCapital(double initialCapital) {
        this.initialCapital = initialCapital;
    }

    public double getPortfolioValue() {
        double totalStockValue = 0.0;
        for (String ticker : portfolio.keySet()) {
            int quantity = stockCounts.getOrDefault(ticker, 0);
            Stock stock = portfolio.get(ticker);
            if (stock != null) {
                totalStockValue += stock.getPrice() * quantity;
            }
        }
        portfolioValue = totalStockValue;
        return portfolioValue;
    }

    public void setPortfolioValue(double portfolioValue) {
        this.portfolioValue = portfolioValue;
    }

    public Map<String, Stock> getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Map<String, Stock> portfolio) {
        this.portfolio = portfolio;
    }

    public Map<String, Integer> getStockCounts() {
        return stockCounts;
    }

    public void setStockCounts(Map<String, Integer> stockCounts) {
        this.stockCounts = stockCounts;
    }

    public Map<String, Stock> getWatchlist() {
        return watchlist;
    }

    public void setWatchlist(Map<String, Stock> watchlist) {
        this.watchlist = watchlist;
    }

    public List<User> getFriends() {
        return friends;
    }

    public void setFriends(List<User> friends) {
        this.friends = friends;
    }

    public League getLeague() {
        return league;
    }

    public void setLeague(League league) {
        this.league = league;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public static List<User> getAllUsers() {
        return allUsers;
    }

    public static void clearAllUsers() {
        allUsers.clear();
    }
}
