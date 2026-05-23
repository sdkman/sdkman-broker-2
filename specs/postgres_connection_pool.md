# PostgreSQL Connection Pool Configuration (HikariCP)

The Broker already pools its PostgreSQL connections with HikariCP, but the pool
is configured by hand: every tuning value is a hardcoded literal in
`PostgresConnectivity.createDataSource()`, the pool is unnamed, the
`DataSource` is never closed, and each repository opens its own
`Database.connect(...)`. The companion service `sdkman-state` recently
reworked the same concern as part of a throughput initiative and arrived at a
more operable standard: pool tuning is externalised to config and environment
variables with documented defaults, the pool is named, the `DataSource`
lifecycle is managed, and a single Exposed `Database.connect` pins an explicit
transaction isolation level. This spec brings the Broker's PostgreSQL pooling
up to that standard so the two services — twin towers either side of the same
Postgres schema — behave and are tuned consistently.

The scope here is **connection pooling and Exposed wiring only**. The Broker's
query logic, the Mongo backend, the `postgres.*` config namespace, and the
read-only contract with `sdkman-state` are all unchanged.

*Reference: `sdkman-state/specs/concurrency-optimisations.md` (R1, R2, R9 and
the "Extra Considerations" notes on isolation level) — the upstream spec that
established this standard.*

## Behaviour

After this change the Broker's pooled `javax.sql.DataSource` is configured
entirely from `application.conf` with environment-variable overrides and the
same defaults `sdkman-state` ships. The five tunables — maximum pool size,
minimum idle, connection timeout, max lifetime, idle timeout — are no longer
literals in code. The pool carries the name `sdkman-broker-pool` so it is
identifiable in HikariCP log lines and JMX/metrics alongside
`sdkman-state-pool`. The `DataSource` is constructed with
`initializationFailTimeout = -1`, so the application boots even when Postgres
is temporarily unreachable and surfaces the outage through `/meta/health`
rather than failing at startup. On shutdown the pool is closed cleanly. Exposed
is wired through a single `Database.connect` that sets
`defaultIsolationLevel = TRANSACTION_READ_COMMITTED`, and every repository
shares that one `Database` rather than connecting independently.

None of this is observable to a download client. The HTTP contract in
`postgres_version_repository.md` is unchanged; this spec only changes how the
connection pool is configured and wired.

## Business Rules

1. **Pool tuning is externalised, with `sdkman-state`'s defaults.** The five
   HikariCP tunables are read from configuration, not hardcoded. The Broker
   keeps its existing `postgres.*` namespace (renaming to state's `database.*`
   would break already-deployed environment variables), so the new keys live
   under `postgres.pool.*` with `POSTGRES_POOL_*` overrides. The structure,
   key names, and **default values match `sdkman-state` exactly**:

   | Setting               | Config key                          | Env override                          | Default     | Meaning                                             |
   |-----------------------|-------------------------------------|---------------------------------------|-------------|-----------------------------------------------------|
   | `maximumPoolSize`     | `postgres.pool.maxSize`             | `POSTGRES_POOL_MAX_SIZE`              | `20`        | Max total connections.                              |
   | `minimumIdle`         | `postgres.pool.minIdle`             | `POSTGRES_POOL_MIN_IDLE`              | `2`         | Warm idle connections kept ready.                   |
   | `connectionTimeout`   | `postgres.pool.connectionTimeoutMs` | `POSTGRES_POOL_CONNECTION_TIMEOUT_MS` | `5000`      | Fail fast when the pool is starved (ms).            |
   | `maxLifetime`         | `postgres.pool.maxLifetimeMs`       | `POSTGRES_POOL_MAX_LIFETIME_MS`       | `1800000`   | Retire a connection after 30 min.                   |
   | `idleTimeout`         | `postgres.pool.idleTimeoutMs`       | `POSTGRES_POOL_IDLE_TIMEOUT_MS`       | `600000`    | Drop idle connections after 10 min.                 |

   This changes two current values: `maximumPoolSize` moves from a hardcoded
   `10` to a default of `20`, and `connectionTimeout` from a hardcoded
   `30000` to a default of `5000`. Both now match state and both are
   overridable per environment.

