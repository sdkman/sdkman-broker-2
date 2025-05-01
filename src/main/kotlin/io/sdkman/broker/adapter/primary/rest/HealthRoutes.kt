package io.sdkman.broker.adapter.primary.rest

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.sdkman.broker.application.service.HealthCheckError
import io.sdkman.broker.application.service.HealthService
import io.sdkman.broker.application.service.HealthStatus
import kotlinx.serialization.Serializable

/**
 * Health check response data class.
 */
@Serializable
data class HealthResponse(val status: String, val reason: String? = null)

/**
 * Configures health check routes for the Ktor application.
 */
fun Application.healthRoutes(healthService: HealthService) {
    routing {
        get("/health/alive") {
            healthService.checkHealth()
                .fold(
                    { error -> call.handleHealthError(error) },
                    { status -> call.handleHealthStatus(status) }
                )
        }
    }
}

/**
 * Handles successful health status by returning appropriate HTTP response.
 */
private suspend fun ApplicationCall.handleHealthStatus(status: HealthStatus) {
    when (status) {
        HealthStatus.UP -> respond(HttpStatusCode.OK, HealthResponse("UP"))
        HealthStatus.DOWN -> respond(HttpStatusCode.ServiceUnavailable, HealthResponse("DOWN"))
    }
}

/**
 * Handles health check errors by returning appropriate HTTP error responses.
 */
private suspend fun ApplicationCall.handleHealthError(error: HealthCheckError) {
    val response = when (error) {
        is HealthCheckError.DatabaseUnavailable -> {
            HealthResponse("DOWN", "Database unavailable: ${error.cause.message}")
        }
        is HealthCheckError.DatabaseError -> {
            HealthResponse("DOWN", "Database error: ${error.cause.message}")
        }
        is HealthCheckError.ApplicationNotFound -> {
            HealthResponse("DOWN", "Application record not found")
        }
        is HealthCheckError.InvalidApplicationState -> {
            HealthResponse("DOWN", "Application in invalid state")
        }
    }
    respond(HttpStatusCode.ServiceUnavailable, response)
} 