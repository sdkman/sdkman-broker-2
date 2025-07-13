# SDKMAN Broker

A service for brokering SDKMAN CLI downloads and SDK binaries.

## Overview

This application implements a health check endpoint that performs a deep health check:
- Connects to MongoDB to verify database availability
- Connects to Postgres for modern data persistence
- Checks for the presence of application record
- Verifies that the application is in a healthy state

## Development

### Prerequisites

- JDK 21 (Temurin recommended)
- MongoDB (or use Docker)
- Postgres (or use Docker)
- Gradle

### SDKMAN Setup

This project uses SDKMAN to manage the JDK version:

```
sdk env
```

### Building and Testing

Build the project:

```
./gradlew build
```

Run tests:

```
./gradlew check
```

### Running Locally

Start MongoDB and Postgres using Docker Compose:

```
docker-compose up -d
```

Or start them individually:

```
# MongoDB
docker run -d -p 27017:27017 --name mongo mongo:5.0

# PostgreSQL
docker run -d -p 5432:5432 --name postgres \
  -e POSTGRES_DB=sdkman \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  postgres:15-alpine
```

Run the database migration:

```
psql -U postgres -d sdkman -h localhost -f src/test/resources/db/migration/V1__Initial_audit_table.sql
```

Initialize test data:

```
mongosh --eval 'db.getSiblingDB("sdkman").application.insertOne({ alive: "OK", stableCliVersion: "5.19.0", betaCliVersion: "latest+b8d230b", stableNativeCliVersion: "0.7.4", betaNativeCliVersion: "0.7.4" })'
```

Run the application:

```
./gradlew run
```

## API Endpoints

### Health Check

```
GET /health
```

Response:
- 200 OK: `{"status":"UP"}` when healthy
- 503 Service Unavailable: `{"status":"DOWN","reason":"..."}` when unhealthy

## Deployment

This project uses GitHub Actions for CI/CD:

- Pull requests and non-main branches are tested automatically
- Merges to `main` trigger an automatic release process:
  - Creates a Git tag with the current version if it doesn't exist
  - Builds a Docker image and pushes it to Digital Ocean Container Registry
  - The image is tagged with the version number, commit hash, and "latest"

The version is managed by the Axion Release Plugin based on Git tags.

### Checking the Current Version

To check the current version of the application, run:

```
./gradlew currentVersion
```

This will display the current version as determined by the Axion Release Plugin.
