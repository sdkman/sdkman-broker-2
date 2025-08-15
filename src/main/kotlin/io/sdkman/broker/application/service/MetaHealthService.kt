package io.sdkman.broker.application.service

import arrow.core.Either
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.some
import arrow.core.toOption
import io.sdkman.broker.domain.model.ApplicationError
import io.sdkman.broker.domain.repository.ApplicationRepository
import io.sdkman.broker.domain.repository.DatabaseFailure
import io.sdkman.broker.domain.repository.HealthRepository
import io.sdkman.broker.domain.repository.convertToApplicationError

interface MetaHealthService {
    fun checkHealth(): Either<HealthCheckError, DatabaseHealthStatus>
}

class MetaHealthServiceImpl(
    private val applicationRepository: ApplicationRepository,
    private val postgresHealthRepository: HealthRepository
) : MetaHealthService {
    override fun checkHealth(): Either<HealthCheckError, DatabaseHealthStatus> {
        val mongoHealthResult = checkMongoHealth()
        val postgresHealthResult = checkPostgresHealth()

        val mongoStatus = mongoHealthResult.fold({ HealthStatus.DOWN }, { HealthStatus.UP })
        val postgresStatus = postgresHealthResult.fold({ HealthStatus.DOWN }, { HealthStatus.UP })

        val databaseStatus = DatabaseHealthStatus(mongoStatus, postgresStatus)

        return mongoHealthResult.fold(
            { mongoError ->
                postgresHealthResult.fold(
                    { postgresError ->
                        HealthCheckError.BothDatabasesUnavailable(mongoError.some(), postgresError.some()).left()
                    },
                    {
                        HealthCheckError.MongoDatabaseUnavailable(mongoError.some()).left()
                    }
                )
            },
            {
                postgresHealthResult.fold(
                    { postgresError ->
                        HealthCheckError.PostgresDatabaseUnavailable(postgresError.some()).left()
                    },
                    {
                        databaseStatus.right()
                    }
                )
            }
        )
    }

    private fun checkMongoHealth(): Either<HealthCheckError, Unit> =
        applicationRepository.findApplication()
            .convertToApplicationError()
            .mapLeft { domainError ->
                when (domainError) {
                    is ApplicationError.SystemError -> {
                        val throwable = domainError.cause
                        val errorMessage = throwable.message.toOption().getOrElse { "" }

                        if (errorMessage.contains("connection") || errorMessage.contains("connect")) {
                            HealthCheckError.DatabaseUnavailable("MongoDB", throwable)
                        } else {
                            HealthCheckError.DatabaseError("MongoDB", throwable)
                        }
                    }
                    is ApplicationError.ApplicationNotFound -> HealthCheckError.ApplicationNotFound
                    is ApplicationError.InvalidAliveStatus -> HealthCheckError.InvalidApplicationState(domainError)
                }
            }
            .flatMap { applicationOption ->
                applicationOption.fold(
                    { HealthCheckError.ApplicationNotFound.left() },
                    { Unit.right() }
                )
            }

    private fun checkPostgresHealth(): Either<HealthCheckError, Unit> =
        postgresHealthRepository.checkConnectivity()
            .mapLeft { databaseFailure ->
                when (databaseFailure) {
                    is DatabaseFailure.ConnectionFailure ->
                        HealthCheckError.DatabaseUnavailable("PostgreSQL", databaseFailure.exception)
                    is DatabaseFailure.QueryExecutionFailure ->
                        HealthCheckError.DatabaseError("PostgreSQL", databaseFailure.exception)
                }
            }
            .map { Unit }
}

enum class HealthStatus {
    UP,
    DOWN
}

data class DatabaseHealthStatus(
    val mongodb: HealthStatus,
    val postgres: HealthStatus
)

sealed class HealthCheckError {
    data class DatabaseUnavailable(val database: String, val cause: Throwable) : HealthCheckError()

    data class DatabaseError(val database: String, val cause: Throwable) : HealthCheckError()

    data object ApplicationNotFound : HealthCheckError()

    data class InvalidApplicationState(val error: ApplicationError) : HealthCheckError()

    data class MongoDatabaseUnavailable(val mongoError: Option<HealthCheckError>) : HealthCheckError()

    data class PostgresDatabaseUnavailable(val postgresError: Option<HealthCheckError>) : HealthCheckError()

    data class BothDatabasesUnavailable(
        val mongoError: Option<HealthCheckError>,
        val postgresError: Option<HealthCheckError>
    ) : HealthCheckError()
}
