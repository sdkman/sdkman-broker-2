package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.None
import arrow.core.Option
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.sdkman.broker.config.AppConfig
import io.sdkman.broker.config.PersistenceBackend
import io.sdkman.broker.support.MongoTestListener
import io.sdkman.broker.support.shouldBeSome
import org.junit.jupiter.api.Tag

@Tag("integration")
class MongoConnectivityIntegrationSpec :
    ShouldSpec({
        listener(MongoTestListener)

        should("successfully connect to MongoDB and get a database instance") {
            // given
            val config =
                object : AppConfig {
                    override val mongodbHost: String = MongoTestListener.host
                    override val mongodbPort: String = MongoTestListener.port.toString()
                    override val mongodbDatabase: String = "sdkman"
                    override val mongodbUsername: Option<String> = None
                    override val mongodbPassword: Option<String> = None
                    override val mongodbAuthMechanism: Option<String> = None
                    override val postgresHost: String = "localhost"
                    override val postgresPort: String = "5432"
                    override val postgresDatabase: String = "sdkman"
                    override val postgresUsername: Option<String> = None
                    override val postgresPassword: Option<String> = None
                    override val postgresSslMode: String = "disable"
                    override val postgresPoolMaxSize: Int = 20
                    override val postgresPoolMinIdle: Int = 2
                    override val postgresPoolConnectionTimeoutMs: Long = 5_000L
                    override val postgresPoolMaxLifetimeMs: Long = 1_800_000L
                    override val postgresPoolIdleTimeoutMs: Long = 600_000L
                    override val serverPort: Int = 8080
                    override val serverHost: String = "127.0.0.1"
                    override val persistenceBackend: PersistenceBackend = PersistenceBackend.Mongo
                }
            val connectivity = MongoConnectivity(config)

            // when
            val database = connectivity.database()

            // then
            database.name shouldBe "sdkman"

            // Verify we can interact with the database
            val collections = Option.fromNullable(database.listCollectionNames().toList())
            collections.shouldBeSome()
        }
    })
