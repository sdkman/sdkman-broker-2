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
import io.sdkman.broker.application.service.ReleaseError
import io.sdkman.broker.application.service.ReleaseService
import kotlinx.serialization.Serializable

fun Application.metaRoutes(
    healthService: HealthService,
    releaseService: ReleaseService
) {
    routing {
        get("/meta/alive") {
            healthService.checkHealth()
                .fold(
                    { error -> call.handleHealthError(error) },
                    { status -> call.handleHealthStatus(status) }
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

private suspend fun ApplicationCall.handleHealthStatus(status: HealthStatus) {
    when (status) {
        HealthStatus.UP -> respond(HttpStatusCode.OK, HealthResponse("UP"))
        HealthStatus.DOWN -> respond(HttpStatusCode.ServiceUnavailable, HealthResponse("DOWN"))
    }
}

private suspend fun ApplicationCall.handleHealthError(error: HealthCheckError) {
    val response =
        when (error) {
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

@Serializable
data class HealthResponse(val status: String, val reason: String? = null)

@Serializable
data class ReleaseResponse(val release: String)

@Serializable
data class ReleaseErrorResponse(val error: String)

private val ReleaseError.cause: Throwable
    get() =
        when (this) {
            is ReleaseError.ReleaseFileError -> cause
        }
