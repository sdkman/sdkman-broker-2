package io.sdkman.broker.support

import io.sdkman.broker.adapter.secondary.persistence.MongoApplicationRepository
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

    val healthService by lazy {
        HealthServiceImpl(applicationRepository)
    }

    val releaseService by lazy {
        ReleaseServiceImpl()
    }
}
