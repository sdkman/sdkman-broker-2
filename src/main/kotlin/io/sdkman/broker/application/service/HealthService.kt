package io.sdkman.broker.application.service

import arrow.core.Either
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.toOption
import io.sdkman.broker.domain.model.ApplicationError
import io.sdkman.broker.domain.repository.ApplicationRepository
import io.sdkman.broker.domain.repository.RepositoryError
import io.sdkman.broker.domain.repository.convertToApplicationError

interface HealthService {
    fun checkHealth(): Either<HealthCheckError, HealthStatus>
}

class HealthServiceImpl(private val repository: ApplicationRepository) : HealthService {
    override fun checkHealth(): Either<HealthCheckError, HealthStatus> =
        repository.findApplication()
            .convertToApplicationError()
            .mapLeft { domainError ->
                when (domainError) {
                    is ApplicationError.SystemError -> {
                        val throwable = domainError.cause
                        val errorMessage = throwable.message.toOption().getOrElse { "" }
                        
                        if (errorMessage.contains("connection") || errorMessage.contains("connect")) {
                            HealthCheckError.DatabaseUnavailable(throwable)
                        } else {
                            HealthCheckError.DatabaseError(throwable)
                        }
                    }
                    is ApplicationError.ApplicationNotFound -> HealthCheckError.ApplicationNotFound
                    is ApplicationError.InvalidAliveStatus -> HealthCheckError.InvalidApplicationState(domainError)
                }
            }
            .flatMap { applicationOption ->
                applicationOption.fold(
                    { HealthCheckError.ApplicationNotFound.left() },
                    { _ -> HealthStatus.UP.right() }
                )
            }
}

enum class HealthStatus {
    UP, DOWN
}

sealed class HealthCheckError {
    data class DatabaseUnavailable(val cause: Throwable) : HealthCheckError()
    data class DatabaseError(val cause: Throwable) : HealthCheckError()
    data object ApplicationNotFound : HealthCheckError()
    data class InvalidApplicationState(val error: ApplicationError) : HealthCheckError()
} 
