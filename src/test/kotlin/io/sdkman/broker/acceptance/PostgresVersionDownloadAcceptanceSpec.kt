package io.sdkman.broker.acceptance

import arrow.core.None
import arrow.core.Some
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.PostgresVersionSupport
import io.sdkman.broker.support.TestDependencyInjection
import io.sdkman.broker.support.configureAppForTesting
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Tag

@Tag("acceptance")
class PostgresVersionDownloadAcceptanceSpec :
    ShouldSpec({
        listener(PostgresTestListener)
        val database = Database.connect(PostgresTestListener.dataSource)

        beforeTest {
            PostgresVersionSupport.truncateVersions(database)
        }

        should("redirect to platform-specific Java binary when distribution suffix matches the Postgres row") {
            // given: Java row stored with distribution=TEMURIN and new-style platform identifier.
            // Version chosen to avoid colliding with the Mongo-backed audit acceptance spec, which
            // writes to the same Postgres `audit` table without truncation between specs.
            PostgresVersionSupport.setupVersion(
                database,
                Version(
                    candidate = "java",
                    version = "17.0.5",
                    platform = "MAC_ARM64",
                    distribution = Some("TEMURIN"),
                    url =
                        "https://github.com/adoptium/temurin17-binaries/releases/" +
                            "download/jdk-17.0.5%2B8/OpenJDK17U-jdk_aarch64_mac_hotspot_17.0.5_8.tar.gz",
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

                // when: client requests Java with distribution short-code suffix
                val response = client.get("/download/java/17.0.5-tem/darwinarm64")

                // then: redirect to the binary with archive type + sha-256 headers
                response.status shouldBe HttpStatusCode.Found
                response.headers["Location"] shouldBe "https://github.com/adoptium/temurin17-binaries/releases/" +
                    "download/jdk-17.0.5%2B8/OpenJDK17U-jdk_aarch64_mac_hotspot_17.0.5_8.tar.gz"
                response.headers["X-Sdkman-ArchiveType"] shouldBe "tar.gz"
                response.headers["X-Sdkman-Checksum-SHA-256"] shouldBe "abc123def456"
            }
        }

        should("fall back to UNIVERSAL row for non-Java candidate when platform-specific row is missing") {
            // given: groovy row exists only for UNIVERSAL with NULL distribution
            PostgresVersionSupport.setupVersion(
                database,
                Version(
                    candidate = "groovy",
                    version = "4.0.5",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url =
                        "https://groovy.jfrog.io/artifactory/dist-release-local/" +
                            "groovy-zips/apache-groovy-binary-4.0.5.zip",
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

                // when: client requests Linux x64 (no platform-specific row exists)
                val response = client.get("/download/groovy/4.0.5/linuxx64")

                // then: redirect to UNIVERSAL binary with checksum + archive headers
                response.status shouldBe HttpStatusCode.Found
                response.headers["Location"] shouldBe "https://groovy.jfrog.io/artifactory/dist-release-local/" +
                    "groovy-zips/apache-groovy-binary-4.0.5.zip"
                response.headers["X-Sdkman-ArchiveType"] shouldBe "zip"
                response.headers["X-Sdkman-Checksum-SHA-256"] shouldBe "def456ghi789"
                response.headers["X-Sdkman-Checksum-MD5"] shouldBe "ghi789jkl012"
            }
        }

        should("preserve distribution when Java request falls back to UNIVERSAL") {
            // given: Java UNIVERSAL row exists for TEMURIN only (defensive scenario from the spec)
            PostgresVersionSupport.setupVersion(
                database,
                Version(
                    candidate = "java",
                    version = "21.0.1",
                    platform = "UNIVERSAL",
                    distribution = Some("TEMURIN"),
                    url = "https://example.com/temurin-21.0.1-universal.tar.gz"
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

                // when: client requests TEMURIN against linuxx64 with no platform-specific row
                val response = client.get("/download/java/21.0.1-tem/linuxx64")

                // then: UNIVERSAL TEMURIN row is served (Business Rule 6)
                response.status shouldBe HttpStatusCode.Found
                response.headers["Location"] shouldBe "https://example.com/temurin-21.0.1-universal.tar.gz"
            }
        }

        should("return 404 when Java UNIVERSAL fallback would cross distribution boundaries") {
            // given: Java UNIVERSAL row exists only for TEMURIN
            PostgresVersionSupport.setupVersion(
                database,
                Version(
                    candidate = "java",
                    version = "21.0.1",
                    platform = "UNIVERSAL",
                    distribution = Some("TEMURIN"),
                    url = "https://example.com/temurin-21.0.1-universal.tar.gz"
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

                // when: client requests ZULU which has no row at all
                val response = client.get("/download/java/21.0.1-zulu/linuxx64")

                // then: distinct distributions never match — 404 (Business Rule 6)
                response.status shouldBe HttpStatusCode.NotFound
            }
        }

        should("not strip the suffix from a hyphenated non-Java version token") {
            // given: groovy row stored with the literal hyphenated version
            PostgresVersionSupport.setupVersion(
                database,
                Version(
                    candidate = "groovy",
                    version = "3.0.0-rc-1",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url = "https://example.com/groovy-3.0.0-rc-1.zip"
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

                // when: client requests the hyphenated version verbatim
                val response = client.get("/download/groovy/3.0.0-rc-1/linuxx64")

                // then: the verbatim token resolves the row (non-Java is never suffix-stripped — Business Rule 3)
                response.status shouldBe HttpStatusCode.Found
                response.headers["Location"] shouldBe "https://example.com/groovy-3.0.0-rc-1.zip"
            }
        }

        should("return 400 for unknown platform code without performing any lookup") {
            testApplication {
                application {
                    configureAppForTesting(
                        TestDependencyInjection.healthService,
                        TestDependencyInjection.metaService,
                        TestDependencyInjection.versionServicePostgres
                    )
                }

                // when: client requests an unrecognised platform
                val response = client.get("/download/java/17.0.2-tem/invalidplatform")

                // then: bad request — Business Rule 7
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        should("return 404 for an unknown candidate") {
            testApplication {
                application {
                    configureAppForTesting(
                        TestDependencyInjection.healthService,
                        TestDependencyInjection.metaService,
                        TestDependencyInjection.versionServicePostgres
                    )
                }

                // when: client requests a candidate that has no rows
                val response = client.get("/download/nonexistent/1.0.0/linuxx64")

                // then: not found
                response.status shouldBe HttpStatusCode.NotFound
            }
        }

        should("emit one X-Sdkman-Checksum header per non-null Postgres column (MD5, SHA-256, SHA-512)") {
            // given: kotlin row populated with all three Postgres-supported algorithms
            PostgresVersionSupport.setupVersion(
                database,
                Version(
                    candidate = "kotlin",
                    version = "1.9.22",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url = "https://example.com/kotlin-1.9.22.zip",
                    checksums =
                        mapOf(
                            "MD5" to "md5val",
                            "SHA-256" to "sha256val",
                            "SHA-512" to "sha512val"
                        )
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

                // when: client requests the version
                val response = client.get("/download/kotlin/1.9.22/linuxx64")

                // then: all three checksum headers are surfaced (Business Rule 8)
                response.status shouldBe HttpStatusCode.Found
                response.headers["X-Sdkman-Checksum-MD5"] shouldBe "md5val"
                response.headers["X-Sdkman-Checksum-SHA-256"] shouldBe "sha256val"
                response.headers["X-Sdkman-Checksum-SHA-512"] shouldBe "sha512val"
            }
        }
    })
