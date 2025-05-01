package io.sdkman.broker.application.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.core.flatMap
import io.sdkman.broker.domain.model.ApplicationError
import io.sdkman.broker.domain.repository.ApplicationRepository
import io.sdkman.broker.domain.repository.RepositoryError

/**
 * Health service interface - primary port
 */
interface HealthService {
    /**
     * Checks if the application is alive by verifying
     * the database connection and application record state.
     */
    fun checkHealth(): Either<HealthCheckError, HealthStatus>
}

/**
 * Implementation of the HealthService.
 * Delegates to the repository to fetch application data
 * and verifies the application is in a healthy state.
 */
class HealthServiceImpl(private val repository: ApplicationRepository) : HealthService {
    override fun checkHealth(): Either<HealthCheckError, HealthStatus> =
        repository.findApplication()
            .mapLeft { error ->
                when (error) {
                    is RepositoryError.ConnectionError -> HealthCheckError.DatabaseUnavailable(error.cause)
                    is RepositoryError.DatabaseError -> HealthCheckError.DatabaseError(error.cause)
                }
            }
            .flatMap { applicationOption ->
                applicationOption.fold(
                    { HealthCheckError.ApplicationNotFound.left() },
                    { _ -> HealthStatus.UP.right() }
                )
            }
}

/**
 * Health status indicating whether the application is healthy.
 */
enum class HealthStatus {
    UP, DOWN
}

/**
 * Errors that can occur during health check.
 */
sealed class HealthCheckError {
    data class DatabaseUnavailable(val cause: Throwable) : HealthCheckError()
    data class DatabaseError(val cause: Throwable) : HealthCheckError()
    data object ApplicationNotFound : HealthCheckError()
    data class InvalidApplicationState(val error: ApplicationError) : HealthCheckError()
} 