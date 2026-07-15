# CLAUDE.md

Operational guardrails for the sdkman-broker service. Keep this concise and operational — architecture and domain details belong in the code.

## Project Overview

SDKMAN Broker is a Kotlin-based Ktor application that brokers SDKMAN candidate artifact downloads, resolving platform-specific binaries and redirecting clients to the correct URL.

## Build & Run

Built with the Kotlin Toolchain (Amper engine). The `./kotlin` wrapper auto-provisions both the JDK and the toolchain; no `setup-java` or local install required.

Built-in toolchain commands (`build`, `check`, `test`, `run`, `clean`) are invoked directly as `./kotlin <command>`. Commands contributed by the local plugins under `plugins/` (e.g. `currentVersion`, `release`, `jib`, `ktlintFormat`) are invoked as `./kotlin do <command>`.

- **Run service:** `./kotlin run` (starts Ktor on port 8080; requires MongoDB and PostgreSQL — see Database Setup below)

## Validation

Run after every implementation to get immediate feedback:

- **Clean build:** `./kotlin clean`
- **Full check:** `./kotlin clean && ./kotlin check` (compile → detekt → ktlint → test)
- **Tests only:** `./kotlin test`
- **Lint auto-fix:** `./kotlin do ktlintFormat`
- **Static analysis:** detekt runs as part of `./kotlin check` (config in `detekt.yml`, plugin in `plugins/detekt/`)
- **Build Docker image:** `./kotlin do jib` (pushes to the registry in `module.yaml`; use `./kotlin do jibDockerBuild` for a local Docker daemon load)
- **Current version:** `./kotlin do currentVersion` (derived from git tags by the local release plugin)

Tests spin up MongoDB and PostgreSQL via Testcontainers — no manual database setup is required to run the test suite.

## Build Configuration

- **`module.yaml`** — root module config (dependencies, settings, plugin config). Layout is `maven-like` (`src/main/kotlin`, `src/test/kotlin`, etc.).
- **`project.yaml`** — registers all modules and local plugins.
- **`gradle/libs.versions.toml`** — Gradle-format version catalog reused as the `$libs.*` catalog (the directory name is historical; the file is consumed natively by the Kotlin Toolchain).
- **`plugins/{release,jib,detekt,ktlint}/`** — local plugins replacing the Gradle plugins of the same name. Reimplement behavior here rather than reaching for Gradle plugins.

## Database Setup (Dev Server Only)

The dev server (`./kotlin run`) connects to MongoDB and PostgreSQL on localhost. Start them in Docker:

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
* Only commit green builds

## Rules

Project rules are defined in `.claude/rules/` — study these before making changes.

## Key Conventions

- Kotlin with Ktor, Exposed ORM, Arrow for functional types
- Kotest for assertions, JUnit Platform as test runner, Testcontainers for integration databases
- MongoDB (legacy, default) and PostgreSQL (migration target) for state
- kotlinx-serialization for JSON
