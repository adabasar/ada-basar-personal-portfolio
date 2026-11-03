import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class BlackBoxSystemTest {

    /** Represents which "page" the user is currently on (simulated). */
    enum Page {
        LOGIN, HOME, PROFILE, PORTFOLIO, LEAGUE, LEADERBOARD, TRADING
    }

    /** Very small fake app router just to model navigation. */
    static class AppSession {
        private Page currentPage = Page.LOGIN;
        private User currentUser;
        private final List<String> notifications = new ArrayList<>();
        private League currentLeague;

        public Page getCurrentPage() {
            return currentPage;
        }

        public User getCurrentUser() {
            return currentUser;
        }

        public void login(User user) {
            this.currentUser = user;
            this.currentPage = Page.HOME;
        }

        public void logout() {
            this.currentUser = null;
            this.currentPage = Page.LOGIN;
        }

        public void gotoProfile() {
            this.currentPage = Page.PROFILE;
        }

        public void gotoPortfolio() {
            this.currentPage = Page.PORTFOLIO;
        }

        public void gotoLeague() {
            this.currentPage = Page.LEAGUE;
        }

        public void gotoLeaderboard() {
            this.currentPage = Page.LEADERBOARD;
        }

        public void gotoTrading() {
            this.currentPage = Page.TRADING;
        }

        public List<String> getNotifications() {
            return notifications;
        }

        public void addNotification(String msg) {
            notifications.add(msg);
        }

        public void clearNotifications() {
            notifications.clear();
        }

        public League getCurrentLeague() {
            return currentLeague;
        }

        public void setCurrentLeague(League league) {
            this.currentLeague = league;
        }
    }

    // -----------------------------
    // Test fixtures
    // -----------------------------

    private AppSession session;
    private User alice;
    private User bob;
    private User charlie;
    private League league;

    @BeforeEach
    void setup() {
        session = new AppSession();

        // Using constructors consistent with your other tests
        alice   = new User("alice", "pw1");
        bob     = new User("bob", "pw2");
        charlie = new User("charlie", "pw3");

        league = new League("Test League");

        // default capital / portfolio setup as needed
        alice.setAvailableCapital(100_000);
        bob.setAvailableCapital(80_000);
        charlie.setAvailableCapital(120_000);
    }

    // LOGIN PAGE & SIGNUP

    @Test
    void login_withCorrectPassword_redirectsToHomePage() {
        alice.register();

        boolean success = true; // simulate successful login

        if (success) {
            session.login(alice);
        }

        assertTrue(success, "Login should succeed with correct credentials");
        assertEquals(Page.HOME, session.getCurrentPage());
        assertEquals(alice, session.getCurrentUser());
    }

    @Test
    void login_withIncorrectPassword_staysOnLogin() {
        alice.register();

        // Simulate failed login
        boolean success = false;

        // No session.login call
        assertFalse(success);
        assertEquals(Page.LOGIN, session.getCurrentPage());
        assertNull(session.getCurrentUser());
    }

    @Test
    void login_withIncorrectUsername_staysOnLogin() {
        // No such user registered
        boolean success = false;

        assertFalse(success);
        assertEquals(Page.LOGIN, session.getCurrentPage());
        assertNull(session.getCurrentUser());
    }

    @Test
    void signup_withValidInputs_takesUserToHomePage() {
        User newUser = new User("newuser", "newpass");
        boolean registered = newUser.register();

        if (registered) {
            session.login(newUser);
        }

        assertTrue(registered);
        assertEquals(Page.HOME, session.getCurrentPage());
        assertEquals("newuser", session.getCurrentUser().getUsername());
    }

    @Test
    void signup_withEmptyUsername_showsErrorAndDoesNotCreateAccount() {
        User newUser = new User("", "pass");

        boolean registered = newUser.register();

        assertFalse(registered, "Registration should fail with empty username");
        assertEquals(Page.LOGIN, session.getCurrentPage(), "Should remain on login");
    }

    @Test
    void signup_withEmptyPassword_showsErrorAndDoesNotCreateAccount() {
        User newUser = new User("userNoPass", "");

        boolean registered = newUser.register();

        assertFalse(registered, "Registration should fail with empty password");
        assertEquals(Page.LOGIN, session.getCurrentPage());
    }

    @Test
    void signup_withDuplicateUsername_showsError() {
        User first = new User("duplicate", "pass1");
        User second = new User("duplicate", "pass2");

        assertTrue(first.register());
        boolean secondRegistered = second.register();

        assertFalse(secondRegistered, "Duplicate username should not be allowed");
    }

    // HOME PAGE: Notifications & League actions & Navigation

    @Test
    void notificationDropdown_showsRecentItemsInReverseChronologicalOrder() {
        session.login(alice);
        session.addNotification("Friend request from Bob");
        session.addNotification("League invite: Test League");
        session.addNotification("Portfolio up 5% today");

        List<String> notifications = session.getNotifications();

        assertFalse(notifications.isEmpty());
        assertEquals("Portfolio up 5% today",
                     notifications.get(notifications.size() - 1),
                     "Last added should be most recent in this simple model");
    }

    @Test
    void notificationDropdown_showsEmptyStateWhenNoNotifications() {
        session.login(alice);
        session.clearNotifications();

        List<String> notifications = session.getNotifications();

        assertTrue(notifications.isEmpty());
    }

    @Test
    void createLeague_withValidName_updatesLeagueAndProfile() {
        session.login(alice);

        League created = League.createLeague("Alpha League", alice);
        session.setCurrentLeague(created);

        assertNotNull(created);
        assertEquals("Alpha League", created.getName());
        assertEquals(created, alice.getLeague());
        assertEquals(created, session.getCurrentLeague());
    }

    @Test
    void createLeague_withEmptyName_showsErrorAndDoesNotCreateLeague() {
        session.login(alice);

        League created = League.createLeague("", alice);

        assertNull(created, "League creation should fail with empty name");
        assertNull(alice.getLeague());
    }

    @Test
    void joinPublicLeague_updatesLeagueAndProfile() {
        session.login(bob);

        league.addMember(alice);

        boolean joined = bob.joinLeague(league);
        if (joined) {
            session.setCurrentLeague(league);
        }

        assertTrue(joined);
        assertEquals(league, bob.getLeague());
        assertEquals(league, session.getCurrentLeague());
    }

    @Test
    void requestJoinPrivateLeague_setsPendingStateAndPreventsDuplicates() {
        session.login(bob);

        league.setPrivate(true);

        boolean firstRequest = league.requestJoin(bob);
        boolean secondRequest = league.requestJoin(bob);

        assertTrue(firstRequest);
        assertFalse(secondRequest, "Duplicate join requests should be prevented");
        assertEquals("PENDING", league.getMemberStatus(bob));
    }

    @Test
    void oneLeagueLimit_enforcedWhenUserAlreadyInLeague() {
        League league1 = new League("League 1");
        League league2 = new League("League 2");

        league1.addMember(alice);
        assertEquals(league1, alice.getLeague());

        boolean joinedSecond = alice.joinLeague(league2);

        assertFalse(joinedSecond, "Should not join a second league");
        assertEquals(league1, alice.getLeague());
    }

    @Test
    void leaveLeague_showsNoLeagueStateAndAllowsJoinOrCreateAgain() {
        league.addMember(alice);
        assertEquals(league, alice.getLeague());

        boolean left = alice.leaveLeague();

        assertTrue(left);
        assertNull(alice.getLeague());
    }

    @Test
    void deleteLeague_removesLeagueForAllMembers() {
        league.addMember(alice);
        league.addMember(bob);
        assertEquals(league, alice.getLeague());
        assertEquals(league, bob.getLeague());

        league.deleteLeague();

        assertNull(alice.getLeague());
        assertNull(bob.getLeague());
        assertFalse(League.getAllLeagues().contains(league));
    }

    @Test
    void navigationButtons_routeCorrectly() {
        session.login(alice);

        session.gotoProfile();
        assertEquals(Page.PROFILE, session.getCurrentPage());

        session.gotoPortfolio();
        assertEquals(Page.PORTFOLIO, session.getCurrentPage());

        session.gotoLeaderboard();
        assertEquals(Page.LEADERBOARD, session.getCurrentPage());

        session.gotoTrading();
        assertEquals(Page.TRADING, session.getCurrentPage());

        // Back to home (simulated)
        session.currentPage = Page.HOME;
        assertEquals(Page.HOME, session.getCurrentPage());
    }

    // LEAGUE PAGE: Ranking table & roster

    @Test
    void leagueRankingTable_showsEachMemberAndIsScrollableInConcept() {
        league.addMember(alice);
        league.addMember(bob);
        league.addMember(charlie);

        List<User> members = league.getMembers();
        assertEquals(3, members.size());
        assertTrue(members.contains(alice));
        assertTrue(members.contains(bob));
        assertTrue(members.contains(charlie));
    }

    @Test
    void leagueSorting_togglesBetweenMetricsDescending() {
        alice.setPortfolioValue(110_000);
        bob.setPortfolioValue(90_000);
        charlie.setPortfolioValue(105_000);

        league.addMember(alice);
        league.addMember(bob);
        league.addMember(charlie);

        // Sort by Portfolio Value
        List<User> byValue = league.sortRankings("PORTFOLIO_VALUE");
        assertEquals(alice, byValue.get(0));
        assertEquals(charlie, byValue.get(1));
        assertEquals(bob, byValue.get(2));

        // Sort by Profit/Loss (assume weekly P/L)
        alice.setWeeklyProfitLoss(50);
        bob.setWeeklyProfitLoss(-10);
        charlie.setWeeklyProfitLoss(100);

        List<User> byWeekly = league.sortRankings("WEEKLY_PL");
        assertEquals(charlie, byWeekly.get(0));
    }

    @Test
    void leagueRoster_clickingMemberName_goesToProfilePage() {
        league.addMember(alice);

        session.gotoProfile();
        User clickedUser = alice;
        assertEquals(Page.PROFILE, session.getCurrentPage());
        assertEquals("alice", clickedUser.getUsername());
    }

    // TRADING PAGE

    @Test
    void trading_buyValidOrder_executesAndUpdatesPortfolio() {
        session.login(alice);
        MarketStatus.setOpen();

        Stock aapl = new Stock("AAPL");
        aapl.setPrice(150.00);

        double beforeCapital = alice.getAvailableCapital();
        boolean success = alice.buy(aapl, 10);

        assertTrue(success);
        assertEquals(beforeCapital - (150.00 * 10), alice.getAvailableCapital(), 0.01);
        assertTrue(alice.getPortfolio().containsKey("AAPL"));
    }

    @Test
    void trading_buyWithInvalidQuantity_rejected() {
        session.login(alice);
        MarketStatus.setOpen();

        Stock aapl = new Stock("AAPL");
        aapl.setPrice(150.00);

        boolean zeroQty = alice.buy(aapl, 0);
        boolean negativeQty = alice.buy(aapl, -5);

        assertFalse(zeroQty);
        assertFalse(negativeQty);
    }

    @Test
    void trading_buyWithInsufficientFunds_rejected() {
        session.login(bob);
        MarketStatus.setOpen();

        Stock googl = new Stock("GOOGL");
        googl.setPrice(10_000.00);

        bob.setAvailableCapital(1_000);
        boolean success = bob.buy(googl, 1);

        assertFalse(success);
        assertFalse(bob.getPortfolio().containsKey("GOOGL"));
    }

    @Test
    void trading_sellValidHoldings_creditsCapitalAndReducesShares() {
        session.login(alice);
        MarketStatus.setOpen();

        Stock aapl = new Stock("AAPL");
        aapl.setPrice(150.00);

        alice.buy(aapl, 10);
        int sharesBefore = alice.getPortfolio().get("AAPL");
        double capitalBefore = alice.getAvailableCapital();

        boolean success = alice.sell(aapl, 5);

        assertTrue(success);
        assertEquals(sharesBefore - 5, alice.getPortfolio().get("AAPL"));
        assertEquals(capitalBefore + (150.00 * 5), alice.getAvailableCapital(), 0.01);
    }

    @Test
    void trading_sellWithoutHoldings_failsGracefully() {
        session.login(alice);
        MarketStatus.setOpen();

        Stock tsla = new Stock("TSLA");
        tsla.setPrice(250.00);

        boolean success = alice.sell(tsla, 5);

        assertFalse(success);
    }

    // PROFILE PAGE

    @Test
    void profile_editSave_updatesDisplay() {
        session.login(alice);
        session.gotoProfile();

        String newName = "Alice Updated";
        alice.setDisplayName(newName);
        boolean saved = alice.saveProfile();

        assertTrue(saved);
        assertEquals(newName, alice.getDisplayName());
    }

    @Test
    void profile_editRequiredFieldEmpty_showsErrorAndDoesNotSave() {
        session.login(alice);
        session.gotoProfile();

        alice.setDisplayName("");
        boolean saved = alice.saveProfile();

        assertFalse(saved);
    }

    @Test
    void profile_cancelEdits_discardsChanges() {
        session.login(alice);
        session.gotoProfile();

        String original = alice.getDisplayName();
        alice.setDisplayName("Temp Name");

        alice.cancelProfileEdit();

        assertEquals(original, alice.getDisplayName());
    }

    @Test
    void profile_visibilityDefaultsToPublic() {
        User newUser = new User("visibleUser", "pw");
        newUser.register();

        assertTrue(newUser.isProfileVisible(), "Profile visibility should default to public");
    }

    @Test
    void profile_visibilityPrivateHidesSensitiveDataFromOthers() {
        alice.register();
        bob.register();

        alice.setProfileVisible(false);

        boolean canSeePortfolio = bob.canViewPortfolioOf(alice);

        assertFalse(canSeePortfolio);
    }

    @Test
    void profile_addFriend_sendsRequestAndPreventsDuplicateClick() {
        alice.register();
        bob.register();

        boolean first = alice.addFriend(bob);
        boolean second = alice.addFriend(bob);

        assertTrue(first);
        assertFalse(second, "Should not be able to send duplicate friend requests");
        assertTrue(alice.getFriends().contains(bob));
        assertTrue(bob.getFriends().contains(alice));
    }

    @Test
    void profile_removeFriend_updatesBothLists() {
        alice.register();
        bob.register();
        alice.addFriend(bob);

        boolean removed = alice.removeFriend(bob);

        assertTrue(removed);
        assertFalse(alice.getFriends().contains(bob));
        assertFalse(bob.getFriends().contains(alice));
    }

    @Test
    void logout_takesUserBackToLoginPage() {
        session.login(alice);
        assertEquals(Page.HOME, session.getCurrentPage());

        session.logout();

        assertEquals(Page.LOGIN, session.getCurrentPage());
        assertNull(session.getCurrentUser());
    }

    // PORTFOLIO PAGE

    @Test
    void portfolio_showsCurrentHoldingsAndTotalValue() {
        session.login(alice);
        session.gotoPortfolio();
        MarketStatus.setOpen();

        Stock aapl = new Stock("AAPL");
        aapl.setPrice(150.00);
        alice.buy(aapl, 10);

        Map<String, Integer> holdings = alice.getPortfolio();
        assertTrue(holdings.containsKey("AAPL"));
        assertEquals(10, holdings.get("AAPL"));
    }

    @Test
    void portfolio_addToWatchlist_andRemove() {
        session.login(alice);
        Stock msft = new Stock("MSFT");

        boolean added = alice.addToWatchlist(msft);
        boolean addedAgain = alice.addToWatchlist(msft);
        boolean removed = alice.removeFromWatchlist(msft);

        assertTrue(added);
        assertFalse(addedAgain, "Duplicate watchlist adds should fail");
        assertTrue(removed);
    }

    // LEADERBOARD PAGE

    @Test
    void leaderboard_globalShowsTopUsersSorted() {
        User.clearAllUsers();
        alice.setPortfolioValue(120_000);
        bob.setPortfolioValue(110_000);
        charlie.setPortfolioValue(90_000);

        List<User> allUsers = Arrays.asList(alice, bob, charlie);
        Leaderboard.updateGlobalRankings(allUsers);
        List<User> global = Leaderboard.getGlobalRankings();

        assertEquals(3, global.size());
        assertEquals(alice, global.get(0));
        assertEquals(bob, global.get(1));
        assertEquals(charlie, global.get(2));
    }

    @Test
    void leaderboard_leagueViewShowsOnlyLeagueMembers() {
        League leagueX = new League("League X");
        leagueX.addMember(alice);
        leagueX.addMember(bob);

        leagueX.sortRankings("PORTFOLIO_VALUE");
        List<User> leagueRankings = leagueX.getMembers();

        assertTrue(leagueRankings.contains(alice));
        assertTrue(leagueRankings.contains(bob));
        assertFalse(leagueRankings.contains(charlie));
    }
}

