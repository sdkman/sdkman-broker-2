package io.sdkman.broker.support

import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.server.testing.testApplication
import io.sdkman.broker.domain.model.Version

fun whenRequested(
    path: String,
    then: suspend (HttpResponse) -> Unit
): Unit =
    testApplication {
        application { configureAppForTesting() }
        then(client.get(path))
    }

fun cliVersion(
    candidate: String,
    version: String,
    platform: String,
    visible: Boolean = true
): Version =
    Version(
        candidate = candidate,
        version = version,
        platform = platform,
        url = "https://example.com/$candidate-$version-$platform.zip",
        visible = visible
    )
