package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.none
import arrow.core.some
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.sdkman.broker.config.AppConfig

class PostgresConnectivitySpec : ShouldSpec({

    should("build basic connection string for localhost without credentials") {
        // given
        val mockConfig = mockk<AppConfig>()
        every { mockConfig.postgresHost } returns "localhost"
        every { mockConfig.postgresPort } returns "5432"
        every { mockConfig.postgresDatabase } returns "sdkman"
        every { mockConfig.postgresUsername } returns none()
        every { mockConfig.postgresPassword } returns none()

        val connectivity = PostgresConnectivity(mockConfig)

        // when
        val result = connectivity.buildConnectionString()

        // then
        result shouldBe "jdbc:postgresql://localhost:5432/sdkman"
    }

    should("build connection string with credentials for localhost") {
        // given
        val mockConfig = mockk<AppConfig>()
        every { mockConfig.postgresHost } returns "localhost"
        every { mockConfig.postgresPort } returns "5432"
        every { mockConfig.postgresDatabase } returns "sdkman"
        every { mockConfig.postgresUsername } returns "broker".some()
        every { mockConfig.postgresPassword } returns "password123".some()

        val connectivity = PostgresConnectivity(mockConfig)

        // when
        val result = connectivity.buildConnectionString()

        // then
        result shouldBe "jdbc:postgresql://broker:password123@localhost:5432/sdkman"
    }

    should("build connection string with credentials for production host") {
        // given
        val mockConfig = mockk<AppConfig>()
        every { mockConfig.postgresHost } returns "postgres.sdkman.io"
        every { mockConfig.postgresPort } returns "5432"
        every { mockConfig.postgresDatabase } returns "sdkman"
        every { mockConfig.postgresUsername } returns "broker".some()
        every { mockConfig.postgresPassword } returns "password123".some()

        val connectivity = PostgresConnectivity(mockConfig)

        // when
        val result = connectivity.buildConnectionString()

        // then
        result shouldBe "jdbc:postgresql://broker:password123@postgres.sdkman.io:5432/sdkman?sslmode=require"
    }

    should("build connection string with custom port") {
        // given
        val mockConfig = mockk<AppConfig>()
        every { mockConfig.postgresHost } returns "localhost"
        every { mockConfig.postgresPort } returns "5433"
        every { mockConfig.postgresDatabase } returns "sdkman"
        every { mockConfig.postgresUsername } returns none()
        every { mockConfig.postgresPassword } returns none()

        val connectivity = PostgresConnectivity(mockConfig)

        // when
        val result = connectivity.buildConnectionString()

        // then
        result shouldBe "jdbc:postgresql://localhost:5433/sdkman"
    }

    should("build connection string with custom database name") {
        // given
        val mockConfig = mockk<AppConfig>()
        every { mockConfig.postgresHost } returns "localhost"
        every { mockConfig.postgresPort } returns "5432"
        every { mockConfig.postgresDatabase } returns "custom_db"
        every { mockConfig.postgresUsername } returns none()
        every { mockConfig.postgresPassword } returns none()

        val connectivity = PostgresConnectivity(mockConfig)

        // when
        val result = connectivity.buildConnectionString()

        // then
        result shouldBe "jdbc:postgresql://localhost:5432/custom_db"
    }

    should("identify localhost as development environment") {
        // given
        val mockConfig = mockk<AppConfig>()
        val connectivity = PostgresConnectivity(mockConfig)

        // when & then
        connectivity.isProductionEnvironment("localhost") shouldBe false
        connectivity.isProductionEnvironment("127.0.0.1") shouldBe false
    }

    should("identify remote hosts as production environment") {
        // given
        val mockConfig = mockk<AppConfig>()
        val connectivity = PostgresConnectivity(mockConfig)

        // when & then
        connectivity.isProductionEnvironment("postgres.sdkman.io") shouldBe true
        connectivity.isProductionEnvironment("10.0.0.1") shouldBe true
        connectivity.isProductionEnvironment("production.db.com") shouldBe true
    }
})
