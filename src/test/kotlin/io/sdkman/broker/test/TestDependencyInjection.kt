package io.sdkman.broker.test

import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import io.sdkman.broker.adapter.secondary.persistence.MongoApplicationRepository
import io.sdkman.broker.application.service.HealthService
import io.sdkman.broker.application.service.HealthServiceImpl
import io.sdkman.broker.config.AppConfig

/**
 * Test-specific dependency injection that uses the test MongoDB instance.
 */
object TestDependencyInjection {
    private val config = AppConfig()
    private val mongoClient by lazy {
        MongoClient(MongoClientURI(System.getProperty("mongodb.uri", config.mongodbUri)))
    }
    
    private val database by lazy {
        mongoClient.getDatabase(config.mongodbDatabase)
    }
    
    private val applicationRepository by lazy {
        MongoApplicationRepository(database)
    }
    
    val healthService: HealthService by lazy {
        HealthServiceImpl(applicationRepository)
    }
    
    fun close() {
        mongoClient.close()
    }
} 