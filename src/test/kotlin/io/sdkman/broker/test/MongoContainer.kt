package io.sdkman.broker.test

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import org.bson.Document
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

/**
 * MongoDB test container for integration and acceptance tests
 * Provides a clean MongoDB instance for tests
 */
object MongoContainer : TestListener {

    private val logger: Logger = LoggerFactory.getLogger(MongoContainer::class.java)
    
    // Use MongoDB 5.0 for tests
    val container = MongoDBContainer(DockerImageName.parse("mongo:5.0"))
    
    private var mongoClient: MongoClient = startContainer()
    
    val database: MongoDatabase
        get() = mongoClient.getDatabase("sdkman")
    
    /**
     * Insert application data needed for health check
     */
    fun setupApplicatonData() {
        val appCollection = database.getCollection("application")
        appCollection.drop()
        database.createCollection("application")
        appCollection.insertOne(
            Document()
                .append("alive", "OK")
                .append("stableCliVersion", "5.19.0")
                .append("betaCliVersion", "latest+b8d230b")
                .append("stableNativeCliVersion", "0.7.4")
                .append("betaNativeCliVersion", "0.7.4")
        )
    }

    /**
     * Drops the application collection
     */
    fun dropApplicationCollection() {
        val appCollection = database.getCollection("application")
        appCollection.drop()
    }
    
    /**
     * Starts the container and reconnects the client
     */
    fun startContainer(): MongoClient {
        container.start()
        return MongoClients.create(container.connectionString)
    }

    /**
     * Stops the container to simulate database unavailability
     */
    fun stopContainer() {
        mongoClient.close()
        container.stop()
    }
} 