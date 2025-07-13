package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.None
import arrow.core.Option
import arrow.core.some
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.sdkman.broker.config.AppConfig
import io.sdkman.broker.support.PostgresTestListener
import org.junit.jupiter.api.Tag

@Tag("integration")
class PostgresConnectivityIntegrationSpec : ShouldSpec({
    listener(PostgresTestListener)

    should("successfully connect to PostgreSQL and get a data source") {
        // given
        val config =
            object : AppConfig {
                override val mongodbHost: String = "localhost"
                override val mongodbPort: String = "27017"
                override val mongodbDatabase: String = "sdkman"
                override val mongodbUsername: Option<String> = None
                override val mongodbPassword: Option<String> = None
                override val postgresHost: String = PostgresTestListener.host
                override val postgresPort: String = PostgresTestListener.port.toString()
                override val postgresDatabase: String = PostgresTestListener.databaseName
                override val postgresUsername: Option<String> = PostgresTestListener.username.some()
                override val postgresPassword: Option<String> = PostgresTestListener.password.some()
                override val serverPort: Int = 8080
                override val serverHost: String = "127.0.0.1"
                override val flywayUrl: String = "jdbc:postgresql://$postgresHost:$postgresDatabase"
                override val flywayUsername: String = "postgres"
                override val flywayPassword: String = "postgres"
            }
        val connectivity = PostgresConnectivity(config)

        // when
        val dataSource = connectivity.dataSource()

        // then
        dataSource shouldNotBe null

        // Verify we can interact with the database
        dataSource.connection.use { connection ->
            connection.isValid(5) shouldBe true
        }
    }
})
