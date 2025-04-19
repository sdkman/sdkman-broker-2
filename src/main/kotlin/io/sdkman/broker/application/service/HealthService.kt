package io.sdkman.broker.application.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.sdkman.broker.domain.error.DomainError
import io.sdkman.broker.domain.repository.AppRepository

/**
 * Service for checking the application health status
 */
class HealthService(private val appRepository: AppRepository) {

    /**
     * Checks if the application is healthy by verifying
     * that the App record exists and has "OK" as alive status
     * 
     * @return Either a domain error or "UP" status string
     */
    fun checkHealth(): Either<DomainError, String> =
        appRepository.findApp().flatMap { app ->
            if (app.alive == "OK") {
                "UP".right()
            } else {
                DomainError.AppNotHealthy().left()
            }
        }
} 