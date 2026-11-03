import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class League {
    private int leagueId;
    private String leagueName;
    private int adminUserId;
    private boolean isPrivate;
    private Timestamp createdAt;
    private List<User> members;
    private List<User> rankings;
    private Connection conn;
    public League(Connection conn, String leagueName, int adminUserId, boolean isPrivate) {
        this.conn = conn;
        this.leagueName = leagueName;
        this.adminUserId = adminUserId;
        this.isPrivate = isPrivate;
        this.members = new ArrayList<>();
        this.rankings = new ArrayList<>();
    }

    public League(Connection conn, int leagueId) throws SQLException {
        this.conn = conn;
        this.leagueId = leagueId;
        this.members = new ArrayList<>();
        this.rankings = new ArrayList<>();
        loadLeagueData();
    }

    private void loadLeagueData() throws SQLException {
        String sql = "SELECT league_name, admin_user_id, is_private, created_at " +
                    "FROM Leagues WHERE league_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                this.leagueName = rs.getString("league_name");
                this.adminUserId = rs.getInt("admin_user_id");
                this.isPrivate = rs.getBoolean("is_private");
                this.createdAt = rs.getTimestamp("created_at");
            } else {
                throw new SQLException("League not found with ID: " + leagueId);
            }
        }
    }

    public void createLeague() throws SQLException {
        if (isUserInAnyLeague(adminUserId)) {
            throw new SQLException("User is already in a league");
        }

        String sql = "INSERT INTO Leagues (league_name, admin_user_id, is_private) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, leagueName);
            stmt.setInt(2, adminUserId);
            stmt.setBoolean(3, isPrivate);
            
            stmt.executeUpdate();
            
            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                this.leagueId = keys.getInt(1);
            }
        }
        
        addMember(adminUserId);
    }

    public void addMember(int userId) throws SQLException {
        if (isUserInAnyLeague(userId)) {
            throw new SQLException("User is already in a league");
        }

        String sql = "INSERT INTO LeagueMembers (user_id, league_id) VALUES (?, ?)";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, leagueId);
            stmt.executeUpdate();
        }
        
        loadMembers();
    }

    public void removeMember(int userId) throws SQLException {
        if (!isMemberOfLeague(userId)) {
            throw new SQLException("User is not part of this league");
        }

        String sql = "DELETE FROM LeagueMembers WHERE user_id = ? AND league_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, leagueId);
            stmt.executeUpdate();
        }
        
        loadMembers();
    }

    public void quitLeague(int userId) throws SQLException {
        removeMember(userId);
    }

    public void deleteLeague() throws SQLException {
        String deleteMembersSql = "DELETE FROM LeagueMembers WHERE league_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteMembersSql)) {
            stmt.setInt(1, leagueId);
            stmt.executeUpdate();
        }

        String deleteLeagueSql = "DELETE FROM Leagues WHERE league_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteLeagueSql)) {
            stmt.setInt(1, leagueId);
            stmt.executeUpdate();
        }
    }

    public void sortRankings() throws SQLException {
        String sql = "SELECT u.user_id, u.username, u.display_name, " +
                    "COALESCE(p.total_portfolio_value, 0) as portfolio_value, " +
                    "COALESCE(p.total_profit_loss, 0) as profit_loss, " +
                    "COALESCE(p.weekly_profit_loss, 0) as weekly_profit_loss " +
                    "FROM LeagueMembers lm " +
                    "JOIN Users u ON lm.user_id = u.user_id " +
                    "LEFT JOIN Portfolios p ON u.user_id = p.user_id " +
                    "WHERE lm.league_id = ? " +
                    "ORDER BY portfolio_value DESC, profit_loss DESC, u.user_id ASC";

        rankings.clear();
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            ResultSet rs = stmt.executeQuery();
            
            int rank = 1;
            while (rs.next()) {
                int userId = rs.getInt("user_id");
                
                updateMemberRank(userId, rank);
                rank++;
            }
        }
        
        loadMembers();
    }

    private void updateMemberRank(int userId, int rank) throws SQLException {
        String sql = "UPDATE LeagueMembers SET rank_position = ? WHERE user_id = ? AND league_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, rank);
            stmt.setInt(2, userId);
            stmt.setInt(3, leagueId);
            stmt.executeUpdate();
        }
    }

    private void loadMembers() throws SQLException {
        members.clear();
        
        String sql = "SELECT u.user_id, u.username, u.display_name " +
                    "FROM LeagueMembers lm " +
                    "JOIN Users u ON lm.user_id = u.user_id " +
                    "WHERE lm.league_id = ? " +
                    "ORDER BY lm.rank_position ASC";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, leagueId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                // TODO: Load user data properly
                // int userId = rs.getInt("user_id");
                // User user = new User(conn, userId);
                // members.add(user);
            }
        }
    }

    private boolean isUserInAnyLeague(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM LeagueMembers WHERE user_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        }
        return false;
    }

    private boolean isMemberOfLeague(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM LeagueMembers WHERE user_id = ? AND league_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, leagueId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        }
        return false;
    }
    
    public int getLeagueId() { return leagueId; }
    public String getLeagueName() { return leagueName; }
    public int getAdminUserId() { return adminUserId; }
    public boolean isPrivate() { return isPrivate; }
    public List<User> getMembers() { return members; }
    public List<User> getRankings() { return rankings; }
    
    public void setLeagueName(String leagueName) { this.leagueName = leagueName; }
}
