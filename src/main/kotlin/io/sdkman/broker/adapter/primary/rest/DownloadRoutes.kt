package io.sdkman.broker.adapter.primary.rest

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.toOption
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.sdkman.broker.domain.model.VersionError
import io.sdkman.broker.domain.service.CandidateDownloadService
import io.sdkman.broker.domain.service.SdkmanCliDownloadService
import io.sdkman.broker.domain.service.SdkmanNativeDownloadService
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("io.sdkman.broker.adapter.primary.rest.DownloadRoutes")

fun Application.downloadRoutes(
    candidateDownloadService: CandidateDownloadService,
    sdkmanCliDownloadService: SdkmanCliDownloadService,
    sdkmanNativeDownloadService: SdkmanNativeDownloadService
) {
    routing {
        get("/download/{candidate}/{version}/{platform}") {
            val candidate = call.parameters["candidate"].toOption().getOrElse { return@get call.respondBadRequest() }
            val version = call.parameters["version"].toOption().getOrElse { return@get call.respondBadRequest() }
            val platform = call.parameters["platform"].toOption().getOrElse { return@get call.respondBadRequest() }

            val auditContext =
                AuditContext(
                    host = Option.fromNullable(call.request.header("X-Real-IP")),
                    agent = Option.fromNullable(call.request.header("User-Agent"))
                )

            candidateDownloadService
                .downloadVersion(candidate, version, platform, auditContext)
                .fold(
                    { error -> call.handleVersionError(error) },
                    { downloadInfo ->
                        val response = DownloadResponse.from(downloadInfo)
                        response.checksumHeaders.forEach { (header, value) ->
                            call.response.header(header, value)
                        }
                        call.response.header("X-Sdkman-ArchiveType", response.archiveType)
                        call.respondRedirect(response.redirectUrl, permanent = false)
                    }
                )
        }

        get("/download/sdkman/{command}/{version}/{platform}") {
            val command = call.parameters["command"].toOption().getOrElse { return@get call.respondBadRequest() }
            val version = call.parameters["version"].toOption().getOrElse { return@get call.respondBadRequest() }
            val platform = call.parameters["platform"].toOption().getOrElse { return@get call.respondBadRequest() }

            sdkmanCliDownloadService
                .downloadSdkmanCli(command, version, platform)
                .fold(
                    { error -> call.handleVersionError(error) },
                    { downloadInfo ->
                        call.response.header("X-Sdkman-ArchiveType", downloadInfo.archiveType)
                        call.respondRedirect(downloadInfo.redirectUrl, permanent = false)
                    }
                )
        }

        get("/download/native/{command}/{version}/{platform}") {
            val command = call.parameters["command"].toOption().getOrElse { return@get call.respondBadRequest() }
            val version = call.parameters["version"].toOption().getOrElse { return@get call.respondBadRequest() }
            val platform = call.parameters["platform"].toOption().getOrElse { return@get call.respondBadRequest() }

            sdkmanNativeDownloadService
                .downloadNativeCli(command, version, platform)
                .fold(
                    { error -> call.handleVersionError(error) },
                    { downloadInfo ->
                        call.response.header("X-Sdkman-ArchiveType", downloadInfo.archiveType)
                        call.respondRedirect(downloadInfo.redirectUrl, permanent = false)
                    }
                )
        }
    }
}

private suspend fun ApplicationCall.handleVersionError(error: VersionError) =
    when (error) {
        is VersionError.InvalidCommand ->
            respondClientError(HttpStatusCode.BadRequest, "INVALID_COMMAND", "Invalid command '${error.command}'")
        is VersionError.InvalidPlatform ->
            respondClientError(HttpStatusCode.BadRequest, "INVALID_PLATFORM", "Invalid platform '${error.platform}'")
        is VersionError.InvalidVersion ->
            respondClientError(HttpStatusCode.BadRequest, "INVALID_VERSION", "Invalid version '${error.version}'")
        is VersionError.VersionNotFound ->
            respondClientError(
                HttpStatusCode.NotFound,
                "VERSION_NOT_FOUND",
                "No ${error.candidate} version '${error.version}' available for platform '${error.platform}'"
            )
        is VersionError.DatabaseError -> respondServerError(error.cause)
    }

private suspend fun ApplicationCall.respondBadRequest() =
    respondClientError(HttpStatusCode.BadRequest, "MISSING_PARAMETER", "Missing required path parameter")

private suspend fun ApplicationCall.respondClientError(
    status: HttpStatusCode,
    code: String,
    message: String
) {
    logger.warn("$code: $message")
    respond(status, ErrorResponse(code, message))
}

private suspend fun ApplicationCall.respondServerError(cause: Throwable) {
    logger.error(INTERNAL_ERROR_MESSAGE, cause)
    respond(HttpStatusCode.InternalServerError, internalErrorResponse())
}
