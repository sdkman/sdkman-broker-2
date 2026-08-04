package io.sdkman.broker.acceptance

import arrow.core.Either
import arrow.core.None
import arrow.core.left
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.sdkman.broker.adapter.primary.rest.AuditContext
import io.sdkman.broker.domain.model.DownloadInfo
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.domain.model.VersionError
import io.sdkman.broker.domain.service.CandidateDownloadService
import io.sdkman.broker.support.MongoTestListener
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.PostgresTestSupport
import io.sdkman.broker.support.TestDependencyInjection
import io.sdkman.broker.support.configureAppForTesting
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Tag

private fun failingService(error: VersionError): CandidateDownloadService =
    object : CandidateDownloadService {
        override fun downloadVersion(
            candidate: String,
            version: String,
            platformCode: String,
            auditContext: AuditContext
        ): Either<VersionError, DownloadInfo> = error.left()
    }

private fun throwingService(cause: Throwable): CandidateDownloadService =
    object : CandidateDownloadService {
        override fun downloadVersion(
            candidate: String,
            version: String,
            platformCode: String,
            auditContext: AuditContext
        ): Either<VersionError, DownloadInfo> = throw cause
    }

@Tag("acceptance")
class ErrorResponseContractAcceptanceSpec :
    ShouldSpec({
        extension(MongoTestListener)
        extension(PostgresTestListener)
        val database = Database.connect(PostgresTestListener.dataSource)

        beforeTest { PostgresTestSupport.clearVersions(database) }

        should("return a specific INVALID_PLATFORM body naming the offending platform at 400") {
            testApplication {
                application {
                    configureAppForTesting(
                        TestDependencyInjection.healthService,
                        TestDependencyInjection.metaService,
                        TestDependencyInjection.versionServicePostgres
                    )
                }

                // when: the client supplies an unknown platform code
                val response = client.get("/download/java/17.0.2-tem/nonsense")
                val body = response.body<String>()

                // then: 400 with a JSON body that echoes the offending client input
                response.status shouldBe HttpStatusCode.BadRequest
                body shouldContain "\"error\": \"INVALID_PLATFORM\""
                body shouldContain "nonsense"
            }
        }

        should("return a descriptive VERSION_NOT_FOUND body naming candidate, version, and platform at 404") {
            testApplication {
                application {
                    configureAppForTesting(
                        TestDependencyInjection.healthService,
                        TestDependencyInjection.metaService,
                        TestDependencyInjection.versionServicePostgres
                    )
                }

                // when: no row matches the requested candidate/version
                val response = client.get("/download/groovy/9.9.9/linuxx64")
                val body = response.body<String>()

                // then: 404 with a JSON body naming the missing coordinates
                response.status shouldBe HttpStatusCode.NotFound
                body shouldContain "\"error\": \"VERSION_NOT_FOUND\""
                body shouldContain "groovy"
                body shouldContain "9.9.9"
                // the error carries the platform's persistent id (linuxx64 -> LINUX_64)
                body shouldContain "LINUX_64"
            }
        }

        should("return a generic INTERNAL_ERROR body that leaks no cause detail when the backend errors at 500") {
            val cause = RuntimeException("permission denied for table versions")
            testApplication {
                application {
                    configureAppForTesting(
                        TestDependencyInjection.healthService,
                        TestDependencyInjection.metaService,
                        failingService(VersionError.DatabaseError(cause))
                    )
                }

                // when: the backend raises a database error on lookup
                val response = client.get("/download/java/25.0.4-tem/linuxx64")
                val body = response.body<String>()

                // then: 500 with a generic body disclosing no SQL, role, or host detail (BR-4)
                response.status shouldBe HttpStatusCode.InternalServerError
                body shouldContain "\"error\": \"INTERNAL_ERROR\""
                body shouldNotContain "permission denied"
                body shouldNotContain "versions"
            }
        }

        should("convert an unhandled exception escaping the handler into a generic INTERNAL_ERROR 500") {
            val cause = IllegalStateException("boom: jdbc:postgresql://secret-host/sdkman")
            testApplication {
                application {
                    configureAppForTesting(
                        TestDependencyInjection.healthService,
                        TestDependencyInjection.metaService,
                        throwingService(cause)
                    )
                }

                // when: a handler throws an exception no VersionError branch anticipated
                val response = client.get("/download/java/25.0.4-tem/linuxx64")
                val body = response.body<String>()

                // then: the StatusPages safety net renders a generic 500 body (BR-6)
                response.status shouldBe HttpStatusCode.InternalServerError
                body shouldContain "\"error\": \"INTERNAL_ERROR\""
                body shouldNotContain "secret-host"
            }
        }

        should("leave the 302 success contract untouched with an empty body") {
            // given: a matching version record exists
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "gradle",
                    version = "8.5",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url = "https://services.gradle.org/distributions/gradle-8.5-bin.zip",
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

                // when: the client requests a valid download URL
                val response = client.get("/download/gradle/8.5/linuxx64")

                // then: 302 with no error body
                response.status shouldBe HttpStatusCode.Found
                response.headers["Location"] shouldBe "https://services.gradle.org/distributions/gradle-8.5-bin.zip"
                response.body<String>() shouldBe ""
            }
        }
    })
