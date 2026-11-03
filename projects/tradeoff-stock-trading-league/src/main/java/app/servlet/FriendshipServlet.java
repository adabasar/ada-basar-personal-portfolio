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

/**
 * Friendship Servlet - Manages friend requests and relationships
 * MVP Implementation - No encryption, direct database access
 */
@WebServlet("/friendship")
public class FriendshipServlet extends HttpServlet {
    
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
            String userIdParam = request.getParameter("userId");
            
            System.out.println("GET /friendship - action: " + action + ", userId: " + userIdParam);
            
            if (userIdParam == null || userIdParam.trim().isEmpty()) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "User ID is required");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(jsonResponse));
                return;
            }
            
            int userId = Integer.parseInt(userIdParam.trim());
            conn = app.database.JDBCConnector.getConnection();
            
            if ("list".equals(action)) {
                // Get list of friends (accepted friendships)
                String sql = "SELECT u.user_id, u.username, u.display_name " +
                            "FROM Friendships f " +
                            "JOIN Users u ON (f.friend_id = u.user_id) " +
                            "WHERE f.user_id = ? AND f.status = 'accepted' " +
                            "UNION " +
                            "SELECT u.user_id, u.username, u.display_name " +
                            "FROM Friendships f " +
                            "JOIN Users u ON (f.user_id = u.user_id) " +
                            "WHERE f.friend_id = ? AND f.status = 'accepted'";
                
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, userId);
                stmt.setInt(2, userId);
                rs = stmt.executeQuery();
                
                List<JsonObject> friends = new ArrayList<>();
                while (rs.next()) {
                    JsonObject friend = new JsonObject();
                    friend.addProperty("userId", rs.getInt("user_id"));
                    friend.addProperty("username", rs.getString("username"));
                    friend.addProperty("displayName", rs.getString("display_name"));
                    friends.add(friend);
                }
                
                jsonResponse.addProperty("success", true);
                jsonResponse.add("friends", gson.toJsonTree(friends));
                response.setStatus(HttpServletResponse.SC_OK);
                
            } else if ("pending".equals(action)) {
                // Get pending friend requests sent to this user
                String sql = "SELECT f.friendship_id, u.user_id, u.username, u.display_name, f.created_at " +
                            "FROM Friendships f " +
                            "JOIN Users u ON f.user_id = u.user_id " +
                            "WHERE f.friend_id = ? AND f.status = 'pending'";
                
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, userId);
                rs = stmt.executeQuery();
                
                List<JsonObject> requests = new ArrayList<>();
                while (rs.next()) {
                    JsonObject request_obj = new JsonObject();
                    request_obj.addProperty("friendshipId", rs.getInt("friendship_id"));
                    request_obj.addProperty("userId", rs.getInt("user_id"));
                    request_obj.addProperty("username", rs.getString("username"));
                    request_obj.addProperty("displayName", rs.getString("display_name"));
                    request_obj.addProperty("createdAt", rs.getTimestamp("created_at").toString());
                    requests.add(request_obj);
                }
                
                jsonResponse.addProperty("success", true);
                jsonResponse.add("requests", gson.toJsonTree(requests));
                response.setStatus(HttpServletResponse.SC_OK);
                
            } else if ("sent".equals(action)) {
                // Get friend requests sent by this user
                String sql = "SELECT f.friendship_id, u.user_id, u.username, u.display_name, f.status, f.created_at " +
                            "FROM Friendships f " +
                            "JOIN Users u ON f.friend_id = u.user_id " +
                            "WHERE f.user_id = ?";
                
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, userId);
                rs = stmt.executeQuery();
                
                List<JsonObject> sentRequests = new ArrayList<>();
                while (rs.next()) {
                    JsonObject request_obj = new JsonObject();
                    request_obj.addProperty("friendshipId", rs.getInt("friendship_id"));
                    request_obj.addProperty("userId", rs.getInt("user_id"));
                    request_obj.addProperty("username", rs.getString("username"));
                    request_obj.addProperty("displayName", rs.getString("display_name"));
                    request_obj.addProperty("status", rs.getString("status"));
                    request_obj.addProperty("createdAt", rs.getTimestamp("created_at").toString());
                    sentRequests.add(request_obj);
                }
                
                jsonResponse.addProperty("success", true);
                jsonResponse.add("sentRequests", gson.toJsonTree(sentRequests));
                response.setStatus(HttpServletResponse.SC_OK);
                
            } else {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Invalid action. Use: list, pending, or sent");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
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
            
            String action = jsonData.has("action") ? jsonData.get("action").getAsString() : null;
            
            if (action == null || action.trim().isEmpty()) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Action is required");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print(gson.toJson(jsonResponse));
                return;
            }
            
            System.out.println("POST /friendship - Action: " + action);
            
            conn = app.database.JDBCConnector.getConnection();
            
            if ("send".equalsIgnoreCase(action)) {
                // Send friend request
                int userId = jsonData.get("userId").getAsInt();
                int friendId = jsonData.get("friendId").getAsInt();
                
                if (userId == friendId) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Cannot send friend request to yourself");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                // Check if friendship already exists
                String checkSql = "SELECT friendship_id, status FROM Friendships " +
                                "WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";
                checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, userId);
                checkStmt.setInt(2, friendId);
                checkStmt.setInt(3, friendId);
                checkStmt.setInt(4, userId);
                rs = checkStmt.executeQuery();
                
                if (rs.next()) {
                    String status = rs.getString("status");
                    if ("accepted".equals(status)) {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message", "Already friends");
                    } else {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message", "Friend request already exists");
                    }
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                // Insert new friend request
                String insertSql = "INSERT INTO Friendships (user_id, friend_id, status) VALUES (?, ?, 'pending')";
                stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                stmt.setInt(1, userId);
                stmt.setInt(2, friendId);
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows > 0) {
                    ResultSet generatedKeys = stmt.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        int friendshipId = generatedKeys.getInt(1);
                        
                        System.out.println("Friend request sent: " + userId + " -> " + friendId);
                        
                        jsonResponse.addProperty("success", true);
                        jsonResponse.addProperty("message", "Friend request sent successfully");
                        jsonResponse.addProperty("friendshipId", friendshipId);
                        response.setStatus(HttpServletResponse.SC_CREATED);
                    }
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Failed to send friend request");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                
            } else if ("accept".equalsIgnoreCase(action)) {
                // Accept friend request
                int friendshipId = jsonData.get("friendshipId").getAsInt();
                int userId = jsonData.get("userId").getAsInt();
                
                // Check if the request exists and user is the recipient
                String checkSql = "SELECT friend_id, status FROM Friendships WHERE friendship_id = ?";
                checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, friendshipId);
                rs = checkStmt.executeQuery();
                
                if (!rs.next()) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Friend request not found");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                int friendId = rs.getInt("friend_id");
                String status = rs.getString("status");
                
                if (friendId != userId) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Unauthorized to accept this request");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                if ("accepted".equals(status)) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Friend request already accepted");
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                // Update status to accepted
                String updateSql = "UPDATE Friendships SET status = 'accepted' WHERE friendship_id = ?";
                stmt = conn.prepareStatement(updateSql);
                stmt.setInt(1, friendshipId);
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows > 0) {
                    System.out.println("Friend request accepted: " + friendshipId);
                    
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Friend request accepted");
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Failed to accept friend request");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                
            } else if ("decline".equalsIgnoreCase(action)) {
                // Decline/remove friend request
                int friendshipId = jsonData.get("friendshipId").getAsInt();
                int userId = jsonData.get("userId").getAsInt();
                
                // Check if the request exists and user is the recipient
                String checkSql = "SELECT friend_id FROM Friendships WHERE friendship_id = ?";
                checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setInt(1, friendshipId);
                rs = checkStmt.executeQuery();
                
                if (!rs.next()) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Friend request not found");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                int friendId = rs.getInt("friend_id");
                
                if (friendId != userId) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Unauthorized to decline this request");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    out.print(gson.toJson(jsonResponse));
                    return;
                }
                
                // Delete the friend request
                String deleteSql = "DELETE FROM Friendships WHERE friendship_id = ?";
                stmt = conn.prepareStatement(deleteSql);
                stmt.setInt(1, friendshipId);
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows > 0) {
                    System.out.println("Friend request declined: " + friendshipId);
                    
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Friend request declined");
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Failed to decline friend request");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                }
                
            } else if ("remove".equalsIgnoreCase(action)) {
                // Remove an existing friend
                int userId = jsonData.get("userId").getAsInt();
                int friendId = jsonData.get("friendId").getAsInt();
                
                // Delete the friendship (works for both directions)
                String deleteSql = "DELETE FROM Friendships " +
                                 "WHERE (user_id = ? AND friend_id = ? AND status = 'accepted') " +
                                 "OR (user_id = ? AND friend_id = ? AND status = 'accepted')";
                stmt = conn.prepareStatement(deleteSql);
                stmt.setInt(1, userId);
                stmt.setInt(2, friendId);
                stmt.setInt(3, friendId);
                stmt.setInt(4, userId);
                
                int affectedRows = stmt.executeUpdate();
                
                if (affectedRows > 0) {
                    System.out.println("Friend removed: " + userId + " <-> " + friendId);
                    
                    jsonResponse.addProperty("success", true);
                    jsonResponse.addProperty("message", "Friend removed successfully");
                    response.setStatus(HttpServletResponse.SC_OK);
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Friendship not found or already removed");
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
                
            } else {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Invalid action. Use: send, accept, decline, or remove");
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
