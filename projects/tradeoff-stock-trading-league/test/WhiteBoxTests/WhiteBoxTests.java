package app.whitebox;

import app.auth.AuthService;
import app.auth.AuthException;
import app.auth.SessionManager;
import app.user.User;
import app.user.UserRepo;
import app.security.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


class LoginWhiteBoxTest {
 @Mock UserRepo users;
 @Mock PasswordHasher hasher;
 @Mock SessionManager sessions;
 @InjectMocks AuthService auth;


 @BeforeEach void init() { MockitoAnnotations.openMocks(this); }


 @Test void valid_credentials_issue_session() {
   when(users.findByUsername("alice")).thenReturn(User.active("alice","HASH",1));
   when(hasher.matches("pw","HASH")).thenReturn(true);
   when(sessions.issue(1, true)).thenReturn("sess-123");


   var out = auth.login("alice","pw", /*rememberMe*/true, "/home");


   assertTrue(out.ok());
   assertEquals("sess-123", out.sessionId());
   verify(sessions).issue(1,true);
 }


 @Test void user_not_found() {
   when(users.findByUsername("ghost")).thenReturn(null);
   var ex = assertThrows(AuthException.class,
       () -> auth.login("ghost","pw",false,null));
   assertEquals("user_not_found", ex.getCode());
   verifyNoInteractions(sessions);
 }


 @Test void password_mismatch() {
   when(users.findByUsername("alice")).thenReturn(User.active("alice","HASH",1));
   when(hasher.matches("bad","HASH")).thenReturn(false);


   var ex = assertThrows(AuthException.class,
       () -> auth.login("alice","bad",false,null));
   assertEquals("bad_credentials", ex.getCode());
   verifyNoInteractions(sessions);
 }
}


class HomeWhiteBoxTest {
 @Mock WatchlistRepo watchlists;
 @Mock ActivityFeedRepo feed;
 @Mock Cache cache;
 @Mock AuthGuard auth;
 @InjectMocks HomeService home;


 @BeforeEach void init() { MockitoAnnotations.openMocks(this); }


 @Test void watchlist_empty_vs_nonempty() {
   when(auth.requireUserId()).thenReturn(1);
   when(cache.get("dash:1")).thenReturn(null);
   when(watchlists.findTickers(1)).thenReturn(List.of()); // empty
   when(feed.recentForUser(1,10,0)).thenReturn(List.of());
   DashboardDTO empty = home.dashboard(10,0);
   assertTrue(empty.watchlist().isEmpty());


   reset(watchlists, feed);
   when(watchlists.findTickers(1)).thenReturn(List.of("AAPL","MSFT"));
   when(feed.recentForUser(1,10,0)).thenReturn(List.of("BUY AAPL"));
   DashboardDTO nonEmpty = home.dashboard(10,0);
   assertEquals(2, nonEmpty.watchlist().size());
   assertEquals(1, nonEmpty.activities().size());
 }


 @Test void cached_vs_cache_miss_first_load() {
   when(auth.requireUserId()).thenReturn(1);
   DashboardDTO cached = new DashboardDTO(List.of("AAPL"), List.of("evt"), List.of());
   when(cache.get("dash:1")).thenReturn(cached);


   DashboardDTO out = home.dashboard(10,0);
   assertSame(cached, out);
   verifyNoInteractions(watchlists, feed);
 }


 @Test void auth_required_401_vs_renders_dashboard() {
   when(auth.isAuthenticated()).thenReturn(false);
   assertThrows(SecurityException.class, () -> home.dashboard(10,0));


   reset(auth);
   when(auth.isAuthenticated()).thenReturn(true);
   when(auth.requireUserId()).thenReturn(1);
   when(cache.get("dash:1")).thenReturn(new DashboardDTO(List.of(), List.of(), List.of()));
   assertDoesNotThrow(() -> home.dashboard(10,0));
 }
}


class LeagueWhiteBoxTest {
 @Mock LeagueRepo leagues;
 @Mock LeagueMemberRepo members;
 @Mock LeagueInviteRepo invites;
 @InjectMocks LeagueService svc;


 @BeforeEach void init(){ MockitoAnnotations.openMocks(this); }


 @Test void public_join_vs_private_pending() {
   when(leagues.get(10)).thenReturn(League.publicLeague(10, 100 /*max*/));
   when(members.isMember(10, 1)).thenReturn(false);


   var ok = svc.join(1,10);
   assertEquals("joined", ok.status());
   verify(members).add(10,1, Role.MEMBER);


   reset(leagues, members);
   when(leagues.get(20)).thenReturn(League.privateLeague(20, 100));
   var pending = svc.join(1,20);
   assertEquals("pending", pending.status());
   verify(members, never()).add(anyInt(), anyInt(), any());
 }


 @Test void already_member() {
   when(leagues.get(10)).thenReturn(League.publicLeague(10, 100));
   when(members.isMember(10, 1)).thenReturn(true);
   assertThrows(LeagueException.class, () -> svc.join(1,10));
 }


