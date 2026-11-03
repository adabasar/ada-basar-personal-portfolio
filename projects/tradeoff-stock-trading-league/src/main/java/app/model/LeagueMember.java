package app.model;

public class LeagueMember {
    private int userId;
    private String username;
    private String displayName;
    private double portfolioValue;
    private double profitLoss;
    private double weeklyProfitLoss;
    private int rankPosition;
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public double getPortfolioValue() { return portfolioValue; }
    public void setPortfolioValue(double portfolioValue) { this.portfolioValue = portfolioValue; }
    
    public double getProfitLoss() { return profitLoss; }
    public void setProfitLoss(double profitLoss) { this.profitLoss = profitLoss; }
    
    public double getWeeklyProfitLoss() { return weeklyProfitLoss; }
    public void setWeeklyProfitLoss(double weeklyProfitLoss) { this.weeklyProfitLoss = weeklyProfitLoss; }
    
    public int getRankPosition() { return rankPosition; }
    public void setRankPosition(int rankPosition) { this.rankPosition = rankPosition; }
}