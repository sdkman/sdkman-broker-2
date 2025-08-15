package io.sdkman.broker.adapter.primary.rest

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.sdkman.broker.application.service.MetaHealthService
import io.sdkman.broker.application.service.MetaError
import io.sdkman.broker.application.service.MetaReleaseService
import kotlinx.serialization.Serializable

fun Application.metaRoutes(
    metaHealthService: MetaHealthService,
    metaReleaseService: MetaReleaseService
) {
    routing {
        get("/meta/health") {
            metaHealthService.checkHealth()
                .fold(
                    { error -> call.handleHealthError(error) },
                    { databaseStatus -> call.handleDatabaseHealthStatus(databaseStatus) }
                )
        }

        get("/meta/release") {
            metaReleaseService.getReleaseVersion()
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

@Serializable
data class DetailedHealthResponse(val mongodb: String, val postgres: String, val reason: String? = null)

@Serializable
data class ReleaseResponse(val release: String)

@Serializable
data class ReleaseErrorResponse(val error: String)

private val MetaError.cause: Throwable
    get() =
        when (this) {
            is MetaError.MetaFileError -> cause
        }
