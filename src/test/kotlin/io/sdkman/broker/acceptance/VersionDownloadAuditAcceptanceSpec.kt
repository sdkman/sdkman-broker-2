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
import io.sdkman.broker.support.MongoSupport.setupVersion
import io.sdkman.broker.support.MongoTestListener
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.PostgresTestSupport
import io.sdkman.broker.support.TestDependencyInjection
import io.sdkman.broker.support.configureAppForTesting
import io.sdkman.broker.support.shouldBeNone
import io.sdkman.broker.support.shouldBeSomeAnd
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Tag

// TODO: only test most important paths in this acceptance spec,
//  rely on unit and integration tests for fine grained testing
@Tag("acceptance")
class VersionDownloadAuditAcceptanceSpec : ShouldSpec({
    listeners(MongoTestListener, PostgresTestListener)
    val database = Database.connect(PostgresTestListener.dataSource)

    should("create audit entry for successful platform-specific download with headers") {
        setupVersion(
            Version(
                candidate = "java",
                version = "17.0.2-tem",
                platform = "MAC_ARM64",
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

            val client = createClient { followRedirects = false }

            val response =
                client.get("/download/java/17.0.2-tem/darwinarm64") {
                    header("X-Real-IP", "203.0.113.195")
                    header("User-Agent", "curl/7.68.0")
                }

            response.status shouldBe HttpStatusCode.Found

            val savedRecord =
                PostgresTestSupport.readSavedAuditRecordByVersion(
                    database = database,
                    candidate = "java",
                    version = "17.0.2-tem",
                    platform = "MAC_ARM64"
                )

            savedRecord shouldBeSomeAnd { record ->
                record[AuditTable.command] shouldBe "install"
                record[AuditTable.candidate] shouldBe "java"
                record[AuditTable.version] shouldBe "17.0.2-tem"
                record[AuditTable.platform] shouldBe "MAC_ARM64"
                record[AuditTable.dist] shouldBe "MAC_ARM64"
                record[AuditTable.vendor] shouldBe "tem"
                record[AuditTable.host] shouldBe "203.0.113.195"
                record[AuditTable.agent] shouldBe "curl/7.68.0"
                record[AuditTable.timestamp] shouldNotBe null
                record[AuditTable.id] shouldNotBe null
            }
        }
    }

    should("create audit entry for UNIVERSAL fallback download") {
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

            val client = createClient { followRedirects = false }

            val response =
                client.get("/download/groovy/4.0.0/linuxx64") {
                    header("X-Real-IP", "192.168.1.100")
                    header("User-Agent", "SDKMAN/5.19.0")
                }

            response.status shouldBe HttpStatusCode.Found

            val savedRecord =
                PostgresTestSupport.readSavedAuditRecordByVersion(
                    database = database,
                    candidate = "groovy",
                    version = "4.0.0",
                    platform = "LINUX_64"
                )

            savedRecord shouldBeSomeAnd { record ->
                record[AuditTable.command] shouldBe "install"
                record[AuditTable.candidate] shouldBe "groovy"
                record[AuditTable.version] shouldBe "4.0.0"
                record[AuditTable.platform] shouldBe "LINUX_64"
                record[AuditTable.dist] shouldBe "UNIVERSAL"
                record[AuditTable.vendor] shouldBe null
                record[AuditTable.host] shouldBe "192.168.1.100"
                record[AuditTable.agent] shouldBe "SDKMAN/5.19.0"
                record[AuditTable.timestamp] shouldNotBe null
                record[AuditTable.id] shouldNotBe null
            }
        }
    }

    should("create audit entry with no host when IP address header missing") {
        setupVersion(
            Version(
                candidate = "kotlin",
                version = "1.6.0",
                platform = "UNIVERSAL",
                url =
                    "https://github.com/JetBrains/kotlin/releases/" +
                        "download/v1.6.0/kotlin-compiler-1.6.0.zip",
                vendor = None,
                visible = true
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

            val client = createClient { followRedirects = false }

            val response = client.get("/download/kotlin/1.6.0/windowsx64")

            response.status shouldBe HttpStatusCode.Found

            val savedRecord =
                PostgresTestSupport.readSavedAuditRecordByVersion(
                    database = database,
                    candidate = "kotlin",
                    version = "1.6.0",
                    platform = "WINDOWS_64"
                )

            savedRecord shouldBeSomeAnd { record ->
                record[AuditTable.host] shouldBe null
                record[AuditTable.agent] shouldBe "Ktor client"
            }
        }
    }

    // TODO: Remove this pointless test
    should("create audit entry with partial headers when only one is present") {
        setupVersion(
            Version(
                candidate = "scala",
                version = "2.13.8",
                platform = "UNIVERSAL",
                url = "https://downloads.lightbend.com/scala/2.13.8/scala-2.13.8.tgz",
                vendor = None,
                visible = true
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

            val client = createClient { followRedirects = false }

            val response =
                client.get("/download/scala/2.13.8/darwinarm64") {
                    header("X-Real-IP", "10.0.0.1")
                }

            response.status shouldBe HttpStatusCode.Found

            val savedRecord =
                PostgresTestSupport.readSavedAuditRecordByVersion(
                    database = database,
                    candidate = "scala",
                    version = "2.13.8",
                    platform = "MAC_ARM64"
                )

            savedRecord.isSome() shouldBe true
            savedRecord.map { record ->
                record[AuditTable.command] shouldBe "install"
                record[AuditTable.candidate] shouldBe "scala"
                record[AuditTable.version] shouldBe "2.13.8"
                record[AuditTable.platform] shouldBe "MAC_ARM64"
                record[AuditTable.dist] shouldBe "UNIVERSAL"
                record[AuditTable.vendor] shouldBe null
                record[AuditTable.host] shouldBe "10.0.0.1"
                record[AuditTable.agent] shouldBe "Ktor client"
                record[AuditTable.timestamp] shouldNotBe null
                record[AuditTable.id] shouldNotBe null
                true
            }
        }
    }

    should("NOT create audit entry for failed requests") {
        testApplication {
            application {
                configureAppForTesting(
                    TestDependencyInjection.healthService,
                    TestDependencyInjection.releaseService,
                    TestDependencyInjection.versionService
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
                    platform = "LINUX_64"
                )

            savedRecord.shouldBeNone()
        }
    }

    should("NOT create audit entry for invalid platform requests") {
        testApplication {
            application {
                configureAppForTesting(
                    TestDependencyInjection.healthService,
                    TestDependencyInjection.releaseService,
                    TestDependencyInjection.versionService
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
                    platform = "invalidplatform"
                )

            savedRecord.shouldBeNone()
        }
    }
})
