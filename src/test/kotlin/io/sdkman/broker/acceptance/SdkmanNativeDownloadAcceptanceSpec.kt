package io.sdkman.broker.acceptance

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.sdkman.broker.support.configureAppForTesting
import org.junit.jupiter.api.Tag

@Tag("acceptance")
class SdkmanNativeDownloadAcceptanceSpec : ShouldSpec({

    context("Native CLI download endpoint") {
        should("redirect to GitHub release for install command") {
            testApplication {
                application { configureAppForTesting() }

                val client = createClient { followRedirects = false }

                // when: client requests native CLI with install command
                val response = client.get("/download/native/install/0.7.4/linuxx64")

                // then: should redirect to GitHub release with proper target triple
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe
                    "https://github.com/sdkman/sdkman-cli-native/releases/download/" +
                    "v0.7.4/sdkman-cli-native-0.7.4-x86_64-unknown-linux-gnu.zip"
                response.headers["X-Sdkman-ArchiveType"] shouldBe "zip"
            }
        }

        should("redirect to GitHub release for selfupdate command") {
            testApplication {
                application { configureAppForTesting() }

                val client = createClient { followRedirects = false }

                // when: client requests native CLI with selfupdate command
                val response = client.get("/download/native/selfupdate/0.7.4/darwinarm64")

                // then: should redirect to GitHub release with proper target triple
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe
                    "https://github.com/sdkman/sdkman-cli-native/releases/download/" +
                    "v0.7.4/sdkman-cli-native-0.7.4-aarch64-apple-darwin.zip"
                response.headers["X-Sdkman-ArchiveType"] shouldBe "zip"
            }
        }

        should("return 400 Bad Request for invalid command") {
            testApplication {
                application { configureAppForTesting() }

                val client = createClient { followRedirects = false }

                // when: client requests with invalid command
                val response = client.get("/download/native/invalid/0.7.4/linuxx64")

                // then: should return bad request
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        should("return 400 Bad Request for invalid platform") {
            testApplication {
                application { configureAppForTesting() }

                val client = createClient { followRedirects = false }

                // when: client requests with invalid platform
                val response = client.get("/download/native/install/0.7.4/invalidplatform")

                // then: should return bad request
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        should("return 400 Bad Request for empty version") {
            testApplication {
                application { configureAppForTesting() }

                val client = createClient { followRedirects = false }

                // when: client requests with blank version (encoded space)
                val response = client.get("/download/native/install/%20/linuxx64")

                // then: should return bad request
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }
    }
})
