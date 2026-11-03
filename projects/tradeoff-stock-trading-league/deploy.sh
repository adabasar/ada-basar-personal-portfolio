#!/bin/bash

# Exit immediately if a command exits with a non-zero status.
set -e

# --- Configuration ---
DB_NAME="stock_trader_db"
DB_USER="stock_user"
DB_PASSWORD="${DB_PASSWORD:?Set DB_PASSWORD before running deploy.sh}"

# --- Functions ---

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# --- Main Script ---

echo "Starting deployment process for Stock Trader application..."

# 1. Check for dependencies
echo "Checking for dependencies (Homebrew, Java, Maven, MySQL)..."
if ! command_exists brew; then
    echo "Homebrew not found. Please install it from https://brew.sh/"
    exit 1
fi

if ! command_exists java; then
    echo "Java not found. Please install it. e.g., 'brew install openjdk'"
    exit 1
fi

if ! command_exists mvn; then
    echo "Maven not found. Please install it. e.g., 'brew install maven'"
    exit 1
fi

if ! command_exists mysql; then
    echo "MySQL not found. Please install it. e.g., 'brew install mysql'"
    exit 1
fi

echo "All dependencies are installed."

# 2. Setup MySQL Database
echo "Setting up MySQL database..."
# Check if MySQL service is running
if ! brew services list | grep -q "mysql.*started"; then
    echo "MySQL service is not running. Starting it now..."
    brew services start mysql
fi

echo "Creating database and user..."
mysql -u root <<-EOSQL
    CREATE DATABASE IF NOT EXISTS ${DB_NAME};
    CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
    GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
    FLUSH PRIVILEGES;
EOSQL

echo "Database and user created."

# 3. Create database schema
echo "Creating database schema..."
mysql -u ${DB_USER} -p${DB_PASSWORD} ${DB_NAME} <<-EOSQL
    CREATE TABLE IF NOT EXISTS Users (
        user_id INT AUTO_INCREMENT PRIMARY KEY,
        username VARCHAR(255) UNIQUE NOT NULL,
        password_hash VARCHAR(255) NOT NULL,
        display_name VARCHAR(255) NOT NULL,
        contact_info VARCHAR(255),
        profile_visibility BOOLEAN NOT NULL DEFAULT TRUE,
        available_capital DECIMAL(15,2) NOT NULL DEFAULT 100000.00,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

    CREATE TABLE IF NOT EXISTS Stocks (
        stock_id VARCHAR(10) PRIMARY KEY,
        company_name VARCHAR(255) NOT NULL,
        description TEXT,
        industry VARCHAR(255),
        market_cap BIGINT,
        pe_ratio DECIMAL(10,2),
        dividend_yield DECIMAL(6,2),
        beta DECIMAL(8,2)
    );

    CREATE TABLE IF NOT EXISTS UserStockInterests (
        interest_id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INTEGER NOT NULL,
        stock_id VARCHAR(10) NOT NULL,
        is_holding BOOLEAN NOT NULL,
        quantity INTEGER,
        average_purchase_price DECIMAL(10,2),
        FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
        FOREIGN KEY (stock_id) REFERENCES Stocks(stock_id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS Leagues (
        league_id INT AUTO_INCREMENT PRIMARY KEY,
        league_name VARCHAR(255) NOT NULL,
        admin_user_id INTEGER NOT NULL,
        is_private BOOLEAN NOT NULL DEFAULT FALSE,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (admin_user_id) REFERENCES Users(user_id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS LeagueMembers (
        membership_id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT NOT NULL,
        league_id INT NOT NULL,
        joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        rank_position INT,
        FOREIGN KEY (league_id) REFERENCES Leagues(league_id) ON DELETE CASCADE,
        FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS Notifications (
        notification_id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT NOT NULL,
        notification_text VARCHAR(255),
        notification_type ENUM('placeholder'),
        is_read BOOLEAN,
        link_url VARCHAR(255),
        created_at TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS Portfolios(
        portfolio_id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT UNIQUE NOT NULL,
        total_portfolio_value DECIMAL(15,2) NOT NULL,
        total_profit_loss DECIMAL (15,2) NOT NULL DEFAULT 0.0,
        weekly_profit_loss DECIMAL (15,2) NOT NULL DEFAULT 0.0,
        updated_at TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS Friendships (
        friendship_id INT AUTO_INCREMENT PRIMARY KEY,
        requester_id INTEGER NOT NULL,
        addressee_id INTEGER NOT NULL,
        status ENUM('pending', 'accepted', 'declined') NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (requester_id) REFERENCES Users(user_id) ON DELETE CASCADE,
        FOREIGN KEY (addressee_id) REFERENCES Users(user_id) ON DELETE CASCADE,
        CHECK (requester_id != addressee_id)
    );

    CREATE TABLE IF NOT EXISTS Ledger(
        trade_id INT AUTO_INCREMENT PRIMARY KEY,
        user_id INT NOT NULL, 
        stock_id VARCHAR(10) NOT NULL,
        trade_type ENUM('Buy' , 'Sell') NOT NULL,
        quantity INT NOT NULL,
        price_per_share DECIMAL (10,2) NOT NULL,
        trade_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES Users(user_id) ON DELETE CASCADE,
        FOREIGN KEY (stock_id) REFERENCES Stocks(stock_id) ON DELETE RESTRICT
    );

    CREATE TABLE IF NOT EXISTS StockHistory (
        history_id INT AUTO_INCREMENT PRIMARY KEY,
        stock_id VARCHAR(10) NOT NULL,
        date DATE NOT NULL,
        open_price DECIMAL(10,2),
        high_price DECIMAL(10,2),
        low_price DECIMAL(10,2),
        close_price DECIMAL(10,2),
        volume BIGINT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (stock_id) REFERENCES Stocks(stock_id) ON DELETE CASCADE,
        UNIQUE KEY unique_stock_date (stock_id, date),
        INDEX idx_date (date)
    );
EOSQL

echo "Database schema created."


# 4. Build the Java application using Maven
echo "Building the application with Maven..."
mvn clean install

# 5. Launch the application (assuming it's a webapp launched with an embedded server or similar)
echo "Launching the application..."
# This part is an assumption. The DEPLOYMENT.md should specify how to run it.
# If it's a Spring Boot app, it might be:
# java -jar target/your-app-name.jar
# If it's a traditional webapp, you might need a servlet container like Tomcat.
# For this example, let's assume it's a runnable jar from the 'main' class.
java -cp "target/classes:$(mvn dependency:build-classpath | grep -v 'INFO')" Main

echo "Deployment script finished."
