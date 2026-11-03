package app.servlet;

import com.google.gson.Gson;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/portfolio")
public class PortfolioServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson gson = new Gson();

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            // Check if user is logged in - matches UserLoginServlet's session attributes
            HttpSession session = request.getSession(false);
            if (session == null || session.getAttribute("loggedIn") == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"success\":false,\"message\":\"Unauthorized - Please log in first\"}");
                out.flush();
                return;
            }

            // Get user ID from session - NOTE: it's "userID" not "userId"
            Integer userId = (Integer) session.getAttribute("userID");
            if (userId == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.print("{\"success\":false,\"message\":\"User ID not found in session\"}");
                out.flush();
                return;
            }

            System.out.println("GET /api/portfolio - userId: " + userId);

            Map<String, Object> portfolioData = new HashMap<String, Object>();
            conn = app.database.JDBCConnector.getConnection();

            // 1. Get User's Financial Summary
            double availableCapital = 100000;
            double initialCapital = 100000;
            double portfolioValue = 0;

            String userSql = "SELECT available_capital FROM Users WHERE user_id = ?";
            stmt = conn.prepareStatement(userSql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                availableCapital = rs.getDouble("available_capital");
            }
            rs.close();
            stmt.close();

            // 2. Get Holdings
            List<Map<String, Object>> holdings = new ArrayList<Map<String, Object>>();
            
            String holdingsSql = "SELECT usi.stock_id AS ticker, s.company_name, usi.quantity AS shares, " +
                                 "usi.average_purchase_price " +
                                 "FROM UserStockInterests usi " +
                                 "LEFT JOIN Stocks s ON usi.stock_id = s.stock_id " +
                                 "WHERE usi.user_id = ? AND usi.is_holding = TRUE";
            
            stmt = conn.prepareStatement(holdingsSql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> holding = new HashMap<String, Object>();
                String ticker = rs.getString("ticker");
                String companyName = rs.getString("company_name");
                int shares = rs.getInt("shares");
                double averagePurchasePrice = rs.getDouble("average_purchase_price");
                
                double currentPrice = averagePurchasePrice;
                
                double totalCost = averagePurchasePrice * shares;
                double currentValue = currentPrice * shares;
                double priceChange = currentValue - totalCost;
                double percentChangeHolding = 0;
                if (averagePurchasePrice > 0) {
                    percentChangeHolding = ((currentPrice - averagePurchasePrice) / averagePurchasePrice) * 100;
                }

                holding.put("ticker", ticker);
                holding.put("companyName", companyName != null ? companyName : ticker);
                holding.put("quantity", shares);
                holding.put("averagePurchasePrice", averagePurchasePrice);
                holding.put("currentPrice", currentPrice);
                holding.put("currentValue", currentValue);
                holding.put("gainLoss", priceChange);
                holding.put("gainLossPercent", percentChangeHolding);
                
                holdings.add(holding);
                portfolioValue = portfolioValue + currentValue;
            }
            rs.close();
            stmt.close();

            // 3. Get Watchlist
            List<Map<String, Object>> watchlist = new ArrayList<Map<String, Object>>();
            String watchlistSql = "SELECT usi.stock_id AS ticker, s.company_name " +
                                  "FROM UserStockInterests usi " +
                                  "LEFT JOIN Stocks s ON usi.stock_id = s.stock_id " +
                                  "WHERE usi.user_id = ? AND usi.is_holding = FALSE";
            
            stmt = conn.prepareStatement(watchlistSql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> item = new HashMap<String, Object>();
                String ticker = rs.getString("ticker");
                String companyName = rs.getString("company_name");
                
                item.put("ticker", ticker);
                item.put("companyName", companyName != null ? companyName : ticker);
                item.put("currentPrice", 0.0);
                item.put("changePercent", 0.0);
                
                watchlist.add(item);
            }
            rs.close();
            stmt.close();

            // 4. Calculate final values
            double totalValue = availableCapital + portfolioValue;
            double profitLoss = totalValue - initialCapital;
            double percentChange = 0;
            if (initialCapital > 0) {
                percentChange = (profitLoss / initialCapital) * 100;
            }

            // 5. Populate response map
            portfolioData.put("success", true);
            portfolioData.put("availableCapital", availableCapital);
            portfolioData.put("portfolioValue", portfolioValue);
            portfolioData.put("totalValue", totalValue);
            portfolioData.put("profitLoss", profitLoss);
            portfolioData.put("percentChange", percentChange);
            portfolioData.put("holdings", holdings);
            portfolioData.put("watchlist", watchlist);

            response.setStatus(HttpServletResponse.SC_OK);
            out.print(gson.toJson(portfolioData));

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"message\":\"Database error: " + e.getMessage() + "\"}");
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
            out.flush();
        }
    }
}
