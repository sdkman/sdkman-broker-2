package io.sdkman.broker.adapter.primary.rest

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.toOption
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.sdkman.broker.application.service.DatabaseHealthStatus
import io.sdkman.broker.application.service.HealthCheckError
import io.sdkman.broker.application.service.HealthStatus
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("io.sdkman.broker.adapter.primary.rest.HealthResponseHandlers")

enum class DatabaseName(
    val displayName: String
) {
    MONGODB("MongoDB"),
    POSTGRESQL("PostgreSQL")
}

enum class DatabaseStatus(
    val status: String
) {
    UP("UP"),
    DOWN("DOWN")
}

suspend fun ApplicationCall.handleDatabaseHealthStatus(databaseStatus: DatabaseHealthStatus) {
    val mongoDbStatus =
        when (databaseStatus.mongodb) {
            HealthStatus.UP -> DatabaseStatus.UP.status
            HealthStatus.DOWN -> DatabaseStatus.DOWN.status
        }

    val postgresStatus =
        when (databaseStatus.postgres) {
            HealthStatus.UP -> DatabaseStatus.UP.status
            HealthStatus.DOWN -> DatabaseStatus.DOWN.status
        }

    val overallHealthy = databaseStatus.mongodb == HealthStatus.UP && databaseStatus.postgres == HealthStatus.UP
    val statusCode = if (overallHealthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable

    respond(statusCode, DetailedHealthResponse(mongoDbStatus, postgresStatus))
}

suspend fun ApplicationCall.handleHealthError(error: HealthCheckError) {
    logHealthCheckError(error)
    val response =
        when (error) {
            is HealthCheckError.DatabaseUnavailable -> {
                createDatabaseErrorResponse(error.database, error.cause.message.toOption())
            }
            is HealthCheckError.DatabaseError -> {
                createDatabaseErrorResponse(error.database, error.cause.message.toOption())
            }
            is HealthCheckError.ApplicationNotFound -> {
                DetailedHealthErrorResponse(
                    DatabaseStatus.DOWN.status,
                    DatabaseStatus.UP.status,
                    "Application record not found"
                )
            }
            is HealthCheckError.InvalidApplicationState -> {
                DetailedHealthErrorResponse(
                    DatabaseStatus.DOWN.status,
                    DatabaseStatus.UP.status,
                    "Application in invalid state"
                )
            }
            is HealthCheckError.MongoDatabaseUnavailable -> {
                DetailedHealthErrorResponse(
                    DatabaseStatus.DOWN.status,
                    DatabaseStatus.UP.status,
                    "MongoDB unavailable"
                )
            }
            is HealthCheckError.PostgresDatabaseUnavailable -> {
                DetailedHealthErrorResponse(
                    DatabaseStatus.UP.status,
                    DatabaseStatus.DOWN.status,
                    "PostgreSQL unavailable"
                )
            }
            is HealthCheckError.BothDatabasesUnavailable -> {
                DetailedHealthErrorResponse(
                    DatabaseStatus.DOWN.status,
                    DatabaseStatus.DOWN.status,
                    "Both databases unavailable"
                )
            }
        }
    respond(HttpStatusCode.ServiceUnavailable, response)
}

// A failed health check is a 503 server fault, so it logs at ERROR (BR-3); the cause-carrying
// variants pass their Throwable so the stack trace reaches the logs.
private fun logHealthCheckError(error: HealthCheckError) =
    when (error) {
        is HealthCheckError.DatabaseUnavailable ->
            logger.error("Health check failed: ${error.database} unavailable", error.cause)
        is HealthCheckError.DatabaseError ->
            logger.error("Health check failed: ${error.database} error", error.cause)
        is HealthCheckError.ApplicationNotFound ->
            logger.error("Health check failed: application record not found")
        is HealthCheckError.InvalidApplicationState ->
            logger.error("Health check failed: application in invalid state")
        is HealthCheckError.MongoDatabaseUnavailable ->
            logger.error("Health check failed: MongoDB unavailable")
        is HealthCheckError.PostgresDatabaseUnavailable ->
            logger.error("Health check failed: PostgreSQL unavailable")
        is HealthCheckError.BothDatabasesUnavailable ->
            logger.error("Health check failed: both databases unavailable")
    }

private fun createDatabaseErrorResponse(
    database: String,
    message: Option<String>
): DetailedHealthErrorResponse {
    val errorMessage = message.getOrElse { "Unknown error" }
    return when (database) {
        DatabaseName.MONGODB.displayName ->
            DetailedHealthErrorResponse(
                DatabaseStatus.DOWN.status,
                DatabaseStatus.UP.status,
                "${DatabaseName.MONGODB.displayName} error: $errorMessage"
            )
        DatabaseName.POSTGRESQL.displayName ->
            DetailedHealthErrorResponse(
                DatabaseStatus.UP.status,
                DatabaseStatus.DOWN.status,
                "${DatabaseName.POSTGRESQL.displayName} error: $errorMessage"
            )
        else ->
            DetailedHealthErrorResponse(
                DatabaseStatus.DOWN.status,
                DatabaseStatus.DOWN.status,
                "Database error: $errorMessage"
            )
    }
}

@Serializable
data class DetailedHealthResponse(
    val mongodb: String,
    val postgres: String
)

@Serializable
data class DetailedHealthErrorResponse(
    val mongodb: String,
    val postgres: String,
    val reason: String
)
