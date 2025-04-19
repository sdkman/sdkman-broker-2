package io.sdkman.broker.test

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import org.bson.Document
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

/**
 * MongoDB test container for integration and acceptance tests
 * Provides a singleton MongoDB instance for tests
 */
class MongoContainer : TestListener {
    // Use MongoDB 5.0 for tests
    private val container = MongoDBContainer(DockerImageName.parse("mongo:5.0"))
    
    private var mongoClient: MongoClient? = null
    
    val database: MongoDatabase
        get() = mongoClient!!.getDatabase("sdkman")
    
    /**
     * Set up the container and initialize test data before each test
     */
    override suspend fun beforeTest(testCase: TestCase) {
        container.start()
        mongoClient = MongoClients.create(container.connectionString)
        
        // Initialize with test data
        setupTestData()
    }
    
    /**
     * Clean up after each test
     */
    override suspend fun afterTest(testCase: TestCase, result: TestResult) {
        mongoClient?.close()
        container.stop()
    }
    
    /**
     * Insert test data needed for health check
     */
    private fun setupTestData() {
        val appCollection = database.getCollection("application")
        appCollection.insertOne(
            Document()
                .append("alive", "OK")
                .append("stableCliVersion", "5.19.0")
                .append("betaCliVersion", "latest+b8d230b")
                .append("stableNativeCliVersion", "0.7.4")
                .append("betaNativeCliVersion", "0.7.4")
        )
    }
} 