import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.*;
import java.util.ArrayList;
import java.util.List;

public class StressTest {
    
    private ExecutorService executorService;
    
    @BeforeEach
    public void setUp() {
        executorService = Executors.newFixedThreadPool(100);
    }
    
    @AfterEach
    public void tearDown() {
        executorService.shutdown();
    }
    
    // User Stress Tests
    
    @Test
    public void testThousandUsersJoiningAtOnce() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1000);
        List<Future<User>> futures = new ArrayList<>();
        
        for (int i = 0; i < 1000; i++) {
            final int userId = i;
            futures.add(executorService.submit(() -> {
                try {
                    User user = new User("user" + userId, "pass" + userId);
                    user.register();
                    return user;
                } finally {
                    latch.countDown();
                }
            }));
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertEquals(1000, futures.stream().filter(f -> {
            try { return f.get() != null; } catch (Exception e) { return false; }
        }).count());
    }
    
    @Test
    public void testAddingHundredFriendsAtOnce() throws InterruptedException {
        User mainUser = new User("mainuser", "pass");
        CountDownLatch latch = new CountDownLatch(100);
        
        for (int i = 0; i < 100; i++) {
            final int friendId = i;
            executorService.submit(() -> {
                try {
                    User friend = new User("friend" + friendId, "pass" + friendId);
                    mainUser.addFriend(friend);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }
    
    @Test
    public void testUpdatingGlobalRankingsForThousandUsers() throws InterruptedException {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            User user = new User("rankuser" + i, "pass" + i);
            user.setPortfolioValue(100000 + (i * 100));
            users.add(user);
        }
        
        long startTime = System.currentTimeMillis();
        Leaderboard.updateGlobalRankings(users);
        long endTime = System.currentTimeMillis();
        
        assertTrue((endTime - startTime) < 5000, "Ranking update took too long");
    }
    
    // League Stress Tests
    
    @Test
    public void testHundredLeaguesCreatedAtOnce() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(100);
        List<Future<League>> futures = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            final int leagueId = i;
            futures.add(executorService.submit(() -> {
                try {
                    User admin = new User("admin" + leagueId, "pass" + leagueId);
                    return League.createLeague("League" + leagueId, admin);
                } finally {
                    latch.countDown();
                }
            }));
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertEquals(100, futures.stream().filter(f -> {
            try { return f.get() != null; } catch (Exception e) { return false; }
        }).count());
    }
    
    @Test
    public void testAddingHundredMembersToLeague() throws InterruptedException {
        League league = new League("Mega League");
        CountDownLatch latch = new CountDownLatch(100);
        
        for (int i = 0; i < 100; i++) {
            final int memberId = i;
            executorService.submit(() -> {
                try {
                    User member = new User("member" + memberId, "pass" + memberId);
                    league.addMember(member);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }
    
    @Test
    public void testDeletingLeagueWithHundredMembers() {
        League league = new League("Big League");
        for (int i = 0; i < 100; i++) {
            User member = new User("member" + i, "pass" + i);
            league.addMember(member);
        }
        
        long startTime = System.currentTimeMillis();
        league.deleteLeague();
        long endTime = System.currentTimeMillis();
        
        assertTrue((endTime - startTime) < 3000, "League deletion took too long");
        assertEquals(0, league.getMembers().size());
    }
    
    @Test
    public void testAdjustingRankingsForHundredMembers() {
        League league = new League("Ranked League");
        for (int i = 0; i < 100; i++) {
            User member = new User("member" + i, "pass" + i);
            member.setPortfolioValue(100000 + (i * 500));
            league.addMember(member);
        }
        
        long startTime = System.currentTimeMillis();
        List<User> rankings = league.sortRankings("PORTFOLIO_VALUE");
        long endTime = System.currentTimeMillis();
        
        assertEquals(100, rankings.size());
        assertTrue((endTime - startTime) < 2000, "Ranking adjustment took too long");
    }
    
    // Stock Stress Tests
    
    @Test
    public void testThousandStockWatchlistingAtOnce() throws InterruptedException {
        User user = new User("watchuser", "pass");
        CountDownLatch latch = new CountDownLatch(1000);
        
        for (int i = 0; i < 1000; i++) {
            final int stockId = i;
            executorService.submit(() -> {
                try {
                    Stock stock = new Stock("STOCK" + stockId);
                    user.addToWatchlist(stock);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }
    
    @Test
    public void testThousandStockBuyingAtOnce() throws InterruptedException {
        MarketStatus.setOpen();
        CountDownLatch latch = new CountDownLatch(1000);
        List<Future<Boolean>> futures = new ArrayList<>();
        
        for (int i = 0; i < 1000; i++) {
            final int userId = i;
            futures.add(executorService.submit(() -> {
                try {
                    User user = new User("buyer" + userId, "pass" + userId);
                    user.setAvailableCapital(100000);
                    Stock stock = new Stock("AAPL");
                    stock.setPrice(150.00);
                    return user.buy(stock, 10);
                } finally {
                    latch.countDown();
                }
            }));
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }
    
    @Test
    public void testThousandStockSellingAtOnce() throws InterruptedException {
        MarketStatus.setOpen();
        CountDownLatch latch = new CountDownLatch(1000);
        List<Future<Boolean>> futures = new ArrayList<>();
        
        for (int i = 0; i < 1000; i++) {
            final int userId = i;
            futures.add(executorService.submit(() -> {
                try {
                    User user = new User("seller" + userId, "pass" + userId);
                    Stock stock = new Stock("AAPL");
                    stock.setPrice(150.00);
                    user.buy(stock, 20);
                    return user.sell(stock, 10);
                } finally {
                    latch.countDown();
                }
            }));
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }
    
    @Test
    public void testFetchingInitialStockDataAtOnce() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1000);
        List<Future<Stock>> futures = new ArrayList<>();
        
        String[] tickers = {"AAPL", "GOOGL", "MSFT", "TSLA", "AMZN"};
        
        for (int i = 0; i < 1000; i++) {
            final int stockId = i;
            futures.add(executorService.submit(() -> {
                try {
                    Stock stock = new Stock(tickers[stockId % tickers.length]);
                    stock.fetchData(60);
                    return stock;
                } finally {
                    latch.countDown();
                }
            }));
        }
        
        assertTrue(latch.await(60, TimeUnit.SECONDS));
    }
}