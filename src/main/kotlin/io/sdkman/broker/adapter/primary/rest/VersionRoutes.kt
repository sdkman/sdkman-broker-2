package io.sdkman.broker.adapter.primary.rest

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.sdkman.broker.application.service.VersionError
import io.sdkman.broker.application.service.VersionService
import kotlinx.serialization.Serializable

fun Application.versionRoutes(versionService: VersionService) {
    routing {
        get("/version") {
            versionService.getVersion()
                .fold(
                    { error ->
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            VersionErrorResponse("Error retrieving version: ${error.cause.message}")
                        )
                    },
                    { version ->
                        call.respond(HttpStatusCode.OK, VersionResponse(version))
                    }
                )
        }
    }
}

@Serializable
data class VersionResponse(val version: String)

@Serializable
data class VersionErrorResponse(val error: String)

private val VersionError.cause: Throwable
    get() =
        when (this) {
            is VersionError.VersionFileError -> cause
        }
