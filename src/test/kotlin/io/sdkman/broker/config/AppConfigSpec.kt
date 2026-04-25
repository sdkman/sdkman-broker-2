package io.sdkman.broker.config

import arrow.core.Option
import com.typesafe.config.ConfigFactory
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class AppConfigSpec :
    ShouldSpec({

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

        should("default persistenceBackend to Mongo when not configured") {
            // given: defaults from application.conf with no override
            val config = DefaultAppConfig()

            // when/then
            config.persistenceBackend shouldBe PersistenceBackend.Mongo
        }

        should("resolve persistenceBackend to Mongo when explicitly configured as 'mongo'") {
            // given
            val raw = ConfigFactory.parseString("""persistence.backend = "mongo"""")
            val config = DefaultAppConfig(raw.withFallback(ConfigFactory.load()))

            // when/then
            config.persistenceBackend shouldBe PersistenceBackend.Mongo
        }

        should("resolve persistenceBackend to Postgres when configured as 'postgres'") {
            // given
            val raw = ConfigFactory.parseString("""persistence.backend = "postgres"""")
            val config = DefaultAppConfig(raw.withFallback(ConfigFactory.load()))

            // when/then
            config.persistenceBackend shouldBe PersistenceBackend.Postgres
        }

        should("fail fast with a descriptive message when persistenceBackend is invalid") {
            // given
            val raw = ConfigFactory.parseString("""persistence.backend = "cassandra"""")
            val merged = raw.withFallback(ConfigFactory.load())

            // when/then: construction must throw — startup must abort on misconfiguration
            val thrown =
                shouldThrow<IllegalArgumentException> {
                    DefaultAppConfig(merged)
                }
            val message = thrown.message.orEmpty()
            message shouldContain "PERSISTENCE_BACKEND"
            message shouldContain "cassandra"
            message shouldContain "mongo"
            message shouldContain "postgres"
        }
    })
