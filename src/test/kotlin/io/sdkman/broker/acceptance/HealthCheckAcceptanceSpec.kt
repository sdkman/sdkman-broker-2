package io.sdkman.broker.acceptance

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.sdkman.broker.support.MongoTestListener
import io.sdkman.broker.support.TestDependencyInjection
import io.sdkman.broker.support.configureAppForTesting

class HealthCheckAcceptanceSpec : ShouldSpec() {
    override suspend fun beforeSpec(spec: io.kotest.core.spec.Spec) {
        listeners(MongoTestListener)
    }

    init {
        should("return 200 OK when database is healthy") {
            // given: an initialised database
            MongoTestListener.setupValidAppRecord()

            // when: a GET request is made for "/health/alive"
            testApplication {
                application { configureAppForTesting(TestDependencyInjection.healthService) }
                val response = client.get("/health/alive")

                // then: the service response status is 200
                response.status shouldBe HttpStatusCode.OK
            }
        }

        should("return 503 Service Unavailable when database is empty") {
            // given: an uninitialised database with NO application record

            // when: a GET request is made for "/health/alive"
            testApplication {
                application { configureAppForTesting(TestDependencyInjection.healthService) }
                val response = client.get("/health/alive")

                // then: the service response status is 503
                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }

        should("return 503 Service Unavailable when database has invalid application record") {
            // given: an uninitialised database with an invalid application record
            MongoTestListener.setupInvalidAppRecord()

            // when: a GET request is made for "/health/alive"
            testApplication {
                application { configureAppForTesting(TestDependencyInjection.healthService) }
                val response = client.get("/health/alive")

                // then: the service response status is 503
                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }

        should("return 503 Service Unavailable when database is inaccessible") {
            // given: an inaccessible database due to connectivity issues
            System.setProperty("mongodb.uri", "mongodb://nonexistent-host:27017")

            // when: a GET request is made for "/health/alive"
            testApplication {
                application { configureAppForTesting(TestDependencyInjection.healthService) }
                val response = client.get("/health/alive")

                // then: the service response status is 503
                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }
    }
}
