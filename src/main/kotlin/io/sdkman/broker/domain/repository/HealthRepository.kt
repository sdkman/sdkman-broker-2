package io.sdkman.broker.domain.repository

import arrow.core.Either

interface HealthRepository {
    fun checkConnectivity(): Either<DatabaseFailure, HealthCheckSuccess>
}

data object HealthCheckSuccess
