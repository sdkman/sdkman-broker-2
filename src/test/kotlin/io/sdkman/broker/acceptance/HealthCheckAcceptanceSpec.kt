package io.sdkman.broker.acceptance

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.sdkman.broker.support.MongoSupport
import io.sdkman.broker.support.MongoTestListener
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.TestDependencyInjection
import io.sdkman.broker.support.configureAppForTesting

class HealthCheckAcceptanceSpec : ShouldSpec() {
    override suspend fun beforeSpec(spec: io.kotest.core.spec.Spec) {
        listeners(MongoTestListener, PostgresTestListener)
    }

    init {
        context("Health Check Acceptance Tests") {

            should("return 200 OK with both databases UP when both are healthy") {
                // given: MongoDB is accessible and contains valid application data
                // and: PostgreSQL is accessible and responsive
                MongoSupport.setupValidAppRecord()

                // when: a GET request is made for "/meta/health"
                testApplication {
                    application {
                        configureAppForTesting(
                            TestDependencyInjection.healthService,
                            TestDependencyInjection.releaseService,
                            TestDependencyInjection.versionService
                        )
                    }
                    val response = client.get("/meta/health")
                    val responseBody = response.body<String>()

                    // then: the service response status is 200
                    // and: the response body contains JSON with "mongodb": "UP" and "postgres": "UP"
                    response.status shouldBe HttpStatusCode.OK
                    responseBody shouldContain "\"mongodb\":\"UP\""
                    responseBody shouldContain "\"postgres\":\"UP\""
                }
            }

            should("return 503 Service Unavailable with MongoDB DOWN and PostgreSQL UP when MongoDB is empty") {
                // given: MongoDB is accessible but contains no application data
                // and: PostgreSQL is accessible and responsive
                // (MongoDB database is empty by default after MongoTestListener reset)

                // when: a GET request is made for "/meta/health"
                testApplication {
                    application {
                        configureAppForTesting(
                            TestDependencyInjection.healthService,
                            TestDependencyInjection.releaseService,
                            TestDependencyInjection.versionService
                        )
                    }
                    val response = client.get("/meta/health")
                    val responseBody = response.body<String>()

                    // then: the service response status is 503
                    // and: the response body contains JSON with "mongodb": "DOWN" and "postgres": "UP"
                    response.status shouldBe HttpStatusCode.ServiceUnavailable
                    responseBody shouldContain "\"mongodb\":\"DOWN\""
                    responseBody shouldContain "\"postgres\":\"UP\""
                }
            }

            should(
                "return 503 Service Unavailable with MongoDB DOWN and PostgreSQL UP when MongoDB has invalid record"
            ) {
                // given: MongoDB contains an invalid application record
                // and: PostgreSQL is accessible and responsive
                MongoSupport.setupInvalidAppRecord()

                // when: a GET request is made for "/meta/health"
                testApplication {
                    application {
                        configureAppForTesting(
                            TestDependencyInjection.healthService,
                            TestDependencyInjection.releaseService,
                            TestDependencyInjection.versionService
                        )
                    }
                    val response = client.get("/meta/health")
                    val responseBody = response.body<String>()

                    // then: the service response status is 503
                    // and: the response body contains JSON with "mongodb": "DOWN" and "postgres": "UP"
                    response.status shouldBe HttpStatusCode.ServiceUnavailable
                    responseBody shouldContain "\"mongodb\":\"DOWN\""
                    responseBody shouldContain "\"postgres\":\"UP\""
                }
            }

            should("return 503 Service Unavailable with both databases DOWN when MongoDB is inaccessible") {
                // given: MongoDB is inaccessible due to connectivity issues
                // and: PostgreSQL is accessible but we simulate failure by using invalid connection
                val originalMongoUri = System.getProperty("mongodb.uri")
                val originalPostgresHost = System.getProperty("postgres.host")

                try {
                    System.setProperty("mongodb.uri", "mongodb://nonexistent-host:27017")
                    System.setProperty("postgres.host", "nonexistent-postgres-host")

                    // when: a GET request is made for "/meta/health"
                    testApplication {
                        application {
                            configureAppForTesting(
                                TestDependencyInjection.healthService,
                                TestDependencyInjection.releaseService,
                                TestDependencyInjection.versionService
                            )
                        }
                        val response = client.get("/meta/health")
                        val responseBody = response.body<String>()

                        // then: the service response status is 503
                        // and: the response body contains JSON with "mongodb": "DOWN" and "postgres": "DOWN"
                        response.status shouldBe HttpStatusCode.ServiceUnavailable
                        responseBody shouldContain "\"mongodb\":\"DOWN\""
                        responseBody shouldContain "\"postgres\":\"DOWN\""
                    }
                } finally {
                    // Restore original system properties
                    if (originalMongoUri != null) {
                        System.setProperty("mongodb.uri", originalMongoUri)
                    } else {
                        System.clearProperty("mongodb.uri")
                    }
                    if (originalPostgresHost != null) {
                        System.setProperty("postgres.host", originalPostgresHost)
                    } else {
                        System.clearProperty("postgres.host")
                    }
                }
            }

            should("verify endpoint path change from /meta/alive to /meta/health") {
                // given: a healthy database setup
                MongoSupport.setupValidAppRecord()

                testApplication {
                    application {
                        configureAppForTesting(
                            TestDependencyInjection.healthService,
                            TestDependencyInjection.releaseService,
                            TestDependencyInjection.versionService
                        )
                    }

                    // when: a GET request is made for the old "/meta/alive" endpoint
                    val oldResponse = client.get("/meta/alive")
                    // then: it should return 404 Not Found (endpoint no longer exists)
                    oldResponse.status shouldBe HttpStatusCode.NotFound

                    // when: a GET request is made for the new "/meta/health" endpoint
                    val newResponse = client.get("/meta/health")
                    // then: it should return 200 OK with the new response format
                    newResponse.status shouldBe HttpStatusCode.OK
                    val responseBody = newResponse.body<String>()
                    responseBody shouldContain "\"mongodb\""
                    responseBody shouldContain "\"postgres\""
                }
            }
        }
    }
}
