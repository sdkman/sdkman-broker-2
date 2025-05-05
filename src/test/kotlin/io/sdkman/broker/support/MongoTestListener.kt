package io.sdkman.broker.support

import com.mongodb.MongoClient
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import org.bson.Document
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

object MongoTestListener : TestListener {
    // Create a single static container for all tests
    private val mongoContainer: MongoDBContainer by lazy {
        MongoDBContainer(DockerImageName.parse("mongo:3.2")).apply {
            start()
        }
    }

    val host: String by lazy { mongoContainer.host }
    val port: Int by lazy { mongoContainer.firstMappedPort }
    val mongoClient: MongoClient by lazy { MongoClient(host, port) }
    val database by lazy { mongoClient.getDatabase("sdkman") }
    val applicationCollection by lazy { database.getCollection("application") }

    override suspend fun beforeSpec(spec: Spec) {
        // Ensure container is started
        mongoContainer

        // Set up environment for tests
        setSystemProperties()

        // Reset database to clean state
        resetDatabase()
    }

    override suspend fun beforeTest(testCase: TestCase) {
        resetDatabase()
    }

    // Sets system properties for tests using TypeSafe Config format
    private fun setSystemProperties() {        
        // Lower case with dots (format used by AppConfig)
        System.setProperty("mongodb.host", host)
        System.setProperty("mongodb.port", port.toString())
        System.setProperty("mongodb.database", "sdkman")
    }

    // Resets the database to a clean state
    fun resetDatabase() {
        applicationCollection.drop()
    }

    // Sets up a valid application record
    fun setupValidAppRecord() {
        resetDatabase()
        applicationCollection.insertOne(Document("alive", "OK"))
    }

    // Sets up an invalid application record
    fun setupInvalidAppRecord() {
        resetDatabase()
        applicationCollection.insertOne(Document("alive", "NOT_OK"))
    }
}
