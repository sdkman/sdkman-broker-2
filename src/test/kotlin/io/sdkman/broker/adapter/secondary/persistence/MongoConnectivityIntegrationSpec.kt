package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Option
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.sdkman.broker.config.AppConfig
import io.sdkman.broker.support.MongoTestListener
import io.sdkman.broker.support.shouldBeSome
import org.junit.jupiter.api.Tag

@Tag("integration")
class MongoConnectivityIntegrationSpec : ShouldSpec({
    listener(MongoTestListener)

    should("successfully connect to MongoDB and get a database instance") {
        // given
        val config = AppConfig()
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
