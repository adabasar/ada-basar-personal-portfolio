import java.time.*;

/**
 * MarketStatus utility class for checking if the market is open.
 * This is a template for developers to implement.
 */
public class MarketStatus {
    private static final int MARKET_OPEN_HOUR = 9;
    private static final int MARKET_OPEN_MINUTE = 30;
    private static final int MARKET_CLOSE_HOUR = 16;
    private static final int MARKET_CLOSE_MINUTE = 0;
    
    private static boolean manuallyOpen = false;
    private static boolean manualOverride = false;
    
    /**
     * Check if the market is currently open
     * TODO: Implement actual market hours checking
     * @return true if market is open, false otherwise
     */
    public static boolean isOpen() {
        if (manualOverride) {
            return manuallyOpen;
        }
        
        // TODO: Implement real market hours checking
        // Check if it's a weekday between 9:30 AM and 4:00 PM EST
        ZonedDateTime easternTime = ZonedDateTime.now(ZoneId.of("America/New_York"));
        DayOfWeek day = easternTime.getDayOfWeek();
        
        // Weekend check
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        
        // Time check
        LocalTime time = easternTime.toLocalTime();
        LocalTime marketOpen = LocalTime.of(MARKET_OPEN_HOUR, MARKET_OPEN_MINUTE);
        LocalTime marketClose = LocalTime.of(MARKET_CLOSE_HOUR, MARKET_CLOSE_MINUTE);
        
        return !time.isBefore(marketOpen) && time.isBefore(marketClose);
    }
    
    /**
     * Manually set market as open (for testing)
     */
    public static void setOpen() {
        manualOverride = true;
        manuallyOpen = true;
    }
    
    /**
     * Manually set market as closed (for testing)
     */
    public static void setClosed() {
        manualOverride = true;
        manuallyOpen = false;
    }
    
    /**
     * Reset to automatic market status detection
     */
    public static void resetOverride() {
        manualOverride = false;
        manuallyOpen = false;
    }
}
