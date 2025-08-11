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
class SdkmanCliDownloadAcceptanceSpec : ShouldSpec({

    context("SDKMAN CLI download endpoint") {
        should("redirect to GitHub release for stable version with install command") {
            testApplication {
                application {
                    configureAppForTesting()
                }

                val client =
                    createClient {
                        followRedirects = false
                    }

                // when: client requests SDKMAN CLI stable version
                val response = client.get("/download/sdkman/install/5.19.0/linuxx64")

                // then: should redirect to GitHub release
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe
                    "https://github.com/sdkman/sdkman-cli/releases/download/5.19.0/sdkman-cli-5.19.0.zip"
                response.headers["X-Sdkman-ArchiveType"] shouldBe "zip"
            }
        }

        should("redirect to GitHub release for stable version with selfupdate command") {
            testApplication {
                application {
                    configureAppForTesting()
                }

                val client =
                    createClient {
                        followRedirects = false
                    }

                // when: client requests SDKMAN CLI stable version with selfupdate
                val response = client.get("/download/sdkman/selfupdate/5.19.0/darwinarm64")

                // then: should redirect to GitHub release
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe
                    "https://github.com/sdkman/sdkman-cli/releases/download/5.19.0/sdkman-cli-5.19.0.zip"
                response.headers["X-Sdkman-ArchiveType"] shouldBe "zip"
            }
        }

        should("redirect to GitHub release for beta version with latest+ prefix") {
            testApplication {
                application {
                    configureAppForTesting()
                }

                val client =
                    createClient {
                        followRedirects = false
                    }

                // when: client requests SDKMAN CLI beta version
                val response = client.get("/download/sdkman/install/latest+b8d230b/linuxx64")

                // then: should redirect to GitHub release with latest tag
                response.status shouldBe HttpStatusCode.Found
                response.headers[HttpHeaders.Location] shouldBe
                    "https://github.com/sdkman/sdkman-cli/releases/download/latest/sdkman-cli-latest+b8d230b.zip"
                response.headers["X-Sdkman-ArchiveType"] shouldBe "zip"
            }
        }

        should("return 400 Bad Request for invalid command") {
            testApplication {
                application {
                    configureAppForTesting()
                }

                val client =
                    createClient {
                        followRedirects = false
                    }

                // when: client requests with invalid command
                val response = client.get("/download/sdkman/invalid/5.19.0/linuxx64")

                // then: should return bad request
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        should("return 400 Bad Request for empty version") {
            testApplication {
                application {
                    configureAppForTesting()
                }

                val client =
                    createClient {
                        followRedirects = false
                    }

                // when: client requests with blank version (space)
                val response = client.get("/download/sdkman/install/%20/linuxx64")

                // then: should return bad request
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        should("return 400 Bad Request for invalid platform") {
            testApplication {
                application {
                    configureAppForTesting()
                }

                val client =
                    createClient {
                        followRedirects = false
                    }

                // when: client requests with invalid platform
                val response = client.get("/download/sdkman/install/5.19.0/invalidplatform")

                // then: should return bad request
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        should("work with different valid platforms (platform validation only)") {
            testApplication {
                application {
                    configureAppForTesting()
                }

                val client =
                    createClient {
                        followRedirects = false
                    }

                // when: client requests with different valid platforms
                val platforms = listOf("linuxx64", "linuxarm64", "darwinx64", "darwinarm64", "windowsx64")

                platforms.forEach { platform ->
                    val response = client.get("/download/sdkman/install/5.19.0/$platform")

                    // then: should redirect successfully for all valid platforms
                    response.status shouldBe HttpStatusCode.Found
                    response.headers[HttpHeaders.Location] shouldBe
                        "https://github.com/sdkman/sdkman-cli/releases/download/5.19.0/sdkman-cli-5.19.0.zip"
                    response.headers["X-Sdkman-ArchiveType"] shouldBe "zip"
                }
            }
        }

        should("handle case sensitivity correctly for commands") {
            testApplication {
                application {
                    configureAppForTesting()
                }

                val client =
                    createClient {
                        followRedirects = false
                    }

                // when: client requests with uppercase command
                val response = client.get("/download/sdkman/INSTALL/5.19.0/linuxx64")

                // then: should return bad request (case sensitive)
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        should("verify that platform parameter is validated but not used in URL construction") {
            testApplication {
                application {
                    configureAppForTesting()
                }

                val client =
                    createClient {
                        followRedirects = false
                    }

                // when: client requests with different valid platforms
                val response1 = client.get("/download/sdkman/install/5.19.0/linuxx64")
                val response2 = client.get("/download/sdkman/install/5.19.0/windowsx64")

                // then: both should redirect to same URL (platform-agnostic)
                response1.status shouldBe HttpStatusCode.Found
                response2.status shouldBe HttpStatusCode.Found
                response1.headers[HttpHeaders.Location] shouldBe response2.headers[HttpHeaders.Location]
            }
        }
    }
})
