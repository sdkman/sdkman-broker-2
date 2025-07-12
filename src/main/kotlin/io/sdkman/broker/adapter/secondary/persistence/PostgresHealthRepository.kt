package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import io.sdkman.broker.domain.repository.HealthCheckFailure
import io.sdkman.broker.domain.repository.HealthCheckSuccess
import io.sdkman.broker.domain.repository.HealthRepository
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

class PostgresHealthRepository(private val dataSource: DataSource) : HealthRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun checkConnectivity(): Either<HealthCheckFailure, HealthCheckSuccess> =
        Either.catch { dataSource.connection }
            .flatMap { executeHealthCheck(it) }
            .map { HealthCheckSuccess }
            .mapLeft { exception ->
                logger.error("PostgreSQL health check failed: {}", exception.message, exception)
                val errorMessage = exception.message.toOption().getOrElse { "" }
                when {
                    errorMessage.contains("connect", ignoreCase = true) ->
                        HealthCheckFailure.ConnectionFailure(exception)

                    else -> HealthCheckFailure.QueryFailure(exception)
                }
            }

    private fun ResultSet.isHealthCheckSuccessful(): Boolean = this.next() && this.getInt(1) == 1

    private fun executeHealthCheck(connection: Connection): Either<RuntimeException, Unit> =
        Either.catch { connection.prepareStatement("SELECT 1") }.flatMap { statement ->
            Either.catch { statement.executeQuery() }.flatMap { resultSet: ResultSet ->
                when {
                    resultSet.isHealthCheckSuccessful() -> Unit.right()
                    else -> RuntimeException("PostgreSQL health check query did not return expected result").left()
                }
            }
        }.mapLeft { RuntimeException(it) }
}
