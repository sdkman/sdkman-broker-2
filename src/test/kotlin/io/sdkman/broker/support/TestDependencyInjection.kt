package io.sdkman.broker.support

import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import io.sdkman.broker.adapter.secondary.persistence.MongoApplicationRepository
import io.sdkman.broker.adapter.secondary.persistence.MongoConnectivity
import io.sdkman.broker.application.service.HealthService
import io.sdkman.broker.application.service.HealthServiceImpl
import io.sdkman.broker.config.AppConfig

object TestDependencyInjection {
    private val config = AppConfig()
    private val mongoConnectivity = MongoConnectivity(config)
    
    private val mongoClient by lazy {
        val connectionString = mongoConnectivity.buildConnectionString()
        MongoClient(MongoClientURI(connectionString))
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
