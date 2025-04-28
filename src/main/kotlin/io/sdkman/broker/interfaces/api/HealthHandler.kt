package io.sdkman.broker.interfaces.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.sdkman.broker.application.service.HealthService
import io.sdkman.broker.domain.error.DomainError

/**
 * HTTP Handler for the health check endpoint
 */
class HealthHandler(private val healthService: HealthService) {
    /**
     * Handles the /health request
     * Returns a JSON response with a status field
     * Status code 200 if healthy, appropriate error code otherwise
     */
    suspend fun handle(call: ApplicationCall): Unit =
        healthService.checkHealth().fold(
            ifLeft = { error -> handleError(call, error) },
            ifRight = { status -> call.respond(HttpStatusCode.OK, mapOf("status" to status)) },
        )

    /**
     * Maps domain errors to appropriate HTTP responses
     */
    private suspend fun handleError(
        call: ApplicationCall,
        error: DomainError,
    ) = when (error) {
        is DomainError.AppNotFound ->
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                mapOf("status" to "DOWN", "reason" to error.message),
            )
        is DomainError.AppNotHealthy ->
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                mapOf("status" to "DOWN", "reason" to error.message),
            )
        is DomainError.RepositoryError ->
            call.respond(
                HttpStatusCode.ServiceUnavailable,
                mapOf("status" to "DOWN", "reason" to "Database error: ${error.cause.message}"),
            )
    }
}
