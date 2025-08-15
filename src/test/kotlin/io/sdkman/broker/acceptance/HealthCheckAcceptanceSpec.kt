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
import org.junit.jupiter.api.Tag

@Tag("acceptance")
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
                            TestDependencyInjection.metaService,
                            TestDependencyInjection.versionService
                        )
                    }
                    val response = client.get("/meta/health")
                    val responseBody = response.body<String>()

                    // then: the service response status is 200
                    // and: the response body contains JSON with "mongodb": "UP" and "postgres": "UP"
                    response.status shouldBe HttpStatusCode.OK
                    responseBody shouldContain "\"mongodb\": \"UP\""
                    responseBody shouldContain "\"postgres\": \"UP\""
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
                            TestDependencyInjection.metaService,
                            TestDependencyInjection.versionService
                        )
                    }
                    val response = client.get("/meta/health")
                    val responseBody = response.body<String>()

                    // then: the service response status is 503
                    // and: the response body contains JSON with "mongodb": "DOWN" and "postgres": "UP"
                    response.status shouldBe HttpStatusCode.ServiceUnavailable
                    responseBody shouldContain "\"mongodb\": \"DOWN\""
                    responseBody shouldContain "\"postgres\": \"UP\""
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
                            TestDependencyInjection.metaService,
                            TestDependencyInjection.versionService
                        )
                    }
                    val response = client.get("/meta/health")
                    val responseBody = response.body<String>()

                    // then: the service response status is 503
                    // and: the response body contains JSON with "mongodb": "DOWN" and "postgres": "UP"
                    response.status shouldBe HttpStatusCode.ServiceUnavailable
                    responseBody shouldContain "\"mongodb\": \"DOWN\""
                    responseBody shouldContain "\"postgres\": \"UP\""
                }
            }

            should("return 503 Service Unavailable with both databases DOWN when MongoDB is inaccessible") {
                // given: MongoDB is accessible, but we simulate failure by using invalid creds
                // and: PostgreSQL is accessible, but we simulate failure by using invalid connection

                // when: a GET request is made for "/meta/health"
                testApplication {
                    application {
                        configureAppForTesting(
                            TestDependencyInjection.healthServiceInvalidCredentials,
                            TestDependencyInjection.metaService,
                            TestDependencyInjection.versionService
                        )
                    }
                    val response = client.get("/meta/health")
                    val responseBody = response.body<String>()

                    // then: the service response status is 503
                    // and: the response body contains JSON with "mongodb": "DOWN" and "postgres": "DOWN"
                    response.status shouldBe HttpStatusCode.ServiceUnavailable
                    responseBody shouldContain "\"mongodb\": \"DOWN\""
                    responseBody shouldContain "\"postgres\": \"DOWN\""
                }
            }
        }
    }
}
