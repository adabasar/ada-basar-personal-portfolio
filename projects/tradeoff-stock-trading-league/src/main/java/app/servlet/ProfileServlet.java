package app.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.sql.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Profile Servlet - Manages user profile information
 * MVP Implementation - No encryption, direct database access
 */
@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {
    
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
            String userIdParam = request.getParameter("userId");
            
            if (userIdParam == null || userIdParam.trim().isEmpty()) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "User ID is required");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(jsonResponse));
                return;
            }
            
            int userId = Integer.parseInt(userIdParam.trim());
            
            System.out.println("GET /profile - userId: " + userId);
            
            conn = app.database.JDBCConnector.getConnection();
            
            String sql = "SELECT user_id, username, display_name, contact_info, " +
                        "profile_visibility, available_capital, created_at, last_login " +
                        "FROM Users WHERE user_id = ?";
            
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                JsonObject profile = new JsonObject();
                profile.addProperty("userId", rs.getInt("user_id"));
                profile.addProperty("username", rs.getString("username"));
                profile.addProperty("displayName", rs.getString("display_name"));
                profile.addProperty("contactInfo", rs.getString("contact_info"));
                profile.addProperty("profileVisibility", rs.getBoolean("profile_visibility"));
                profile.addProperty("availableCapital", rs.getDouble("available_capital"));
                
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    profile.addProperty("createdAt", createdAt.toString());
                }
                
                Timestamp lastLogin = rs.getTimestamp("last_login");
                if (lastLogin != null) {
                    profile.addProperty("lastLogin", lastLogin.toString());
                }
                
                jsonResponse.addProperty("success", true);
                jsonResponse.add("profile", profile);
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "User not found");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format: " + e.getMessage());
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Invalid user ID format");
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
        PreparedStatement stmt = null;
        PreparedStatement checkStmt = null;
        ResultSet rs = null;
        
        try {
            BufferedReader reader = request.getReader();
            JsonObject jsonData = gson.fromJson(reader, JsonObject.class);
            
            if (jsonData == null) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Invalid JSON data");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(jsonResponse));
                return;
            }
            
            String action = jsonData.has("action") ? jsonData.get("action").getAsString() : "update";
            
            System.out.println("POST /profile - Action: " + action);
            
            if ("update".equalsIgnoreCase(action)) {
                // Update user profile
                if (!jsonData.has("userId")) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "User ID is required");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                int userId = jsonData.get("userId").getAsInt();
                
                // Verify user exists
                conn = app.database.JDBCConnector.getConnection();
                String checkSql = "SELECT user_id FROM Users WHERE user_id = ?";
                checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, userId);
                rs = checkStmt.executeQuery();
                
                if (!rs.next()) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "User not found");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                // Build dynamic UPDATE query based on provided fields
                StringBuilder updateSql = new StringBuilder("UPDATE Users SET ");
                boolean hasField = false;
                
                if (jsonData.has("displayName")) {
                    updateSql.append("display_name = ?, ");
                    hasField = true;
                }
                
                if (jsonData.has("contactInfo")) {
                    updateSql.append("contact_info = ?, ");
                    hasField = true;
                }
                
                if (jsonData.has("profileVisibility")) {
                    updateSql.append("profile_visibility = ?, ");
                    hasField = true;
                }
                
                if (!hasField) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "No fields to update. Provide displayName, contactInfo, or profileVisibility");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                // Remove trailing comma and space, add WHERE clause
                updateSql.setLength(updateSql.length() - 2);
                updateSql.append(" WHERE user_id = ?");
                
                stmt = conn.prepareStatement(updateSql.toString());
                
                int paramIndex = 1;
                
                if (jsonData.has("displayName")) {
                    String displayName = jsonData.get("displayName").getAsString();
                    if (displayName == null || displayName.trim().isEmpty()) {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message", "Display name cannot be empty");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        out.print(gson.toJson(jsonResponse));
                        return;
                    }
                    stmt.setString(paramIndex++, displayName.trim());
                }
                
                if (jsonData.has("contactInfo")) {
                    String contactInfo = jsonData.get("contactInfo").getAsString();
                    stmt.setString(paramIndex++, contactInfo);
                }
                
                if (jsonData.has("profileVisibility")) {
                    boolean profileVisibility = jsonData.get("profileVisibility").getAsBoolean();
                    stmt.setBoolean(paramIndex++, profileVisibility);
                }
                
                stmt.setInt(paramIndex, userId);
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows > 0) {
                    System.out.println("Profile updated for user: " + userId);
                    
                    // Fetch updated profile
                    String selectSql = "SELECT user_id, username, display_name, contact_info, " +
                                     "profile_visibility, available_capital FROM Users WHERE user_id = ?";
                    checkStmt = conn.prepareStatement(selectSql);
                    checkStmt.setInt(1, userId);
                    rs = checkStmt.executeQuery();
                    
                    if (rs.next()) {
                        JsonObject profile = new JsonObject();
                        profile.addProperty("userId", rs.getInt("user_id"));
                        profile.addProperty("username", rs.getString("username"));
                        profile.addProperty("displayName", rs.getString("display_name"));
                        profile.addProperty("contactInfo", rs.getString("contact_info"));
                        profile.addProperty("profileVisibility", rs.getBoolean("profile_visibility"));
                        profile.addProperty("availableCapital", rs.getDouble("available_capital"));
                        
                        jsonResponse.addProperty("success", true);
                        jsonResponse.addProperty("message", "Profile updated successfully");
                        jsonResponse.add("profile", profile);
                        response.setStatus(HttpServletResponse.SC_OK);
                    }
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Failed to update profile");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                
            } else {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Invalid action. Use: update");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            
        } catch (JsonSyntaxException e) {
            System.err.println("JSON parsing error: " + e.getMessage());
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Invalid JSON format");
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
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            out.print(gson.toJson(jsonResponse));
            out.flush();
        }
    }
}