2. **`AppConfig` exposes the pool tunables.** The `AppConfig` interface gains
   five members and `DefaultAppConfig` reads them with the existing
   `getIntOrDefault` / `getLongOrDefault` helpers so absent keys fall back to
   the defaults in Rule 1. Timeouts are `Long` (milliseconds); sizes are `Int`.
   `PostgresConnectivity` consumes these instead of literals.

3. **The pool is named.** `HikariConfig.poolName = "sdkman-broker-pool"`,
   mirroring state's `sdkman-state-pool`. This makes the two pools
   distinguishable in logs and metrics when both services share infrastructure.

4. **The pool boots even when Postgres is down.** Set
   `initializationFailTimeout = -1` so `HikariDataSource` construction does not
   block on or fail because of an unreachable database. This is a deliberate
   behaviour change: today `PostgresConnectivity.dataSource()` wraps
   construction in `Either.catch` and rethrows `IllegalStateException`, so a
   database that is down at boot kills the process. Under this rule the process
   starts and the outage is reported through the existing
   `PostgresHealthRepository` / `/meta/health` path instead. The `Either.catch`
   wrapper stays (it still guards against genuine misconfiguration such as a
   bad driver class), but connectivity failures no longer abort startup.

   > **Why this value.** The choice is driven purely by **parity with
   > `sdkman-state`**, which sets `-1`, not by a resilience requirement —
   > managed Postgres downtime is effectively a non-event in this deployment.
   > Fail-fast-at-boot would be an equally defensible standard; what matters is
   > that both services use the *same* one. If the teams ever prefer fail-fast,
   > change it in both services together, never in just one.

5. **One Exposed `Database`, with an explicit isolation level.** Replace the
   per-repository `Database.connect(dataSource)` (currently created `by lazy`
   independently in `PostgresVersionRepository` and `PostgresAuditRepository`)
   with a single `Database.connect` performed once at wiring time and shared by
   all repositories:

   ```kotlin
   Database.connect(
       datasource = dataSource,
       databaseConfig = DatabaseConfig { defaultIsolationLevel = Connection.TRANSACTION_READ_COMMITTED },
   )
   ```

   Pinning `defaultIsolationLevel` matches state and makes the isolation level
   deterministic instead of driver-default; it also stops Exposed probing the
   database for its default isolation level on the first transaction.

   > **Note on motivation.** In `sdkman-state` this setting is load-bearing: it
   > runs queries inside `newSuspendedTransaction`, where a first-transaction
   > metadata probe against an unreachable database deadlocks the request
   > coroutine (state R9). The Broker today uses **blocking** `transaction(db)`
   > and its health check uses **raw JDBC** (`dataSource.connection`), so it is
   > not exposed to that coroutine deadlock. Here the setting is adopted for
   > consistency, deterministic isolation, and one fewer startup round-trip —
   > and as cheap insurance should the Broker later move to suspended
   > transactions.

6. **The pool is closed on shutdown.** The `HikariDataSource` must be closed
   when the application stops. Because the Broker builds the `DataSource` in
   `App.main` (outside the Ktor module, before `embeddedServer(...).start`),
   register a JVM shutdown hook — or subscribe to Ktor's `ApplicationStopped`
   monitor event — to call `dataSource.close()`. State closes its pool via
   `monitor.subscribe(ApplicationStopped) { dataSource.close() }`; the Broker
   should achieve the equivalent given its wiring.

