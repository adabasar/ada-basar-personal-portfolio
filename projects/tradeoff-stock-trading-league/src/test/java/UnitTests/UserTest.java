import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class UserTest {
    
    private User user;
    private Stock stock;
    
    @BeforeEach
    public void setUp() {
        user = new User("testUser", "password123");
        user.setAvailableCapital(100000.00);
        stock = new Stock("AAPL");
        stock.setPrice(150.00);
    }
    
    // User.GetPercentChange() Tests
    
    @Test
    public void testGetPercentChangeBasicProfit() {
        user.setInitialCapital(100000.00);
        user.setAvailableCapital(110000.00);
        
        assertEquals(10.0, user.getPercentChange(), 0.01);
    }
    
    @Test
    public void testGetPercentChangeBasicLoss() {
        user.setInitialCapital(100000.00);
        user.setAvailableCapital(85000.00);
        
        assertEquals(-15.0, user.getPercentChange(), 0.01);
    }
    
    @Test
    public void testGetPercentChangeZeroReturn() {
        user.setInitialCapital(100000.00);
        user.setAvailableCapital(100000.00);
        
        assertEquals(0.0, user.getPercentChange(), 0.01);
    }
    
    @Test
    public void testGetPercentChangeHandlesDivisionByZero() {
        user.setInitialCapital(0.00);
        user.setAvailableCapital(100000.00);
        
        assertThrows(ArithmeticException.class, () -> user.getPercentChange());
    }
    
    // User.AddFriend Tests
    
    @Test
    public void testAddFriendValidUser() {
        User friend = new User("friend1", "pass1");
        assertTrue(user.addFriend(friend));
    }
    
    @Test
    public void testAddFriendRequestorAddedToList() {
        User friend = new User("friend1", "pass1");
        user.addFriend(friend);
        
        assertTrue(user.getFriends().contains(friend));
    }
    
    @Test
    public void testAddFriendRecipientAddedToList() {
        User friend = new User("friend1", "pass1");
        user.addFriend(friend);
        
        assertTrue(friend.getFriends().contains(user));
    }
    
    // User.Buy Tests
    
    @Test
    public void testBuyChecksMarketOpen() {
        // Assuming market closed
        MarketStatus.setClosed();
        assertFalse(user.buy(stock, 10));
    }
    
    @Test
    public void testBuyRequestSent() {
        MarketStatus.setOpen();
        assertTrue(user.buy(stock, 10));
    }
    
    @Test
    public void testBuyUserOwnsStock() {
        MarketStatus.setOpen();
        user.buy(stock, 10);
        
        assertTrue(user.getPortfolio().containsKey(stock.getTicker()));
    }
    
    @Test
    public void testBuyReducesUserBalance() {
        MarketStatus.setOpen();
        double initialBalance = user.getAvailableCapital();
        user.buy(stock, 10);
        
        assertEquals(initialBalance - (150.00 * 10), user.getAvailableCapital(), 0.01);
    }
    
    // User.Sell Tests
    
    @Test
    public void testSellRequestSent() {
        MarketStatus.setOpen();
        user.buy(stock, 10);
        assertTrue(user.sell(stock, 5));
    }
    
    @Test
    public void testSellUserDoesNotOwnStock() {
        assertFalse(user.sell(stock, 5));
    }
    
    @Test
    public void testSellIncreasesUserBalance() {
        MarketStatus.setOpen();
        user.buy(stock, 10);
        double balanceAfterBuy = user.getAvailableCapital();
        user.sell(stock, 5);
        
        assertEquals(balanceAfterBuy + (150.00 * 5), user.getAvailableCapital(), 0.01);
    }
    
    // User.AddToWatchlist Tests
    
    @Test
    public void testAddToWatchlistSuccess() {
        assertTrue(user.addToWatchlist(stock));
        assertTrue(user.getPortfolio().containsKey(stock.getTicker()));
    }
    
    @Test
    public void testAddToWatchlistDuplicate() {
        user.addToWatchlist(stock);
        assertFalse(user.addToWatchlist(stock));
    }
    
    // User.RemoveStock Tests
    
    @Test
    public void testRemoveStockFromPortfolio() {
        MarketStatus.setOpen();
        user.buy(stock, 10);
        assertTrue(user.removeStock(stock));
    }
    
    @Test
    public void testRemoveStockNotInPortfolio() {
        assertFalse(user.removeStock(stock));
    }
    
    // User.JoinLeague Tests
    
    @Test
    public void testJoinLeagueSuccess() {
        League league = new League("Test League");
        assertTrue(user.joinLeague(league));
        assertEquals(league, user.getLeague());
    }
    
    @Test
    public void testJoinLeagueAlreadyInLeague() {
        League league1 = new League("League 1");
        League league2 = new League("League 2");
        user.joinLeague(league1);
        
        assertFalse(user.joinLeague(league2));
    }
    
    // User.LeaveLeague Tests
    
    @Test
    public void testLeaveLeagueSuccess() {
        League league = new League("Test League");
        user.joinLeague(league);
        assertTrue(user.leaveLeague());
        assertNull(user.getLeague());
    }
    
    @Test
    public void testLeaveLeagueNotInLeague() {
        assertFalse(user.leaveLeague());
    }
    
    // User.GetLeagueRanking Tests
    
    @Test
    public void testGetLeagueRankingValid() {
        League league = new League("Test League");
        User user2 = new User("user2", "pass2");
        user.setPortfolioValue(110000);
        user2.setPortfolioValue(105000);
        
        league.addMember(user);
        league.addMember(user2);
        user.joinLeague(league);
        
        assertEquals(1, user.getLeagueRanking());
    }
    
    @Test
    public void testGetLeagueRankingNotInLeague() {
        assertEquals(-1, user.getLeagueRanking());
    }
    
    // User.GetGlobalRanking Tests
    
    @Test
    public void testGetGlobalRankingValid() {
        User.clearAllUsers();
        User user2 = new User("user2", "pass2");
        user.setPortfolioValue(110000);
        user2.setPortfolioValue(105000);
        
        assertTrue(user.getGlobalRanking() > 0);
    }
}