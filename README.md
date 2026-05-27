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

- MongoDB (or use Docker)
- Postgres (or use Docker)
- The Kotlin Toolchain (formerly Amper) — auto-provisioned by the bundled `./kotlin` wrapper. Installing it globally is optional.

### SDKMAN Setup

This project uses SDKMAN to manage the JDK and the Kotlin Toolchain CLI:

```
sdk env
```

That installs the JDK declared in `.sdkmanrc` and the Kotlin Toolchain CLI. Alternatively, the `./kotlin` wrapper checked into the project root auto-provisions both on first use.

### Building and Testing

Build the project:

```
./kotlin build
```

Run tests:

```
./kotlin test
```

Run all verification checks (detekt + ktlint):

```
./kotlin check
```

Auto-format Kotlin sources:

```
./kotlin do ktlintFormat
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
psql -U postgres -d sdkman -h localhost -f src/test/resources/db/migration/V1__create_audit_table.sql
```

Initialize test data:

```
mongosh --eval 'db.getSiblingDB("sdkman").application.insertOne({ alive: "OK", stableCliVersion: "5.19.0", betaCliVersion: "latest+b8d230b", stableNativeCliVersion: "0.7.4", betaNativeCliVersion: "0.7.4" })'
```

Run the application:

```
./kotlin run
```

## API Endpoints

### Health Check

```
GET /health
```

Response:
- 200 OK: `{"status":"UP"}` when healthy
- 503 Service Unavailable: `{"status":"DOWN","reason":"..."}` when unhealthy

## Production Configuration

The service is configured via environment variables. Values default to sensible local-development settings; production deployments must set the following:

### Required

| Variable            | Purpose                                                                 |
| ------------------- | ----------------------------------------------------------------------- |
| `MONGODB_HOST`      | MongoDB host.                                                           |
| `MONGODB_PORT`      | MongoDB port.                                                           |
| `MONGODB_DATABASE`  | MongoDB database name.                                                  |
| `MONGODB_USERNAME`  | MongoDB user.                                                           |
| `MONGODB_PASSWORD`  | MongoDB password.                                                       |
| `POSTGRES_HOST`     | PostgreSQL host.                                                        |
| `POSTGRES_PORT`     | PostgreSQL port.                                                        |
| `POSTGRES_DATABASE` | PostgreSQL database name.                                               |
| `POSTGRES_USERNAME` | PostgreSQL user.                                                        |
| `POSTGRES_PASSWORD` | PostgreSQL password.                                                    |

### Secure Connections

In production, both database connections must be hardened:

| Variable                | Value          | Purpose                                                                 |
| ----------------------- | -------------- | ----------------------------------------------------------------------- |
| `POSTGRES_SSLMODE`      | `require`      | Forces TLS on the PostgreSQL connection. Defaults to `disable` locally. |
| `MONGODB_AUTHMECHANISM` | `SCRAM-SHA-1`  | Enables SCRAM-SHA-1 auth on the MongoDB connection. Unset locally.      |

### Server

| Variable | Purpose                                            |
| -------- | -------------------------------------------------- |
| `PORT`   | HTTP port to bind. Defaults to `8080`.             |
| `HOST`   | Bind address. Defaults to `0.0.0.0` in production. |

## Deployment

This project uses GitHub Actions for CI/CD:

- Pull requests and non-main branches are tested automatically
- Merges to `main` trigger an automatic release process:
  - Creates a Git tag with the current version if it doesn't exist
  - Builds a Docker image and pushes it to Digital Ocean Container Registry
  - The image is tagged with the version number, commit hash, and "latest"

The version is managed by the local `release` Kotlin Toolchain plugin (`plugins/release/`), which derives the version from Git tags.

### Checking the Current Version

To check the current version of the application, run:

```
./kotlin do currentVersion
```

This will display the current version as determined by the local release plugin.
