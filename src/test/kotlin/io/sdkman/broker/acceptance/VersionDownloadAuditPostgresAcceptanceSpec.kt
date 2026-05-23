package io.sdkman.broker.acceptance

import arrow.core.None
import arrow.core.Some
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.sdkman.broker.adapter.secondary.persistence.AuditTable
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.support.MongoTestListener
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.PostgresTestSupport
import io.sdkman.broker.support.TestDependencyInjection
import io.sdkman.broker.support.configureAppForTesting
import io.sdkman.broker.support.shouldBeNone
import io.sdkman.broker.support.shouldBeSomeAnd
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Tag

@Tag("acceptance")
class VersionDownloadAuditPostgresAcceptanceSpec :
    ShouldSpec({
        listeners(MongoTestListener, PostgresTestListener)
        val database = Database.connect(PostgresTestListener.dataSource)

        beforeTest { PostgresTestSupport.clearVersions(database) }

        should("write audit row for Java platform-specific Postgres-backed download") {
            // given: TEMURIN/MAC_ARM64 row in the new-style schema (full enum name)
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "java",
                    version = "21.0.5",
                    platform = "MAC_ARM64",
                    distribution = Some("TEMURIN"),
                    url =
                        "https://github.com/adoptium/temurin21-binaries/releases/" +
                            "download/jdk-21.0.5%2B11/OpenJDK21U-jdk_aarch64_mac_hotspot_21.0.5_11.tar.gz",
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

                val response =
                    client.get("/download/java/21.0.5-tem/darwinarm64") {
                        header("X-Real-IP", "203.0.113.195")
                        header("User-Agent", "curl/7.68.0")
                    }

                response.status shouldBe HttpStatusCode.Found

                // then: audit row carries suffix-stripped version, full distribution enum name, and new-style platforms
                val savedRecord =
                    PostgresTestSupport.readSavedAuditRecordByVersion(
                        database = database,
                        candidate = "java",
                        version = "21.0.5",
                        platform = "MAC_ARM64"
                    )

                savedRecord shouldBeSomeAnd { record ->
                    record[AuditTable.command] shouldBe "install"
                    record[AuditTable.candidate] shouldBe "java"
                    record[AuditTable.version] shouldBe "21.0.5"
                    record[AuditTable.clientPlatform] shouldBe "MAC_ARM64"
                    record[AuditTable.candidatePlatform] shouldBe "MAC_ARM64"
                    record[AuditTable.distribution] shouldBe "TEMURIN"
                    record[AuditTable.host] shouldBe "203.0.113.195"
                    record[AuditTable.agent] shouldBe "curl/7.68.0"
                    record[AuditTable.timestamp] shouldNotBe null
                    record[AuditTable.id] shouldNotBe null
                }
            }
        }

        should("write audit row for non-Java UNIVERSAL fallback with NULL distribution") {
            // given: only a UNIVERSAL row exists (Business Rule 1: distribution IS NULL for non-Java)
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "scala",
                    version = "3.4.0",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url = "https://github.com/lampepfl/dotty/releases/download/3.4.0/scala3-3.4.0.tar.gz",
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

                val response =
                    client.get("/download/scala/3.4.0/linuxx64") {
                        header("X-Real-IP", "192.168.1.100")
                        header("User-Agent", "SDKMAN/5.19.0")
                    }

                response.status shouldBe HttpStatusCode.Found

                // then: client_platform = LINUX_X64, candidate_platform = UNIVERSAL, distribution = null
                val savedRecord =
                    PostgresTestSupport.readSavedAuditRecordByVersion(
                        database = database,
                        candidate = "scala",
                        version = "3.4.0",
                        platform = "LINUX_X64"
                    )

                savedRecord shouldBeSomeAnd { record ->
                    record[AuditTable.command] shouldBe "install"
                    record[AuditTable.candidate] shouldBe "scala"
                    record[AuditTable.version] shouldBe "3.4.0"
                    record[AuditTable.clientPlatform] shouldBe "LINUX_X64"
                    record[AuditTable.candidatePlatform] shouldBe "UNIVERSAL"
                    record[AuditTable.distribution] shouldBe null
                    record[AuditTable.host] shouldBe "192.168.1.100"
                    record[AuditTable.agent] shouldBe "SDKMAN/5.19.0"
                    record[AuditTable.timestamp] shouldNotBe null
                    record[AuditTable.id] shouldNotBe null
                }
            }
        }

        should("write audit row with null host when X-Real-IP is absent") {
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "groovy",
                    version = "4.5.0",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url =
                        "https://groovy.jfrog.io/artifactory/dist-release-local/" +
                            "groovy-zips/apache-groovy-binary-4.5.0.zip",
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

                val response = client.get("/download/groovy/4.5.0/windowsx64")

                response.status shouldBe HttpStatusCode.Found

                val savedRecord =
                    PostgresTestSupport.readSavedAuditRecordByVersion(
                        database = database,
                        candidate = "groovy",
                        version = "4.5.0",
                        platform = "WINDOWS_X64"
                    )

                savedRecord shouldBeSomeAnd { record ->
                    record[AuditTable.host] shouldBe null
                    record[AuditTable.agent] shouldBe "ktor-client"
                }
            }
        }

        should("not write audit row when candidate is not found") {
            testApplication {
                application {
                    configureAppForTesting(
                        TestDependencyInjection.healthService,
                        TestDependencyInjection.metaService,
                        TestDependencyInjection.versionServicePostgres
                    )
                }

                val client = createClient { followRedirects = false }

                val response =
                    client.get("/download/missingpg/1.0.0/linuxx64") {
                        header("X-Real-IP", "10.0.0.1")
                        header("User-Agent", "TestClient/1.0")
                    }

                response.status shouldBe HttpStatusCode.NotFound

                val savedRecord =
                    PostgresTestSupport.readSavedAuditRecordByVersion(
                        database = database,
                        candidate = "missingpg",
                        version = "1.0.0",
                        platform = "LINUX_X64"
                    )

                savedRecord.shouldBeNone()
            }
        }

        should("not write audit row when platform is invalid") {
            testApplication {
                application {
                    configureAppForTesting(
                        TestDependencyInjection.healthService,
                        TestDependencyInjection.metaService,
                        TestDependencyInjection.versionServicePostgres
                    )
                }

                val client = createClient { followRedirects = false }

                val response =
                    client.get("/download/java/21.0.5-tem/invalidplatform") {
                        header("X-Real-IP", "10.0.0.1")
                        header("User-Agent", "TestClient/1.0")
                    }

                response.status shouldBe HttpStatusCode.BadRequest

                // even the suffix is irrelevant here — no lookup, no audit
                val savedRecord =
                    PostgresTestSupport.readSavedAuditRecordByVersion(
                        database = database,
                        candidate = "java",
                        version = "21.0.5-tem",
                        platform = "INVALID_PLATFORM"
                    )

                savedRecord.shouldBeNone()
            }
        }

        should("still redirect with 302 when audit save fails for Postgres-backed download") {
            // given: a valid row exists in Postgres versions but the audit repository is broken
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "kotlin",
                    version = "2.0.0",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url =
                        "https://github.com/JetBrains/kotlin/releases/download/v2.0.0/" +
                            "kotlin-compiler-2.0.0.zip",
                    visible = true
                )
            )

            testApplication {
                application {
                    configureAppForTesting(
                        TestDependencyInjection.healthService,
                        TestDependencyInjection.metaService,
                        TestDependencyInjection.versionServicePostgresWithBrokenAuditRepo
                    )
                }

                val client = createClient { followRedirects = false }

                val response =
                    client.get("/download/kotlin/2.0.0/linuxx64") {
                        header("X-Real-IP", "10.0.0.1")
                        header("User-Agent", "TestClient/1.0")
                    }

                // then: audit failure does not block the redirect (Business Rule 10)
                response.status shouldBe HttpStatusCode.Found
            }
        }
    })
