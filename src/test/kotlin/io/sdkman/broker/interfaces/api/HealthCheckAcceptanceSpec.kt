package io.sdkman.broker.interfaces.api

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.sdkman.broker.application.service.HealthService
import io.sdkman.broker.configureTestApplication
import io.sdkman.broker.infra.mongo.MongoAppRepository
import io.sdkman.broker.test.MongoContainer
import kotlinx.serialization.json.Json

/**
 * Acceptance tests for the health check endpoint
 */
class HealthCheckAcceptanceSpec : ShouldSpec({

    // Register the test listener
    listener(MongoContainer)

    beforeTest {
        MongoContainer.startContainer()
        MongoContainer.dropApplicationCollection()
    }

    should("return status UP when the database is healthy") {
        // given: an initialised database
        MongoContainer.setupApplicationData()

        val repository = MongoAppRepository(MongoContainer.database)
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

    should("return status DOWN when the database is inconsistent") {
        // given: an uninitialised database (no application collection)
        // Do NOT set up application data

        val repository = MongoAppRepository(MongoContainer.database)
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
            responseJson["reason"] shouldBe "Application record not found"
        }
    }

    should("return status DOWN when the database is inaccessible") {
        // given: an inaccessible database
        MongoContainer.stopContainer()

        val repository = MongoAppRepository(MongoContainer.database)
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
            responseJson["reason"] shouldContain "Database error"
        }
    }
})
