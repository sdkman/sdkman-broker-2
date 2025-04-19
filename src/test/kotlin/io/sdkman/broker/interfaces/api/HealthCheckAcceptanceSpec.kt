package io.sdkman.broker.interfaces.api

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.sdkman.broker.test.configureTestApplication
import io.sdkman.broker.application.service.HealthService
import io.sdkman.broker.infra.mongo.MongoAppRepository
import io.sdkman.broker.test.MongoContainer
import kotlinx.serialization.json.Json

/**
 * Acceptance tests for the health check endpoint
 */
class HealthCheckAcceptanceSpec : ShouldSpec({
    
    val mongo = MongoContainer()
    
    // Register the test listener
    listener(mongo)
    
    should("return status UP when the application is healthy") {
        // given: an application with MongoDB containing a healthy app record
        val repository = MongoAppRepository(mongo.database)
        val healthService = HealthService(repository)
        val healthHandler = HealthHandler(healthService)
        
        // when: making a request to the /health endpoint
        testApplication {
            application {
                configureTestApplication(healthHandler)
            }
            
            val response = client.get("/health")
            
            // then: response should be 200 OK with status UP
            response.status shouldBe HttpStatusCode.OK
            
            val responseText = response.bodyAsText()
            val responseJson = Json.decodeFromString<Map<String, String>>(responseText)
            responseJson["status"] shouldBe "UP"
        }
    }
    
    should("return status DOWN when the application is not healthy") {
        // given: an application with MongoDB containing an app with wrong status
        // First, update the app to have NOT_OK status
        mongo.database.getCollection("application").updateOne(
            org.bson.Document(),
            org.bson.Document("\$set", org.bson.Document("alive", "NOT_OK"))
        )
        
        val repository = MongoAppRepository(mongo.database)
        val healthService = HealthService(repository)
        val healthHandler = HealthHandler(healthService)
        
        // when: making a request to the /health endpoint
        testApplication {
            application {
                configureTestApplication(healthHandler)
            }
            
            val response = client.get("/health")
            
            // then: response should be 503 with status DOWN
            response.status shouldBe HttpStatusCode.ServiceUnavailable
            
            val responseText = response.bodyAsText()
            val responseJson = Json.decodeFromString<Map<String, String>>(responseText)
            responseJson["status"] shouldBe "DOWN"
            responseJson["reason"] shouldBe "Application is not healthy"
        }
    }
}) 