# TradeOff — Stock Trading League

A web app where friends compete in leagues with simulated stock portfolios. Users sign in, buy and sell stocks at real market prices, keep a watchlist, add friends, and climb league and global leaderboards.

**Team project, USC CSCI 201 (Principles of Software Development), Fall 2025.**
Original repository: [miguelgambalan/201-Final](https://github.com/miguelgambalan/201-Final)

## Features
- Account login and user profiles
- Buy and sell stocks with live quotes from the Alpha Vantage API
- Portfolio tracking with percent change over time
- Leagues: create, join and leave, with rankings inside each league
- Friends system and a global leaderboard
- Market open/closed status
- Price history for 10 large-cap stocks (AAPL, MSFT, NVDA, GOOGL, AMZN, META, TSLA, NFLX, JPM, BAC) in `data/history/`

## Tech stack
Java · Java Servlets · MySQL (JDBC) · Maven · HTML / CSS / JavaScript · JUnit

## Structure
| Path | Contents |
|---|---|
| `src/main/java/` | Domain classes (`User`, `Stock`, `League`, `Leaderboard`, `MarketStatus`) and servlets (`app/servlet/`) |
| `src/main/webapp/` | Front-end pages: login, home, trading, portfolio, league, leaderboard, profile |
| `test/`, `src/test/` | Unit, black-box, white-box and database integration tests |
| `deploy.sh`, `DEPLOYMENT.md` | Build and deployment steps |

## Run it
```bash
mvn clean compile
mvn test
```
Set a `DB_PASSWORD` environment variable for your local MySQL user first. See `DEPLOYMENT.md` for database setup. The team's original README is kept in `README.original.md`.
