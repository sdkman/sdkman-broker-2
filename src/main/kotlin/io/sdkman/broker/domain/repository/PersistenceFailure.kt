package io.sdkman.broker.domain.repository

sealed class PersistenceFailure {
    data class DatabaseConnectionFailure(val exception: Throwable) : PersistenceFailure()
    data class QueryExecutionFailure(val exception: Throwable) : PersistenceFailure()
}