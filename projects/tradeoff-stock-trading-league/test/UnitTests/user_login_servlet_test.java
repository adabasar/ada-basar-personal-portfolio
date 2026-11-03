package app.servlet;

import org.junit.jupiter.api.*;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class UserLoginServletTest {
    
    @Mock
    private HttpServletRequest mockRequest;
    
    @Mock
    private HttpServletResponse mockResponse;
    
    @Mock
    private HttpSession mockSession;
    
    @Mock
    private Connection mockConnection;
    
    @Mock
    private PreparedStatement mockStatement;
    
    @Mock
    private ResultSet mockResultSet;
    
    private UserLoginServlet servlet;
    private StringWriter responseWriter;
    private PrintWriter writer;
    
    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        servlet = new UserLoginServlet();
        responseWriter = new StringWriter();
        writer = new PrintWriter(responseWriter);
        
        when(mockResponse.getWriter()).thenReturn(writer);
    }
    
    @AfterEach
    public void tearDown() {
        writer.close();
    }
    
    @Test
    public void testDoPost_InvalidJsonData() throws Exception {
        BufferedReader reader = new BufferedReader(new StringReader("invalid json"));
        when(mockRequest.getReader()).thenReturn(reader);
        
        servlet.doPost(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertFalse(jsonResponse.get("success").getAsBoolean());
        verify(mockResponse).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
    
    @Test
    public void testDoPost_NullJsonData() throws Exception {
        BufferedReader reader = new BufferedReader(new StringReader("null"));
        when(mockRequest.getReader()).thenReturn(reader);
        
        servlet.doPost(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertFalse(jsonResponse.get("success").getAsBoolean());
        assertEquals("Invalid JSON data", jsonResponse.get("message").getAsString());
    }
    
    @Test
    public void testDoPost_Login_MissingUsername() throws Exception {
        String json = "{\"action\": \"login\", \"password\": \"pass123\"}";
        BufferedReader reader = new BufferedReader(new StringReader(json));
        when(mockRequest.getReader()).thenReturn(reader);
        
        servlet.doPost(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertFalse(jsonResponse.get("success").getAsBoolean());
        assertEquals("Username and password are required", jsonResponse.get("message").getAsString());
        verify(mockResponse).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
    
    @Test
    public void testDoPost_Login_MissingPassword() throws Exception {
        String json = "{\"action\": \"login\", \"username\": \"testuser\"}";
        BufferedReader reader = new BufferedReader(new StringReader(json));
        when(mockRequest.getReader()).thenReturn(reader);
        
        servlet.doPost(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertFalse(jsonResponse.get("success").getAsBoolean());
        assertEquals("Username and password are required", jsonResponse.get("message").getAsString());
    }
    
    @Test
    public void testDoPost_Login_EmptyUsername() throws Exception {
        String json = "{\"action\": \"login\", \"username\": \"\", \"password\": \"pass123\"}";
        BufferedReader reader = new BufferedReader(new StringReader(json));
        when(mockRequest.getReader()).thenReturn(reader);
        
        servlet.doPost(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertFalse(jsonResponse.get("success").getAsBoolean());
        assertEquals("Username and password are required", jsonResponse.get("message").getAsString());
    }
    
    @Test
    public void testDoPost_Logout_NoSession() throws Exception {
        String json = "{\"action\": \"logout\"}";
        BufferedReader reader = new BufferedReader(new StringReader(json));
        when(mockRequest.getReader()).thenReturn(reader);
        when(mockRequest.getSession(false)).thenReturn(null);
        
        servlet.doPost(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertFalse(jsonResponse.get("success").getAsBoolean());
        assertEquals("No active session found", jsonResponse.get("message").getAsString());
    }
    
    @Test
    public void testDoPost_Logout_WithSession() throws Exception {
        String json = "{\"action\": \"logout\"}";
        BufferedReader reader = new BufferedReader(new StringReader(json));
        when(mockRequest.getReader()).thenReturn(reader);
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute("userID")).thenReturn(1);
        when(mockSession.getAttribute("username")).thenReturn("testuser");
        
        servlet.doPost(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertTrue(jsonResponse.get("success").getAsBoolean());
        assertEquals("Logout successful", jsonResponse.get("message").getAsString());
        verify(mockSession).invalidate();
    }
    
    @Test
    public void testDoPost_CheckSession_NotLoggedIn() throws Exception {
        String json = "{\"action\": \"check-session\"}";
        BufferedReader reader = new BufferedReader(new StringReader(json));
        when(mockRequest.getReader()).thenReturn(reader);
        when(mockRequest.getSession(false)).thenReturn(null);
        
        servlet.doPost(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertTrue(jsonResponse.get("success").getAsBoolean());
        assertFalse(jsonResponse.get("loggedIn").getAsBoolean());
    }
    
    @Test
    public void testDoPost_CheckSession_LoggedIn() throws Exception {
        String json = "{\"action\": \"check-session\"}";
        BufferedReader reader = new BufferedReader(new StringReader(json));
        when(mockRequest.getReader()).thenReturn(reader);
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute("loggedIn")).thenReturn(true);
        when(mockSession.getAttribute("userID")).thenReturn(1);
        when(mockSession.getAttribute("username")).thenReturn("testuser");
        when(mockSession.getAttribute("displayName")).thenReturn("Test User");
        
        servlet.doPost(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertTrue(jsonResponse.get("success").getAsBoolean());
        assertTrue(jsonResponse.get("loggedIn").getAsBoolean());
        assertEquals(1, jsonResponse.get("userID").getAsInt());
        assertEquals("testuser", jsonResponse.get("username").getAsString());
    }
    
    @Test
    public void testDoPost_Register_MissingFields() throws Exception {
        String json = "{\"action\": \"register\", \"username\": \"testuser\"}";
        BufferedReader reader = new BufferedReader(new StringReader(json));
        when(mockRequest.getReader()).thenReturn(reader);
        
        servlet.doPost(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertFalse(jsonResponse.get("success").getAsBoolean());
        assertEquals("Username, password, and display name are required", 
                    jsonResponse.get("message").getAsString());
    }
    
    @Test
    public void testDoPost_InvalidAction() throws Exception {
        String json = "{\"action\": \"invalid\"}";
        BufferedReader reader = new BufferedReader(new StringReader(json));
        when(mockRequest.getReader()).thenReturn(reader);
        
        servlet.doPost(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertFalse(jsonResponse.get("success").getAsBoolean());
        assertTrue(jsonResponse.get("message").getAsString().contains("Invalid action"));
    }
    
    @Test
    public void testDoPost_ContentTypeSet() throws Exception {
        String json = "{\"action\": \"invalid\"}";
        BufferedReader reader = new BufferedReader(new StringReader(json));
        when(mockRequest.getReader()).thenReturn(reader);
        
        servlet.doPost(mockRequest, mockResponse);
        
        verify(mockResponse).setContentType("application/json");
        verify(mockResponse).setCharacterEncoding("UTF-8");
    }
    
    @Test
    public void testDoGet_CheckSession_NotLoggedIn() throws Exception {
        when(mockRequest.getParameter("action")).thenReturn("check-session");
        when(mockRequest.getSession(false)).thenReturn(null);
        
        servlet.doGet(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertTrue(jsonResponse.get("success").getAsBoolean());
        assertFalse(jsonResponse.get("loggedIn").getAsBoolean());
    }
    
    @Test
    public void testDoGet_CheckSession_LoggedIn() throws Exception {
        when(mockRequest.getParameter("action")).thenReturn("check-session");
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute("loggedIn")).thenReturn(true);
        when(mockSession.getAttribute("userID")).thenReturn(1);
        when(mockSession.getAttribute("username")).thenReturn("testuser");
        when(mockSession.getAttribute("displayName")).thenReturn("Test User");
        
        servlet.doGet(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertTrue(jsonResponse.get("success").getAsBoolean());
        assertTrue(jsonResponse.get("loggedIn").getAsBoolean());
    }
    
    @Test
    public void testDoGet_Logout() throws Exception {
        when(mockRequest.getParameter("action")).thenReturn("logout");
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockSession.getAttribute("userID")).thenReturn(1);
        when(mockSession.getAttribute("username")).thenReturn("testuser");
        
        servlet.doGet(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertTrue(jsonResponse.get("success").getAsBoolean());
        verify(mockSession).invalidate();
    }
    
    @Test
    public void testDoGet_InvalidAction() throws Exception {
        when(mockRequest.getParameter("action")).thenReturn("invalid");
        
        servlet.doGet(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertFalse(jsonResponse.get("success").getAsBoolean());
        assertEquals("Invalid action", jsonResponse.get("message").getAsString());
    }
    
    @Test
    public void testDoGet_ContentTypeSet() throws Exception {
        when(mockRequest.getParameter("action")).thenReturn("check-session");
        when(mockRequest.getSession(false)).thenReturn(null);
        
        servlet.doGet(mockRequest, mockResponse);
        
        verify(mockResponse).setContentType("application/json");
        verify(mockResponse).setCharacterEncoding("UTF-8");
    }
}
