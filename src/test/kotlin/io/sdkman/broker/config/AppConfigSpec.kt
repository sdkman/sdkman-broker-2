package io.sdkman.broker.config

import arrow.core.Option
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe

class AppConfigSpec : ShouldSpec({

    should("read database name from configuration") {
        // given
        val config = AppConfig()

        // when/then
        config.mongodbDatabase shouldBe "sdkman"
    }

    should("read MongoDB host from configuration") {
        // given
        val config = AppConfig()

        // when/then
        config.mongodbHost shouldBe "127.0.0.1"
    }

    should("read MongoDB port from configuration") {
        // given
        val config = AppConfig()

        // when/then
        config.mongodbPort shouldBe "27017"
    }

    should("handle null values for credentials") {
        // given
        val config = AppConfig()

        // when/then
        config.mongodbUsername shouldBe Option.fromNullable(null)
        config.mongodbPassword shouldBe Option.fromNullable(null)
    }

    should("read server settings from configuration") {
        // given
        val config = AppConfig()

        // when/then
        config.serverPort shouldBe 8080
        config.serverHost shouldBe "0.0.0.0"
    }
})
