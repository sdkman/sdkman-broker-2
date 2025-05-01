package io.sdkman.broker.test

import io.ktor.server.application.Application
import io.sdkman.broker.application.service.HealthService
import io.sdkman.broker.configureApp

/**
 * Test-specific configuration function.
 * Allows tests to provide mock dependencies.
 */
fun Application.configureAppForTesting(healthService: HealthService) {
    configureApp(healthService)
} 