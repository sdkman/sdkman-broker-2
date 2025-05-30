package io.sdkman.broker.adapter.primary.rest

import arrow.core.fold
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.util.getOrFail
import io.sdkman.broker.application.service.DownloadError
import io.sdkman.broker.application.service.DownloadRequest
import io.sdkman.broker.application.service.DownloadResponse
import io.sdkman.broker.application.service.DownloadService

fun Route.downloadRoutes(downloadService: DownloadService) {
    get("/download/{candidate}/{version}/{platform}") {
        val candidate = call.parameters.getOrFail("candidate")
        val version = call.parameters.getOrFail("version")
        val platform = call.parameters.getOrFail("platform")

        val request = DownloadRequest(candidate, version, platform)

        downloadService.resolveDownload(request).fold(
            { error -> call.handleDownloadError(error) },
            { response -> call.handleDownloadSuccess(response) }
        )
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.handleDownloadError(error: DownloadError) {
    when (error) {
        is DownloadError.InvalidPlatform -> {
            respond(HttpStatusCode.BadRequest)
        }
        is DownloadError.CandidateNotFound,
        is DownloadError.PlatformNotFound -> {
            respond(HttpStatusCode.NotFound)
        }
        is DownloadError.SystemError -> {
            respond(HttpStatusCode.InternalServerError)
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.handleDownloadSuccess(response: DownloadResponse) {
    // Add checksum headers with algorithm priority
    response.checksums.forEach { (algorithm, checksum) ->
        val headerName = "X-Sdkman-Checksum-${algorithm.uppercase().replace("-", "")}"
        this.response.header(headerName, checksum)
    }

    // Add archive type header
    this.response.header("X-Sdkman-ArchiveType", response.archiveType.extension)

    // Issue 302 redirect to the binary URL
    respondRedirect(response.url, permanent = false)
}