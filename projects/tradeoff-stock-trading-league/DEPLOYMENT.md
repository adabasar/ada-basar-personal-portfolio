# Deployment Documentation
**201-Final Trading System Template**  
**Version:** 1.0-SNAPSHOT  
**Last Updated:** November 16, 2025

---

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Environment Setup](#environment-setup)
3. [Database Configuration](#database-configuration)
4. [Building the Application](#building-the-application)
5. [Running Tests](#running-tests)
6. [Deployment Steps](#deployment-steps)
7. [Troubleshooting](#troubleshooting)
8. [Server Configuration](#server-configuration)

---

## Prerequisites

Before deploying the application, ensure the following software is installed on your target environment:

### Required Software
- **Java Development Kit (JDK)**: Version 11 or higher
  - Download from: https://adoptium.net/ or https://www.oracle.com/java/technologies/downloads/
  - Verify installation: `java -version`
  
- **Apache Maven**: Version 3.6.0 or higher
  - Download from: https://maven.apache.org/download.cgi
  - Verify installation: `mvn -version`
  
- **MySQL Database Server**: Version 8.0 or higher (for integration tests only)
  - Download from: https://dev.mysql.com/downloads/mysql/
  - Verify installation: `mysql --version`

### Optional Software
- **Git**: Version 2.0 or higher (for source code management)
  - Download from: https://git-scm.com/downloads
  - Verify installation: `git --version`

### System Requirements
- **Operating System**: Linux, macOS, or Windows
- **RAM**: Minimum 2GB (4GB recommended for stress tests)
- **Disk Space**: Minimum 500MB free space
- **Network**: Internet connection required for Maven dependency downloads

---

FOR MAC OSX
chmod +x deploy.sh
./deploy.sh


## Environment Setup

### Step 1: Install Java JDK

**For Linux (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install openjdk-11-jdk
```

**For macOS:**
```bash
brew install openjdk@11
```

**For Windows:**
1. Download the JDK installer from https://adoptium.net/
2. Run the installer and follow the prompts
3. Add `JAVA_HOME` environment variable pointing to JDK installation directory
4. Add `%JAVA_HOME%\bin` to system PATH

### Step 2: Install Apache Maven

**For Linux (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install maven
```

**For macOS:**
```bash
brew install maven
```

**For Windows:**
1. Download Maven from https://maven.apache.org/download.cgi
2. Extract to `C:\Program Files\Apache\maven`
3. Add `M2_HOME` environment variable pointing to Maven directory
4. Add `%M2_HOME%\bin` to system PATH

### Step 3: Verify Installation

Run the following commands to verify all prerequisites are installed correctly:

```bash
java -version    # Should display Java 11 or higher
mvn -version     # Should display Maven 3.6.0 or higher
```

---

## Database Configuration

### MySQL Setup (Required for Integration Tests Only)

**Note:** The unit tests and stress tests do not require a database. Database setup is only necessary if you plan to run integration tests.

### Step 1: Install MySQL Server

**For Linux (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql
```

**For macOS:**
```bash
brew install mysql
brew services start mysql
```

**For Windows:**
1. Download MySQL Installer from https://dev.mysql.com/downloads/installer/
2. Run the installer and select "Developer Default"
3. Complete the installation wizard

### Step 2: Create Database and User

Log into MySQL as root:
```bash
mysql -u root -p
```

Execute the following SQL commands:
```sql
-- Create database for integration tests
CREATE DATABASE tradeoff_test;

-- Create test user with password
CREATE USER 'testuser'@'localhost' IDENTIFIED BY 'testpass';

-- Grant all privileges on test database
GRANT ALL PRIVILEGES ON tradeoff_test.* TO 'testuser'@'localhost';

-- Apply changes
FLUSH PRIVILEGES;

-- Exit MySQL
EXIT;
```

### Step 3: Create Database Schema

```sql
-- Use the test database
USE tradeoff_test;

-- Create users table
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    available_capital DECIMAL(15,2) DEFAULT 100000.00,
    initial_capital DECIMAL(15,2) DEFAULT 100000.00,
    portfolio_value DECIMAL(15,2) DEFAULT 100000.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create ledger table for transaction history
CREATE TABLE ledger (
    transaction_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    ticker VARCHAR(10) NOT NULL,
    quantity INT NOT NULL,
    transaction_type ENUM('BUY', 'SELL') NOT NULL,
    price DECIMAL(15,2),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_user_id ON ledger(user_id);
CREATE INDEX idx_ticker ON ledger(ticker);
CREATE INDEX idx_timestamp ON ledger(timestamp);
```

### Step 4: Verify Database Setup

```bash
mysql -u testuser -ptestpass tradeoff_test -e "SHOW TABLES;"
```

Expected output:
```
+-------------------------+
| Tables_in_tradeoff_test |
+-------------------------+
| ledger                  |
| users                   |
+-------------------------+
```

---

## Building the Application

### Step 1: Clone the Repository

```bash
git clone https://github.com/miguelgambalan/201-Final.git
cd 201-Final
```

### Step 2: Clean Previous Builds

```bash
mvn clean
```

This command removes the `target/` directory and any previously compiled files.

### Step 3: Compile the Source Code

```bash
mvn compile
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] Compiling 6 source files to target/classes
```

### Step 4: Compile Test Classes

```bash
mvn test-compile
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] Compiling 5 source files to target/test-classes
```

---

## Running Tests

### Run All Tests

```bash
mvn test
```

**Note:** Integration tests will fail if MySQL database is not configured. This is expected behavior.

### Run Specific Test Suites

**Unit Tests Only:**
```bash
mvn test -Dtest=UserTest,LeagueTest,StockTest
```

**Stress Tests Only:**
```bash
mvn test -Dtest=StressTest
```

**Integration Tests Only (requires database):**
```bash
mvn test -Dtest=SchemaIntegrationTest
```

### Expected Test Results

Without database configuration:
- **Unit Tests:** 37/37 passing ✓
- **Stress Tests:** 11/11 passing ✓
- **Integration Tests:** 0/6 (database connection errors - expected)
- **Total:** 48/54 tests passing (88.9%)

With database configuration:
- **All Tests:** 54/54 passing ✓

---

## Deployment Steps

### Step-by-Step Deployment Process

#### Step 1: Download the Project

If you haven't cloned the repository yet:
```bash
git clone https://github.com/miguelgambalan/201-Final.git
cd 201-Final
```

#### Step 2: Verify Prerequisites

Run the verification script:
```bash
java -version && mvn -version
```

Ensure both commands execute without errors.

#### Step 3: Install Dependencies

Maven will automatically download all required dependencies:
```bash
mvn clean install
```

**Dependencies that will be downloaded:**
- JUnit Jupiter 5.9.2 (testing framework)
- MySQL Connector/J 8.0.33 (database driver)
- Mockito 5.3.1 (mocking framework)
- AssertJ 3.24.2 (assertion library)

#### Step 4: Build the Application

```bash
mvn clean package
```

This creates a JAR file in the `target/` directory:
```
target/201-Final-1.0-SNAPSHOT.jar
```

#### Step 5: Run the Application

```bash
java -jar target/201-Final-1.0-SNAPSHOT.jar
```

**Note:** The current template outputs "Hello, world!" as the main method is not yet implemented.

#### Step 6: Verify Deployment

Run a quick verification test:
```bash
mvn test -Dtest=UserTest#testGetPercentChangeBasicProfit
```

If this test passes, your deployment is successful.

---

## Server Configuration

### Production Environment Setup

#### Memory Settings

For production deployment, configure JVM memory settings:

```bash
export MAVEN_OPTS="-Xms512m -Xmx2048m"
```

Or when running the JAR:
```bash
java -Xms512m -Xmx2048m -jar target/201-Final-1.0-SNAPSHOT.jar
```

#### Database Connection Pool

For production, update database connection settings in your implementation:

**Recommended Settings:**
- **Maximum Pool Size:** 20 connections
- **Minimum Idle:** 5 connections
- **Connection Timeout:** 30 seconds
- **Idle Timeout:** 10 minutes

#### Server Requirements

**Minimum Production Server Specifications:**
- CPU: 2 cores
- RAM: 4GB
- Disk: 10GB
- OS: Linux (Ubuntu 20.04 LTS or later recommended)

**Recommended Production Server Specifications:**
- CPU: 4 cores
- RAM: 8GB
- Disk: 50GB SSD
- OS: Linux (Ubuntu 22.04 LTS recommended)

#### Firewall Configuration

If deploying with MySQL on the same server:
```bash
# Allow MySQL connections (internal only)
sudo ufw allow from 127.0.0.1 to any port 3306
```

If deploying web interface (future implementation):
```bash
# Allow HTTP/HTTPS traffic
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
```

---

## Troubleshooting

### Common Issues and Solutions

#### Issue 1: "JAVA_HOME is not set"

**Solution:**
```bash
# Linux/macOS
export JAVA_HOME=/path/to/jdk
export PATH=$JAVA_HOME/bin:$PATH

# Windows (Command Prompt)
set JAVA_HOME=C:\Program Files\Java\jdk-11
set PATH=%JAVA_HOME%\bin;%PATH%
```

#### Issue 2: "Maven command not found"

**Solution:**
Ensure Maven is installed and added to PATH:
```bash
# Linux/macOS
export PATH=/path/to/maven/bin:$PATH

# Verify
which mvn
```

#### Issue 3: "Could not resolve dependencies"

**Solution:**
Clear Maven cache and retry:
```bash
rm -rf ~/.m2/repository
mvn clean install
```

#### Issue 4: "Communications link failure" (Database)

**Solution:**
1. Verify MySQL is running:
   ```bash
   # Linux
   sudo systemctl status mysql
   
   # macOS
   brew services list | grep mysql
   ```

2. Check database credentials:
   ```bash
   mysql -u testuser -ptestpass tradeoff_test
   ```

3. Verify MySQL is listening on localhost:3306:
   ```bash
   netstat -an | grep 3306
   ```

#### Issue 5: "Tests are failing"

**Solution:**
1. Run tests individually to isolate the issue:
   ```bash
   mvn test -Dtest=UserTest
   ```

2. Check if it's a database-related test:
   - Integration tests require database configuration
   - Unit tests and stress tests do not require database

3. Review test logs in `target/surefire-reports/`

#### Issue 6: "Port already in use"

**Solution:**
If MySQL port 3306 is already in use:
```bash
# Find process using port 3306
sudo lsof -i :3306

# Kill the process (if safe to do so)
sudo kill -9 <PID>

# Or configure MySQL to use a different port
```

---

## Additional Configuration

### Maven Settings

Create or edit `~/.m2/settings.xml` for custom Maven configuration:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
    
    <localRepository>${user.home}/.m2/repository</localRepository>
    
    <mirrors>
        <mirror>
            <id>central</id>
            <mirrorOf>central</mirrorOf>
            <url>https://repo.maven.apache.org/maven2</url>
        </mirror>
    </mirrors>
</settings>
```

### Build Profiles

The project uses the default Maven build profile. No special profiles are required.

### Environment Variables

No environment variables are required for basic deployment. Optional variables:

- `JAVA_HOME`: Path to JDK installation
- `MAVEN_OPTS`: Maven JVM options (e.g., `-Xmx2048m`)
- `MYSQL_HOST`: MySQL server host (default: localhost)
- `MYSQL_PORT`: MySQL server port (default: 3306)

---

## Support and Maintenance

### Logs Location

- **Maven logs:** Console output (can be redirected to file)
- **Test reports:** `target/surefire-reports/`
- **Build logs:** `target/maven-*.log`

### Monitoring

For production deployment, monitor:
- Application logs for errors
- Database connection pool status
- Memory usage (`jstat` command)
- CPU usage (`top` or `htop`)

### Backup Recommendations

**Database Backups:**
```bash
# Daily backup script
mysqldump -u testuser -ptestpass tradeoff_test > backup_$(date +%Y%m%d).sql
```

**Code Backups:**
- Use Git for version control
- Tag releases: `git tag -a v1.0 -m "Version 1.0"`

---

## Quick Reference

### Essential Commands

| Task | Command |
|------|---------|
| Build project | `mvn clean package` |
| Run tests | `mvn test` |
| Run application | `java -jar target/201-Final-1.0-SNAPSHOT.jar` |
| Clean build | `mvn clean` |
| Install dependencies | `mvn clean install` |
| Run specific test | `mvn test -Dtest=TestName` |

### Important File Locations

| File/Directory | Purpose |
|----------------|---------|
| `src/main/java/` | Application source code |
| `src/test/java/` | Test source code |
| `pom.xml` | Maven project configuration |
| `target/` | Compiled classes and JAR files |
| `README.md` | Project documentation |
| `.gitignore` | Git ignore rules |

---

## Contact Information

For technical support or questions about deployment:
- **Repository:** https://github.com/miguelgambalan/201-Final
- **Issue Tracker:** https://github.com/miguelgambalan/201-Final/issues

---

**Document Version:** 1.0  
**Last Updated:** November 16, 2025  
**Prepared by:** Development Team
