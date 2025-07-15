package io.sdkman.broker.domain.repository

sealed class DatabaseFailure {
    abstract val exception: Throwable

    data class ConnectionFailure(override val exception: Throwable) : DatabaseFailure()

    data class QueryExecutionFailure(override val exception: Throwable) : DatabaseFailure()
}
