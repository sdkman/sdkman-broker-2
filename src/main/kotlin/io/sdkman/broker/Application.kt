package io.sdkman.broker

import com.mongodb.client.MongoClients
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.sdkman.broker.application.service.HealthService
import io.sdkman.broker.domain.repository.AppRepository
import io.sdkman.broker.infra.mongo.MongoAppRepository
import io.sdkman.broker.interfaces.api.HealthHandler
import io.sdkman.broker.interfaces.config.configureRoutes
import io.sdkman.broker.interfaces.config.configureSerialization
import kotlin.system.exitProcess

/**
 * Main application entry point
 */
fun main() {
    try {
        // Read MongoDB connection string from environment or use default
        val mongodbUri = System.getenv("MONGODB_URI") ?: "mongodb://localhost:27017"
        val dbName = System.getenv("MONGODB_DATABASE") ?: "sdkman"

        // Set up MongoDB client
        val mongoClient = MongoClients.create(mongodbUri)
        val mongoDatabase = mongoClient.getDatabase(dbName)

        // Create repositories and services
        val appRepository: AppRepository = MongoAppRepository(mongoDatabase)
        val healthService = HealthService(appRepository)
        val healthHandler = HealthHandler(healthService)

        // Determine port from environment or use default
        val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

        // Start the server
        embeddedServer(Netty, port = port, host = "0.0.0.0") {
            module(healthHandler)
        }.start(wait = true)
    } catch (e: Exception) {
        println("Failed to start application: ${e.message}")
        exitProcess(1)
    }
}

/**
 * Application module configuration
 */
fun Application.module(healthHandler: HealthHandler) {
    // Configure serialization
    configureSerialization()

    // Configure routes
    configureRoutes(healthHandler)

    // Log application startup
    log.info("SDKMAN Broker application started successfully")
}
