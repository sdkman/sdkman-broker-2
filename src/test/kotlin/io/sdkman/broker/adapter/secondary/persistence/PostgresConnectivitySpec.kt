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

    should("not include credentials in URL for localhost connections") {
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
        result shouldBe "jdbc:postgresql://localhost:5432/sdkman"
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
        result shouldBe
            "jdbc:postgresql://postgres.sdkman.io:5432/sdkman?user=broker&password=password123&sslmode=require"
    }
})
