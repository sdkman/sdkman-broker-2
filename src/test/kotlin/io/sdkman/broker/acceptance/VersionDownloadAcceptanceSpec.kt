package io.sdkman.broker.acceptance

import arrow.core.None
import arrow.core.Some
import io.kotest.assertions.fail
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.support.MongoTestListener
import io.sdkman.broker.support.TestDependencyInjection
import io.sdkman.broker.support.configureAppForTesting
import org.bson.Document

class VersionDownloadAcceptanceSpec : ShouldSpec({
    listener(MongoTestListener)

    should("redirect to platform-specific binary when exact match exists") {
        // given: Java version with ARM64 macOS binary
        setupVersion(
            Version(
                candidate = "java",
                version = "17.0.2-tem",
                platform = "DarwinARM64",
                url =
                    "https://github.com/adoptium/temurin17-binaries/releases/" +
                        "download/jdk-17.0.2%2B8/OpenJDK17U-jdk_aarch64_mac_hotspot_17.0.2_8.tar.gz",
                vendor = Some("tem"),
                visible = true,
                checksums = mapOf("SHA-256" to "abc123def456")
            )
        )

        testApplication {
            application {
                configureAppForTesting(
                    TestDependencyInjection.healthService,
                    TestDependencyInjection.releaseService,
                    TestDependencyInjection.versionService
                )
            }

            val client =
                createClient {
                    followRedirects = false
                }

            // when: client requests ARM64 macOS build
            val response = client.get("/download/java/17.0.2-tem/darwinarm64")

            // then: redirect to platform-specific binary with headers
            response.status shouldBe HttpStatusCode.Found
            response.headers["Location"] shouldBe "https://github.com/adoptium/temurin17-binaries/releases/" +
                "download/jdk-17.0.2%2B8/OpenJDK17U-jdk_aarch64_mac_hotspot_17.0.2_8.tar.gz"

            // TODO: remove header assertions, not this test's concern
            response.headers["X-Sdkman-Checksum-SHA-256"] shouldBe "abc123def456"
            response.headers["X-Sdkman-ArchiveType"] shouldBe "tar.gz"
        }
    }

    should("redirect to UNIVERSAL binary when platform-specific not found") {
        // given: Groovy version with UNIVERSAL binary only
        setupVersion(
            Version(
                candidate = "groovy",
                version = "4.0.0",
                platform = "UNIVERSAL",
                url =
                    "https://groovy.jfrog.io/artifactory/dist-release-local/" +
                        "groovy-zips/apache-groovy-binary-4.0.0.zip",
                vendor = None,
                visible = true,
                checksums = mapOf("SHA-256" to "def456ghi789", "MD5" to "ghi789jkl012")
            )
        )

        testApplication {
            application {
                configureAppForTesting(
                    TestDependencyInjection.healthService,
                    TestDependencyInjection.releaseService,
                    TestDependencyInjection.versionService
                )
            }

            val client =
                createClient {
                    followRedirects = false
                }

            // when: client requests Linux x64 build (which doesn't exist)
            val response = client.get("/download/groovy/4.0.0/linuxx64")

            // then: redirect to UNIVERSAL binary with multiple checksum headers
            response.status shouldBe HttpStatusCode.Found
            response.headers["Location"] shouldBe "https://groovy.jfrog.io/artifactory/dist-release-local/" +
                "groovy-zips/apache-groovy-binary-4.0.0.zip"

            // TODO: remove header assertions, not this test's concern
            response.headers["X-Sdkman-Checksum-SHA-256"] shouldBe "def456ghi789"
            response.headers["X-Sdkman-Checksum-MD5"] shouldBe "ghi789jkl012"
            response.headers["X-Sdkman-ArchiveType"] shouldBe "zip"
        }
    }

    should("return 400 for invalid platform") {
        testApplication {
            application {
                configureAppForTesting(
                    TestDependencyInjection.healthService,
                    TestDependencyInjection.releaseService,
                    TestDependencyInjection.versionService
                )
            }

            // when: client requests with invalid platform
            val response = client.get("/download/java/17.0.2-tem/invalidplatform")

            // then: bad request status
            response.status shouldBe HttpStatusCode.BadRequest
        }
    }

    should("return 404 when candidate not found") {
        testApplication {
            application {
                configureAppForTesting(
                    TestDependencyInjection.healthService,
                    TestDependencyInjection.releaseService,
                    TestDependencyInjection.versionService
                )
            }

            // when: client requests non-existent candidate
            val response = client.get("/download/nonexistent/1.0.0/linuxx64")

            // then: not found status
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    should("return 404 when version not found") {
        // given: Java candidate exists but not version 99.0.0
        setupVersion(
            Version(
                candidate = "java",
                version = "17.0.2-tem",
                platform = "DarwinARM64",
                url = "https://example.com/java-17.0.2.tar.gz",
                vendor = Some("tem")
            )
        )

        testApplication {
            application {
                configureAppForTesting(
                    TestDependencyInjection.healthService,
                    TestDependencyInjection.releaseService,
                    TestDependencyInjection.versionService
                )
            }

            // when: client requests non-existent version
            val response = client.get("/download/java/99.0.0/linuxx64")

            // then: not found status
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    should("return 404 when platform not found and no UNIVERSAL fallback") {
        // given: Java version with only ARM64 binary, no UNIVERSAL
        setupVersion(
            Version(
                candidate = "java",
                version = "17.0.2-tem",
                platform = "DarwinARM64",
                url = "https://example.com/java-arm64.tar.gz",
                vendor = Some("tem")
            )
        )

        testApplication {
            application {
                configureAppForTesting(
                    TestDependencyInjection.healthService,
                    TestDependencyInjection.releaseService,
                    TestDependencyInjection.versionService
                )
            }

            // when: client requests Linux x64 (no UNIVERSAL fallback)
            val response = client.get("/download/java/17.0.2-tem/linuxx64")

            // then: not found status
            response.status shouldBe HttpStatusCode.NotFound
        }
    }

    should("include multiple checksum headers when available") {
        // given: version with multiple checksums
        setupVersion(
            Version(
                candidate = "kotlin",
                version = "1.5.31",
                platform = "UNIVERSAL",
                url = "https://github.com/JetBrains/kotlin/releases/download/v1.5.31/kotlin-compiler-1.5.31.zip",
                vendor = None,
                checksums =
                    mapOf(
                        "SHA-256" to "sha256value",
                        "SHA-1" to "sha1value",
                        "MD5" to "md5value"
                    )
            )
        )

        testApplication {
            application {
                configureAppForTesting(
                    TestDependencyInjection.healthService,
                    TestDependencyInjection.releaseService,
                    TestDependencyInjection.versionService
                )
            }

            val client =
                createClient {
                    followRedirects = false
                }

            // when: successful download request
            val response = client.get("/download/kotlin/1.5.31/linuxx64")

            // then: all checksum headers present
            response.status shouldBe HttpStatusCode.Found
            response.headers["X-Sdkman-Checksum-SHA-256"] shouldBe "sha256value"
            response.headers["X-Sdkman-Checksum-SHA-1"] shouldBe "sha1value"
            response.headers["X-Sdkman-Checksum-MD5"] shouldBe "md5value"
        }
    }

    should("detect archive type from URL extension") {
        // given: versions with different archive types
        setupVersion(
            Version(
                candidate = "gradle",
                version = "7.0",
                platform = "UNIVERSAL",
                url = "https://services.gradle.org/distributions/gradle-7.0-bin.zip",
                vendor = None
            )
        )

        setupVersion(
            Version(
                candidate = "maven",
                version = "3.8.1",
                platform = "UNIVERSAL",
                url = "https://apache.org/dist/maven/maven-3/3.8.1/binaries/apache-maven-3.8.1-bin.tar.gz",
                vendor = None
            )
        )

        testApplication {
            application {
                configureAppForTesting(
                    TestDependencyInjection.healthService,
                    TestDependencyInjection.releaseService,
                    TestDependencyInjection.versionService
                )
            }

            val client =
                createClient {
                    followRedirects = false
                }

            // when: download requests for different archive types
            val zipResponse = client.get("/download/gradle/7.0/linuxx64")
            val tarGzResponse = client.get("/download/maven/3.8.1/linuxx64")

            // then: correct archive type headers
            zipResponse.headers["X-Sdkman-ArchiveType"] shouldBe "zip"
            tarGzResponse.headers["X-Sdkman-ArchiveType"] shouldBe "tar.gz"
        }
    }
})

// TODO: move this fixture helper into a `MongoSupport` helper object under the test `support` package
private fun setupVersion(version: Version) {
    val versionsCollection = MongoTestListener.database.getCollection("versions")
    versionsCollection.insertOne(
        Document().apply {
            put("candidate", version.candidate)
            put("version", version.version)
            put("platform", version.platform)
            put("url", version.url)
            version.vendor.map { put("vendor", it) }
            put("visible", version.visible)
            if (version.checksums.isNotEmpty()) {
                put("checksums", Document(version.checksums))
            }
        }
    )
}
