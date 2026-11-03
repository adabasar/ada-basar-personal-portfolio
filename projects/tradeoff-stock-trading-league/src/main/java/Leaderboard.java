import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import com.tradeoff.util.DatabaseManager;

/**
 * Leaderboard class for managing global rankings.
 * Implements in-memory sorting, O(1) rank lookup, and pagination.
 */
public class Leaderboard {
    // 1) Static List<User> that stores users sorted by rank
    private static List<User> globalRankings = new ArrayList<>();

    // 2) Static Map<Integer, Integer> that maps userId -> rank for O(1) lookup
    private static Map<Integer, Integer> userRankings = new HashMap<>();

    /**
     * Update global rankings for all users.
     * Sorts by: portfolio value desc -> percent change desc -> username asc.
     * Rebuilds the rank map.
     * 
     * @param users list of users to rank
     */
    public static void updateGlobalRankings(List<User> users) {
        if (users == null) {
            return;
        }

        // 3) Copy the list
        List<User> sortedUsers = new ArrayList<>(users);

        // Sort users by appropriate criteria
        sortedUsers.sort((u1, u2) -> {
            // Priority 1: Portfolio Value (Desc)
            int pvCompare = Double.compare(u2.getPortfolioValue(), u1.getPortfolioValue());
            if (pvCompare != 0)
                return pvCompare;

            // Priority 2: Percent Change (Desc)
            int pcCompare = Double.compare(u2.getPercentChange(), u1.getPercentChange());
            if (pcCompare != 0)
                return pcCompare;

            // Priority 3: Username (Asc)
            String name1 = u1.getUsername() != null ? u1.getUsername() : "";
            String name2 = u2.getUsername() != null ? u2.getUsername() : "";
            return name1.compareTo(name2);
        });

        // Update static list reference
        globalRankings = sortedUsers;

        // Rebuild the rank map
        userRankings.clear();
        for (int i = 0; i < sortedUsers.size(); i++) {
            User user = sortedUsers.get(i);
            // Rank is 1-indexed (i + 1)
            userRankings.put(user.getUserId(), i + 1);
        }
    }

    /**
     * Get global rankings (defensive copy).
     * 
     * @return list of users sorted by ranking
     */
    public static List<User> getGlobalRankings() {
        // 4) Return a defensive copy
        return new ArrayList<>(globalRankings);
    }

    /**
     * Get global rankings with pagination.
     * Safely returns a sublist or empty list for invalid ranges.
     * 
     * @param page     page number (1-indexed)
     * @param pageSize number of users per page
     * @return sublist of users for the requested page
     */
    public static List<User> getGlobalRankings(int page, int pageSize) {
        if (page < 1 || pageSize <= 0) {
            return new ArrayList<>();
        }

        int totalUsers = globalRankings.size();
        int fromIndex = (page - 1) * pageSize;

        if (fromIndex >= totalUsers) {
            return new ArrayList<>();
        }

        int toIndex = Math.min(fromIndex + pageSize, totalUsers);

        // Return a copy of the sublist to maintain safety
        return new ArrayList<>(globalRankings.subList(fromIndex, toIndex));
    }

    public static void updateGlobalRankingsFromDB() {
        String sql = "SELECT u.user_id, u.username, " +
                "COALESCE(u.available_capital, 0) AS available_capital, " +
                "COALESCE(u.initial_capital, 100000) AS initial_capital, " +
                "COALESCE(p.total_portfolio_value, 0) AS portfolio_value " +
                "FROM Users u " +
                "LEFT JOIN Portfolios p ON u.user_id = p.user_id " +
                "ORDER BY portfolio_value DESC, u.username ASC";

        List<User> users = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int userId = rs.getInt("user_id");
                String username = rs.getString("username");
                double availableCapital = rs.getDouble("available_capital");
                double initialCapital = rs.getDouble("initial_capital");
                double portfolioValue = rs.getDouble("portfolio_value");

                // Use hydration constructor
                User u = new User(userId, username, availableCapital, initialCapital, portfolioValue);
                users.add(u);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Use your existing ranking logic
        updateGlobalRankings(users);
    }

    /**
     * Get a user's position in global rankings using the rank map.
     * 
     * @param user the user to find
     * @return ranking position (1-indexed), or -1 if not found
     */
    public static int getUserRanking(User user) {
        if (user == null) {
            return -1;
        }
        // 6) Use rank map instead of scanning
        // Note: This relies on user.getUserId() being stable and present in the map
        return userRankings.getOrDefault(user.getUserId(), -1);
    }

    /**
     * Clear all rankings (for testing).
     */
    public static void clearRankings() {
        // 7) Clear both lists
        globalRankings.clear();
        userRankings.clear();
    }
}
