package io.sdkman.broker.adapter.primary.rest

import arrow.core.getOrElse
import arrow.core.toOption
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.path
import io.ktor.server.response.cacheControl
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.sdkman.broker.domain.model.VersionTagError
import io.sdkman.broker.domain.service.VersionTagService
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("io.sdkman.broker.adapter.primary.rest.VersionRoutes")

fun Application.versionRoutes(
    versionTagService: VersionTagService,
    cacheMaxAgeSeconds: Int
) {
    routing {
        get("/version/{candidate}/{tag}/{platform}") {
            val candidate =
                call.parameters["candidate"].toOption().getOrElse {
                    return@get call.respondMissingParameter()
                }
            val tag =
                call.parameters["tag"].toOption().getOrElse {
                    return@get call.respondMissingParameter()
                }
            val platform =
                call.parameters["platform"].toOption().getOrElse {
                    return@get call.respondMissingParameter()
                }

            versionTagService
                .findVersionByTag(candidate, tag, platform)
                .fold(
                    { error -> call.handleVersionTagError(error) },
                    { version ->
                        call.response.cacheControl(CacheControl.MaxAge(maxAgeSeconds = cacheMaxAgeSeconds))
                        call.respondText(version, ContentType.Text.Plain)
                    }
                )
        }
    }
}

val ApplicationCall.isVersionLookup: Boolean
    get() = request.path().startsWith("/version/")

suspend fun ApplicationCall.respondEmptyError(status: HttpStatusCode) {
    response.cacheControl(CacheControl.NoStore(visibility = null))
    respond(status)
}

private suspend fun ApplicationCall.handleVersionTagError(error: VersionTagError) =
    when (error) {
        is VersionTagError.InvalidPlatform ->
            respondEmptyClientError(
                HttpStatusCode.BadRequest,
                "INVALID_PLATFORM",
                "Invalid platform '${error.platform}'"
            )
        is VersionTagError.TagNotFound ->
            respondEmptyClientError(
                HttpStatusCode.NotFound,
                "TAG_NOT_FOUND",
                "No ${error.candidate} tag '${error.tag}' available for platform '${error.platform}'"
            )
        is VersionTagError.DatabaseError -> respondEmptyServerError(error.cause)
    }

private suspend fun ApplicationCall.respondMissingParameter() =
    respondEmptyClientError(HttpStatusCode.BadRequest, "MISSING_PARAMETER", "Missing required path parameter")

private suspend fun ApplicationCall.respondEmptyClientError(
    status: HttpStatusCode,
    code: String,
    message: String
) {
    logger.warn("$code: $message")
    respondEmptyError(status)
}

private suspend fun ApplicationCall.respondEmptyServerError(cause: Throwable) {
    logger.error(INTERNAL_ERROR_MESSAGE, cause)
    respondEmptyError(HttpStatusCode.InternalServerError)
}
