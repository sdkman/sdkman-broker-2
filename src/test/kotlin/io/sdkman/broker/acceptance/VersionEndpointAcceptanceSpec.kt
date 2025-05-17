package io.sdkman.broker.acceptance

import arrow.core.Either
import arrow.core.left
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.sdkman.broker.application.service.VersionError
import io.sdkman.broker.application.service.VersionService
import io.sdkman.broker.application.service.VersionServiceImpl
import io.sdkman.broker.support.configureAppForVersionTesting

class VersionEndpointAcceptanceSpec : ShouldSpec({

    should("return 200 OK with version when version.properties file is present") {
        // given: a running application and the version.properties file is present
        testApplication {
            // Using the test properties file in src/test/resources
            application { configureAppForVersionTesting(VersionServiceImpl()) }

            // when: a GET request is made for "/version"
            val response = client.get("/version")
            val responseText = response.body<String>()

            // then: the service response status is 200
            response.status shouldBe HttpStatusCode.OK

            // Verify the response contains the expected version
            responseText.contains("1.0.0-test") shouldBe true
        }
    }

    should("return 500 Internal Server Error when version.properties file is not present") {
        // given: a running application but the version.properties file will not be found
        testApplication {
            // Using a service that can't find the file
            val mockService =
                object : VersionService {
                    override fun getVersion(): Either<VersionError, String> =
                        VersionError.VersionFileError(
                            IllegalStateException("Could not load version.properties")
                        ).left()
                }

            application { configureAppForVersionTesting(mockService) }

            // when: a GET request is made for "/version"
            val response = client.get("/version")

            // then: the service response status is 500
            response.status shouldBe HttpStatusCode.InternalServerError
        }
    }
})
