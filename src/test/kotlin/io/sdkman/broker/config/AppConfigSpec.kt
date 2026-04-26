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
            error.message shouldContain "cassandra"
            error.message shouldContain "mongo"
            error.message shouldContain "postgres"
        }
    })
