package io.sdkman.broker.domain.repository

import arrow.core.Either
import arrow.core.Option
import io.sdkman.broker.domain.model.Application
import io.sdkman.broker.domain.model.ApplicationError

/**
 * Repository interface (port) for accessing application data.
 * This is a secondary port in the hexagonal architecture.
 */
interface ApplicationRepository {
    /**
     * Fetches the application record.
     * Returns an Option wrapping the Application if found, otherwise None.
     */
    fun findApplication(): Either<RepositoryError, Option<Application>>
}

/**
 * Repository errors that can occur when accessing the data store.
 */
sealed class RepositoryError {
    data class ConnectionError(val cause: Throwable) : RepositoryError()
    data class DatabaseError(val cause: Throwable) : RepositoryError()
}

/**
 * Maps repository errors to domain application errors.
 * This ensures proper error translation at the boundary between layers.
 */
fun RepositoryError.toApplicationError(): ApplicationError = ApplicationError.SystemError(
    when (this) {
        is RepositoryError.ConnectionError -> cause
        is RepositoryError.DatabaseError -> cause
    }
)

/**
 * Maps Either<RepositoryError, T> to Either<ApplicationError, T>.
 * Converts repository errors to application errors while preserving the success path.
 */
fun <T> Either<RepositoryError, T>.convertToApplicationError(): Either<ApplicationError, T> =
    mapLeft { repoError -> repoError.toApplicationError() } 