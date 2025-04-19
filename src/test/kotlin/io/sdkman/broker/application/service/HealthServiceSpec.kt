package io.sdkman.broker.application.service

import arrow.core.left
import arrow.core.right
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.sdkman.broker.domain.error.DomainError
import io.sdkman.broker.domain.model.App
import io.sdkman.broker.domain.repository.AppRepository

/**
 * Unit tests for the HealthService
 */
class HealthServiceSpec : ShouldSpec({
    
    should("return UP when app status is OK") {
        // given: a repository returning a healthy app
        val appRepository = StubAppRepository(
            App(
                alive = "OK",
                stableCliVersion = "5.19.0",
                betaCliVersion = "latest+123",
                stableNativeCliVersion = "0.7.4",
                betaNativeCliVersion = "0.7.4"
            ).right()
        )
        val healthService = HealthService(appRepository)
        
        // when: checking health
        val result = healthService.checkHealth()
        
        // then: should be right with UP status
        result.isRight() shouldBe true
        result.getOrNull() shouldBe "UP"
    }
    
    should("return AppNotHealthy error when app status is not OK") {
        // given: a repository returning an unhealthy app
        val appRepository = StubAppRepository(
            App(
                alive = "NOT_OK",
                stableCliVersion = "5.19.0",
                betaCliVersion = "latest+123",
                stableNativeCliVersion = "0.7.4",
                betaNativeCliVersion = "0.7.4"
            ).right()
        )
        val healthService = HealthService(appRepository)
        
        // when: checking health
        val result = healthService.checkHealth()
        
        // then: should be left with AppNotHealthy error
        result.isLeft() shouldBe true
        result.swap().getOrNull() shouldBe DomainError.AppNotHealthy()
    }
    
    should("return RepositoryRrror when findApp fails") {
        // given: a repository returning an error
        val error = DomainError.RepositoryError(RuntimeException("DB error"))
        val appRepository = StubAppRepository(error.left())
        val healthService = HealthService(appRepository)
        
        // when: checking health
        val result = healthService.checkHealth()
        
        // then: should be left with the repository error
        result.isLeft() shouldBe true
        result.swap().getOrNull() shouldBe error
    }
})

/**
 * Test double that returns a predefined result
 */
class StubAppRepository(private val result: arrow.core.Either<DomainError, App>) : AppRepository {
    override fun findApp() = result
} 