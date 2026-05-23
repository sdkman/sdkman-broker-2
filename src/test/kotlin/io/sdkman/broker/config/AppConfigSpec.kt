package io.sdkman.broker.config

import arrow.core.Option
import com.typesafe.config.ConfigFactory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

private const val PERSISTENCE_BACKEND_PROPERTY = "persistence.backend"

// HOCON's `${?VAR}` substitution and `ConfigFactory.load()` cache pick up *any* matching system property,
// including those set by `MongoTestListener` / `PostgresTestListener` in other specs sharing this JVM.
// To assert against `application.conf` defaults reliably we must clear those pollutants and invalidate
// the resolved-config cache around every test.
private val managedSystemProperties =
    listOf(
        "mongodb.host",
        "mongodb.port",
        "mongodb.database",
        "mongodb.username",
        "mongodb.password",
        "mongodb.authmechanism",
        "postgres.host",
        "postgres.port",
        "postgres.database",
        "postgres.username",
        "postgres.password",
        "postgres.sslmode",
        "postgres.pool.maxSize",
        "postgres.pool.minIdle",
        "postgres.pool.connectionTimeoutMs",
        "postgres.pool.maxLifetimeMs",
        "postgres.pool.idleTimeoutMs",
        PERSISTENCE_BACKEND_PROPERTY
    )

class AppConfigSpec :
    ShouldSpec({

        beforeTest {
            managedSystemProperties.forEach { System.clearProperty(it) }
            ConfigFactory.invalidateCaches()
        }

        afterTest {
            managedSystemProperties.forEach { System.clearProperty(it) }
            ConfigFactory.invalidateCaches()
        }

        should("read database name from configuration") {
            // given
            val config = DefaultAppConfig()

            // when/then
            config.mongodbDatabase shouldBe "sdkman"
        }

        should("read MongoDB host from configuration") {
            // given
            val config = DefaultAppConfig()

            // when/then
            config.mongodbHost shouldBe "127.0.0.1"
        }

        should("read MongoDB port from configuration") {
            // given
            val config = DefaultAppConfig()

            // when/then
            config.mongodbPort shouldBe "27017"
        }

        should("handle null values for credentials") {
            // given
            val config = DefaultAppConfig()

            // when/then
            config.mongodbUsername shouldBe Option.fromNullable(null)
            config.mongodbPassword shouldBe Option.fromNullable(null)
        }

        should("read server settings from configuration") {
            // given
            val config = DefaultAppConfig()

            // when/then
            config.serverPort shouldBe 8080
            config.serverHost shouldBe "0.0.0.0"
        }

        should("apply default Postgres pool settings when no overrides are configured") {
            // given: no POSTGRES_POOL_* overrides set (cleared in beforeTest) and test HOCON has no pool block

            // when
            val config = DefaultAppConfig()

            // then: code-level defaults from Business Rule 1 apply
            config.postgresPoolMaxSize shouldBe 20
            config.postgresPoolMinIdle shouldBe 2
            config.postgresPoolConnectionTimeoutMs shouldBe 5_000L
            config.postgresPoolMaxLifetimeMs shouldBe 1_800_000L
            config.postgresPoolIdleTimeoutMs shouldBe 600_000L
        }

        should("override the Postgres pool max size from configuration") {
            // given
            System.setProperty("postgres.pool.maxSize", "50")
            ConfigFactory.invalidateCaches()

            // when
            val config = DefaultAppConfig()

            // then
            config.postgresPoolMaxSize shouldBe 50
        }

        should("override the Postgres pool min idle from configuration") {
            // given
            System.setProperty("postgres.pool.minIdle", "5")
            ConfigFactory.invalidateCaches()

            // when
            val config = DefaultAppConfig()

            // then
            config.postgresPoolMinIdle shouldBe 5
        }

        should("override the Postgres pool connection timeout from configuration") {
            // given
            System.setProperty("postgres.pool.connectionTimeoutMs", "1000")
            ConfigFactory.invalidateCaches()

            // when
            val config = DefaultAppConfig()

            // then
            config.postgresPoolConnectionTimeoutMs shouldBe 1_000L
        }

        should("override the Postgres pool max lifetime from configuration") {
            // given
            System.setProperty("postgres.pool.maxLifetimeMs", "900000")
            ConfigFactory.invalidateCaches()

            // when
            val config = DefaultAppConfig()

            // then
            config.postgresPoolMaxLifetimeMs shouldBe 900_000L
        }

        should("override the Postgres pool idle timeout from configuration") {
            // given
            System.setProperty("postgres.pool.idleTimeoutMs", "300000")
            ConfigFactory.invalidateCaches()

            // when
            val config = DefaultAppConfig()

            // then
            config.postgresPoolIdleTimeoutMs shouldBe 300_000L
        }

        should("default the persistence backend to mongo when not configured") {
            // given: PERSISTENCE_BACKEND not set (cleared in beforeTest)

            // when
            val config = DefaultAppConfig()

            // then
            config.persistenceBackend shouldBe PersistenceBackend.Mongo
        }

        should("read the persistence backend as mongo when explicitly configured") {
            // given
            System.setProperty(PERSISTENCE_BACKEND_PROPERTY, "mongo")
            ConfigFactory.invalidateCaches()

            // when
            val config = DefaultAppConfig()

            // then
            config.persistenceBackend shouldBe PersistenceBackend.Mongo
        }

        should("read the persistence backend as postgres when configured") {
            // given
            System.setProperty(PERSISTENCE_BACKEND_PROPERTY, "postgres")
            ConfigFactory.invalidateCaches()

            // when
            val config = DefaultAppConfig()

            // then
            config.persistenceBackend shouldBe PersistenceBackend.Postgres
        }

        should("fail at startup with a descriptive error for an invalid persistence backend value") {
            // given
            System.setProperty(PERSISTENCE_BACKEND_PROPERTY, "cassandra")
            ConfigFactory.invalidateCaches()

            // when/then: construction must blow up so the broker never serves traffic against an unsupported backend
            val error = shouldThrow<IllegalArgumentException> { DefaultAppConfig() }
            error.message shouldContain "PERSISTENCE_BACKEND value not found or incorrect."
            error.message shouldContain "Supported values: mongo, postgres."
        }
    })
