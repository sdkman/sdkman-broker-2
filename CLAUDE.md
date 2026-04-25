# CLAUDE.md

Operational guardrails for the sdkman-broker service. Keep this concise and operational — architecture and domain details belong in the code.

## Project Overview

SDKMAN Broker is a Kotlin-based Ktor application that brokers SDKMAN candidate artifact downloads, resolving platform-specific binaries and redirecting clients to the correct URL.

## Build & Run

- **Run service:** `./gradlew run` (starts Ktor on port 8080; requires MongoDB and PostgreSQL — see Database Setup below)

## Validation

Run after every implementation to get immediate feedback:

- **Full chain:** `./gradlew check` (compile → detekt → ktlintCheck → test)
- **Tests only:** `./gradlew test`
- **Lint only:** `./gradlew ktlintCheck`
- **Lint auto-fix:** `./gradlew ktlintFormat`
- **Static analysis:** `./gradlew detekt` (config in `detekt.yml`)
- **Build Docker image:** `./gradlew jib` (or `jibDockerBuild` for local)

Tests spin up MongoDB and PostgreSQL via Testcontainers — no manual database setup is required to run the test suite.

## Database Setup (Dev Server Only)

The dev server (`./gradlew run`) connects to MongoDB and PostgreSQL on localhost. Start them in Docker:

```bash
docker run -d --restart=always -p=27017:27017 --name mongo mongo:3.2
```

```bash
docker run --restart=always \
    --name postgres \
    -p 5432:5432 \
    -e POSTGRES_USER=postgres \
    -e POSTGRES_PASSWORD=postgres \
    -e POSTGRES_DB=sdkman \
    -d postgres
```

## Git Commit

* Use atomic Git commits with small incremental changes. **NO BULK COMMITS PLEASE!**
* Use Conventional Commits for well structured commit messages: https://www.conventionalcommits.org/en/v1.0.0/
* Always delegate Git commits to the `/commit` skill if available on the system. This ensures Conventional Commit format.

## Rules

Project rules are defined in `.claude/rules/` — study these before making changes.

## Key Conventions

- Kotlin with Ktor, Exposed ORM, Arrow for functional types
- Kotest for assertions, JUnit Platform as test runner, Testcontainers for integration databases
- MongoDB (legacy, default) and PostgreSQL (migration target) for state
- kotlinx-serialization for JSON
