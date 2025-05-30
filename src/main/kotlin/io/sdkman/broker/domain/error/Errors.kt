package io.sdkman.broker.domain.error

sealed class DomainError {
    data class EntityNotFound(val id: String) : DomainError()
    data class ValidationError(val message: String) : DomainError()
    data class SystemError(val message: String) : DomainError()
}

sealed class RepositoryError {
    data class NotFound(val id: String) : RepositoryError()
    data class DatabaseError(val cause: Throwable) : RepositoryError()
}

fun RepositoryError.toDomainError(): DomainError = when (this) {
    is RepositoryError.NotFound -> DomainError.EntityNotFound(id)
    is RepositoryError.DatabaseError -> DomainError.SystemError(cause.message ?: "Unknown database error")
}