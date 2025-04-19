package io.sdkman.broker.interfaces.config

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.sdkman.broker.interfaces.api.HealthHandler

/**
 * Configure all application routes
 */
fun Application.configureRoutes(
    healthHandler: HealthHandler
) {
    routing {
        // Health check endpoint
        get("/health") {
            healthHandler.handle(call)
        }
    }
} 