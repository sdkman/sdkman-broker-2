package io.sdkman.broker.infra.mongo

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.sdkman.broker.domain.error.DomainError
import io.sdkman.broker.test.MongoContainer
import org.bson.Document

/**
 * Integration tests for MongoAppRepository
 */
class MongoAppRepositoryIntegrationSpec : ShouldSpec({
    val mongo = MongoContainer()
    
    // Register the test listener
    listener(mongo)
    
    should("retrieve the app record when it exists") {
        // given: a repository connected to the test MongoDB
        val repository = MongoAppRepository(mongo.database)
        
        // when: retrieving the app
        val result = repository.findApp()
        
        // then: should get the test app with status OK
        result.isRight() shouldBe true
        val app = result.getOrNull()
        app?.alive shouldBe "OK"
        app?.stableCliVersion shouldBe "5.19.0"
        app?.betaCliVersion shouldBe "latest+b8d230b"
        app?.stableNativeCliVersion shouldBe "0.7.4"
        app?.betaNativeCliVersion shouldBe "0.7.4"
    }
    
    should("return AppNotFound error when app record doesn't exist") {
        // given: a fresh database with the app collection emptied
        mongo.database.getCollection("application").deleteMany(Document())
        val repository = MongoAppRepository(mongo.database)
        
        // when: retrieving the app
        val result = repository.findApp()
        
        // then: should return a left with AppNotFound
        result.isLeft() shouldBe true
        val error = result.swap().getOrNull()
        error shouldBe DomainError.AppNotFound()
    }
}) 