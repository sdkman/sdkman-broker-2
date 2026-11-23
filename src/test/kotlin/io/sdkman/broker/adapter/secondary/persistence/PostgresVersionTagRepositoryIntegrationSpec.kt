package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.None
import arrow.core.Some
import io.kotest.core.spec.style.ShouldSpec
import io.sdkman.broker.domain.model.Platform
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.domain.model.VersionTagError
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.PostgresTestSupport
import io.sdkman.broker.support.shouldBeLeftAnd
import io.sdkman.broker.support.shouldBeRight
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Tag

@Tag("integration")
class PostgresVersionTagRepositoryIntegrationSpec :
    ShouldSpec({
        extension(PostgresTestListener)

        val database = Database.connect(PostgresTestListener.dataSource)
        val repository = PostgresVersionTagRepository(database)

        beforeTest { PostgresTestSupport.clearVersions(database) }

        should("return the version carrying the tag, not another version of the candidate") {
            // given: two sdkcli releases on UNIVERSAL, each carrying its own tag
            PostgresTestSupport.setupTaggedVersion(
                database,
                Version(
                    candidate = "sdkcli",
                    version = "5.18.0",
                    platform = "UNIVERSAL",
                    url = "https://example.com/sdkcli-5.18.0.zip"
                ),
                listOf("stable")
            )
            PostgresTestSupport.setupTaggedVersion(
                database,
                Version(
                    candidate = "sdkcli",
                    version = "5.24.0",
                    platform = "UNIVERSAL",
                    url = "https://example.com/sdkcli-5.24.0.zip"
                ),
                listOf("latest")
            )

            // when: the stable tag is looked up
            val result = repository.findTaggedVersion("sdkcli", "stable", Platform.Universal)

            // then: the stable release answers
            result shouldBeRight Some("5.18.0")
        }

        should("match a native platform on its audit id") {
            // given: an sdkrs row stored under the audit id LINUX_X64
            PostgresTestSupport.setupTaggedVersion(
                database,
                Version(
                    candidate = "sdkrs",
                    version = "0.7.19",
                    platform = "LINUX_X64",
                    url = "https://example.com/sdkrs-0.7.19-linux-x64.tar.gz"
                ),
                listOf("stable", "latest")
            )

            // when: looked up with the linuxx64 platform
            val result = repository.findTaggedVersion("sdkrs", "latest", Platform.LinuxX64)

            // then: the row is found
            result shouldBeRight Some("0.7.19")
        }

        should("return a tagged version that is not visible") {
            // given: a tagged row hidden from listings
            PostgresTestSupport.setupTaggedVersion(
                database,
                Version(
                    candidate = "sdkcli",
                    version = "5.18.0",
                    platform = "UNIVERSAL",
                    url = "https://example.com/sdkcli-5.18.0.zip",
                    visible = false
                ),
                listOf("stable")
            )

            // when: the tag is looked up
            val result = repository.findTaggedVersion("sdkcli", "stable", Platform.Universal)

            // then: visibility makes no difference
            result shouldBeRight Some("5.18.0")
        }

        should("not fall back to UNIVERSAL when the platform carries no tag") {
            // given: only a UNIVERSAL row carries the tag
            PostgresTestSupport.setupTaggedVersion(
                database,
                Version(
                    candidate = "sdkcli",
                    version = "5.18.0",
                    platform = "UNIVERSAL",
                    url = "https://example.com/sdkcli-5.18.0.zip"
                ),
                listOf("stable")
            )

            // when: another platform is looked up
            val result = repository.findTaggedVersion("sdkcli", "stable", Platform.LinuxARM64)

            // then: there is no answer
            result shouldBeRight None
        }

        should("not return a row that carries a distribution") {
            // given: a tagged row with a distribution
            PostgresTestSupport.setupTaggedVersion(
                database,
                Version(
                    candidate = "java",
                    version = "21.0.1",
                    platform = "UNIVERSAL",
                    distribution = Some("TEMURIN"),
                    url = "https://example.com/temurin-21.0.1.tar.gz"
                ),
                listOf("latest")
            )

            // when: the tag is looked up with no distribution
            val result = repository.findTaggedVersion("java", "latest", Platform.Universal)

            // then: the distribution row does not match
            result shouldBeRight None
        }

        should("return nothing for a tag the candidate does not carry") {
            // given: a row carrying only the stable tag
            PostgresTestSupport.setupTaggedVersion(
                database,
                Version(
                    candidate = "sdkcli",
                    version = "5.18.0",
                    platform = "UNIVERSAL",
                    url = "https://example.com/sdkcli-5.18.0.zip"
                ),
                listOf("stable")
            )

            // when: an unknown tag is looked up
            val result = repository.findTaggedVersion("sdkcli", "nightly", Platform.Universal)

            // then: there is no answer
            result shouldBeRight None
        }

        should("return a DatabaseError when the database is unreachable") {
            // given: a repository over an unreachable database
            val invalidDataSource =
                PostgresTestListener.createDataSource(
                    "jdbc:postgresql://invalid-host:5432/invalid-db",
                    "invalid-user",
                    "invalid-password"
                )
            val invalidRepository = PostgresVersionTagRepository(Database.connect(invalidDataSource))

            // when: a tag is looked up
            val result = invalidRepository.findTaggedVersion("sdkcli", "stable", Platform.Universal)

            // then: the failure surfaces as a DatabaseError
            result shouldBeLeftAnd { error -> error is VersionTagError.DatabaseError }
        }
    })
