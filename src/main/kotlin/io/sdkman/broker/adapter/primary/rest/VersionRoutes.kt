package io.sdkman.broker.adapter.primary.rest

import arrow.core.getOrElse
import arrow.core.toOption
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.cacheControl
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.sdkman.broker.domain.model.VersionTagError
import io.sdkman.broker.domain.service.VersionTagService

fun Application.versionRoutes(
    versionTagService: VersionTagService,
    cacheMaxAgeSeconds: Int
) {
    routing {
        get("/version/{candidate}/{tag}/{platform}") {
            val candidate =
                call.parameters["candidate"].toOption().getOrElse {
                    return@get call.respondEmptyError(HttpStatusCode.BadRequest)
                }
            val tag =
                call.parameters["tag"].toOption().getOrElse {
                    return@get call.respondEmptyError(HttpStatusCode.BadRequest)
                }
            val platform =
                call.parameters["platform"].toOption().getOrElse {
                    return@get call.respondEmptyError(HttpStatusCode.BadRequest)
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

suspend fun ApplicationCall.respondEmptyError(status: HttpStatusCode) {
    response.cacheControl(CacheControl.NoStore(visibility = null))
    respond(status)
}

private suspend fun ApplicationCall.handleVersionTagError(error: VersionTagError) =
    when (error) {
        is VersionTagError.InvalidPlatform -> respondEmptyError(HttpStatusCode.BadRequest)
        is VersionTagError.TagNotFound -> respondEmptyError(HttpStatusCode.NotFound)
        is VersionTagError.DatabaseError -> respondEmptyError(HttpStatusCode.InternalServerError)
    }
