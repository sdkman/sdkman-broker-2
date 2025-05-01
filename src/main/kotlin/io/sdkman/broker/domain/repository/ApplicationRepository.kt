package io.sdkman.broker.domain.repository

import arrow.core.Either
import arrow.core.Option
import io.sdkman.broker.domain.model.Application

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