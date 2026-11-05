package io.sdkman.broker.acceptance

import arrow.core.None
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.sdkman.broker.adapter.secondary.persistence.AuditTable
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.support.MongoTestListener
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.PostgresTestSupport
import io.sdkman.broker.support.TestDependencyInjection
import io.sdkman.broker.support.configureAppForTesting
import io.sdkman.broker.support.shouldBeSomeAnd
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Tag

private const val LOOPBACK_ADDRESS = "127.0.0.1"

@Tag("acceptance")
class ClientAddressAuditAcceptanceSpec :
    ShouldSpec({
        extension(MongoTestListener)
        extension(PostgresTestListener)
        val database = Database.connect(PostgresTestListener.dataSource)

        // The test engine reports its peer as "localhost", so the peer address is only provable over a socket.
        val server =
            embeddedServer(Netty, host = LOOPBACK_ADDRESS, port = 0) {
                configureAppForTesting(
                    TestDependencyInjection.healthService,
                    TestDependencyInjection.metaService,
                    TestDependencyInjection.versionServicePostgres
                )
            }
        val client = HttpClient(OkHttp) { followRedirects = false }

        suspend fun download(
            path: String,
            vararg headers: Pair<String, String>
        ): HttpResponse {
            val port =
                server.engine
                    .resolvedConnectors()
                    .first()
                    .port
            return client.get("http://$LOOPBACK_ADDRESS:$port$path") {
                headers.forEach { (name, value) -> header(name, value) }
            }
        }

        fun seedUniversalVersion(
            candidate: String,
            version: String
        ) = PostgresTestSupport.setupVersion(
            database,
            Version(
                candidate = candidate,
                version = version,
                platform = "UNIVERSAL",
                distribution = None,
                url = "https://example.org/$candidate-$version.zip",
                visible = true
            )
        )

        beforeSpec { server.startSuspend() }

        afterSpec {
            client.close()
            server.stopSuspend()
        }

        beforeTest { PostgresTestSupport.clearVersions(database) }

        should("audit the peer address when the request carries only X-Real-IP") {
            // given
            seedUniversalVersion("gradle", "9.1.0")

            // when
            val response = download("/download/gradle/9.1.0/linuxx64", "X-Real-IP" to "10.0.0.1")

            // then
            response.status shouldBe HttpStatusCode.Found
            PostgresTestSupport.readSavedAuditRecordByVersion(
                database = database,
                candidate = "gradle",
                version = "9.1.0",
                platform = "LINUX_X64"
            ) shouldBeSomeAnd { record ->
                record[AuditTable.host] shouldBe LOOPBACK_ADDRESS
            }
        }

        should("audit the peer IP address when the request carries no forwarded header") {
            // given
            seedUniversalVersion("maven", "3.9.11")

            // when
            val response = download("/download/maven/3.9.11/linuxx64")

            // then
            response.status shouldBe HttpStatusCode.Found
            PostgresTestSupport.readSavedAuditRecordByVersion(
                database = database,
                candidate = "maven",
                version = "3.9.11",
                platform = "LINUX_X64"
            ) shouldBeSomeAnd { record ->
                record[AuditTable.host] shouldBe LOOPBACK_ADDRESS
            }
        }

        should("audit the peer address when the left-most X-Forwarded-For entry is a name") {
            // given
            seedUniversalVersion("kotlin", "2.2.21")

            // when
            val response =
                download("/download/kotlin/2.2.21/linuxx64", "X-Forwarded-For" to "unknown, 45.55.42.78")

            // then
            response.status shouldBe HttpStatusCode.Found
            PostgresTestSupport.readSavedAuditRecordByVersion(
                database = database,
                candidate = "kotlin",
                version = "2.2.21",
                platform = "LINUX_X64"
            ) shouldBeSomeAnd { record ->
                record[AuditTable.host] shouldBe LOOPBACK_ADDRESS
            }
        }

        should("audit the peer address when the left-most X-Forwarded-For entry is blank") {
            // given
            seedUniversalVersion("groovy", "4.0.28")

            // when
            val response =
                download("/download/groovy/4.0.28/linuxx64", "X-Forwarded-For" to ", 45.55.42.78")

            // then
            response.status shouldBe HttpStatusCode.Found
            PostgresTestSupport.readSavedAuditRecordByVersion(
                database = database,
                candidate = "groovy",
                version = "4.0.28",
                platform = "LINUX_X64"
            ) shouldBeSomeAnd { record ->
                record[AuditTable.host] shouldBe LOOPBACK_ADDRESS
            }
        }

        should("audit the left-most X-Forwarded-For entry trimmed of whitespace") {
            // given
            seedUniversalVersion("ant", "1.10.15")

            // when
            val response =
                download("/download/ant/1.10.15/linuxx64", "X-Forwarded-For" to "203.0.113.195 , 45.55.42.78")

            // then
            response.status shouldBe HttpStatusCode.Found
            PostgresTestSupport.readSavedAuditRecordByVersion(
                database = database,
                candidate = "ant",
                version = "1.10.15",
                platform = "LINUX_X64"
            ) shouldBeSomeAnd { record ->
                record[AuditTable.host] shouldBe "203.0.113.195"
            }
        }

        should("redirect a download that carries a non-numeric X-Forwarded-Port") {
            // given
            seedUniversalVersion("scala", "3.7.3")

            // when
            val response = download("/download/scala/3.7.3/linuxx64", "X-Forwarded-Port" to "abc")

            // then
            response.status shouldBe HttpStatusCode.Found
        }
    })
