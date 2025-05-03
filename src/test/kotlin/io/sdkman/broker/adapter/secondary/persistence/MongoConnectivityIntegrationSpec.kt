package io.sdkman.broker.adapter.secondary.persistence

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.sdkman.broker.config.AppConfig
import io.sdkman.broker.support.MongoTestListener
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
        //TODO: nothing should be null, use arrow.core.Option if needed
        database shouldNotBe null
        database.name shouldBe "sdkman"
        
        // Verify we can interact with the database
        val collections = database.listCollectionNames().toList()
        collections shouldNotBe null
    }
}) 