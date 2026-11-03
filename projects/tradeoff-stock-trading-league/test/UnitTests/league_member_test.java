package app.model;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class LeagueMemberTest {
    
    private LeagueMember member;
    
    @BeforeEach
    public void setUp() {
        member = new LeagueMember();
    }
    
    @Test
    public void testSetAndGetUserId() {
        member.setUserId(123);
        assertEquals(123, member.getUserId());
    }
    
    @Test
    public void testSetAndGetUsername() {
        member.setUsername("testuser");
        assertEquals("testuser", member.getUsername());
    }
    
    @Test
    public void testSetAndGetDisplayName() {
        member.setDisplayName("Test User");
        assertEquals("Test User", member.getDisplayName());
    }
    
    @Test
    public void testSetAndGetPortfolioValue() {
        member.setPortfolioValue(150000.50);
        assertEquals(150000.50, member.getPortfolioValue(), 0.01);
    }
    
    @Test
    public void testSetAndGetProfitLoss() {
        member.setProfitLoss(5000.25);
        assertEquals(5000.25, member.getProfitLoss(), 0.01);
    }
    
    @Test
    public void testSetAndGetProfitLoss_Negative() {
        member.setProfitLoss(-2500.75);
        assertEquals(-2500.75, member.getProfitLoss(), 0.01);
    }
    
    @Test
    public void testSetAndGetWeeklyProfitLoss() {
        member.setWeeklyProfitLoss(1200.00);
        assertEquals(1200.00, member.getWeeklyProfitLoss(), 0.01);
    }
    
    @Test
    public void testSetAndGetWeeklyProfitLoss_Negative() {
        member.setWeeklyProfitLoss(-800.50);
        assertEquals(-800.50, member.getWeeklyProfitLoss(), 0.01);
    }
    
    @Test
    public void testSetAndGetRankPosition() {
        member.setRankPosition(5);
        assertEquals(5, member.getRankPosition());
    }
    
    @Test
    public void testSetAndGetRankPosition_FirstPlace() {
        member.setRankPosition(1);
        assertEquals(1, member.getRankPosition());
    }
    
    @Test
    public void testAllProperties() {
        member.setUserId(456);
        member.setUsername("john_doe");
        member.setDisplayName("John Doe");
        member.setPortfolioValue(125000.00);
        member.setProfitLoss(12500.00);
        member.setWeeklyProfitLoss(2500.00);
        member.setRankPosition(3);
        
        assertEquals(456, member.getUserId());
        assertEquals("john_doe", member.getUsername());
        assertEquals("John Doe", member.getDisplayName());
        assertEquals(125000.00, member.getPortfolioValue(), 0.01);
        assertEquals(12500.00, member.getProfitLoss(), 0.01);
        assertEquals(2500.00, member.getWeeklyProfitLoss(), 0.01);
        assertEquals(3, member.getRankPosition());
    }
    
    @Test
    public void testDefaultValues() {
        LeagueMember newMember = new LeagueMember();
        
        assertEquals(0, newMember.getUserId());
        assertNull(newMember.getUsername());
        assertNull(newMember.getDisplayName());
        assertEquals(0.0, newMember.getPortfolioValue(), 0.01);
        assertEquals(0.0, newMember.getProfitLoss(), 0.01);
        assertEquals(0.0, newMember.getWeeklyProfitLoss(), 0.01);
        assertEquals(0, newMember.getRankPosition());
    }
    
    @Test
    public void testPortfolioValue_LargeNumber() {
        member.setPortfolioValue(999999999.99);
        assertEquals(999999999.99, member.getPortfolioValue(), 0.01);
    }
    
    @Test
    public void testPortfolioValue_Zero() {
        member.setPortfolioValue(0.0);
        assertEquals(0.0, member.getPortfolioValue(), 0.01);
    }
}