 @Test void admin_vs_regular_permissions() {
   when(members.role(10,1)).thenReturn(Role.ADMIN);
   assertDoesNotThrow(() -> svc.kickMember(1,10, 2));


   when(members.role(10,1)).thenReturn(Role.MEMBER);
   assertThrows(LeagueException.class, () -> svc.kickMember(1,10, 2));
 }


 @Test void max_members_limit_reached() {
   when(leagues.get(10)).thenReturn(League.publicLeague(10, 2));
   when(members.count(10)).thenReturn(2);
   assertThrows(LeagueException.class, () -> svc.join(3,10));
 }


 @Test void leave_owner_cannot_leave_if_last_admin() {
   when(members.isOnlyAdmin(10,1)).thenReturn(true);
   assertThrows(LeagueException.class, () -> svc.leave(1,10));


   when(members.isOnlyAdmin(10,1)).thenReturn(false);
   assertDoesNotThrow(() -> svc.leave(1,10));
 }


 @Test void invite_flow_valid_vs_expired() {
   when(invites.consume("ok-token")).thenReturn(new Invite(20, /*valid*/true));
   when(invites.consume("bad-token")).thenReturn(new Invite(0, /*valid*/false));


   assertEquals("joined", svc.acceptInvite(1,"ok-token").status());
   assertThrows(LeagueException.class, () -> svc.acceptInvite(1,"bad-token"));
 }


 @Test void sorting_members_by_equity_with_ties() {
   var rows = List.of(
     new MemberEquity(2, 120_000.00),
     new MemberEquity(1, 120_000.00),
     new MemberEquity(3, 90_000.00)
   );
   when(members.membersWithEquity(10)).thenReturn(rows);
   var sorted = svc.sortedMembers(10);
   assertEquals(1, sorted.get(0).userId()); // tie broken by userId
   assertEquals(2, sorted.get(1).userId());
   assertEquals(3, sorted.get(2).userId());
 }
}


class TradingWhiteBoxTest {
 @Mock PriceProvider prices;
 @Mock LedgerRepo ledger;
 @Mock PortfolioRepo portfolios;
 @Mock TxManager tx; // your transaction boundary (begin/commit/rollback)
 @InjectMocks TradeService trades;


 @BeforeEach void init(){ MockitoAnnotations.openMocks(this); }


 @Test void buy_qty_positive_sufficient_cash_avg_price_recompute() {
   var u = Fixtures.userWithCash(1, bd("1000.00"));
   when(prices.price("AAPL")).thenReturn(bd("150.00"));


   var res = trades.buy(u,"AAPL",4,"idemp-1");


   assertEquals(bd("600.00"), res.cost());
   assertEquals(4, u.holding("AAPL").qty());
   assertEquals(bd("150.00"), u.holding("AAPL").avg());
   assertEquals(bd("400.00"), u.cash());
   verify(ledger).append(any(LedgerEntry.class));
   verify(portfolios).save(u);
 }


 @Test void sell_qty_positive_sufficient_shares_realized_pnl_update() {
   var u = Fixtures.userHolding("AAPL", 10, bd("100.00")); // avg=100
   when(prices.price("AAPL")).thenReturn(bd("150.00"));


   var res = trades.sell(u,"AAPL",4,"idemp-2");


   assertEquals(bd("600.00"), res.proceeds());
   assertEquals(6, u.holding("AAPL").qty());
   assertEquals(bd("200.00"), u.realizedPnl()); // (150-100)*4
 }


 @Test void price_provider_success_vs_timeout_fallback_retry() throws Exception {
   var u = Fixtures.userWithCash(1, bd("1000.00"));
   when(prices.price("AAPL")).thenThrow(new TimeoutException());
   when(prices.fallbackPrice("AAPL")).thenReturn(bd("151.00"));


   trades.buy(u,"AAPL",1,"idemp-3");


   verify(prices).fallbackPrice("AAPL");
   verify(ledger).append(any());
 }


 @Test void rounding_precision_boundaries() {
   var u = Fixtures.userWithCash(1, bd("10.00"));
   when(prices.price("PENNY")).thenReturn(new BigDecimal("0.333333"));
   trades.buy(u,"PENNY", 3, "idemp-4");
   // cost 0.999999 -> store/display as 1.00 with HALF_UP
   assertEquals(new BigDecimal("9.00"), u.cash());
   assertEquals(new BigDecimal("0.33"), u.holding("PENNY").avg()); // scale(2)
 }


 @Test void ledger_write_ok_vs_db_error_rollback() {
   var u = Fixtures.userWithCash(1, bd("1000.00"));
   when(prices.price("AAPL")).thenReturn(bd("100.00"));


   doThrow(new RuntimeException("DB down")).when(ledger).append(any());


   assertThrows(RuntimeException.class, () -> trades.buy(u,"AAPL",1,"idemp-5"));
   verify(tx).rollback(); // confirm we rolled back on failure
   // state should be unchanged after rollback:
   assertEquals(bd("1000.00"), u.cash());
   assertNull(u.holdingOrNull("AAPL"));
 }


