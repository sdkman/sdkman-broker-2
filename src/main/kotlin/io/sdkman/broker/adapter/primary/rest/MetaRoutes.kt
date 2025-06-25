package io.sdkman.broker.adapter.primary.rest

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.sdkman.broker.application.service.DatabaseHealthStatus
import io.sdkman.broker.application.service.HealthCheckError
import io.sdkman.broker.application.service.HealthService
import io.sdkman.broker.application.service.HealthStatus
import io.sdkman.broker.application.service.ReleaseError
import io.sdkman.broker.application.service.ReleaseService
import kotlinx.serialization.Serializable

fun Application.metaRoutes(
    healthService: HealthService,
    releaseService: ReleaseService
) {
    routing {
        get("/meta/health") {
            healthService.checkHealth()
                .fold(
                    { error -> call.handleHealthError(error) },
                    { databaseStatus -> call.handleDatabaseHealthStatus(databaseStatus) }
                )
        }

        get("/meta/release") {
            releaseService.getRelease()
                .fold(
                    { error ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ReleaseErrorResponse("Error retrieving release: ${error.cause.message}")
                        )
                    },
                    { release ->
                        call.respond(HttpStatusCode.OK, ReleaseResponse(release))
                    }
                )
        }
    }
}

//TODO: Move this out of the MetaRoutes into a new file
private suspend fun ApplicationCall.handleDatabaseHealthStatus(databaseStatus: DatabaseHealthStatus) {
    val mongoDbStatus = databaseStatus.mongodb.name
    val postgresStatus = databaseStatus.postgres.name

    val overallHealthy = databaseStatus.mongodb == HealthStatus.UP && databaseStatus.postgres == HealthStatus.UP
    val statusCode = if (overallHealthy) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable

    respond(statusCode, DetailedHealthResponse(mongoDbStatus, postgresStatus))
}

//TODO: Move this out of the MetaRoutes into a new file
private suspend fun ApplicationCall.handleHealthError(error: HealthCheckError) {
    val response =
        //TODO: This code relies and magic strings to represent database names and statuses. Use enums where possible
        when (error) {
            is HealthCheckError.DatabaseUnavailable -> {
                //TODO: Do not rely on `if` statement. Use a functional approach
                val mongoStatus = if (error.database == "MongoDB") "DOWN" else "UP"
                val postgresStatus = if (error.database == "PostgreSQL") "DOWN" else "UP"
                DetailedHealthResponse(
                    mongoStatus,
                    postgresStatus,
                    "${error.database} unavailable: ${error.cause.message}"
                )
            }
            is HealthCheckError.DatabaseError -> {
                val mongoStatus = if (error.database == "MongoDB") "DOWN" else "UP"
                val postgresStatus = if (error.database == "PostgreSQL") "DOWN" else "UP"
                DetailedHealthResponse(mongoStatus, postgresStatus, "${error.database} error: ${error.cause.message}")
            }
            is HealthCheckError.ApplicationNotFound -> {
                DetailedHealthResponse("DOWN", "UP", "Application record not found")
            }
            is HealthCheckError.InvalidApplicationState -> {
                DetailedHealthResponse("DOWN", "UP", "Application in invalid state")
            }
            is HealthCheckError.MongoDatabaseUnavailable -> {
                DetailedHealthResponse("DOWN", "UP", "MongoDB unavailable")
            }
            is HealthCheckError.PostgresDatabaseUnavailable -> {
                DetailedHealthResponse("UP", "DOWN", "PostgreSQL unavailable")
            }
            is HealthCheckError.BothDatabasesUnavailable -> {
                DetailedHealthResponse("DOWN", "DOWN", "Both databases unavailable")
            }
        }
    respond(HttpStatusCode.ServiceUnavailable, response)
}

@Serializable
data class HealthResponse(val status: String, val reason: String? = null)

@Serializable
data class DetailedHealthResponse(val mongodb: String, val postgres: String, val reason: String? = null)

@Serializable
data class ReleaseResponse(val release: String)

@Serializable
data class ReleaseErrorResponse(val error: String)

private val ReleaseError.cause: Throwable
    get() =
        when (this) {
            is ReleaseError.ReleaseFileError -> cause
        }
