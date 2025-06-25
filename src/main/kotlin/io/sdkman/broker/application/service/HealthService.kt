package io.sdkman.broker.application.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import io.sdkman.broker.domain.model.ApplicationError
import io.sdkman.broker.domain.repository.ApplicationRepository
import io.sdkman.broker.domain.repository.HealthCheckFailure
import io.sdkman.broker.domain.repository.HealthRepository
import io.sdkman.broker.domain.repository.convertToApplicationError

interface HealthService {
    fun checkHealth(): Either<HealthCheckError, DatabaseHealthStatus>
}

class HealthServiceImpl(
    private val applicationRepository: ApplicationRepository,
    private val postgresHealthRepository: HealthRepository
) : HealthService {
    override fun checkHealth(): Either<HealthCheckError, DatabaseHealthStatus> {
        val mongoHealthResult = checkMongoHealth()
        val postgresHealthResult = checkPostgresHealth()

        val mongoStatus = mongoHealthResult.fold({ HealthStatus.DOWN }, { HealthStatus.UP })
        val postgresStatus = postgresHealthResult.fold({ HealthStatus.DOWN }, { HealthStatus.UP })

        val databaseStatus = DatabaseHealthStatus(mongoStatus, postgresStatus)

        //TODO: use accumulation of errors using zipOrAccumulate as described here:
        // https://arrow-kt.io/learn/typed-errors/validation/#fail-first-vs-accumulation
        return when {
            //TODO: Do NOT use isLeft()
            mongoHealthResult.isLeft() && postgresHealthResult.isLeft() -> {
                //TODO: Do NOT use nullable types
                val mongoError = mongoHealthResult.fold({ it }, { null })
                //TODO: Do NOT use nullable types
                val postgresError = postgresHealthResult.fold({ it }, { null })
                HealthCheckError.BothDatabasesUnavailable(mongoError, postgresError).left()
            }
            //TODO: Do NOT use isLeft()
            mongoHealthResult.isLeft() -> {
                //TODO: Do NOT use nullable types
                val mongoError = mongoHealthResult.fold({ it }, { null })
                HealthCheckError.MongoDatabaseUnavailable(mongoError).left()
            }
            //TODO: Do NOT use isLeft()
            postgresHealthResult.isLeft() -> {
                //TODO: Do NOT use nullable types
                val postgresError = postgresHealthResult.fold({ it }, { null })
                HealthCheckError.PostgresDatabaseUnavailable(postgresError).left()
            }
            else -> databaseStatus.right()
        }
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
            .mapLeft { healthCheckFailure ->
                when (healthCheckFailure) {
                    is HealthCheckFailure.ConnectionFailure ->
                        HealthCheckError.DatabaseUnavailable("PostgreSQL", healthCheckFailure.cause)
                    is HealthCheckFailure.QueryFailure ->
                        HealthCheckError.DatabaseError("PostgreSQL", healthCheckFailure.cause)
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

    data class MongoDatabaseUnavailable(val mongoError: HealthCheckError?) : HealthCheckError()

    data class PostgresDatabaseUnavailable(val postgresError: HealthCheckError?) : HealthCheckError()

    //TODO: Do NOT use nullable types. Use `Option` instead!
    data class BothDatabasesUnavailable(
        val mongoError: HealthCheckError?,
        val postgresError: HealthCheckError?
    ) : HealthCheckError()
}
