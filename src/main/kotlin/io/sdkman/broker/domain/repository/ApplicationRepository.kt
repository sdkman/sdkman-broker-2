package io.sdkman.broker.domain.repository

import arrow.core.Either
import arrow.core.Option
import io.sdkman.broker.domain.model.Application
import io.sdkman.broker.domain.model.ApplicationError

interface ApplicationRepository {
    fun findApplication(): Either<RepositoryError, Option<Application>>
}

sealed class RepositoryError {
    data class ConnectionError(val cause: Throwable) : RepositoryError()
    data class DatabaseError(val cause: Throwable) : RepositoryError()
}

fun RepositoryError.toApplicationError(): ApplicationError = ApplicationError.SystemError(
    when (this) {
        is RepositoryError.ConnectionError -> cause
        is RepositoryError.DatabaseError -> cause
    }
)

fun <T> Either<RepositoryError, T>.convertToApplicationError(): Either<ApplicationError, T> =
    mapLeft { repoError -> repoError.toApplicationError() } 
