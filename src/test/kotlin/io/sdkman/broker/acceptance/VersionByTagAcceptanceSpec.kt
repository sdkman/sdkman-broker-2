package io.sdkman.broker.acceptance

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.support.MongoTestListener
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.PostgresTestSupport
import io.sdkman.broker.support.configureAppForTesting
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Tag

private fun whenRequested(
    path: String,
    then: suspend (HttpResponse) -> Unit
) = testApplication {
    application { configureAppForTesting() }
    then(client.get(path))
}

private fun cliVersion(
    candidate: String,
    version: String,
    platform: String,
    visible: Boolean = true
) = Version(
    candidate = candidate,
    version = version,
    platform = platform,
    url = "https://example.com/$candidate-$version-$platform.zip",
    visible = visible
)

@Tag("acceptance")
class VersionByTagAcceptanceSpec :
    ShouldSpec({
        extension(MongoTestListener)
        extension(PostgresTestListener)
        val database = Database.connect(PostgresTestListener.dataSource)

        beforeTest { PostgresTestSupport.clearVersions(database) }

        should("answer the stable sdkcli version on universal as bare text") {
            // given: two sdkcli releases, each carrying its own tag
            PostgresTestSupport.setupTaggedVersion(
                database,
                cliVersion("sdkcli", "5.18.0", "UNIVERSAL"),
                listOf("stable")
            )
            PostgresTestSupport.setupTaggedVersion(
                database,
                cliVersion("sdkcli", "5.24.0", "UNIVERSAL"),
                listOf("latest")
            )

            // when: the stable tag is requested
            whenRequested("/version/sdkcli/stable/universal") { response ->
                // then: 200 with the stored version and nothing else
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe "5.18.0"
            }
        }

        should("answer the latest sdkcli version when the platform code is upper case") {
            // given: a latest sdkcli release on UNIVERSAL
            PostgresTestSupport.setupTaggedVersion(
                database,
                cliVersion("sdkcli", "5.24.0", "UNIVERSAL"),
                listOf("latest")
            )

            // when: the platform is written UNIVERSAL
            whenRequested("/version/sdkcli/latest/UNIVERSAL") { response ->
                // then: platform matching ignores case
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe "5.24.0"
            }
        }

        should("answer the stable sdkrs version on linuxx64") {
            // given: a stable sdkrs release on LINUX_X64
            PostgresTestSupport.setupTaggedVersion(
                database,
                cliVersion("sdkrs", "0.7.19", "LINUX_X64"),
                listOf("stable")
            )

            // when: stable is requested for linuxx64
            whenRequested("/version/sdkrs/stable/linuxx64") { response ->
                // then: the native version answers
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe "0.7.19"
            }
        }

        should("answer the stable sdkrs version on linuxarm64") {
            // given: stable sdkrs releases on two native platforms, moving together
            PostgresTestSupport.setupTaggedVersion(
                database,
                cliVersion("sdkrs", "0.7.19", "LINUX_X64"),
                listOf("stable")
            )
            PostgresTestSupport.setupTaggedVersion(
                database,
                cliVersion("sdkrs", "0.7.19", "LINUX_ARM64"),
                listOf("stable")
            )

            // when: stable is requested for linuxarm64
            whenRequested("/version/sdkrs/stable/linuxarm64") { response ->
                // then: the same version answers
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe "0.7.19"
            }
        }

        should("answer the latest sdkrs version on linuxx64") {
            // given: stable and latest sdkrs releases on LINUX_X64, one row per tag
            PostgresTestSupport.setupTaggedVersion(
                database,
                cliVersion("sdkrs", "0.7.19", "LINUX_X64"),
                listOf("stable")
            )
            PostgresTestSupport.setupTaggedVersion(
                database,
                cliVersion("sdkrs", "0.8.0", "LINUX_X64"),
                listOf("latest")
            )

            // when: latest is requested for linuxx64
            whenRequested("/version/sdkrs/latest/linuxx64") { response ->
                // then: the latest-tagged release answers
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe "0.8.0"
            }
        }

        should("answer a tagged version that is not visible") {
            // given: a tagged sdkcli row hidden from listings
            PostgresTestSupport.setupTaggedVersion(
                database,
                cliVersion("sdkcli", "5.18.0", "UNIVERSAL", visible = false),
                listOf("stable")
            )

            // when: its tag is requested
            whenRequested("/version/sdkcli/stable/universal") { response ->
                // then: visibility makes no difference
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldBe "5.18.0"
            }
        }

        should("answer 404 with an empty body when the platform has no tagged row for sdkrs") {
            // given: sdkrs tagged on LINUX_X64 only
            PostgresTestSupport.setupTaggedVersion(
                database,
                cliVersion("sdkrs", "0.7.19", "LINUX_X64"),
                listOf("stable")
            )

            // when: stable is requested for universal
            whenRequested("/version/sdkrs/stable/universal") { response ->
                // then: not found
                response.status shouldBe HttpStatusCode.NotFound
                response.bodyAsText() shouldBe ""
            }
        }

        should("answer 404 with an empty body without falling back to universal") {
            // given: a stable sdkcli tag on UNIVERSAL only
            PostgresTestSupport.setupTaggedVersion(
                database,
                cliVersion("sdkcli", "5.18.0", "UNIVERSAL"),
                listOf("stable")
            )

            // when: stable is requested for linuxarm64
            whenRequested("/version/sdkcli/stable/linuxarm64") { response ->
                // then: the universal row does not answer
                response.status shouldBe HttpStatusCode.NotFound
                response.bodyAsText() shouldBe ""
            }
        }

        should("answer 404 with an empty body for a tag the candidate does not carry") {
            // given: sdkcli carries stable but not nightly
            PostgresTestSupport.setupTaggedVersion(
                database,
                cliVersion("sdkcli", "5.18.0", "UNIVERSAL"),
                listOf("stable")
            )

            // when: nightly is requested
            whenRequested("/version/sdkcli/nightly/universal") { response ->
                // then: not found
                response.status shouldBe HttpStatusCode.NotFound
                response.bodyAsText() shouldBe ""
            }
        }

        should("answer 404 with an empty body for a candidate other than sdkcli or sdkrs") {
            // given: gradle carries the stable tag on UNIVERSAL
            PostgresTestSupport.setupTaggedVersion(database, cliVersion("gradle", "8.5", "UNIVERSAL"), listOf("stable"))

            // when: its tag is requested
            whenRequested("/version/gradle/stable/universal") { response ->
                // then: the route does not serve it
                response.status shouldBe HttpStatusCode.NotFound
                response.bodyAsText() shouldBe ""
            }
        }

        should("answer 400 with an empty body for an unknown platform") {
            // given: no data needed

            // when: a platform outside the download routes' set is requested
            whenRequested("/version/sdkcli/stable/bogus") { response ->
                // then: client error
                response.status shouldBe HttpStatusCode.BadRequest
                response.bodyAsText() shouldBe ""
            }
        }

        should("check the platform before the candidate") {
            // given: no data needed

            // when: an unserved candidate comes with an unknown platform
            whenRequested("/version/gradle/stable/bogus") { response ->
                // then: the platform decides
                response.status shouldBe HttpStatusCode.BadRequest
                response.bodyAsText() shouldBe ""
            }
        }
    })
