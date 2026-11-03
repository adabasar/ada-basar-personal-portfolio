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
import com.google.gson.JsonSyntaxException;
import app.model.LeagueMember;
import app.model.LeagueInfo;

@WebServlet("/league")
public class LeagueServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    private Gson gson = new Gson();
    
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
            String leagueIdParam = request.getParameter("leagueId");
            String userIdParam = request.getParameter("userId");
            
            System.out.println("GET /league - action: " + action + ", leagueId: " + leagueIdParam);
            
            conn = app.database.JDBCConnector.getConnection();
            
            if ("rankings".equals(action)) {
                if (leagueIdParam == null || leagueIdParam.trim().isEmpty()) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "League ID is required");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                int leagueId = Integer.parseInt(leagueIdParam.trim());
                
                String sql = "SELECT u.user_id, u.username, u.display_name, " +
                            "COALESCE(p.total_portfolio_value, 0) as portfolio_value, " +
                            "COALESCE(p.total_profit_loss, 0) as profit_loss, " +
                            "COALESCE(p.weekly_profit_loss, 0) as weekly_profit_loss, " +
                            "lm.rank_position " +
                            "FROM LeagueMembers lm " +
                            "JOIN Users u ON lm.user_id = u.user_id " +
                            "LEFT JOIN Portfolios p ON u.user_id = p.user_id " +
                            "WHERE lm.league_id = ? " +
                            "ORDER BY portfolio_value DESC, profit_loss DESC, u.user_id ASC";
                
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, leagueId);
                rs = stmt.executeQuery();
                
                List<LeagueMember> rankings = new ArrayList<>();
                
                while (rs.next()) {
                    LeagueMember member = new LeagueMember();
                    member.setUserId(rs.getInt("user_id"));
                    member.setUsername(rs.getString("username"));
                    member.setDisplayName(rs.getString("display_name"));
                    member.setPortfolioValue(rs.getDouble("portfolio_value"));
                    member.setProfitLoss(rs.getDouble("profit_loss"));
                    member.setWeeklyProfitLoss(rs.getDouble("weekly_profit_loss"));
                    member.setRankPosition(rs.getInt("rank_position"));
                    rankings.add(member);
                }
                
                System.out.println("Found " + rankings.size() + " members in league " + leagueId);
                
                jsonResponse.addProperty("success", true);
                jsonResponse.addProperty("message", "Rankings retrieved successfully");
                jsonResponse.add("rankings", gson.toJsonTree(rankings));
                response.setStatus(HttpServletResponse.SC_OK);
                
            } else if ("user-league".equals(action)) {
                if (userIdParam == null || userIdParam.trim().isEmpty()) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "User ID is required");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                int userId = Integer.parseInt(userIdParam.trim());
                
                String sql = "SELECT l.league_id, l.league_name, l.admin_user_id, " +
                            "l.is_private, l.created_at, lm.rank_position " +
                            "FROM LeagueMembers lm " +
                            "JOIN Leagues l ON lm.league_id = l.league_id " +
                            "WHERE lm.user_id = ?";
                
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, userId);
                rs = stmt.executeQuery();
                
                if (rs.next()) {
                    LeagueInfo league = new LeagueInfo();
                    league.setLeagueId(rs.getInt("league_id"));
                    league.setLeagueName(rs.getString("league_name"));
                    league.setAdminUserId(rs.getInt("admin_user_id"));
                    league.setIsPrivate(rs.getBoolean("is_private"));
                    league.setCreatedAt(rs.getTimestamp("created_at"));
                    league.setUserRank(rs.getInt("rank_position"));
                    
                    jsonResponse.addProperty("success", true);
                    jsonResponse.add("league", gson.toJsonTree(league));
                } else {
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "User is not in any league");
                    jsonResponse.addProperty("inLeague", false);
                }
                
                response.setStatus(HttpServletResponse.SC_OK);
                
            } else if ("list".equals(action)) {
                String sql = "SELECT l.league_id, l.league_name, l.admin_user_id, " +
                            "l.is_private, l.created_at, COUNT(lm.user_id) as member_count " +
                            "FROM Leagues l " +
                            "LEFT JOIN LeagueMembers lm ON l.league_id = lm.league_id " +
                            "WHERE l.is_private = FALSE " +
                            "GROUP BY l.league_id " +
                            "ORDER BY l.created_at DESC";
                
                stmt = conn.prepareStatement(sql);
                rs = stmt.executeQuery();
                
                List<LeagueInfo> leagues = new ArrayList<>();
                
                while (rs.next()) {
                    LeagueInfo league = new LeagueInfo();
                    league.setLeagueId(rs.getInt("league_id"));
                    league.setLeagueName(rs.getString("league_name"));
                    league.setAdminUserId(rs.getInt("admin_user_id"));
                    league.setIsPrivate(rs.getBoolean("is_private"));
                    league.setCreatedAt(rs.getTimestamp("created_at"));
                    league.setMemberCount(rs.getInt("member_count"));
                    leagues.add(league);
                }
                
                jsonResponse.addProperty("success", true);
                jsonResponse.add("leagues", gson.toJsonTree(leagues));
                response.setStatus(HttpServletResponse.SC_OK);
                
            } else {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Invalid action");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format: " + e.getMessage());
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Invalid ID format");
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
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();
        
        Connection conn = null;
        PreparedStatement checkStmt = null;
        PreparedStatement insertStmt = null;
        PreparedStatement updateStmt = null;
        PreparedStatement deleteStmt = null;
        ResultSet rs = null;
        
        try {
            // Read request body
            StringBuilder sb = new StringBuilder();
            String line;
            try (BufferedReader reader = request.getReader()) {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            
            String requestBody = sb.toString();
            System.out.println("POST /league - Request body: " + requestBody);
            
            JsonObject jsonData = gson.fromJson(requestBody, JsonObject.class);
            
            if (jsonData == null) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Invalid JSON data");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(jsonResponse));
                return;
            }
            
            String action = jsonData.has("action") ? jsonData.get("action").getAsString() : null;
            
            if (action == null || action.trim().isEmpty()) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Action is required");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(jsonResponse));
                return;
            }
            
            conn = app.database.JDBCConnector.getConnection();
            
            if ("create".equalsIgnoreCase(action)) {
                int userId = jsonData.get("userId").getAsInt();
                String leagueName = jsonData.get("leagueName").getAsString();
                boolean isPrivate = jsonData.has("isPrivate") ? jsonData.get("isPrivate").getAsBoolean() : false;
                
                String checkSql = "SELECT COUNT(*) as count FROM LeagueMembers WHERE user_id = ?";
                checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, userId);
                rs = checkStmt.executeQuery();
                
                if (rs.next() && rs.getInt("count") > 0) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "User is already in a league");
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                String insertSql = "INSERT INTO Leagues (league_name, admin_user_id, is_private) VALUES (?, ?, ?)";
                insertStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                insertStmt.setString(1, leagueName);
                insertStmt.setInt(2, userId);
                insertStmt.setBoolean(3, isPrivate);
                
                int affectedRows = insertStmt.executeUpdate();
                
                if (affectedRows > 0) {
                    ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        int leagueId = generatedKeys.getInt(1);
                        
                        String addMemberSql = "INSERT INTO LeagueMembers (user_id, league_id, rank_position) VALUES (?, ?, 1)";
                        insertStmt = conn.prepareStatement(addMemberSql);
                        insertStmt.setInt(1, userId);
                        insertStmt.setInt(2, leagueId);
                        insertStmt.executeUpdate();
                        
                        jsonResponse.addProperty("success", true);
                        jsonResponse.addProperty("message", "League created successfully");
                        jsonResponse.addProperty("leagueId", leagueId);
                        response.setStatus(HttpServletResponse.SC_CREATED);
                    }
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Failed to create league");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                
            } else if ("join".equalsIgnoreCase(action)) {
                int userId = jsonData.get("userId").getAsInt();
                int leagueId = jsonData.get("leagueId").getAsInt();
                
                String checkSql = "SELECT COUNT(*) as count FROM LeagueMembers WHERE user_id = ?";
                checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, userId);
                rs = checkStmt.executeQuery();
                
                if (rs.next() && rs.getInt("count") > 0) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "User is already in a league");
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                String leagueCheckSql = "SELECT is_private FROM Leagues WHERE league_id = ?";
                checkStmt = conn.prepareStatement(leagueCheckSql);
                checkStmt.setInt(1, leagueId);
                rs = checkStmt.executeQuery();
                
                if (!rs.next()) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "League not found");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                boolean isPrivate = rs.getBoolean("is_private");
                if (isPrivate) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Cannot join private league without invitation");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                String insertSql = "INSERT INTO LeagueMembers (user_id, league_id) VALUES (?, ?)";
                insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setInt(1, userId);
                insertStmt.setInt(2, leagueId);
                
                int affectedRows = insertStmt.executeUpdate();
                
                if (affectedRows > 0) {
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Successfully joined league");
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Failed to join league");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                
            } else if ("leave".equalsIgnoreCase(action)) {
                int userId = jsonData.get("userId").getAsInt();
                int leagueId = jsonData.get("leagueId").getAsInt();
                
                String checkSql = "SELECT COUNT(*) as count FROM LeagueMembers WHERE user_id = ? AND league_id = ?";
                checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, userId);
                checkStmt.setInt(2, leagueId);
                rs = checkStmt.executeQuery();
                
                if (!rs.next() || rs.getInt("count") == 0) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "User is not in this league");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                String deleteSql = "DELETE FROM LeagueMembers WHERE user_id = ? AND league_id = ?";
                deleteStmt = conn.prepareStatement(deleteSql);
                deleteStmt.setInt(1, userId);
                deleteStmt.setInt(2, leagueId);
                
                int affectedRows = deleteStmt.executeUpdate();
                
                if (affectedRows > 0) {
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Successfully left league");
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Failed to leave league");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                
            } else if ("delete".equalsIgnoreCase(action)) {
                int userId = jsonData.get("userId").getAsInt();
                int leagueId = jsonData.get("leagueId").getAsInt();
                
                String checkSql = "SELECT admin_user_id FROM Leagues WHERE league_id = ?";
                checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, leagueId);
                rs = checkStmt.executeQuery();
                
                if (!rs.next()) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "League not found");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                int adminId = rs.getInt("admin_user_id");
                if (adminId != userId) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Only league admin can delete the league");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                String deleteMembersSql = "DELETE FROM LeagueMembers WHERE league_id = ?";
                deleteStmt = conn.prepareStatement(deleteMembersSql);
                deleteStmt.setInt(1, leagueId);
                deleteStmt.executeUpdate();
                
                String deleteLeagueSql = "DELETE FROM Leagues WHERE league_id = ?";
                deleteStmt = conn.prepareStatement(deleteLeagueSql);
                deleteStmt.setInt(1, leagueId);
                
                int affectedRows = deleteStmt.executeUpdate();
                
                if (affectedRows > 0) {
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "League deleted successfully");
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Failed to delete league");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                
            } else {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Invalid action");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            
        } catch (JsonSyntaxException e) {
            System.err.println("JSON parsing error: " + e.getMessage());
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Invalid JSON format: " + e.getMessage());
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
                if (checkStmt != null) checkStmt.close();
                if (insertStmt != null) insertStmt.close();
                if (updateStmt != null) updateStmt.close();
                if (deleteStmt != null) deleteStmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            out.print(gson.toJson(jsonResponse));
            out.flush();
        }
    }
}