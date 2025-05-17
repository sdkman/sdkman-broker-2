package io.sdkman.broker.acceptance

import arrow.core.Either
import arrow.core.left
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.sdkman.broker.application.service.ReleaseError
import io.sdkman.broker.application.service.ReleaseService
import io.sdkman.broker.application.service.ReleaseServiceImpl
import io.sdkman.broker.support.TestDependencyInjection
import io.sdkman.broker.support.configureAppForTesting

class ReleaseEndpointAcceptanceSpec : ShouldSpec({

    should("return 200 OK with release when release.properties file is present") {
        // given: a running application and the release.properties file is present
        testApplication {
            // Using the test properties file in src/test/resources
            application {
                configureAppForTesting(
                    TestDependencyInjection.healthService,
                    ReleaseServiceImpl()
                )
            }

            // when: a GET request is made for "/meta/release"
            val response = client.get("/meta/release")
            val responseText = response.body<String>()

            // then: the service response status is 200
            response.status shouldBe HttpStatusCode.OK

            // Verify the response contains the expected release
            responseText.contains("1.0.0-test") shouldBe true
        }
    }

    should("return 500 Internal Server Error when release.properties file is not present") {
        // given: a running application but the release.properties file will not be found
        testApplication {
            // Using a service that can't find the file
            val mockService =
                object : ReleaseService {
                    override fun getRelease(): Either<ReleaseError, String> =
                        ReleaseError.ReleaseFileError(
                            IllegalStateException("Could not load release.properties")
                        ).left()
                }

            application {
                configureAppForTesting(
                    TestDependencyInjection.healthService,
                    mockService
                )
            }

            // when: a GET request is made for "/meta/release"
            val response = client.get("/meta/release")

            // then: the service response status is 500
            response.status shouldBe HttpStatusCode.InternalServerError

            // Verify the response contains the expected error message
            response.body<String>().contains("Could not load release.properties") shouldBe true
        }
    }
})
