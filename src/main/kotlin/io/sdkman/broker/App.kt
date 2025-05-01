package io.sdkman.broker

import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.sdkman.broker.adapter.primary.rest.healthRoutes
import io.sdkman.broker.adapter.secondary.persistence.MongoApplicationRepository
import io.sdkman.broker.application.service.HealthService
import io.sdkman.broker.application.service.HealthServiceImpl
import io.sdkman.broker.config.AppConfig
import kotlinx.serialization.json.Json

/**
 * Main entry point for the SDKMAN Broker application.
 */
fun main() {
    val config = AppConfig()
    
    // Create MongoDB client
    val mongoClient = MongoClient(MongoClientURI(config.mongodbUri))
    val database = mongoClient.getDatabase(config.mongodbDatabase)
    
    // Initialize repositories
    val applicationRepository = MongoApplicationRepository(database)
    
    // Initialize services
    val healthService = HealthServiceImpl(applicationRepository)
    
    // Start Ktor server
    embeddedServer(Netty, port = config.serverPort, host = config.serverHost) {
        module(healthService)
    }.start(wait = true)
}

/**
 * Application module function that can be used in tests.
 * Creates all dependencies and calls the parameterized module function.
 */
fun Application.module() {
    //TODO: Why are we duplicating this setup again? Already done in the main function!
    val config = AppConfig()
    
    // Create MongoDB client
    val mongoClient = MongoClient(MongoClientURI(config.mongodbUri))
    val database = mongoClient.getDatabase(config.mongodbDatabase)
    
    // Initialize repositories
    val applicationRepository = MongoApplicationRepository(database)
    
    // Initialize services
    val healthService = HealthServiceImpl(applicationRepository)
    
    // Configure the application
    module(healthService)
}

/**
 * Application module that configures Ktor with the provided service.
 */
fun Application.module(healthService: HealthService) {
    // Install plugins
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
    
    // Configure routes
    healthRoutes(healthService)
} 