7. **The corrected SSL handling is preserved, not regressed.** The Broker's
   `buildConnectionString()` already appends a lowercase `?sslmode=...` (the
   parameter name the PostgreSQL JDBC driver actually honours) and is
   configurable via `postgres.sslmode` / `POSTGRES_SSLMODE`. This is **more
   correct** than state, which hardcodes `?sslMode=prefer` (camel-cased, and
   therefore likely ignored by pgjdbc). The Broker keeps its own handling
   unchanged; aligning state's URL is a separate, upstream concern (see Out of
   Scope).

## Examples

### `application.conf` — new `postgres.pool` block

The `postgres` block gains a nested `pool` block; the existing connection keys
are unchanged.

```hocon
postgres {
  database = "sdkman"
  database = ${?POSTGRES_DATABASE}
  host = "127.0.0.1"
  host = ${?POSTGRES_HOST}
  port = "5432"
  port = ${?POSTGRES_PORT}
  username = "postgres"
  username = ${?POSTGRES_USERNAME}
  password = "postgres"
  password = ${?POSTGRES_PASSWORD}
  sslmode = "disable"
  sslmode = ${?POSTGRES_SSLMODE}

  pool {
    maxSize = 20
    maxSize = ${?POSTGRES_POOL_MAX_SIZE}
    minIdle = 2
    minIdle = ${?POSTGRES_POOL_MIN_IDLE}
    connectionTimeoutMs = 5000
    connectionTimeoutMs = ${?POSTGRES_POOL_CONNECTION_TIMEOUT_MS}
    maxLifetimeMs = 1800000
    maxLifetimeMs = ${?POSTGRES_POOL_MAX_LIFETIME_MS}
    idleTimeoutMs = 600000
    idleTimeoutMs = ${?POSTGRES_POOL_IDLE_TIMEOUT_MS}
  }
}
```

### `AppConfig` additions

```kotlin
interface AppConfig {
    // existing postgres.* members …
    val postgresPoolMaxSize: Int
    val postgresPoolMinIdle: Int
    val postgresPoolConnectionTimeoutMs: Long
    val postgresPoolMaxLifetimeMs: Long
    val postgresPoolIdleTimeoutMs: Long
}

class DefaultAppConfig : AppConfig {
    // …
    override val postgresPoolMaxSize: Int = config.getIntOrDefault("postgres.pool.maxSize", 20)
    override val postgresPoolMinIdle: Int = config.getIntOrDefault("postgres.pool.minIdle", 2)
    override val postgresPoolConnectionTimeoutMs: Long =
        config.getLongOrDefault("postgres.pool.connectionTimeoutMs", 5_000L)
    override val postgresPoolMaxLifetimeMs: Long =
        config.getLongOrDefault("postgres.pool.maxLifetimeMs", 1_800_000L)
    override val postgresPoolIdleTimeoutMs: Long =
        config.getLongOrDefault("postgres.pool.idleTimeoutMs", 600_000L)
}
```

### `PostgresConnectivity.createDataSource` — before and after

```kotlin
// before — hardcoded literals, no pool name, fail-fast on a down database
private fun createDataSource(connectionString: String): DataSource =
    with(HikariConfig()) {
        jdbcUrl = connectionString
        config.postgresUsername.map { username = it }
        config.postgresPassword.map { password = it }
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
        minimumIdle = 2
        connectionTimeout = 30000
        idleTimeout = 600000
        maxLifetime = 1800000
        HikariDataSource(this)
    }

// after — sourced from AppConfig, named, boots even if Postgres is down
private fun createDataSource(connectionString: String): DataSource =
    with(HikariConfig()) {
        jdbcUrl = connectionString
        config.postgresUsername.map { username = it }
        config.postgresPassword.map { password = it }
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = config.postgresPoolMaxSize
        minimumIdle = config.postgresPoolMinIdle
        connectionTimeout = config.postgresPoolConnectionTimeoutMs
        maxLifetime = config.postgresPoolMaxLifetimeMs
        idleTimeout = config.postgresPoolIdleTimeoutMs
        poolName = "sdkman-broker-pool"
        initializationFailTimeout = -1
        HikariDataSource(this)
    }
```

