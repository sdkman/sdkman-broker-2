package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.None
import arrow.core.Some
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.sdkman.broker.config.AppConfig
import org.junit.jupiter.api.Tag

@Tag("unit")
class MongoConnectivitySpec : ShouldSpec({

    should("generate basic connection string for localhost with no credentials") {
        // given
        val config = mockk<AppConfig>()
        every { config.mongodbHost } returns "localhost"
        every { config.mongodbPort } returns "27017"
        every { config.mongodbDatabase } returns "sdkman"
        every { config.mongodbUsername } returns None
        every { config.mongodbPassword } returns None

        val connectivity = MongoConnectivity(config)

        // when
        val connectionString = connectivity.buildConnectionString()

        // then
        connectionString shouldBe "mongodb://localhost:27017/sdkman"
    }

    should("use custom host and port when provided") {
        // given
        val config = mockk<AppConfig>()
        every { config.mongodbHost } returns "mongo.example.com"
        every { config.mongodbPort } returns "12345"
        every { config.mongodbDatabase } returns "sdkman"
        every { config.mongodbUsername } returns None
        every { config.mongodbPassword } returns None

        val connectivity = MongoConnectivity(config)

        // when
        val connectionString = connectivity.buildConnectionString()

        // then
        connectionString shouldBe "mongodb://mongo.example.com:12345/sdkman"
    }

    should("include credentials when username and password are provided") {
        // given
        val config = mockk<AppConfig>()
        every { config.mongodbHost } returns "localhost"
        every { config.mongodbPort } returns "27017"
        every { config.mongodbDatabase } returns "sdkman"
        every { config.mongodbUsername } returns Some("broker")
        every { config.mongodbPassword } returns Some("password123")

        val connectivity = MongoConnectivity(config)

        // when
        val connectionString = connectivity.buildConnectionString()

        // then
        connectionString shouldBe "mongodb://broker:password123@localhost:27017/sdkman"
    }

    should("add auth mechanism for non-localhost production environments") {
        // given
        val config = mockk<AppConfig>()
        every { config.mongodbHost } returns "mongo.sdkman.io"
        every { config.mongodbPort } returns "16434"
        every { config.mongodbDatabase } returns "sdkman"
        every { config.mongodbUsername } returns Some("broker")
        every { config.mongodbPassword } returns Some("password123")

        // Create a spy of MongoConnectivity to override the isProductionEnvironment method
        val connectivity = spyk(MongoConnectivity(config))
        every { connectivity.isProductionEnvironment(any()) } returns true

        // when
        val connectionString = connectivity.buildConnectionString()

        // then
        connectionString shouldBe "mongodb://broker:password123@mongo.sdkman.io:16434/sdkman?authMechanism=SCRAM-SHA-1"
    }
}) 
