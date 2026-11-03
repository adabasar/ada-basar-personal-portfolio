package app.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;

public class LeagueInfoTest {
    
    private LeagueInfo leagueInfo;
    
    @BeforeEach
    public void setUp() {
        leagueInfo = new LeagueInfo();
    }
    
    @Test
    public void testSetAndGetLeagueId() {
        leagueInfo.setLeagueId(100);
        assertEquals(100, leagueInfo.getLeagueId());
    }
    
    @Test
    public void testSetAndGetLeagueName() {
        leagueInfo.setLeagueName("Elite Traders");
        assertEquals("Elite Traders", leagueInfo.getLeagueName());
    }
    
    @Test
    public void testSetAndGetAdminUserId() {
        leagueInfo.setAdminUserId(50);
        assertEquals(50, leagueInfo.getAdminUserId());
    }
    
    @Test
    public void testSetAndGetIsPrivate_True() {
        leagueInfo.setIsPrivate(true);
        assertTrue(leagueInfo.getIsPrivate());
    }
    
    @Test
    public void testSetAndGetIsPrivate_False() {
        leagueInfo.setIsPrivate(false);
        assertFalse(leagueInfo.getIsPrivate());
    }
    
    @Test
    public void testSetAndGetCreatedAt() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        leagueInfo.setCreatedAt(timestamp);
        assertEquals(timestamp, leagueInfo.getCreatedAt());
    }
    
    @Test
    public void testSetAndGetMemberCount() {
        leagueInfo.setMemberCount(25);
        assertEquals(25, leagueInfo.getMemberCount());
    }
    
    @Test
    public void testSetAndGetMemberCount_Zero() {
        leagueInfo.setMemberCount(0);
        assertEquals(0, leagueInfo.getMemberCount());
    }
    
    @Test
    public void testSetAndGetUserRank() {
        leagueInfo.setUserRank(7);
        assertEquals(7, leagueInfo.getUserRank());
    }
    
    @Test
    public void testSetAndGetUserRank_FirstPlace() {
        leagueInfo.setUserRank(1);
        assertEquals(1, leagueInfo.getUserRank());
    }
    
    @Test
    public void testAllProperties() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        
        leagueInfo.setLeagueId(200);
        leagueInfo.setLeagueName("Pro League");
        leagueInfo.setAdminUserId(10);
        leagueInfo.setIsPrivate(true);
        leagueInfo.setCreatedAt(timestamp);
        leagueInfo.setMemberCount(15);
        leagueInfo.setUserRank(3);
        
        assertEquals(200, leagueInfo.getLeagueId());
        assertEquals("Pro League", leagueInfo.getLeagueName());
        assertEquals(10, leagueInfo.getAdminUserId());
        assertTrue(leagueInfo.getIsPrivate());
        assertEquals(timestamp, leagueInfo.getCreatedAt());
        assertEquals(15, leagueInfo.getMemberCount());
        assertEquals(3, leagueInfo.getUserRank());
    }
    
    @Test
    public void testDefaultValues() {
        LeagueInfo newInfo = new LeagueInfo();
        
        assertEquals(0, newInfo.getLeagueId());
        assertNull(newInfo.getLeagueName());
        assertEquals(0, newInfo.getAdminUserId());
        assertFalse(newInfo.getIsPrivate());
        assertNull(newInfo.getCreatedAt());
        assertEquals(0, newInfo.getMemberCount());
        assertEquals(0, newInfo.getUserRank());
    }
    
    @Test
    public void testLeagueName_EmptyString() {
        leagueInfo.setLeagueName("");
        assertEquals("", leagueInfo.getLeagueName());
    }
    
    @Test
    public void testLeagueName_LongName() {
        String longName = "A".repeat(255);
        leagueInfo.setLeagueName(longName);
        assertEquals(longName, leagueInfo.getLeagueName());
    }
    
    @Test
    public void testMemberCount_LargeNumber() {
        leagueInfo.setMemberCount(10000);
        assertEquals(10000, leagueInfo.getMemberCount());
    }
    
    @Test
    public void testPublicLeague() {
        leagueInfo.setLeagueId(1);
        leagueInfo.setLeagueName("Public Trading League");
        leagueInfo.setAdminUserId(1);
        leagueInfo.setIsPrivate(false);
        leagueInfo.setMemberCount(50);
        
        assertFalse(leagueInfo.getIsPrivate());
        assertEquals(50, leagueInfo.getMemberCount());
    }
    
    @Test
    public void testPrivateLeague() {
        leagueInfo.setLeagueId(2);
        leagueInfo.setLeagueName("Private VIP League");
        leagueInfo.setAdminUserId(2);
        leagueInfo.setIsPrivate(true);
        leagueInfo.setMemberCount(5);
        
        assertTrue(leagueInfo.getIsPrivate());
        assertEquals(5, leagueInfo.getMemberCount());
    }
}
