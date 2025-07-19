package io.sdkman.broker.adapter.primary.rest

import arrow.core.Option
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.response.header
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.sdkman.broker.application.service.VersionService
import io.sdkman.broker.domain.model.VersionError

fun Application.downloadRoutes(versionService: VersionService) {
    routing {
        get("/download/{candidate}/{version}/{platform}") {
            val candidate = call.parameters["candidate"] ?: return@get call.respondBadRequest()
            val version = call.parameters["version"] ?: return@get call.respondBadRequest()
            val platform = call.parameters["platform"] ?: return@get call.respondBadRequest()

            val auditContext =
                AuditContext(
                    host = Option.fromNullable(call.request.header("X-Real-IP")),
                    agent = Option.fromNullable(call.request.header("User-Agent"))
                )

            versionService.downloadVersion(candidate, version, platform, auditContext)
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
    }
}

private fun ApplicationCall.handleVersionError(error: VersionError) =
    when (error) {
        is VersionError.InvalidPlatform -> respondWithStatus(HttpStatusCode.BadRequest)
        is VersionError.VersionNotFound -> respondWithStatus(HttpStatusCode.NotFound)
        is VersionError.DatabaseError -> respondWithStatus(HttpStatusCode.InternalServerError)
    }

private fun ApplicationCall.respondBadRequest() = respondWithStatus(HttpStatusCode.BadRequest)

private fun ApplicationCall.respondWithStatus(status: HttpStatusCode) = response.status(status)
