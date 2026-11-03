import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

public class LeagueTest {
    
    private League league;
    private User user1, user2, user3;
    
    @BeforeEach
    public void setUp() {
        league = new League("Test League");
        user1 = new User("user1", "pass1");
        user2 = new User("user2", "pass2");
        user3 = new User("user3", "pass3");
    }
    
    // League.SortRankings() Tests
    
    @Test
    public void testSortRankingsByProfitLoss() {
        user1.setPortfolioValue(110000);
        user2.setPortfolioValue(95000);
        user3.setPortfolioValue(105000);
        
        league.addMember(user1);
        league.addMember(user2);
        league.addMember(user3);
        
        List<User> rankings = league.sortRankings("PROFIT_LOSS");
        
        assertEquals("user1", rankings.get(0).getUsername());
        assertEquals("user3", rankings.get(1).getUsername());
        assertEquals("user2", rankings.get(2).getUsername());
    }
    
    @Test
    public void testSortRankingsHandlesTies() {
        user1.setPortfolioValue(100000);
        user2.setPortfolioValue(100000);
        
        league.addMember(user1);
        league.addMember(user2);
        
        List<User> rankings = league.sortRankings("PORTFOLIO_VALUE");
        
        assertEquals(2, rankings.size());
        assertNotNull(rankings.get(0));
        assertNotNull(rankings.get(1));
    }
    
    @Test
    public void testSortRankingsHandlesEmptyDataSet() {
        List<User> rankings = league.sortRankings("PROFIT_LOSS");
        assertNotNull(rankings);
        assertEquals(0, rankings.size());
    }
    
    @Test
    public void testSortRankingsHandlesNegativeValues() {
        user1.setPortfolioValue(90000); // -10%
        user2.setPortfolioValue(85000); // -15%
        
        league.addMember(user1);
        league.addMember(user2);
        
        List<User> rankings = league.sortRankings("PROFIT_LOSS");
        
        assertEquals("user1", rankings.get(0).getUsername());
        assertEquals("user2", rankings.get(1).getUsername());
    }
    
    // AddMember Tests
    
    @Test
    public void testAddMemberSuccessfully() {
        assertTrue(league.addMember(user1));
        assertTrue(league.getMembers().contains(user1));
    }
    
    @Test
    public void testAddMemberAlreadyInLeague() {
        League otherLeague = new League("Other League");
        otherLeague.addMember(user1);
        
        assertFalse(league.addMember(user1));
    }
    
    // RemoveMember Tests
    
    @Test
    public void testRemoveMemberValidUser() {
        league.addMember(user1);
        assertTrue(league.removeMember(user1));
    }
    
    @Test
    public void testRemoveMemberNotInLeague() {
        assertFalse(league.removeMember(user1));
    }
    
    @Test
    public void testRemoveMemberNoLongerInLeague() {
        league.addMember(user1);
        league.removeMember(user1);
        assertFalse(league.getMembers().contains(user1));
    }
    
    // CreateLeague Tests
    
    @Test
    public void testCreateLeagueUserNotInAnotherLeague() {
        League newLeague = League.createLeague("New League", user1);
        assertNotNull(newLeague);
        assertEquals(user1, newLeague.getAdmin());
    }
    
    // DeleteLeague Tests
    
    @Test
    public void testDeleteLeagueRemovesAllMembers() {
        league.addMember(user1);
        league.addMember(user2);
        league.addMember(user3);
        
        league.deleteLeague();
        
        assertNull(user1.getLeague());
        assertNull(user2.getLeague());
        assertNull(user3.getLeague());
    }
    
    @Test
    public void testDeleteLeagueRemovesFromLeagueList() {
        league.addMember(user1);
        int initialCount = League.getAllLeagues().size();
        
        league.deleteLeague();
        
        assertFalse(League.getAllLeagues().contains(league));
    }
}