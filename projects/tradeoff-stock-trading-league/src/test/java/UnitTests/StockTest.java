import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;

public class StockTest {
    
    private Stock stock;
    
    @BeforeEach
    public void setUp() {
        stock = new Stock("AAPL");
    }
    
    // Stock.fetchData(int time) Tests
    
    @Test
    public void testFetchDataValidParameterHandlingForAPI() {
        assertDoesNotThrow(() -> stock.fetchData(300));
    }
    
    @Test
    public void testFetchDataValidParameterBeforeAPICalled() {
        assertThrows(IllegalArgumentException.class, () -> stock.fetchData(-1));
        assertThrows(IllegalArgumentException.class, () -> stock.fetchData(0));
    }
    
    @Test
    public void testUniformParsingOfAPIData() {
        String[] testStocks = {"AAPL", "GOOGL", "MSFT", "TSLA", "AMZN"};
        for (String ticker : testStocks) {
            Stock testStock = new Stock(ticker);
            assertDoesNotThrow(() -> testStock.fetchData(60));
            assertNotNull(testStock.getPrice());
        }
    }
    
    @Test
    public void testCachingLogicForRecentCalls() throws InterruptedException {
        stock.fetchData(600);
        double firstPrice = stock.getPrice();
        long firstCallTime = System.currentTimeMillis();
        
        // Make another call within 10 minutes
        stock.fetchData(600);
        double secondPrice = stock.getPrice();
        long secondCallTime = System.currentTimeMillis();
        
        // If within cache window, should use cached data
        if ((secondCallTime - firstCallTime) < 600000) {
            assertEquals(firstPrice, secondPrice);
        }
    }
}