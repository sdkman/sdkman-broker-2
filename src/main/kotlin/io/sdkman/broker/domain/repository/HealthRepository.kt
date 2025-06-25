package io.sdkman.broker.domain.repository

import arrow.core.Either

interface HealthRepository {
    fun checkConnectivity(): Either<HealthCheckFailure, HealthCheckSuccess>
}

sealed class HealthCheckFailure {
    data class ConnectionFailure(val cause: Throwable) : HealthCheckFailure()

    data class QueryFailure(val cause: Throwable) : HealthCheckFailure()
}

data object HealthCheckSuccess
