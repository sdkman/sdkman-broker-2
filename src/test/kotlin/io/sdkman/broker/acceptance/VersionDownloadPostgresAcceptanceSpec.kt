package io.sdkman.broker.acceptance

import arrow.core.None
import arrow.core.Some
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.support.MongoTestListener
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.PostgresTestSupport
import io.sdkman.broker.support.TestDependencyInjection
import io.sdkman.broker.support.configureAppForTesting
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Tag

@Tag("acceptance")
class VersionDownloadPostgresAcceptanceSpec :
    ShouldSpec({
        register(MongoTestListener)
        register(PostgresTestListener)
        val database = Database.connect(PostgresTestListener.dataSource)

        beforeTest { PostgresTestSupport.clearVersions(database) }

        should("redirect to platform-specific Java binary using parsed distribution suffix") {
            // given: TEMURIN/MAC_ARM64 row stored under the new-style schema (Postgres distribution = full enum name)
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "java",
                    version = "17.0.2",
                    platform = "MAC_ARM64",
                    distribution = Some("TEMURIN"),
                    url =
                        "https://github.com/adoptium/temurin17-binaries/releases/" +
                            "download/jdk-17.0.2%2B8/OpenJDK17U-jdk_aarch64_mac_hotspot_17.0.2_8.tar.gz",
                    visible = true,
                    checksums = mapOf("SHA-256" to "abc123def456")
                )
            )

            testApplication {
                application {
                    configureAppForTesting(
                        TestDependencyInjection.healthService,
                        TestDependencyInjection.metaService,
                        TestDependencyInjection.versionServicePostgres
                    )
                }

                val client = createClient { followRedirects = false }

                // when: Java request carries the historic short-code suffix in the URL
                val response = client.get("/download/java/17.0.2-tem/darwinarm64")

                // then: Location, archive type, and checksum header are produced from the Postgres row
                response.status shouldBe HttpStatusCode.Found
                response.headers["Location"] shouldBe "https://github.com/adoptium/temurin17-binaries/releases/" +
                    "download/jdk-17.0.2%2B8/OpenJDK17U-jdk_aarch64_mac_hotspot_17.0.2_8.tar.gz"
                response.headers["X-Sdkman-ArchiveType"] shouldBe "tar.gz"
                response.headers["X-Sdkman-Checksum-SHA-256"] shouldBe "abc123def456"
            }
        }

        should("fall back to UNIVERSAL row for non-Java request when platform-specific row is absent") {
            // given: only a UNIVERSAL groovy row exists (distribution IS NULL per Business Rule 1)
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "groovy",
                    version = "4.0.0",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url =
                        "https://groovy.jfrog.io/artifactory/dist-release-local/" +
                            "groovy-zips/apache-groovy-binary-4.0.0.zip",
                    visible = true,
                    checksums = mapOf("SHA-256" to "def456ghi789", "MD5" to "ghi789jkl012")
                )
            )

            testApplication {
                application {
                    configureAppForTesting(
                        TestDependencyInjection.healthService,
                        TestDependencyInjection.metaService,
                        TestDependencyInjection.versionServicePostgres
                    )
                }

                val client = createClient { followRedirects = false }

                // when: client requests Linux x64 (no platform-specific row)
                val response = client.get("/download/groovy/4.0.0/linuxx64")

                // then: UNIVERSAL row is returned with both checksum headers
                response.status shouldBe HttpStatusCode.Found
                response.headers["Location"] shouldBe "https://groovy.jfrog.io/artifactory/dist-release-local/" +
                    "groovy-zips/apache-groovy-binary-4.0.0.zip"
                response.headers["X-Sdkman-ArchiveType"] shouldBe "zip"
                response.headers["X-Sdkman-Checksum-SHA-256"] shouldBe "def456ghi789"
                response.headers["X-Sdkman-Checksum-MD5"] shouldBe "ghi789jkl012"
            }
        }

        should("preserve distribution when Java request falls back to UNIVERSAL (defensive)") {
            // given: only a TEMURIN UNIVERSAL row exists for java/21.0.1 — defensive scenario per Business Rule 6
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "java",
                    version = "21.0.1",
                    platform = "UNIVERSAL",
                    distribution = Some("TEMURIN"),
                    url = "https://example.com/temurin-21.0.1-universal.tar.gz",
                    visible = true
                )
            )

            testApplication {
                application {
                    configureAppForTesting(
                        TestDependencyInjection.healthService,
                        TestDependencyInjection.metaService,
                        TestDependencyInjection.versionServicePostgres
                    )
                }

                val client = createClient { followRedirects = false }

                // when: Java/Linux x64 request for the same distribution falls back to the UNIVERSAL TEMURIN row
                val response = client.get("/download/java/21.0.1-tem/linuxx64")

                // then: redirect to the TEMURIN UNIVERSAL binary
                response.status shouldBe HttpStatusCode.Found
                response.headers["Location"] shouldBe "https://example.com/temurin-21.0.1-universal.tar.gz"
            }
        }

        should("return 404 when Java UNIVERSAL fallback would cross distributions (defensive)") {
            // given: only a TEMURIN UNIVERSAL row exists; no ZULU UNIVERSAL row (Business Rule 6)
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "java",
                    version = "21.0.1",
                    platform = "UNIVERSAL",
                    distribution = Some("TEMURIN"),
                    url = "https://example.com/temurin-21.0.1-universal.tar.gz",
                    visible = true
                )
            )

            testApplication {
                application {
                    configureAppForTesting(
                        TestDependencyInjection.healthService,
                        TestDependencyInjection.metaService,
                        TestDependencyInjection.versionServicePostgres
                    )
                }

                val client = createClient { followRedirects = false }

                // when: client asks for the ZULU distribution at the same version
                val response = client.get("/download/java/21.0.1-zulu/linuxx64")

                // then: 404 — distribution-preservation forbids matching the TEMURIN row
                response.status shouldBe HttpStatusCode.NotFound
            }
        }

        should("not strip suffix from hyphenated non-Java version") {
            // given: a non-Java row whose version contains hyphens (Business Rule 3)
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "groovy",
                    version = "3.0.0-rc-1",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url = "https://example.com/groovy-3.0.0-rc-1.zip",
                    visible = true
                )
            )

            testApplication {
                application {
                    configureAppForTesting(
                        TestDependencyInjection.healthService,
                        TestDependencyInjection.metaService,
                        TestDependencyInjection.versionServicePostgres
                    )
                }

                val client = createClient { followRedirects = false }

                // when: client requests the verbatim hyphenated version
                val response = client.get("/download/groovy/3.0.0-rc-1/linuxx64")

                // then: redirect succeeds — the suffix was not stripped before lookup
                response.status shouldBe HttpStatusCode.Found
                response.headers["Location"] shouldBe "https://example.com/groovy-3.0.0-rc-1.zip"
            }
        }

        should("return 400 when platform code is unknown") {
            testApplication {
                application {
                    configureAppForTesting(
                        TestDependencyInjection.healthService,
                        TestDependencyInjection.metaService,
                        TestDependencyInjection.versionServicePostgres
                    )
                }

                // when: client supplies an unknown platform
                val response = client.get("/download/java/17.0.2-tem/invalidplatform")

                // then: bad request — no lookup performed (Business Rule 7)
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        should("return 404 when no row matches the requested candidate") {
            testApplication {
                application {
                    configureAppForTesting(
                        TestDependencyInjection.healthService,
                        TestDependencyInjection.metaService,
                        TestDependencyInjection.versionServicePostgres
                    )
                }

                // when: candidate is absent from the versions table
                val response = client.get("/download/nonexistent/1.0.0/linuxx64")

                // then: 404 (no row matches)
                response.status shouldBe HttpStatusCode.NotFound
            }
        }

        should("emit only checksum headers for non-null algorithm columns") {
            // given: only SHA-256 is populated; MD5 and SHA-512 are NULL on the row
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "kotlin",
                    version = "1.5.31",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url =
                        "https://github.com/JetBrains/kotlin/releases/download/v1.5.31/" +
                            "kotlin-compiler-1.5.31.zip",
                    checksums = mapOf("SHA-256" to "sha256value")
                )
            )

            testApplication {
                application {
                    configureAppForTesting(
                        TestDependencyInjection.healthService,
                        TestDependencyInjection.metaService,
                        TestDependencyInjection.versionServicePostgres
                    )
                }

                val client = createClient { followRedirects = false }

                // when: client requests Linux x64
                val response = client.get("/download/kotlin/1.5.31/linuxx64")

                // then: only the populated SHA-256 header is present (Business Rule 8)
                response.status shouldBe HttpStatusCode.Found
                response.headers["X-Sdkman-Checksum-SHA-256"] shouldBe "sha256value"
                response.headers["X-Sdkman-Checksum-MD5"] shouldBe null
                response.headers["X-Sdkman-Checksum-SHA-512"] shouldBe null
            }
        }

        should("derive archive type header from URL extension for both zip and tar.gz") {
            // given: two rows with distinct archive extensions (Business Rule 9)
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "gradle",
                    version = "7.0",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url = "https://services.gradle.org/distributions/gradle-7.0-bin.zip"
                )
            )
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "maven",
                    version = "3.8.1",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url = "https://apache.org/dist/maven/maven-3/3.8.1/binaries/apache-maven-3.8.1-bin.tar.gz"
                )
            )

            testApplication {
                application {
                    configureAppForTesting(
                        TestDependencyInjection.healthService,
                        TestDependencyInjection.metaService,
                        TestDependencyInjection.versionServicePostgres
                    )
                }

                val client = createClient { followRedirects = false }

                // when: download requests for both extensions
                val zipResponse = client.get("/download/gradle/7.0/linuxx64")
                val tarGzResponse = client.get("/download/maven/3.8.1/linuxx64")

                // then: each archive type is derived from the URL extension only
                zipResponse.headers["X-Sdkman-ArchiveType"] shouldBe "zip"
                tarGzResponse.headers["X-Sdkman-ArchiveType"] shouldBe "tar.gz"
            }
        }
    })
