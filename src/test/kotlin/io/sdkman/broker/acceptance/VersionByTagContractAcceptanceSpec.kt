package io.sdkman.broker.acceptance

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.sdkman.broker.adapter.secondary.persistence.AuditTable
import io.sdkman.broker.support.MongoTestListener
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.PostgresTestSupport
import io.sdkman.broker.support.TestDependencyInjection
import io.sdkman.broker.support.cliVersion
import io.sdkman.broker.support.configureAppForTesting
import io.sdkman.broker.support.whenRequested
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Tag

private fun auditRowCount(database: Database): Long = transaction(database) { AuditTable.selectAll().count() }

private suspend fun HttpResponse.shouldBeEmptyUncacheableError(expected: HttpStatusCode) {
    status shouldBe expected
    bodyAsText() shouldBe ""
    headers.contains(HttpHeaders.ContentType) shouldBe false
    headers[HttpHeaders.CacheControl] shouldBe "no-store"
}

@Tag("acceptance")
class VersionByTagContractAcceptanceSpec :
    ShouldSpec({
        extension(MongoTestListener)
        extension(PostgresTestListener)
        val database = Database.connect(PostgresTestListener.dataSource)

        beforeTest { PostgresTestSupport.clearVersions(database) }

        should("answer 200 with a text/plain content type and a UTF-8 charset") {
            // given: a stable sdkcli release on UNIVERSAL
            PostgresTestSupport.setupTaggedVersion(
                database,
                cliVersion("sdkcli", "5.18.0", "UNIVERSAL"),
                listOf("stable")
            )

            // when: its tag is requested
            whenRequested("/version/sdkcli/stable/universal") { response ->
                // then: the content type is plain text in UTF-8
                response.status shouldBe HttpStatusCode.OK
                response.headers[HttpHeaders.ContentType] shouldBe "text/plain; charset=UTF-8"
            }
        }

        should("answer 200 with the stored version as the exact body") {
            // given: a stable sdkcli release on UNIVERSAL
            PostgresTestSupport.setupTaggedVersion(
                database,
                cliVersion("sdkcli", "5.18.0", "UNIVERSAL"),
                listOf("stable")
            )

            // when: its tag is requested
            whenRequested("/version/sdkcli/stable/universal") { response ->
                // then: the body is the version alone, with no trailing newline
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe "5.18.0"
            }
        }

        should("answer 200 with a max-age of 600 seconds under the default wiring") {
            // given: a stable sdkcli release on UNIVERSAL
            PostgresTestSupport.setupTaggedVersion(
                database,
                cliVersion("sdkcli", "5.18.0", "UNIVERSAL"),
                listOf("stable")
            )

            // when: its tag is requested
            whenRequested("/version/sdkcli/stable/universal") { response ->
                // then: the edge may cache it for the default lifetime
                response.status shouldBe HttpStatusCode.OK
                response.headers[HttpHeaders.CacheControl] shouldBe "max-age=600"
            }
        }

        should("answer 200 with a max-age of 60 seconds when the lifetime is configured to 60") {
            // given: a stable sdkcli release on UNIVERSAL
            PostgresTestSupport.setupTaggedVersion(
                database,
                cliVersion("sdkcli", "5.18.0", "UNIVERSAL"),
                listOf("stable")
            )

            testApplication {
                application { configureAppForTesting(versionCacheMaxAgeSeconds = 60) }

                // when: its tag is requested
                val response = client.get("/version/sdkcli/stable/universal")

                // then: the configured lifetime replaces the default
                response.status shouldBe HttpStatusCode.OK
                response.headers[HttpHeaders.CacheControl] shouldBe "max-age=60"
            }
        }

        should("answer 400 with an empty body, no content type and no-store for an unknown platform") {
            // given: no data needed

            // when: a platform outside the download routes' set is requested
            whenRequested("/version/sdkcli/stable/bogus") { response ->
                // then: the client error is empty and uncacheable
                response.shouldBeEmptyUncacheableError(HttpStatusCode.BadRequest)
            }
        }

        should("answer 404 with an empty body, no content type and no-store for an unknown tag") {
            // given: sdkcli carries stable but not nightly
            PostgresTestSupport.setupTaggedVersion(
                database,
                cliVersion("sdkcli", "5.18.0", "UNIVERSAL"),
                listOf("stable")
            )

            // when: nightly is requested
            whenRequested("/version/sdkcli/nightly/universal") { response ->
                // then: the miss is empty and uncacheable
                response.shouldBeEmptyUncacheableError(HttpStatusCode.NotFound)
            }
        }

        should("write no audit row after a successful lookup") {
            // given: a stable sdkcli release on UNIVERSAL and an empty audit table
            PostgresTestSupport.setupTaggedVersion(
                database,
                cliVersion("sdkcli", "5.18.0", "UNIVERSAL"),
                listOf("stable")
            )

            // when: its tag is requested
            whenRequested("/version/sdkcli/stable/universal") { response ->
                response.status shouldBe HttpStatusCode.OK

                // then: a lookup is not a download
                auditRowCount(database) shouldBe 0L
            }
        }

        should("answer a candidate download redirect without a cache control header") {
            // given: a groovy release on UNIVERSAL
            PostgresTestSupport.setupVersion(
                database,
                cliVersion("groovy", "4.0.0", "UNIVERSAL")
            )

            testApplication {
                application {
                    configureAppForTesting(candidateDownloadService = TestDependencyInjection.versionServicePostgres)
                }

                val client = createClient { followRedirects = false }

                // when: the download is requested
                val response = client.get("/download/groovy/4.0.0/linuxx64")

                // then: the redirect carries no Cache-Control, unlike the version route
                response.status shouldBe HttpStatusCode.Found
                response.headers.contains(HttpHeaders.CacheControl) shouldBe false
            }
        }
    })
