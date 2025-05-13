package io.sdkman.broker.acceptance

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.sdkman.broker.adapter.primary.rest.VersionResponse
import io.sdkman.broker.application.service.VersionServiceImpl
import io.sdkman.broker.support.configureAppForVersionTesting
import java.io.File

class VersionEndpointAcceptanceSpec : ShouldSpec({

    should("return 200 OK with version when version.properties file is present") {
        // given: a running application and the version.properties file is present
        testApplication {
            // Using the test properties file in src/test/resources
            application { configureAppForVersionTesting(VersionServiceImpl()) }

            // when: a GET request is made for "/version"
            val response = client.get("/version")
            val body = response.body<VersionResponse>()

            // then: the service response status is 200 and contains correct version
            response.status shouldBe HttpStatusCode.OK
            body.version shouldBe "1.0.0-test"
        }
    }

    should("return 500 Internal Server Error when version.properties file is not present") {
        // given: a running application but the version.properties file will not be found
        testApplication {
            // Using a service that can't find the file
            //TODO: Use a mockk or a stub instead of a real file
            val mockService = object : VersionServiceImpl() {
                override fun getVersion() =
                    //TODO: Create the Either.left() using an arrow extension function
                    arrow.core.left(io.sdkman.broker.application.service.VersionError.VersionFileError(
                        IllegalStateException("Could not load version.properties")
                    ))
            }

            application { configureAppForVersionTesting(mockService) }

            // when: a GET request is made for "/version"
            val response = client.get("/version")

            // then: the service response status is 500
            response.status shouldBe HttpStatusCode.InternalServerError
        }
    }
})
