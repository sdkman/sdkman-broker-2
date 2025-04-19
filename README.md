# SDKMAN Broker

A service for brokering SDKMAN CLI downloads and SDK binaries.

## Overview

This application implements a health check endpoint that performs a deep health check:
- Connects to MongoDB to verify database availability
- Checks for the presence of application record
- Verifies that the application is in a healthy state

## Development

### Prerequisites

- JDK 21 (Temurin recommended)
- MongoDB (or use Docker)
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

Start MongoDB:

```
docker run -d -p 27017:27017 --name mongo mongo:5.0
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