 private static BigDecimal bd(String s){ return new BigDecimal(s); }
}


class PortfolioWhiteBoxTest {
 @Mock PriceProvider prices;
 @InjectMocks PortfolioService svc;


 @BeforeEach void init(){ MockitoAnnotations.openMocks(this); }


 @Test void equity_calc_multiple_holdings() {
   var u = Fixtures.userWithCash(1, bd("100.00"));
   Fixtures.addHolding(u,"AAPL", 2, bd("150.00"));
   Fixtures.addHolding(u,"MSFT", 1, bd("300.00"));
   when(prices.price("AAPL")).thenReturn(bd("160.00"));
   when(prices.price("MSFT")).thenReturn(bd("290.00"));


   PortfolioSummary s = svc.summary(u, /*prevEquity*/ bd("1000.00"));


   // equity = cash 100 + 2*160 + 1*290 = 710
   assertEquals(bd("710.00"), s.equity());
   assertEquals(2, s.positions().size());
 }


 @Test void percent_change_old_zero_defined_output() {
   assertEquals(0.0, svc.percentChange(BigDecimal.ZERO, bd("123.45")));
 }


 @Test void empty_portfolio_zero_summary() {
   var u = Fixtures.userWithCash(1, bd("500.00"));
   when(prices.price(anyString())).thenReturn(bd("0"));
   var s = svc.summary(u, bd("500.00"));
   assertEquals(bd("500.00"), s.equity());
   assertTrue(s.positions().isEmpty());
 }


 @Test void sorting_by_value_then_name() {
   var u = Fixtures.userWithCash(1, bd("0.00"));
   Fixtures.addHolding(u,"AAPL", 1, bd("150.00"));
   Fixtures.addHolding(u,"AMD",  1, bd("150.00"));
   when(prices.price("AAPL")).thenReturn(bd("200.00"));
   when(prices.price("AMD")).thenReturn(bd("200.00"));


   var s = svc.summary(u, bd("0.00"));
   // equal values → sort by symbol/name
   assertEquals("AAPL", s.positions().get(0).symbol());
   assertEquals("AMD",  s.positions().get(1).symbol());
 }


 private static BigDecimal bd(String s){ return new BigDecimal(s); }
}


class LeaderboardWhiteBoxTest {
 @Mock EquityRepo equity;
 @Mock VisibilityRepo visibility;
 @Mock Cache cache;
 @InjectMocks LeaderboardService svc;


 @BeforeEach void init(){ MockitoAnnotations.openMocks(this); }


 @Test void sort_by_equity_tiebreak_by_userid() {
   when(cache.get("lb:global:1:100")).thenReturn(null);
   when(equity.topN(100)).thenReturn(List.of(
     row(2, bd("120000.00")),
     row(1, bd("120000.00")),
     row(3, bd("90000.00"))
   ));
   when(visibility.allowed(anyInt())).thenReturn(true);


   var list = svc.global(/*page*/1, /*size*/100);


   assertEquals(1, list.get(0).userId());
   assertEquals(2, list.get(1).userId());
   assertEquals(3, list.get(2).userId());
 }


 @Test void exclude_users_without_visibility() {
   when(cache.get("lb:global:1:50")).thenReturn(null);
   when(equity.topN(50)).thenReturn(List.of(row(9, bd("110000.00")), row(10, bd("100000.00"))));
   when(visibility.allowed(9)).thenReturn(true);
   when(visibility.allowed(10)).thenReturn(false);


   var list = svc.global(1,50);


   assertEquals(1, list.size());
   assertEquals(9, list.get(0).userId());
 }


 @Test void cache_warm_vs_recompute() {
   var cached = List.of(new LeaderboardService.Row(1, bd("100000.00")));
   when(cache.get("lb:global:1:10")).thenReturn(cached);


   var fromCache = svc.global(1,10);
   assertSame(cached, fromCache);
   verifyNoInteractions(equity);


   reset(cache);
   when(cache.get("lb:global:1:10")).thenReturn(null);
   when(equity.topN(10)).thenReturn(cached);
   when(visibility.allowed(anyInt())).thenReturn(true);


   var recomputed = svc.global(1,10);
   assertEquals(1, recomputed.size());
   verify(cache).put(eq("lb:global:1:10"), any());
 }


 @Test void friends_only_vs_global_view() {
   when(equity.topForFriends(42, 10)).thenReturn(List.of(row(99, bd("130000.00"))));
   when(visibility.allowed(99)).thenReturn(true);


   var friends = svc.friendsOf(42, 1, 10);
   assertEquals(99, friends.get(0).userId());
   verify(equity).topForFriends(42,10);
   verify(equity, never()).topN(anyInt());
 }


 private static BigDecimal bd(String s){ return new BigDecimal(s); }
 private static LeaderboardService.Row row(int uid, BigDecimal eq){ return new LeaderboardService.Row(uid, eq); }
}
