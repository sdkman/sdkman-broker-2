package io.sdkman.broker.support

import com.mongodb.MongoClient
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import org.bson.Document
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

object MongoTestListener : TestListener {
    private val mongoContainer = MongoDBContainer(DockerImageName.parse("mongo:3.2"))
        .apply { start() }
    
    val host: String = mongoContainer.host
    val port: Int = mongoContainer.firstMappedPort
    
    val mongoClient = MongoClient(host, port)
    val database = mongoClient.getDatabase("sdkman")
    val applicationCollection = database.getCollection("application")
    
    fun resetDatabase() {
        applicationCollection.drop()
    }
    
    fun setupValidAppRecord() {
        applicationCollection.insertOne(Document("alive", "OK"))
    }
    
    fun setupInvalidAppRecord() {
        applicationCollection.insertOne(Document("alive", "NOT_OK"))
    }
    
    override suspend fun beforeTest(testCase: TestCase) {
        resetDatabase()
        System.setProperty("mongodb.uri", "mongodb://$host:$port")
        System.setProperty("mongodb.database", "sdkman")
    }
} 
