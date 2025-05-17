package io.sdkman.broker.adapter.primary.rest

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.sdkman.broker.application.service.ReleaseError
import io.sdkman.broker.application.service.ReleaseService
import kotlinx.serialization.Serializable

fun Application.releaseRoutes(releaseService: ReleaseService) {
    routing {
        get("/release") {
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

@Serializable
data class ReleaseResponse(val release: String)

@Serializable
data class ReleaseErrorResponse(val error: String)

private val ReleaseError.cause: Throwable
    get() =
        when (this) {
            is ReleaseError.ReleaseFileError -> cause
        }
