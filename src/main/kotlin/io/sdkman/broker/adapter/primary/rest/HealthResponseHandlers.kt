package io.sdkman.broker.adapter.primary.rest

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.sdkman.broker.application.service.DatabaseHealthStatus
import io.sdkman.broker.application.service.HealthCheckError
import io.sdkman.broker.application.service.HealthStatus

enum class DatabaseName(val displayName: String) {
    MONGODB("MongoDB"),
    POSTGRESQL("PostgreSQL")
}

enum class DatabaseStatus(val status: String) {
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
    val response =
        when (error) {
            is HealthCheckError.DatabaseUnavailable -> {
                createDatabaseErrorResponse(error.database, error.cause.message)
            }
            is HealthCheckError.DatabaseError -> {
                createDatabaseErrorResponse(error.database, error.cause.message)
            }
            is HealthCheckError.ApplicationNotFound -> {
                DetailedHealthResponse(
                    DatabaseStatus.DOWN.status,
                    DatabaseStatus.UP.status,
                    "Application record not found"
                )
            }
            is HealthCheckError.InvalidApplicationState -> {
                DetailedHealthResponse(
                    DatabaseStatus.DOWN.status,
                    DatabaseStatus.UP.status,
                    "Application in invalid state"
                )
            }
            is HealthCheckError.MongoDatabaseUnavailable -> {
                DetailedHealthResponse(
                    DatabaseStatus.DOWN.status,
                    DatabaseStatus.UP.status,
                    "MongoDB unavailable"
                )
            }
            is HealthCheckError.PostgresDatabaseUnavailable -> {
                DetailedHealthResponse(
                    DatabaseStatus.UP.status,
                    DatabaseStatus.DOWN.status,
                    "PostgreSQL unavailable"
                )
            }
            is HealthCheckError.BothDatabasesUnavailable -> {
                DetailedHealthResponse(
                    DatabaseStatus.DOWN.status,
                    DatabaseStatus.DOWN.status,
                    "Both databases unavailable"
                )
            }
        }
    respond(HttpStatusCode.ServiceUnavailable, response)
}

private fun createDatabaseErrorResponse(
    database: String,
    message: String?
): DetailedHealthResponse {
    val errorMessage = message ?: "Unknown error"
    return when (database) {
        DatabaseName.MONGODB.displayName ->
            DetailedHealthResponse(
                DatabaseStatus.DOWN.status,
                DatabaseStatus.UP.status,
                "${DatabaseName.MONGODB.displayName} error: $errorMessage"
            )
        DatabaseName.POSTGRESQL.displayName ->
            DetailedHealthResponse(
                DatabaseStatus.UP.status,
                DatabaseStatus.DOWN.status,
                "${DatabaseName.POSTGRESQL.displayName} error: $errorMessage"
            )
        else ->
            DetailedHealthResponse(
                DatabaseStatus.DOWN.status,
                DatabaseStatus.DOWN.status,
                "Database error: $errorMessage"
            )
    }
}
