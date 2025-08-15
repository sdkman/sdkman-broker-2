package io.sdkman.broker.support

import io.ktor.server.application.Application
import io.sdkman.broker.application.service.CandidateDownloadService
import io.sdkman.broker.application.service.HealthService
import io.sdkman.broker.application.service.MetaService
import io.sdkman.broker.configureApp
import io.sdkman.broker.domain.service.SdkmanCliDownloadService
import io.sdkman.broker.domain.service.SdkmanNativeDownloadService

fun Application.configureAppForTesting(
    healthService: HealthService = TestDependencyInjection.healthService,
    metaService: MetaService = TestDependencyInjection.metaService,
    candidateDownloadService: CandidateDownloadService = TestDependencyInjection.versionService,
    sdkmanCliDownloadService: SdkmanCliDownloadService = TestDependencyInjection.sdkmanCliDownloadService,
    sdkmanNativeDownloadService: SdkmanNativeDownloadService = TestDependencyInjection.sdkmanNativeDownloadService
) {
    configureApp(
        healthService,
        metaService,
        candidateDownloadService,
        sdkmanCliDownloadService,
        sdkmanNativeDownloadService
    )
}
