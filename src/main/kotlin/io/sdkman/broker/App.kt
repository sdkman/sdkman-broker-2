package io.sdkman.broker

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.sdkman.broker.adapter.primary.rest.healthRoutes
import io.sdkman.broker.adapter.secondary.persistence.MongoApplicationRepository
import io.sdkman.broker.adapter.secondary.persistence.MongoConnectivity
import io.sdkman.broker.application.service.HealthService
import io.sdkman.broker.application.service.HealthServiceImpl
import io.sdkman.broker.config.DefaultAppConfig
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

object App {
    @JvmStatic
    fun main(args: Array<String>) {
        val config = DefaultAppConfig()

        // Create MongoDB connection
        val mongoConnectivity = MongoConnectivity(config)
        val database = mongoConnectivity.database()

        // Initialize repositories
        val applicationRepository = MongoApplicationRepository(database)

        // Initialize services
        val healthService = HealthServiceImpl(applicationRepository)

        // Start Ktor server
        embeddedServer(Netty, port = config.serverPort, host = config.serverHost) {
            configureApp(healthService)
        }.start(wait = true)
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureApp(healthService: HealthService) {
    // Install plugins
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            }
        )
    }

    // Configure routes
    healthRoutes(healthService)
}
