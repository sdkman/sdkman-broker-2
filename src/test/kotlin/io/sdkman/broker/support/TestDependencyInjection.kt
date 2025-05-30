package io.sdkman.broker.support

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.sdkman.broker.adapter.primary.rest.metaRoutes
import io.sdkman.broker.adapter.primary.rest.downloadRoutes
import io.sdkman.broker.adapter.secondary.persistence.MongoApplicationRepository
import io.sdkman.broker.adapter.secondary.persistence.MongoVersionRepository
import io.sdkman.broker.application.service.DownloadServiceImpl
import io.sdkman.broker.application.service.HealthServiceImpl
import io.sdkman.broker.application.service.ReleaseServiceImpl
import io.sdkman.broker.config.DefaultAppConfig

// Dependency injection for tests
// Uses the shared MongoTestListener to provide consistent MongoDB access across all tests
object TestDependencyInjection {
    // Use the shared container from MongoTestListener
    val config by lazy { DefaultAppConfig() }

    // Use the database from MongoTestListener directly
    val database by lazy { MongoTestListener.database }

    val applicationRepository by lazy {
        MongoApplicationRepository(database)
    }

    val versionRepository by lazy {
        MongoVersionRepository(database)
    }

    val healthService by lazy {
        HealthServiceImpl(applicationRepository)
    }

    val releaseService by lazy {
        ReleaseServiceImpl()
    }

    val downloadService by lazy {
        DownloadServiceImpl(versionRepository)
    }

    fun Application.configureApplication() {
        metaRoutes(healthService, releaseService)
        routing {
            downloadRoutes(downloadService)
        }
    }
}
