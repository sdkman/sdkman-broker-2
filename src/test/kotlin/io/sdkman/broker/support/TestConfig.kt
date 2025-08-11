package io.sdkman.broker.support

import io.ktor.server.application.Application
import io.sdkman.broker.application.service.CandidateDownloadService
import io.sdkman.broker.application.service.HealthService
import io.sdkman.broker.application.service.ReleaseService
import io.sdkman.broker.application.service.SdkmanCliDownloadService
import io.sdkman.broker.configureApp

fun Application.configureAppForTesting(
    healthService: HealthService = TestDependencyInjection.healthService,
    releaseService: ReleaseService = TestDependencyInjection.releaseService,
    candidateDownloadService: CandidateDownloadService = TestDependencyInjection.versionService,
    sdkmanCliDownloadService: SdkmanCliDownloadService = TestDependencyInjection.sdkmanCliDownloadService
) {
    configureApp(healthService, releaseService, candidateDownloadService, sdkmanCliDownloadService)
}
