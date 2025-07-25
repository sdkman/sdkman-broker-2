package io.sdkman.broker.support

import io.ktor.server.application.Application
import io.sdkman.broker.application.service.HealthService
import io.sdkman.broker.application.service.ReleaseService
import io.sdkman.broker.application.service.VersionService
import io.sdkman.broker.configureApp

fun Application.configureAppForTesting(
    healthService: HealthService,
    releaseService: ReleaseService,
    versionService: VersionService
) {
    configureApp(healthService, releaseService, versionService)
}
