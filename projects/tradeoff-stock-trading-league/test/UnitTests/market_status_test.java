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

public class MarketStatusTest {
    
    @Mock
    private HttpServletRequest mockRequest;
    
    @Mock
    private HttpServletResponse mockResponse;
    
    @Mock
    private Connection mockConnection;
    
    @Mock
    private PreparedStatement mockStatement;
    
    private MarketStatus servlet;
    private StringWriter responseWriter;
    private PrintWriter writer;
    
    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        servlet = new MarketStatus();
        responseWriter = new StringWriter();
        writer = new PrintWriter(responseWriter);
        
        when(mockResponse.getWriter()).thenReturn(writer);
    }
    
    @AfterEach
    public void tearDown() {
        writer.close();
    }
    
    @Test
    public void testDoGet_Success() throws Exception {
        servlet.doGet(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        assertNotNull(response);
        assertTrue(response.contains("success"));
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertTrue(jsonResponse.get("success").getAsBoolean());
        assertTrue(jsonResponse.has("isOpen"));
        assertTrue(jsonResponse.has("currentTime"));
        assertTrue(jsonResponse.has("currentDate"));
        assertTrue(jsonResponse.has("dayOfWeek"));
    }
    
    @Test
    public void testDoGet_ContainsMarketHours() throws Exception {
        servlet.doGet(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertTrue(jsonResponse.has("marketOpenTime"));
        assertTrue(jsonResponse.has("marketCloseTime"));
        assertEquals("9:30", jsonResponse.get("marketOpenTime").getAsString());
        assertEquals("16:00", jsonResponse.get("marketCloseTime").getAsString());
    }
    
    @Test
    public void testDoGet_ContainsTimezone() throws Exception {
        servlet.doGet(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertTrue(jsonResponse.has("timezone"));
        assertEquals("America/New_York", jsonResponse.get("timezone").getAsString());
    }
    
    @Test
    public void testDoGet_ContainsMessage() throws Exception {
        servlet.doGet(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        assertTrue(jsonResponse.has("message"));
        String message = jsonResponse.get("message").getAsString();
        assertTrue(message.contains("Market is"));
    }
    
    @Test
    public void testDoGet_ResponseContentType() throws Exception {
        servlet.doGet(mockRequest, mockResponse);
        
        verify(mockResponse).setContentType("application/json");
        verify(mockResponse).setCharacterEncoding("UTF-8");
    }
    
    @Test
    public void testDoGet_ResponseStatus() throws Exception {
        servlet.doGet(mockRequest, mockResponse);
        
        verify(mockResponse).setStatus(HttpServletResponse.SC_OK);
    }
    
    @Test
    public void testDoGet_MarketOpenHasNextClose() throws Exception {
        servlet.doGet(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        boolean isOpen = jsonResponse.get("isOpen").getAsBoolean();
        
        if (isOpen) {
            assertTrue(jsonResponse.has("nextClose"));
        }
    }
    
    @Test
    public void testDoGet_MarketClosedHasNextOpen() throws Exception {
        servlet.doGet(mockRequest, mockResponse);
        
        writer.flush();
        String response = responseWriter.toString();
        
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        boolean isOpen = jsonResponse.get("isOpen").getAsBoolean();
        
        if (!isOpen) {
            assertTrue(jsonResponse.has("nextOpen"));
        }
    }
}
