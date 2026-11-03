package app.model;

/**
 * LeaderboardUser model - Represents a user in the global leaderboard
 */
public class LeaderboardInfo {
    private int userId;
    private String username;
    private String displayName;
    private double portfolioValue;
    private double profitLoss;
    private double weeklyProfitLoss;
    private double percentChange;
    private int rank;
    
    // Getters and Setters
    
    public int getUserId() {
        return userId;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public double getPortfolioValue() {
        return portfolioValue;
    }
    
    public void setPortfolioValue(double portfolioValue) {
        this.portfolioValue = portfolioValue;
    }
    
    public double getProfitLoss() {
        return profitLoss;
    }
    
    public void setProfitLoss(double profitLoss) {
        this.profitLoss = profitLoss;
    }
    
    public double getWeeklyProfitLoss() {
        return weeklyProfitLoss;
    }
    
    public void setWeeklyProfitLoss(double weeklyProfitLoss) {
        this.weeklyProfitLoss = weeklyProfitLoss;
    }
    
    public double getPercentChange() {
        return percentChange;
    }
    
    public void setPercentChange(double percentChange) {
        this.percentChange = percentChange;
    }
    
    public int getRank() {
        return rank;
    }
    
    public void setRank(int rank) {
        this.rank = rank;
    }
    
    @Override
    public String toString() {
        return "LeaderboardUser{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", displayName='" + displayName + '\'' +
                ", portfolioValue=" + portfolioValue +
                ", profitLoss=" + profitLoss +
                ", weeklyProfitLoss=" + weeklyProfitLoss +
                ", percentChange=" + percentChange +
                ", rank=" + rank +
                '}';
    }
}