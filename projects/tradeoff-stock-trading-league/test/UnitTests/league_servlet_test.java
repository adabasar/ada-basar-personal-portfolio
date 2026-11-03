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

public class LeagueServletTest {
    
    @Mock
    private HttpServletRequest mockRequest;
    
    @Mock
    private HttpServletResponse mockResponse;
    
    @Mock
    private Connection mockConnection;
    
    @Mock
    private PreparedStatement mockStatement;
    
    @Mock
    private ResultSet mockResultSet;
    
    private LeagueServlet servlet;
    private StringWriter responseWriter;
    private PrintWriter writer;
    
    @BeforeEach
    public void setUp() throws IOException, SQLException {
        MockitoAnnotations.openMocks(this);
        servlet = new LeagueServlet();
        responseWriter = new StringWriter();
        writer = new PrintWriter(responseWriter);
        
        when(mockResponse.getWriter()).thenReturn(writer);
    }
    
    @AfterEach
    public void tearDown() {
        writer.close();
    }
    
    @Test
    public void testDoGet_Rankings_MissingLeagueId() throws Exception {
        when(mockRequest.getParameter("action")).thenReturn("rankings");
        when(mockRequest.getParameter("leagueId")).thenReturn(null);
        
        servlet.doGet(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertFalse(jsonResponse.get("success").getAsBoolean());
        assertEquals("League ID is required", jsonResponse.get("message").getAsString());
        verify(mockResponse).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
    
    @Test
    public void testDoGet_UserLeague_MissingUserId() throws Exception {
        when(mockRequest.getParameter("action")).thenReturn("user-league");
        when(mockRequest.getParameter("userId")).thenReturn(null);
        
        servlet.doGet(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertFalse(jsonResponse.get("success").getAsBoolean());
        assertEquals("User ID is required", jsonResponse.get("message").getAsString());
        verify(mockResponse).setStatus(HttpServletResponse.SC_BAD_REQUEST);
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
        verify(mockResponse).setStatus(HttpServletResponse.SC_BAD_REQUEST);
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
    public void testDoPost_MissingAction() throws Exception {
        String json = "{\"userId\": 1, \"leagueName\": \"Test\"}";
        BufferedReader reader = new BufferedReader(new StringReader(json));
        when(mockRequest.getReader()).thenReturn(reader);
        
        servlet.doPost(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertFalse(jsonResponse.get("success").getAsBoolean());
        assertEquals("Action is required", jsonResponse.get("message").getAsString());
    }
    
    @Test
    public void testDoPost_InvalidAction() throws Exception {
        String json = "{\"action\": \"invalid\", \"userId\": 1}";
        BufferedReader reader = new BufferedReader(new StringReader(json));
        when(mockRequest.getReader()).thenReturn(reader);
        
        servlet.doPost(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertFalse(jsonResponse.get("success").getAsBoolean());
        assertEquals("Invalid action", jsonResponse.get("message").getAsString());
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
    public void testDoGet_ContentTypeSet() throws Exception {
        when(mockRequest.getParameter("action")).thenReturn("invalid");
        
        servlet.doGet(mockRequest, mockResponse);
        
        verify(mockResponse).setContentType("application/json");
        verify(mockResponse).setCharacterEncoding("UTF-8");
    }
    
    @Test
    public void testDoGet_InvalidNumberFormat() throws Exception {
        when(mockRequest.getParameter("action")).thenReturn("rankings");
        when(mockRequest.getParameter("leagueId")).thenReturn("abc");
        
        servlet.doGet(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertFalse(jsonResponse.get("success").getAsBoolean());
        assertEquals("Invalid ID format", jsonResponse.get("message").getAsString());
        verify(mockResponse).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
}
