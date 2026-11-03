package app.model;

import java.sql.Timestamp;

public class LeagueInfo {
    private int leagueId;
    private String leagueName;
    private int adminUserId;
    private boolean isPrivate;
    private Timestamp createdAt;
    private int memberCount;
    private int userRank;
    
    public int getLeagueId() { return leagueId; }
    public void setLeagueId(int leagueId) { this.leagueId = leagueId; }
    
    public String getLeagueName() { return leagueName; }
    public void setLeagueName(String leagueName) { this.leagueName = leagueName; }
    
    public int getAdminUserId() { return adminUserId; }
    public void setAdminUserId(int adminUserId) { this.adminUserId = adminUserId; }
    
    public boolean getIsPrivate() { return isPrivate; }
    public void setIsPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
    
    public int getUserRank() { return userRank; }
    public void setUserRank(int userRank) { this.userRank = userRank; }
}