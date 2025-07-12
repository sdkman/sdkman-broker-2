package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.sdkman.broker.domain.repository.HealthCheckFailure
import io.sdkman.broker.domain.repository.HealthCheckSuccess
import io.sdkman.broker.domain.repository.HealthRepository
import org.slf4j.LoggerFactory
import java.sql.Connection
import javax.sql.DataSource

// TODO: Do NOT resort to the `use` combinator. Prefer Arrow's `Option` and `Either` combinators wherever possible.
class PostgresHealthRepository(private val dataSource: DataSource) : HealthRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun checkConnectivity(): Either<HealthCheckFailure, HealthCheckSuccess> =
        Either.catch {
            dataSource.connection.use { connection ->
                executeHealthCheck(connection)
            }
        }.fold(
            { exception ->
                logger.error("PostgreSQL health check failed: ${exception.message}", exception)
                when {
                    // TODO: Do NOT use nullable types. Use `Option` instead
                    exception.message?.contains("connect", ignoreCase = true) == true ||
                        // TODO: Do NOT use nullable types. Use `Option` instead
                        exception.message?.contains("connection", ignoreCase = true) == true ->
                        HealthCheckFailure.ConnectionFailure(exception).left()
                    else -> HealthCheckFailure.QueryFailure(exception).left()
                }
            },
            { HealthCheckSuccess.right() }
        )

    private fun executeHealthCheck(connection: Connection) {
        connection.prepareStatement("SELECT 1").use { statement ->
            statement.executeQuery().use { resultSet ->
                // TODO: Add a well-named extension method to ResultSet that hides these operations
                if (!resultSet.next() || resultSet.getInt(1) != 1) {
                    // TODO: do NOT rely on throwing exceptions. Change the method signature of `executeHealthCheck`
                    // to instead return `Either`
                    throw RuntimeException("PostgreSQL health check query did not return expected result")
                }
            }
        }
    }
}
