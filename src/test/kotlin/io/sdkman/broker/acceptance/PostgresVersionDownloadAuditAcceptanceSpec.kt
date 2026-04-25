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
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.PostgresTestSupport
import io.sdkman.broker.support.PostgresVersionSupport
import io.sdkman.broker.support.TestDependencyInjection
import io.sdkman.broker.support.configureAppForTesting
import io.sdkman.broker.support.shouldBeNone
import io.sdkman.broker.support.shouldBeSomeAnd
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Tag

@Tag("acceptance")
class PostgresVersionDownloadAuditAcceptanceSpec :
    ShouldSpec({
        listener(PostgresTestListener)
        val database = Database.connect(PostgresTestListener.dataSource)

        beforeTest {
            PostgresVersionSupport.truncateVersions(database)
            transaction(database) { AuditTable.deleteAll() }
        }

        should("write audit row with canonical version and full distribution enum for Java download") {
            // Note: 17.0.5/MAC_ARM64 is chosen so that the (candidate, version, clientPlatform)
            // triple does not collide with the rows the Mongo-backed audit acceptance spec
            // writes to the same Postgres `audit` table — there is no cross-spec truncation.
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

                val response =
                    client.get("/download/java/17.0.5-tem/darwinarm64") {
                        header("X-Real-IP", "203.0.113.195")
                        header("User-Agent", "curl/7.68.0")
                    }

                response.status shouldBe HttpStatusCode.Found

                val savedRecord =
                    PostgresTestSupport.readSavedAuditRecordByVersion(
                        database = database,
                        candidate = "java",
                        version = "17.0.5",
                        platform = "MAC_ARM64"
                    )

                savedRecord shouldBeSomeAnd { record ->
                    record[AuditTable.command] shouldBe "install"
                    record[AuditTable.candidate] shouldBe "java"
                    record[AuditTable.version] shouldBe "17.0.5"
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

        should("write audit row with UNIVERSAL candidate platform and null distribution for non-Java fallback") {
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

                val response =
                    client.get("/download/groovy/4.0.5/linuxx64") {
                        header("X-Real-IP", "192.168.1.100")
                        header("User-Agent", "SDKMAN/5.19.0")
                    }

                response.status shouldBe HttpStatusCode.Found

                val savedRecord =
                    PostgresTestSupport.readSavedAuditRecordByVersion(
                        database = database,
                        candidate = "groovy",
                        version = "4.0.5",
                        platform = "LINUX_X64"
                    )

                savedRecord shouldBeSomeAnd { record ->
                    record[AuditTable.command] shouldBe "install"
                    record[AuditTable.candidate] shouldBe "groovy"
                    record[AuditTable.version] shouldBe "4.0.5"
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

        should("leave the audit host column null when the X-Real-IP header is absent") {
            PostgresVersionSupport.setupVersion(
                database,
                Version(
                    candidate = "kotlin",
                    version = "1.6.10",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url =
                        "https://github.com/JetBrains/kotlin/releases/" +
                            "download/v1.6.10/kotlin-compiler-1.6.10.zip",
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

                val response = client.get("/download/kotlin/1.6.10/windowsx64")

                response.status shouldBe HttpStatusCode.Found

                val savedRecord =
                    PostgresTestSupport.readSavedAuditRecordByVersion(
                        database = database,
                        candidate = "kotlin",
                        version = "1.6.10",
                        platform = "WINDOWS_X64"
                    )

                savedRecord shouldBeSomeAnd { record ->
                    record[AuditTable.host] shouldBe null
                    record[AuditTable.agent] shouldBe "Ktor client"
                }
            }
        }

        should("not write any audit row when the candidate is unknown") {
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
                    client.get("/download/nonexistent/1.0.0/linuxx64") {
                        header("X-Real-IP", "10.0.0.1")
                        header("User-Agent", "TestClient/1.0")
                    }

                response.status shouldBe HttpStatusCode.NotFound

                val savedRecord =
                    PostgresTestSupport.readSavedAuditRecordByVersion(
                        database = database,
                        candidate = "nonexistent",
                        version = "1.0.0",
                        platform = "LINUX_X64"
                    )

                savedRecord.shouldBeNone()
            }
        }

        should("not write any audit row when the platform code is unrecognised") {
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
                    client.get("/download/java/17.0.2-tem/invalidplatform") {
                        header("X-Real-IP", "10.0.0.1")
                        header("User-Agent", "TestClient/1.0")
                    }

                response.status shouldBe HttpStatusCode.BadRequest

                val savedRecord =
                    PostgresTestSupport.readSavedAuditRecordByVersion(
                        database = database,
                        candidate = "java",
                        version = "17.0.2-tem",
                        platform = "INVALID_PLATFORM"
                    )

                savedRecord.shouldBeNone()
            }
        }

        should("still respond 302 when the audit write fails (best-effort audit per Business Rule 10)") {
            PostgresVersionSupport.setupVersion(
                database,
                Version(
                    candidate = "kotlin",
                    version = "1.6.10",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url =
                        "https://github.com/JetBrains/kotlin/releases/" +
                            "download/v1.6.10/kotlin-compiler-1.6.10.zip",
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
                    client.get("/download/kotlin/1.6.10/linuxx64") {
                        header("X-Real-IP", "10.0.0.1")
                        header("User-Agent", "TestClient/1.0")
                    }

                response.status shouldBe HttpStatusCode.Found
            }
        }
    })
