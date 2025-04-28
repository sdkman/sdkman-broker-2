package io.sdkman.broker.test

import io.ktor.server.application.Application
import io.sdkman.broker.interfaces.api.HealthHandler
import io.sdkman.broker.interfaces.config.configureRoutes
import io.sdkman.broker.interfaces.config.configureSerialization

/**
 * Module function used by tests to configure the application
 */
fun Application.configureTestApplication(healthHandler: HealthHandler) {
    // Configure serialization
    configureSerialization()

    // Configure routes
    configureRoutes(healthHandler)
}
