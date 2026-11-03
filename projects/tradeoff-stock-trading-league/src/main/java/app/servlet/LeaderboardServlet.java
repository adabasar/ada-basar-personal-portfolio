package app.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import app.model.LeaderboardInfo;

/**
 * LeaderboardServlet - Handles global leaderboard rankings
 * Supports pagination and individual user rank lookup
 */
@WebServlet("/leaderboard")
public class LeaderboardServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    private Gson gson = new Gson();
    
    // In-memory cache for global rankings
    private static List<LeaderboardInfo> globalRankings = new ArrayList<>();
    private static long lastUpdateTime = 0;
    private static final long CACHE_DURATION_MS = 60000; // 1 minute cache
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            String action = request.getParameter("action");
            String pageParam = request.getParameter("page");
            String pageSizeParam = request.getParameter("pageSize");
            String userIdParam = request.getParameter("userId");
            
            System.out.println("GET /leaderboard - action: " + action);
            
            // Default action is to get rankings
            if (action == null || "rankings".equals(action)) {
                // Get pagination parameters
                int page = 1;
                int pageSize = 50; // Default page size
                
                if (pageParam != null && !pageParam.trim().isEmpty()) {
                    try {
                        page = Integer.parseInt(pageParam.trim());
                        if (page < 1) page = 1;
                    } catch (NumberFormatException e) {
                        page = 1;
                    }
                }
                
                if (pageSizeParam != null && !pageSizeParam.trim().isEmpty()) {
                    try {
                        pageSize = Integer.parseInt(pageSizeParam.trim());
                        if (pageSize < 1) pageSize = 50;
                        if (pageSize > 100) pageSize = 100; // Max page size
                    } catch (NumberFormatException e) {
                        pageSize = 50;
                    }
                }
                
                // Check if cache needs refresh
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastUpdateTime > CACHE_DURATION_MS || globalRankings.isEmpty()) {
                    updateGlobalRankings();
                }
                
                // Calculate pagination
                int totalUsers = globalRankings.size();
                int fromIndex = (page - 1) * pageSize;
                
                List<LeaderboardInfo> paginatedRankings;
                if (fromIndex >= totalUsers) {
                    paginatedRankings = new ArrayList<>();
                } else {
                    int toIndex = Math.min(fromIndex + pageSize, totalUsers);
                    paginatedRankings = new ArrayList<>(globalRankings.subList(fromIndex, toIndex));
                }
                
                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("page", page);
                jsonResponse.addProperty("pageSize", pageSize);
                jsonResponse.addProperty("totalUsers", totalUsers);
                jsonResponse.addProperty("totalPages", (int) Math.ceil((double) totalUsers / pageSize));
                jsonResponse.add("rankings", gson.toJsonTree(paginatedRankings));
                response.setStatus(HttpServletResponse.SC_OK);
                
            } else if ("user-rank".equals(action)) {
                // Get specific user's rank
                if (userIdParam == null || userIdParam.trim().isEmpty()) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "User ID is required");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                int userId = Integer.parseInt(userIdParam.trim());
                
                // Check if cache needs refresh
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastUpdateTime > CACHE_DURATION_MS || globalRankings.isEmpty()) {
                    updateGlobalRankings();
                }
                
                // Find user in rankings
                int rank = -1;
                LeaderboardInfo userInfo = null;
                
                for (int i = 0; i < globalRankings.size(); i++) {
                	LeaderboardInfo user = globalRankings.get(i);
                    if (user.getUserId() == userId) {
                        rank = i + 1; // 1-indexed rank
                        userInfo = user;
                        break;
                    }
                }
                
                if (rank != -1 && userInfo != null) {
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("rank", rank);
                    jsonResponse.addProperty("totalUsers", globalRankings.size());
                    jsonResponse.add("user", gson.toJsonTree(userInfo));
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "User not found in rankings");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
                
            } else if ("refresh".equals(action)) {
                // Force refresh the cache
                updateGlobalRankings();
                
                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("message", "Rankings refreshed successfully");
                jsonResponse.addProperty("totalUsers", globalRankings.size());
                response.setStatus(HttpServletResponse.SC_OK);
                
            } else {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Invalid action. Use: rankings, user-rank, or refresh");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format: " + e.getMessage());
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Invalid ID or page format");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } catch (SQLException e) {
            e.printStackTrace();
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Database error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Server error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            out.print(gson.toJson(jsonResponse));
            out.flush();
        }
    }
    
    /**
     * Update global rankings from database.
     * Sorts by: portfolio value DESC, profit/loss percentage DESC, username ASC
     */
    private synchronized void updateGlobalRankings() throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = app.database.JDBCConnector.getConnection();
            
            // Calculate profit/loss percentage based on initial capital of 100,000
            String sql = "SELECT u.user_id, u.username, u.display_name, " +
                        "COALESCE(p.total_portfolio_value, 100000.00) as portfolio_value, " +
                        "COALESCE(p.total_profit_loss, 0.00) as profit_loss, " +
                        "COALESCE(p.weekly_profit_loss, 0.00) as weekly_profit_loss, " +
                        "((COALESCE(p.total_profit_loss, 0.00) / 100000.00) * 100) as percent_change " +
                        "FROM Users u " +
                        "LEFT JOIN Portfolios p ON u.user_id = p.user_id " +
                        "ORDER BY portfolio_value DESC, percent_change DESC, u.username ASC";
            
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            
            List<LeaderboardInfo> rankings = new ArrayList<>();
            int rank = 1;
            
            while (rs.next()) {
            	LeaderboardInfo user = new LeaderboardInfo();
                user.setUserId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setDisplayName(rs.getString("display_name"));
                user.setPortfolioValue(rs.getDouble("portfolio_value"));
                user.setProfitLoss(rs.getDouble("profit_loss"));
                user.setWeeklyProfitLoss(rs.getDouble("weekly_profit_loss"));
                user.setPercentChange(rs.getDouble("percent_change"));
                user.setRank(rank++);
                rankings.add(user);
            }
            
            // Update cache
            globalRankings = rankings;
            lastUpdateTime = System.currentTimeMillis();
            
            System.out.println("Global rankings updated: " + rankings.size() + " users");
            
        } finally {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        }
    }
    
    /**
     * Clear the rankings cache (for testing or manual refresh).
     */
    public static synchronized void clearCache() {
        globalRankings.clear();
        lastUpdateTime = 0;
    }
}