package io.sdkman.broker.acceptance

import com.mongodb.MongoClient
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.sdkman.broker.module
import org.bson.Document
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

/**
 * Acceptance tests for the health check endpoint based on BDD scenarios.
 */
//TODO: Use a MongoTestListener instead to hand off MongoDB interaction to the listener
 class HealthCheckAcceptanceSpec : ShouldSpec() {

    private val mongoContainer = MongoDBContainer(DockerImageName.parse("mongo:3.2"))
        .apply { start() }
    
    private val mongoClient = MongoClient(
        mongoContainer.host,
        mongoContainer.firstMappedPort
    )
    
    private val database = mongoClient.getDatabase("sdkman")
    private val applicationCollection = database.getCollection("application")
    
    private fun resetDatabase() {
        applicationCollection.drop()
    }
    
    override suspend fun beforeTest(testCase: TestCase) {
        resetDatabase()
        
        // Set MongoDB connection for tests (default)
        System.setProperty("mongodb.uri", 
            "mongodb://${mongoContainer.host}:${mongoContainer.firstMappedPort}")
        System.setProperty("mongodb.database", "sdkman")
    }
    
    override suspend fun afterSpec(spec: io.kotest.core.spec.Spec) {
        mongoClient.close()
        mongoContainer.stop()
    }
    
    init {
        should("return 200 OK when database is healthy") {
            // given: an initialised database
            applicationCollection.insertOne(Document("alive", "OK"))
            
            // when: a GET request is made for "/health/alive"
            testApplication {
                application { module() }
                
                val response = client.get("/health/alive")
                val responseBody = response.bodyAsText()
                println("DEBUG - Response body: $responseBody")
                
                // then: the service response status is 200
                response.status shouldBe HttpStatusCode.OK
            }
        }
        
        should("return 503 Service Unavailable when database is empty") {
            // given: an uninitialised database with NO application record
            
            // when: a GET request is made for "/health/alive"
            testApplication {
                application { module() }
                
                val response = client.get("/health/alive")
                val responseBody = response.bodyAsText()
                println("DEBUG - Response body: $responseBody")
                
                // then: the service response status is 503
                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }
        
        should("return 503 Service Unavailable when database has invalid application record") {
            // given: an uninitialised database with an invalid application record
            applicationCollection.insertOne(Document("alive", "NOT_OK"))
            
            // when: a GET request is made for "/health/alive"
            testApplication {
                application { module() }
                
                val response = client.get("/health/alive")
                val responseBody = response.bodyAsText()
                //TODO: remove this println
                println("DEBUG - Response body: $responseBody")
                
                // then: the service response status is 503
                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }
        
        should("return 503 Service Unavailable when database is inaccessible") {
            // given: an inaccessible database due to connectivity issues
            System.setProperty("mongodb.uri", "mongodb://nonexistent-host:27017")
            
            // when: a GET request is made for "/health/alive"
            testApplication {
                application { module() }
                
                val response = client.get("/health/alive")
                val responseBody = response.bodyAsText()
                //TODO: remove this println
                println("DEBUG - Response body: $responseBody")
                
                // then: the service response status is 503
                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
            
            // Reset to valid connection for other tests
            //TODO: Use a MongoTestListener instead to hand off MongoDB interaction to the listener
            System.setProperty("mongodb.uri", 
                "mongodb://${mongoContainer.host}:${mongoContainer.firstMappedPort}")
        }
    }
} 