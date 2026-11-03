package app.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

@WebServlet("/market-status")
public class MarketStatusServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    private Gson gson = new Gson();
    
    private static final int MARKET_OPEN_HOUR = 9;
    private static final int MARKET_OPEN_MINUTE = 30;
    private static final int MARKET_CLOSE_HOUR = 16;
    private static final int MARKET_CLOSE_MINUTE = 0;
    
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
            System.out.println("GET /market-status");
            
            ZonedDateTime easternTime = ZonedDateTime.now(ZoneId.of("America/New_York"));
            
            boolean isOpen = isMarketOpen(easternTime);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            String currentTime = easternTime.format(timeFormatter);
            String currentDate = easternTime.format(dateFormatter);
            String dayOfWeek = easternTime.getDayOfWeek().toString();
            
            ZonedDateTime nextOpen = calculateNextOpen(easternTime);
            ZonedDateTime nextClose = calculateNextClose(easternTime);
            
            conn = app.database.JDBCConnector.getConnection();
            String logSql = "INSERT INTO market_status_log (check_time, is_open, day_of_week) " +
                          "VALUES (NOW(), ?, ?)";
            stmt = conn.prepareStatement(logSql);
            stmt.setBoolean(1, isOpen);
            stmt.setString(2, dayOfWeek);
            stmt.executeUpdate();
            
            jsonResponse.addProperty("success", true);
            jsonResponse.addProperty("isOpen", isOpen);
            jsonResponse.addProperty("currentTime", currentTime);
            jsonResponse.addProperty("currentDate", currentDate);
            jsonResponse.addProperty("dayOfWeek", dayOfWeek);
            jsonResponse.addProperty("timezone", "America/New_York");
            jsonResponse.addProperty("marketOpenTime", MARKET_OPEN_HOUR + ":" + 
                String.format("%02d", MARKET_OPEN_MINUTE));
            jsonResponse.addProperty("marketCloseTime", MARKET_CLOSE_HOUR + ":" + 
                String.format("%02d", MARKET_CLOSE_MINUTE));
            
            if (isOpen) {
                jsonResponse.addProperty("message", "Market is currently open for trading");
                jsonResponse.addProperty("nextClose", nextClose.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            } else {
                jsonResponse.addProperty("message", "Market is currently closed");
                jsonResponse.addProperty("nextOpen", nextOpen.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            
            response.setStatus(HttpServletResponse.SC_OK);
            System.out.println("Market status: " + (isOpen ? "OPEN" : "CLOSED"));
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
            
            ZonedDateTime easternTime = ZonedDateTime.now(ZoneId.of("America/New_York"));
            boolean isOpen = isMarketOpen(easternTime);
            
            jsonResponse.addProperty("success", true);
            jsonResponse.addProperty("isOpen", isOpen);
            jsonResponse.addProperty("message", isOpen ? "Market is open" : "Market is closed");
            jsonResponse.addProperty("note", "Status retrieved without database logging");
            response.setStatus(HttpServletResponse.SC_OK);
            
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
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
    
    private boolean isMarketOpen(ZonedDateTime currentTime) {
        DayOfWeek day = currentTime.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        
        LocalTime time = currentTime.toLocalTime();
        LocalTime marketOpen = LocalTime.of(MARKET_OPEN_HOUR, MARKET_OPEN_MINUTE);
        LocalTime marketClose = LocalTime.of(MARKET_CLOSE_HOUR, MARKET_CLOSE_MINUTE);
        
        return !time.isBefore(marketOpen) && time.isBefore(marketClose);
    }
    
    private ZonedDateTime calculateNextOpen(ZonedDateTime currentTime) {
        ZonedDateTime nextOpen = currentTime;
        LocalTime marketOpenTime = LocalTime.of(MARKET_OPEN_HOUR, MARKET_OPEN_MINUTE);
        
        if (currentTime.toLocalTime().isBefore(marketOpenTime) && 
            currentTime.getDayOfWeek() != DayOfWeek.SATURDAY && 
            currentTime.getDayOfWeek() != DayOfWeek.SUNDAY) {
            return currentTime.with(marketOpenTime);
        }
        
        do {
            nextOpen = nextOpen.plusDays(1);
        } while (nextOpen.getDayOfWeek() == DayOfWeek.SATURDAY || 
                 nextOpen.getDayOfWeek() == DayOfWeek.SUNDAY);
        
        return nextOpen.with(marketOpenTime);
    }
    
    private ZonedDateTime calculateNextClose(ZonedDateTime currentTime) {
        LocalTime marketCloseTime = LocalTime.of(MARKET_CLOSE_HOUR, MARKET_CLOSE_MINUTE);
        
        if (isMarketOpen(currentTime)) {
            return currentTime.with(marketCloseTime);
        }
        
        ZonedDateTime nextOpen = calculateNextOpen(currentTime);
        return nextOpen.with(marketCloseTime);
    }
    
    private boolean isMarketHoliday(ZonedDateTime date) {
        int month = date.getMonthValue();
        int day = date.getDayOfMonth();
        
        if (month == 1 && day == 1) return true;
        if (month == 7 && day == 4) return true;
        if (month == 12 && day == 25) return true;
        
        return false;
    }
}