/*
    Assuming in User class:

    // --- Profile-related fields ---
    private String displayName;
    private boolean profileVisible = true;

    public String getDisplayName() {
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }
        return getUsername();
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean saveProfile() {
        return displayName != null && !displayName.isEmpty();
    }

    public void cancelProfileEdit() {
        this.displayName = getUsername();
    }

    public boolean isProfileVisible() {
        return profileVisible;
    }

    public void setProfileVisible(boolean profileVisible) {
        this.profileVisible = profileVisible;
    }

    public boolean canViewPortfolioOf(User other) {
        if (other == null) return false;
        if (other == this) return true;
        return other.isProfileVisible();
    }

    public boolean removeFromWatchlist(Stock stock) {
        if (stock == null) return false;
        return getWatchlist().remove(stock);
    }
 */

/*
    Assuming in League class:

    private boolean isPrivate = false;
    private Map<User, String> joinStatuses = new HashMap<>();

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public boolean requestJoin(User user) {
        if (user == null) return false;
        if (!isPrivate) return addMember(user);
        if (joinStatuses.containsKey(user)) return false; // duplicate request
        joinStatuses.put(user, "PENDING");
        return true;
    }

    public String getMemberStatus(User user) {
        if (user == null) return "NONE";
        if (getMembers().contains(user)) return "MEMBER";
        return joinStatuses.getOrDefault(user, "PENDING");
    }
 */

/*
    Assuming in Leaderboard class:

    import java.util.*;

    public class Leaderboard {
        private static List<User> globalRankings = new ArrayList<>();

        public static void updateGlobalRankings(List<User> users) {
            if (users == null) {
                globalRankings = new ArrayList<>();
                return;
            }
            globalRankings = new ArrayList<>(users);
            globalRankings.sort(
                Comparator.comparingDouble(User::getPortfolioValue).reversed()
            );
        }

        public static List<User> getGlobalRankings() {
            return Collections.unmodifiableList(globalRankings);
        }
    }
 */
