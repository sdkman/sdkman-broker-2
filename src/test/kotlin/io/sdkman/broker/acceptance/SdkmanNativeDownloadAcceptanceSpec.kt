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
        should("redirect to GitHub release for linuxx64 platform with install command") {
            testApplication {
                application { configureAppForTesting() }

                val client = createClient { followRedirects = false }

                // when: client requests native CLI for linuxx64
                val response = client.get("/download/native/install/0.7.4/linuxx64")

                // then: should redirect to GitHub release with proper target triple
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe
                    "https://github.com/sdkman/sdkman-cli-native/releases/download/" +
                    "v0.7.4/sdkman-cli-native-0.7.4-x86_64-unknown-linux-gnu.zip"
                response.headers["X-Sdkman-ArchiveType"] shouldBe "zip"
            }
        }

        should("redirect to GitHub release for darwinarm64 platform with selfupdate command") {
            testApplication {
                application { configureAppForTesting() }

                val client = createClient { followRedirects = false }

                // when: client requests native CLI for darwinarm64 with selfupdate
                val response = client.get("/download/native/selfupdate/0.7.4/darwinarm64")

                // then: should redirect to GitHub release with proper target triple
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe
                    "https://github.com/sdkman/sdkman-cli-native/releases/download/" +
                    "v0.7.4/sdkman-cli-native-0.7.4-aarch64-apple-darwin.zip"
                response.headers["X-Sdkman-ArchiveType"] shouldBe "zip"
            }
        }

        should("redirect to GitHub release for windowsx64 platform") {
            testApplication {
                application { configureAppForTesting() }

                val client = createClient { followRedirects = false }

                // when: client requests native CLI for windowsx64
                val response = client.get("/download/native/install/0.8.0/windowsx64")

                // then: should redirect to GitHub release with proper target triple
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe
                    "https://github.com/sdkman/sdkman-cli-native/releases/download/" +
                    "v0.8.0/sdkman-cli-native-0.8.0-x86_64-pc-windows-msvc.zip"
                response.headers["X-Sdkman-ArchiveType"] shouldBe "zip"
            }
        }

        should("handle all supported platforms correctly") {
            testApplication {
                application { configureAppForTesting() }

                val client = createClient { followRedirects = false }

                // when: client requests with different supported platforms
                val platformTargetMap =
                    mapOf(
                        "linuxx64" to "x86_64-unknown-linux-gnu",
                        "linuxarm64" to "aarch64-unknown-linux-gnu",
                        "linuxx32" to "i686-unknown-linux-gnu",
                        "darwinx64" to "x86_64-apple-darwin",
                        "darwinarm64" to "aarch64-apple-darwin",
                        "windowsx64" to "x86_64-pc-windows-msvc"
                    )

                platformTargetMap.forEach { (platform, targetTriple) ->
                    val response = client.get("/download/native/install/0.7.4/$platform")

                    // then: should redirect successfully with correct target triple
                    response.status shouldBe HttpStatusCode.Found
                    response.headers[HttpHeaders.Location] shouldBe
                        "https://github.com/sdkman/sdkman-cli-native/releases/download/" +
                        "v0.7.4/sdkman-cli-native-0.7.4-$targetTriple.zip"
                    response.headers["X-Sdkman-ArchiveType"] shouldBe "zip"
                }
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

        should("return 400 Bad Request for exotic platform") {
            testApplication {
                application { configureAppForTesting() }

                val client = createClient { followRedirects = false }

                // when: client requests with exotic platform
                val response = client.get("/download/native/install/0.7.4/exotic")

                // then: should return bad request
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        should("return 400 Bad Request for unsupported platforms") {
            testApplication {
                application { configureAppForTesting() }

                val client = createClient { followRedirects = false }

                // when: client requests with unsupported platforms
                val unsupportedPlatforms = listOf("linuxarm32hf", "linuxarm32sf", "universal")

                unsupportedPlatforms.forEach { platform ->
                    val response = client.get("/download/native/install/0.7.4/$platform")

                    // then: should return bad request
                    response.status shouldBe HttpStatusCode.BadRequest
                }
            }
        }

        should("handle case sensitivity correctly for commands") {
            testApplication {
                application { configureAppForTesting() }

                val client = createClient { followRedirects = false }

                // when: client requests with uppercase command
                val response = client.get("/download/native/INSTALL/0.7.4/linuxx64")

                // then: should return bad request (case sensitive)
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        should("verify URL construction includes version prefix 'v'") {
            testApplication {
                application { configureAppForTesting() }

                val client = createClient { followRedirects = false }

                // when: client requests native CLI
                val response = client.get("/download/native/install/1.0.0/linuxx64")

                // then: should redirect with version prefixed with 'v'
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe
                    "https://github.com/sdkman/sdkman-cli-native/releases/download/" +
                    "v1.0.0/sdkman-cli-native-1.0.0-x86_64-unknown-linux-gnu.zip"
            }
        }

        should("verify that platform parameter affects URL construction (unlike SDKMAN CLI)") {
            testApplication {
                application { configureAppForTesting() }

                val client = createClient { followRedirects = false }

                // when: client requests with different platforms
                val response1 = client.get("/download/native/install/0.7.4/linuxx64")
                val response2 = client.get("/download/native/install/0.7.4/windowsx64")

                // then: should redirect to different URLs (platform-specific)
                response1.status shouldBe HttpStatusCode.Found
                response2.status shouldBe HttpStatusCode.Found
                response1.headers[HttpHeaders.Location] shouldBe
                    "https://github.com/sdkman/sdkman-cli-native/releases/download/" +
                    "v0.7.4/sdkman-cli-native-0.7.4-x86_64-unknown-linux-gnu.zip"
                response2.headers[HttpHeaders.Location] shouldBe
                    "https://github.com/sdkman/sdkman-cli-native/releases/download/" +
                    "v0.7.4/sdkman-cli-native-0.7.4-x86_64-pc-windows-msvc.zip"

                // URLs should be different due to platform-specific target triples
                response1.headers[HttpHeaders.Location] shouldBe response1.headers[HttpHeaders.Location]
                response2.headers[HttpHeaders.Location] shouldBe response2.headers[HttpHeaders.Location]
            }
        }

        should("work with both install and selfupdate commands") {
            testApplication {
                application { configureAppForTesting() }

                val client = createClient { followRedirects = false }

                // when: client requests with both valid commands
                val commands = listOf("install", "selfupdate")

                commands.forEach { command ->
                    val response = client.get("/download/native/$command/0.7.4/linuxx64")

                    // then: should redirect successfully for both commands
                    response.status shouldBe HttpStatusCode.Found
                    response.headers[HttpHeaders.Location] shouldBe
                        "https://github.com/sdkman/sdkman-cli-native/releases/download/" +
                        "v0.7.4/sdkman-cli-native-0.7.4-x86_64-unknown-linux-gnu.zip"
                    response.headers["X-Sdkman-ArchiveType"] shouldBe "zip"
                }
            }
        }
    }
})
