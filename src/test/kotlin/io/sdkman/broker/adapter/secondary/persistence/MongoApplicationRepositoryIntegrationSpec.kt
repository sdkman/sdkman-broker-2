package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.None
import arrow.core.Some
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.sdkman.broker.domain.model.Application
import io.sdkman.broker.domain.repository.RepositoryError
import io.sdkman.broker.test.MongoTestListener

/**
 * Integration test for the MongoDB implementation of ApplicationRepository.
 */
class MongoApplicationRepositoryIntegrationSpec : ShouldSpec({
    listener(MongoTestListener)
    
    val repository = MongoApplicationRepository(MongoTestListener.database)
    
    should("return application when record exists with valid alive status") {
        // given
        MongoTestListener.setupValidAppRecord()
        
        // when
        val result = repository.findApplication()
        
        // then
        result.isRight() shouldBe true
        result.fold(
            { error -> throw RuntimeException("Repository error: $error") },
            { applicationOption ->
                Application.of("OK").fold(
                    { error -> throw RuntimeException("Failed to create test application: $error") },
                    { expectedApp -> applicationOption shouldBe Some(expectedApp) }
                )
            }
        )
    }
    
    should("return None when application record does not exist") {
        // given: empty database (MongoTestListener.resetDatabase() is called automatically)
        
        // when
        val result = repository.findApplication()
        
        // then
        result.isRight() shouldBe true
        result.fold(
            { error -> throw RuntimeException("Repository error: $error") },
            { applicationOption ->
                applicationOption shouldBe None
            }
        )
    }
    
    should("return an error when application record has invalid alive status") {
        // given
        MongoTestListener.setupInvalidAppRecord()
        
        // when
        val result = repository.findApplication()
        
        // then
        //TODO: Write Either matchers for Kotest
        result.isLeft() shouldBe true
    }
})