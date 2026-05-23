package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.None
import arrow.core.Option
import arrow.core.some
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.sdkman.broker.config.AppConfig
import io.sdkman.broker.config.PersistenceBackend
import org.junit.jupiter.api.Tag

/**
 * Proves the deliberate behaviour change of spec Business Rule 4: with
 * `initializationFailTimeout = -1`, `PostgresConnectivity.dataSource()` constructs a
 * usable pool even when Postgres is unreachable, so the application boots and surfaces
 * the outage through `/meta/health` rather than aborting at startup. This automates
 * the spec acceptance criterion "the application starts successfully against an
 * unreachable Postgres" instead of relying on a manual `./gradlew run` smoke.
 *
 * No Testcontainers Postgres is needed — the whole point is that there is no database
 * to reach. The endpoint is an unused localhost port (connection refused) and a small
 * `connectionTimeoutMs` keeps the health check's failure fast and deterministic.
 */
@Tag("integration")
class PostgresConnectivityBootIntegrationSpec :
    ShouldSpec({
        val connectivity = PostgresConnectivity(unreachablePostgresConfig())
        val dataSource = connectivity.dataSource()

        afterSpec { dataSource.close() }

        should("construct a usable pool without throwing when Postgres is unreachable") {
            // given: a PostgresConnectivity pointed at an unused port (built in the spec body,
            // which proves construction itself does not throw)
            // then: the pool is handed back rather than the process aborting at boot
            withClue("initializationFailTimeout = -1 must let the pool build despite a down database") {
                dataSource shouldNotBe null
            }
        }

        should("surface the outage as a health-check failure rather than at startup") {
            // given: the booted pool wrapped in the production health repository
            val health = PostgresHealthRepository(dataSource)

            // when: connectivity is probed
            val result = health.checkConnectivity()

            // then: the unreachable database is reported as a Left, not a boot crash
            withClue("an unreachable Postgres must report unhealthy via /meta/health, not kill the process") {
                result.isLeft() shouldBe true
            }
        }
    })

private const val UNREACHABLE_PORT = "59999"

private fun unreachablePostgresConfig(): AppConfig =
    object : AppConfig {
        override val mongodbHost: String = "localhost"
        override val mongodbPort: String = "27017"
        override val mongodbDatabase: String = "sdkman"
        override val mongodbUsername: Option<String> = None
        override val mongodbPassword: Option<String> = None
        override val mongodbAuthMechanism: Option<String> = None
        override val postgresHost: String = "127.0.0.1"
        override val postgresPort: String = UNREACHABLE_PORT
        override val postgresDatabase: String = "sdkman"
        override val postgresUsername: Option<String> = "postgres".some()
        override val postgresPassword: Option<String> = "postgres".some()
        override val postgresSslMode: String = "disable"
        override val postgresPoolMaxSize: Int = 2
        override val postgresPoolMinIdle: Int = 1
        override val postgresPoolConnectionTimeoutMs: Long = 1_000L
        override val postgresPoolMaxLifetimeMs: Long = 1_800_000L
        override val postgresPoolIdleTimeoutMs: Long = 600_000L
        override val serverPort: Int = 8080
        override val serverHost: String = "127.0.0.1"
        override val persistenceBackend: PersistenceBackend = PersistenceBackend.Mongo
    }
