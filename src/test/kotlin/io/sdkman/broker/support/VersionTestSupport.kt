package io.sdkman.broker.support

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.sdkman.broker.adapter.primary.rest.versionRoutes
import io.sdkman.broker.application.service.VersionService
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureAppForVersionTesting(versionService: VersionService) {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            }
        )
    }

    versionRoutes(versionService)
}