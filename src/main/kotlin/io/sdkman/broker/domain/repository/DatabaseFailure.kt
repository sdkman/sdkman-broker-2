package io.sdkman.broker.domain.repository

sealed class DatabaseFailure {
    data class ConnectionFailure(val exception: Throwable) : DatabaseFailure()

    data class QueryExecutionFailure(val exception: Throwable) : DatabaseFailure()
}
