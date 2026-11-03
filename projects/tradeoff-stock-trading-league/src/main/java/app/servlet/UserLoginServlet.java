package app.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.sql.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

@WebServlet("/user-auth")
public class UserLoginServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    private Gson gson = new Gson();
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();
        
        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement updateStmt = null;
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
            
            String action = jsonData.has("action") ? jsonData.get("action").getAsString() : "login";
            
            System.out.println("POST /user-auth - Action: " + action);
            
            if ("login".equalsIgnoreCase(action)) {
                String username = jsonData.has("username") ? jsonData.get("username").getAsString() : null;
                String password = jsonData.has("password") ? jsonData.get("password").getAsString() : null;
                
                if (username == null || username.trim().isEmpty() ||
                    password == null || password.trim().isEmpty()) {
                    
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Username and password are required");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                username = username.trim();
                password = password.trim();
                
                System.out.println("Login attempt for user: " + username);
                
                conn = app.database.JDBCConnector.getConnection();
                String sql = "SELECT user_id, username, display_name, contact_info, " +
                            "profile_visibility, available_capital, created_at " +
                            "FROM Users WHERE username = ? AND password_hash = ?";
                stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                stmt.setString(2, password);
                
                rs = stmt.executeQuery();
                
                if (rs.next()) {
                    int userID = rs.getInt("user_id");
                    String displayName = rs.getString("display_name");
                    String contactInfo = rs.getString("contact_info");
                    boolean profileVisibility = rs.getBoolean("profile_visibility");
                    double availableCapital = rs.getDouble("available_capital");
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    
                    System.out.println("Login successful for user ID: " + userID);
                    
                    String updateSql = "UPDATE Users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";
                    updateStmt = conn.prepareStatement(updateSql);
                    updateStmt.setInt(1, userID);
                    updateStmt.executeUpdate();
                    
                    HttpSession session = request.getSession(true);
                    session.setAttribute("userID", userID);
                    session.setAttribute("username", username);
                    session.setAttribute("displayName", displayName);
                    session.setAttribute("loggedIn", true);
                    session.setMaxInactiveInterval(3600);
                    
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Login successful");
                    jsonResponse.addProperty("userID", userID);
                    jsonResponse.addProperty("username", username);
                    jsonResponse.addProperty("displayName", displayName);
                    jsonResponse.addProperty("contactInfo", contactInfo);
                    jsonResponse.addProperty("profileVisibility", profileVisibility);
                    jsonResponse.addProperty("availableCapital", availableCapital);
                    jsonResponse.addProperty("sessionID", session.getId());
                    response.setStatus(HttpServletResponse.SC_OK);
                    
                } else {
                    System.out.println("Login failed for user: " + username);
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Invalid username or password");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                }
            
            } else if ("logout".equalsIgnoreCase(action)) {
                HttpSession session = request.getSession(false);
                
                if (session != null) {
                    Integer userID = (Integer) session.getAttribute("userID");
                    String username = (String) session.getAttribute("username");
                    
                    System.out.println("Logout for user: " + username + " (ID: " + userID + ")");
                    
                    session.invalidate();
                    
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Logout successful");
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "No active session found");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
            
            } else if ("check-session".equalsIgnoreCase(action)) {
                HttpSession session = request.getSession(false);
                
                if (session != null && session.getAttribute("loggedIn") != null) {
                    Integer userID = (Integer) session.getAttribute("userID");
                    String username = (String) session.getAttribute("username");
                    String displayName = (String) session.getAttribute("displayName");
                    
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("loggedIn", true);
                    jsonResponse.addProperty("userID", userID);
                    jsonResponse.addProperty("username", username);
                    jsonResponse.addProperty("displayName", displayName);
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("loggedIn", false);
                    jsonResponse.addProperty("message", "No active session");
                    response.setStatus(HttpServletResponse.SC_OK);
                }
            
            } else if ("register".equalsIgnoreCase(action)) {
                String username = jsonData.has("username") ? jsonData.get("username").getAsString() : null;
                String password = jsonData.has("password") ? jsonData.get("password").getAsString() : null;
                String displayName = jsonData.has("displayName") ? jsonData.get("displayName").getAsString() : null;
                String contactInfo = jsonData.has("contactInfo") ? jsonData.get("contactInfo").getAsString() : null;
                
                if (username == null || username.trim().isEmpty() ||
                    password == null || password.trim().isEmpty() ||
                    displayName == null || displayName.trim().isEmpty()) {
                    
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Username, password, and display name are required");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                username = username.trim();
                password = password.trim();
                displayName = displayName.trim();
                
                System.out.println("Registration attempt for user: " + username);
                
                conn = app.database.JDBCConnector.getConnection();
                String checkSql = "SELECT user_id FROM Users WHERE username = ?";
                stmt = conn.prepareStatement(checkSql);
                stmt.setString(1, username);
                rs = stmt.executeQuery();
                
                if (rs.next()) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Username already taken");
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                String insertSql = "INSERT INTO Users (username, password_hash, display_name, contact_info, " +
                                 "profile_visibility, available_capital) VALUES (?, ?, ?, ?, TRUE, 100000.00)";
                stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.setString(3, displayName);
                stmt.setString(4, contactInfo);
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows > 0) {
                    ResultSet generatedKeys = stmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        int userID = generatedKeys.getInt(1);
                        
                        System.out.println("User registered successfully: " + username + " (ID: " + userID + ")");
                        
                        String portfolioSql = "INSERT INTO Portfolios (user_id, total_portfolio_value, " +
                                            "total_profit_loss, weekly_profit_loss, updated_at) " +
                                            "VALUES (?, 100000.00, 0.00, 0.00, CURRENT_TIMESTAMP)";
                        updateStmt = conn.prepareStatement(portfolioSql);
                        updateStmt.setInt(1, userID);
                        updateStmt.executeUpdate();
                        
                        HttpSession session = request.getSession(true);
                        session.setAttribute("userID", userID);
                        session.setAttribute("username", username);
                        session.setAttribute("displayName", displayName);
                        session.setAttribute("loggedIn", true);
                        session.setMaxInactiveInterval(3600);
                        
                        jsonResponse.addProperty("success", true);
                        jsonResponse.addProperty("message", "User registered successfully");
                        jsonResponse.addProperty("userID", userID);
                        jsonResponse.addProperty("username", username);
                        jsonResponse.addProperty("displayName", displayName);
                        response.setStatus(HttpServletResponse.SC_CREATED);
                    }
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Failed to register user");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                
            } else {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Invalid action. Use: login, logout, check-session, or register");
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
                if (stmt != null) stmt.close();
                if (updateStmt != null) updateStmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            out.print(gson.toJson(jsonResponse));
            out.flush();
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();
        
        try {
            String action = request.getParameter("action");
            
            System.out.println("GET /user-auth - Action: " + action);
            
            if ("check-session".equals(action) || action == null) {
                HttpSession session = request.getSession(false);
                
                if (session != null && session.getAttribute("loggedIn") != null) {
                    Integer userID = (Integer) session.getAttribute("userID");
                    String username = (String) session.getAttribute("username");
                    String displayName = (String) session.getAttribute("displayName");
                    
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("loggedIn", true);
                    jsonResponse.addProperty("userID", userID);
                    jsonResponse.addProperty("username", username);
                    jsonResponse.addProperty("displayName", displayName);
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("loggedIn", false);
                    jsonResponse.addProperty("message", "No active session");
                    response.setStatus(HttpServletResponse.SC_OK);
                }
            
            } else if ("logout".equals(action)) {
                HttpSession session = request.getSession(false);
                
                if (session != null) {
                    Integer userID = (Integer) session.getAttribute("userID");
                    String username = (String) session.getAttribute("username");
                    
                    System.out.println("Logout (GET) for user: " + username + " (ID: " + userID + ")");
                    
                    session.invalidate();
                    
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Logout successful");
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "No active session found");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
                
            } else {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Invalid action");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Server error: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            out.print(gson.toJson(jsonResponse));
            out.flush();
        }
    }
}