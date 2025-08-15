package io.sdkman.broker.support

import io.ktor.server.application.Application
import io.sdkman.broker.application.service.MetaHealthService
import io.sdkman.broker.application.service.MetaReleaseService
import io.sdkman.broker.configureApp
import io.sdkman.broker.domain.service.CandidateDownloadService
import io.sdkman.broker.domain.service.SdkmanCliDownloadService
import io.sdkman.broker.domain.service.SdkmanNativeDownloadService

fun Application.configureAppForTesting(
    metaHealthService: MetaHealthService = TestDependencyInjection.healthService,
    metaReleaseService: MetaReleaseService = TestDependencyInjection.metaService,
    candidateDownloadService: CandidateDownloadService = TestDependencyInjection.versionService,
    sdkmanCliDownloadService: SdkmanCliDownloadService = TestDependencyInjection.sdkmanCliDownloadService,
    sdkmanNativeDownloadService: SdkmanNativeDownloadService = TestDependencyInjection.sdkmanNativeDownloadService
) {
    configureApp(
        metaHealthService,
        metaReleaseService,
        candidateDownloadService,
        sdkmanCliDownloadService,
        sdkmanNativeDownloadService
    )
}
