package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import arrow.core.getOrElse
import io.sdkman.broker.domain.repository.DatabaseFailure
import io.sdkman.broker.domain.repository.HealthCheckSuccess
import io.sdkman.broker.domain.repository.HealthRepository
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.ResultSet
import javax.sql.DataSource

class PostgresHealthRepository(private val dataSource: DataSource) : HealthRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun checkConnectivity(): Either<DatabaseFailure, HealthCheckSuccess> =
        Either.catch {
            dataSource.connection.use { connection ->
                executeHealthCheck(connection).getOrElse { throw it }
            }
        }
            .map { HealthCheckSuccess }
            .mapLeft { exception ->
                logger.error("PostgreSQL health check failed: {}", exception.message, exception)
                exception.toDatabaseFailure()
            }

    private fun ResultSet.isHealthCheckSuccessful(): Boolean = this.next() && this.getInt(1) == 1

    private fun executeHealthCheck(connection: Connection): Either<RuntimeException, Unit> =
        Either.catch {
            connection.prepareStatement("SELECT 1").use { statement ->
                statement.executeQuery().use { resultSet ->
                    when {
                        resultSet.isHealthCheckSuccessful() -> Unit
                        else -> throw RuntimeException("PostgreSQL health check query did not return expected result")
                    }
                }
            }
        }.mapLeft { RuntimeException(it) }
}