### Environment-variable overrides (operations)

| Env variable                          | Overrides              | Default   |
|---------------------------------------|------------------------|-----------|
| `POSTGRES_POOL_MAX_SIZE`              | `maximumPoolSize`      | `20`      |
| `POSTGRES_POOL_MIN_IDLE`              | `minimumIdle`          | `2`       |
| `POSTGRES_POOL_CONNECTION_TIMEOUT_MS` | `connectionTimeout`    | `5000`    |
| `POSTGRES_POOL_MAX_LIFETIME_MS`       | `maxLifetime`          | `1800000` |
| `POSTGRES_POOL_IDLE_TIMEOUT_MS`       | `idleTimeout`          | `600000`  |

## Out of Scope

- **The `postgres.*` → `database.*` namespace rename.** State uses `database.*`;
  the Broker keeps `postgres.*` to avoid breaking deployed environment
  variables. Only the pool sub-structure, keys, defaults, and behaviour are
  aligned — not the prefix.
- **Aligning the JDBC driver version.** The Broker is on `postgresql:42.7.11`,
  state on `42.7.2`. Bumping is unrelated to pooling; if the teams want a
  matched driver it should be a deliberate, separate change (state catching up
  is preferable to the Broker downgrading).
- **Gradle dependency declaration.** Both services already declare HikariCP
  (`5.1.0`) and the Postgres driver via a `gradle/libs.versions.toml` version
  catalog (`libs.hikaricp` / `libs.postgresql`), so build-declaration style is
  already consistent — nothing to change here.
- **Fixing state's `sslMode=prefer` typo.** That lives in
  `sdkman-state`'s `ConfigExtensions.kt` and is an upstream fix; the Broker's
  correct lowercase `sslmode` handling is preserved as-is (Business Rule 7).
- **The Mongo backend.** `MongoConnectivity` and the Mongo repositories are
  untouched.
- **Query logic, the UPSERT, and single-transaction write path** from state's
  spec (R3–R6). The Broker is a read-only consumer of the `versions` table and
  performs no writes to it, so those requirements do not apply.
- **Production Flyway wiring.** State runs Flyway at startup against its pool;
  the Broker owns no migrations (Flyway is a `testImplementation` only) because
  the schema is owned by `sdkman-state`. No production migration wiring is
  added.

## Acceptance Criteria

- [ ] The five HikariCP tunables are read from `postgres.pool.*` config with
      `POSTGRES_POOL_*` env overrides; no pool tunable remains a literal in
      `PostgresConnectivity`.
- [ ] Defaults applied when env vars are absent match `sdkman-state`: maxSize
      `20`, minIdle `2`, connectionTimeout `5000`, maxLifetime `1800000`,
      idleTimeout `600000`.
- [ ] `AppConfig` / `DefaultAppConfig` expose the five pool members and a unit
      test verifies they load from HOCON with defaults applied when env vars
      are unset.
- [ ] The pool is named `sdkman-broker-pool`.
- [ ] `initializationFailTimeout = -1` is set, and the application starts
      successfully against an unreachable Postgres, with `/meta/health`
      reporting the database as unhealthy rather than the process aborting at
      boot.
- [ ] Exposed is wired through a single `Database.connect` with
      `defaultIsolationLevel = TRANSACTION_READ_COMMITTED`, shared by all
      Postgres repositories (no per-repository `Database.connect`).
- [ ] The `HikariDataSource` is closed on application shutdown.
- [ ] The existing lowercase `sslmode` connection-string handling is unchanged.
- [ ] An integration test opens more concurrent queries than a small configured
      `maxSize` and asserts they all complete (queued requests served as
      connections free up), proving the pool is the active path.
- [ ] Existing download and health acceptance/integration specs pass unchanged.
- [ ] All quality gates pass (`./gradlew clean check` — compile, detekt,
      ktlint, tests).
