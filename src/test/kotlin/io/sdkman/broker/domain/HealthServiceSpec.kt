package io.sdkman.broker.domain

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.right
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.sdkman.broker.application.service.HealthCheckError
import io.sdkman.broker.application.service.HealthServiceImpl
import io.sdkman.broker.application.service.HealthStatus
import io.sdkman.broker.domain.model.Application
import io.sdkman.broker.domain.repository.ApplicationRepository
import io.sdkman.broker.domain.repository.RepositoryError

/**
 * Unit test for the HealthService implementation.
 */
class HealthServiceSpec : ShouldSpec({
    val applicationOK = Application.of("OK").fold(
        { error -> throw IllegalStateException("Failed to create test application: $error") },
        { it }
    )
    
    should("return UP status when application record exists and is valid") {
        // given
        val mockRepo = MockApplicationRepository(Some(applicationOK).right())
        val service = HealthServiceImpl(mockRepo)
        
        // when
        val result = service.checkHealth()
        
        // then
        //TODO: Write Either matchers for Kotest
        result.isRight() shouldBe true
        result.fold(
            { error -> throw IllegalStateException("Unexpected error: $error") },
            { status -> status shouldBe HealthStatus.UP }
        )
    }
    
    should("return ApplicationNotFound error when application record doesn't exist") {
        // given
        val mockRepo = MockApplicationRepository(None.right())
        val service = HealthServiceImpl(mockRepo)
        
        // when
        val result = service.checkHealth()
        
        // then
        //TODO: Write Either matchers for Kotest
        result.isLeft() shouldBe true
        result.fold(
            { error -> error shouldBe HealthCheckError.ApplicationNotFound },
            { throw AssertionError("Expected Left but got Right with $it") }
        )
    }
    
    should("return DatabaseError when repository encounters a database error") {
        // given
        val dbError = RepositoryError.DatabaseError(RuntimeException("DB error"))
        val mockRepo = MockApplicationRepository(Either.Left(dbError))
        val service = HealthServiceImpl(mockRepo)
        
        // when
        val result = service.checkHealth()
        
        // then
        //TODO: Write Either matchers for Kotest
        result.isLeft() shouldBe true
        result.fold(
            { error -> error::class shouldBe HealthCheckError.DatabaseError::class },
            { throw AssertionError("Expected Left but got Right with $it") }
        )
    }
})

/**
 * Mock implementation of ApplicationRepository for testing.
 */
//TODO: Use Mockk instead
class MockApplicationRepository(
    private val returnValue: Either<RepositoryError, Option<Application>>
) : ApplicationRepository {
    override fun findApplication(): Either<RepositoryError, Option<Application>> = returnValue
} 