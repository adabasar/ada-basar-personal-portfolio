# 201-Final Trading System Template

This repository contains template classes for a trading system application. All classes are stubbed with method signatures and TODO comments to guide developer implementation.

## Project Structure

```
201-Final/
├── src/
│   ├── main/java/
│   │   ├── User.java              # User management and portfolio operations
│   │   ├── League.java            # League management and rankings
│   │   ├── Stock.java             # Stock data fetching
│   │   ├── MarketStatus.java      # Market open/close status
│   │   ├── Leaderboard.java       # Global rankings
│   │   └── Main.java              # Application entry point
│   └── test/java/
│       ├── UnitTests/             # Unit tests for individual classes
│       ├── IntegrationTests/      # Database integration tests
│       └── StressTests/           # Concurrency and performance tests
├── pom.xml                        # Maven configuration
└── README.md                      # This file
```

## Building the Project

```bash
# Compile the project
mvn clean compile

# Compile tests
mvn test-compile

# Run tests
mvn test
```

## Template Classes

### User.java
Handles user account management, portfolio operations, friends, and league participation.

**Key Methods to Implement:**
- `getPercentChange()` - Calculate portfolio performance
- `addFriend(User)` - Bidirectional friend relationship
- `buy(Stock, int)` - Purchase stocks with API integration
- `sell(Stock, int)` - Sell stocks with API integration
- `addToWatchlist(Stock)` - Track stocks of interest
- `joinLeague(League)` - Join a competitive league
- `leaveLeague()` - Leave current league
- `getLeagueRanking()` - Get position in league
- `getGlobalRanking()` - Get global position
- `register()` - Register user in system
- `saveToDatabase(Connection)` - Persist user data

### League.java
Manages competitive leagues where users can compete based on portfolio performance.

**Key Methods to Implement:**
- `sortRankings(String)` - Sort members by criteria (PROFIT_LOSS, PORTFOLIO_VALUE)
- `addMember(User)` - Add user to league
- `removeMember(User)` - Remove user from league
- `createLeague(String, User)` - Static factory method
- `deleteLeague()` - Remove league and clean up members

### Stock.java
Handles stock data fetching and caching.

**Key Methods to Implement:**
- `fetchData(int)` - Fetch stock data from API with caching
- Implement caching logic (10-minute cache window)
- Validate parameters before API calls
- Parse API responses uniformly across different stocks

### MarketStatus.java
Utility class for checking if the market is open.

**Key Methods to Implement:**
- `isOpen()` - Check market hours (9:30 AM - 4:00 PM EST)
- Consider holidays and special trading days
- Handle timezone conversions

### Leaderboard.java
Manages global user rankings.

**Key Methods to Implement:**
- `updateGlobalRankings(List<User>)` - Efficient ranking algorithm
- `getGlobalRankings()` - Get sorted rankings with pagination
- `getUserRanking(User)` - Efficient lookup (consider HashMap)
- Must handle 1000+ users in < 5 seconds


## Testing

### Unit Tests (37 tests)
Located in `src/test/java/UnitTests/`
- `UserTest.java` - Tests user operations
- `LeagueTest.java` - Tests league management
- `StockTest.java` - Tests stock data fetching

### Integration Tests (6 tests)
Located in `src/test/java/IntegrationTests/`
- `SchemaIntegrationTest.java` - Tests database operations
- Requires MySQL database running on localhost:3306
- Database: `tradeoff_test`
- User: `testuser` / Password: `testpass`

### Stress Tests (11 tests)
Located in `src/test/java/StressTests/`
- Tests concurrent operations (1000+ users)
- Tests performance requirements
- Validates thread safety

## Current Test Status

- **Unit Tests**: 37/37 passing ✅
- **Stress Tests**: 11/11 passing ✅
- **Integration Tests**: 0/6 (requires database setup) ⚠️

## Dependencies

- **Java**: 11+
- **JUnit**: 5.9.2 (testing)
- **MySQL Connector**: 8.0.33
- **Mockito**: 5.3.1 (mocking)
- **AssertJ**: 3.24.2 (assertions)

## Database Schema

Integration tests expect the following schema:

### users table
- `user_id` INT PRIMARY KEY AUTO_INCREMENT
- `username` VARCHAR(255) UNIQUE NOT NULL
- `password_hash` VARCHAR(255) NOT NULL
- `available_capital` DECIMAL(15,2)
- Other user fields as needed

### ledger table
- `transaction_id` INT PRIMARY KEY AUTO_INCREMENT
- `user_id` INT FOREIGN KEY → users(user_id)
- `ticker` VARCHAR(10)
- `quantity` INT
- `transaction_type` ENUM('BUY', 'SELL')
- `timestamp` TIMESTAMP DEFAULT CURRENT_TIMESTAMP

## API Integration

**NOT IMPLEMENTED** - This is a template only.

When implementing:
1. Add API endpoint configuration
2. Implement authentication
3. Add rate limiting
4. Handle API errors gracefully
5. Implement data parsing for:
   - Stock prices
   - Historical data
   - Market status


