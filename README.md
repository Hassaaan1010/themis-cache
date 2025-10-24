# Themis Cache - Setup Guide

Fairness balancing, secure multi-tentant in-memory caching service in Java built with Netty, Caffeine (pun intended) and Java 24.

## Prerequisites

- **Java 24** or higher
- **Maven 3.6+**
- **MongoDB 4.0+**


## Project Structure

```
themis-cache/
├── core/          # Server module (server.EchoServer)
├── sdk/           # Client SDK module (instance.Instance)
├── common/        # Shared utilities and protobuf definitions
└── pom.xml        # Parent POM
```


***

## MongoDB Setup

### Linux (Ubuntu/Debian)

```bash
# Import MongoDB GPG key
curl -fsSL https://www.mongodb.org/static/pgp/server-8.0.asc | \
  sudo gpg -o /usr/share/keyrings/mongodb-server-8.0.gpg --dearmor

# Add MongoDB repository
echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-8.0.gpg ] \
  https://repo.mongodb.org/apt/ubuntu $(lsb_release -cs)/mongodb-org/8.0 multiverse" | \
  sudo tee /etc/apt/sources.list.d/mongodb-org-8.0.list

# Update and install
sudo apt update
sudo apt install -y mongodb-org

# Start MongoDB service
sudo systemctl start mongod
sudo systemctl enable mongod

# Verify installation
sudo systemctl status mongod
```


### Windows

1. Download MongoDB Community Server from https://www.mongodb.com/try/download/community
2. Run the `.msi` installer and select **Complete** installation[^4]
3. Choose "Run service as Network Service user" during setup[^4]
4. Add MongoDB `bin` directory to PATH:
    - Typical path: `C:\Program Files\MongoDB\Server\8.0\bin`
    - System Properties → Environment Variables → Path → Edit → New[^4]
5. Start MongoDB service:

```cmd
net start MongoDB
```


### Create Database and Sample User

Connect to MongoDB shell:

```bash
mongosh
```

Execute the following in the MongoDB shell:

```javascript
// Switch to ThemisDB database
use ThemisDB

// Insert sample user into Users collection
db.Users.insertOne({
  tenantName: "SomeTenant",
  password: "SomeTenantPassword",
})

// Verify insertion
db.Users.find().pretty()
```

**Note:** The `_id` value should match the `CACHE_TENANT_ID` in the SDK .env configuration.

***

## Environment Configuration

### 1. Core Module (Server)

Create `core/.env`:

```properties
DB_CONNECTION_STRING=mongodb://localhost:27017
DB_NAME=ThemisDB
```


### 2. SDK Module (Client)

Create `sdk/.env`:

```properties
CACHE_TENANT_ID=<user._id>
CACHE_TENANT_NAME=SomeTenant
CACHE_PASSWORD=SomeTenantPassword
```


***

## Building the Project

### Build All Modules


```bash
mvn clean package
```

This creates JAR files with dependencies in each module's `target/` directory:

- `core/target/core-0.1-SNAPSHOT-jar-with-dependencies.jar`
- `sdk/target/sdk-0.1-SNAPSHOT-jar-with-dependencies.jar`

***

## Running the Application

### Option 1: Run with `mvn exec:java`

#### Start Core Server


```bash
cd core
mvn exec:java
```

#### Start SDK Client (in a separate terminal)


```bash
cd sdk
mvn exec:java
```


### Option 2: Run Pre-built JAR with Dependencies

#### Start Core Server

```bash
java -jar core/target/core-0.1-SNAPSHOT-jar-with-dependencies.jar
```


#### Start SDK Client (in a separate terminal)

```bash
java -jar sdk/target/sdk-0.1-SNAPSHOT-jar-with-dependencies.jar
```

***

## Quick Start Commands

### Complete Setup

```bash
# 1. Build project
mvn clean package

# 2. Start server (terminal 1)
cd core && mvn exec:java

# 3. Start client (terminal 2)
cd sdk && mvn exec:java
```


***


## Module Details

- **Core:** Netty-based server (`server.EchoServer`) with MongoDB persistence and Caffeine caching
- **SDK:** Client SDK (`instance.Instance`) for connecting to the cache server
- **Common:** Shared protobuf definitions and utilities used by both modules
