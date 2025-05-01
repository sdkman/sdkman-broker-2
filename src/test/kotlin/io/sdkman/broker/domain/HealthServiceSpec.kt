package io.sdkman.broker.domain

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.right
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.every
import io.mockk.mockk
import io.sdkman.broker.application.service.HealthCheckError
import io.sdkman.broker.application.service.HealthServiceImpl
import io.sdkman.broker.application.service.HealthStatus
import io.sdkman.broker.domain.model.Application
import io.sdkman.broker.domain.repository.ApplicationRepository
import io.sdkman.broker.domain.repository.RepositoryError
import io.sdkman.broker.test.shouldBeLeft
import io.sdkman.broker.test.shouldBeLeftAnd
import io.sdkman.broker.test.shouldBeRight

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
        val mockRepo = mockk<ApplicationRepository>()
        every { mockRepo.findApplication() } returns Some(applicationOK).right()
        val service = HealthServiceImpl(mockRepo)
        
        // when
        val result = service.checkHealth()
        
        // then
        result shouldBeRight HealthStatus.UP
    }
    
    should("return ApplicationNotFound error when application record doesn't exist") {
        // given
        val mockRepo = mockk<ApplicationRepository>()
        every { mockRepo.findApplication() } returns None.right()
        val service = HealthServiceImpl(mockRepo)
        
        // when
        val result = service.checkHealth()
        
        // then
        result shouldBeLeft HealthCheckError.ApplicationNotFound
    }
    
    should("return DatabaseError when repository encounters a database error") {
        // given
        val dbError = RepositoryError.DatabaseError(RuntimeException("DB error"))
        val mockRepo = mockk<ApplicationRepository>()
        every { mockRepo.findApplication() } returns Either.Left(dbError)
        val service = HealthServiceImpl(mockRepo)
        
        // when
        val result = service.checkHealth()
        
        // then
        result shouldBeLeftAnd { it is HealthCheckError.DatabaseError }
    }
}) 