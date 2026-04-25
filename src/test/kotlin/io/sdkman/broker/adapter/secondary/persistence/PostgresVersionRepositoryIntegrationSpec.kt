package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.None
import arrow.core.Some
import io.kotest.assertions.fail
import io.kotest.core.spec.style.ShouldSpec
import io.sdkman.broker.domain.model.Platform
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.PostgresVersionSupport
import io.sdkman.broker.support.shouldBeRightAnd
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Tag

@Tag("integration")
class PostgresVersionRepositoryIntegrationSpec :
    ShouldSpec({
        listener(PostgresTestListener)

        val repository = PostgresVersionRepository(PostgresTestListener.dataSource)
        val database = Database.connect(PostgresTestListener.dataSource)

        beforeTest {
            PostgresVersionSupport.truncateVersions(database)
        }

        should("find Java row by exact match with full distribution enum") {
            // given: Java row stored with distribution=TEMURIN and new-style platform identifier
            PostgresVersionSupport.setupVersion(
                database,
                Version(
                    candidate = "java",
                    version = "17.0.2",
                    platform = "MAC_ARM64",
                    distribution = Some("TEMURIN"),
                    url = "https://example.com/temurin-17.0.2-aarch64-mac.tar.gz",
                    visible = true,
                    checksums = mapOf("SHA-256" to "abc123")
                )
            )

            // when: caller queries with the canonical (stripped, full enum) form
            val result = repository.findByQuery("java", "17.0.2", Some("TEMURIN"), Platform.DarwinARM64)

            // then: row is returned in canonical shape — distribution stays as the full enum name
            result shouldBeRightAnd { versionOption ->
                versionOption ==
                    Some(
                        Version(
                            candidate = "java",
                            version = "17.0.2",
                            platform = "MAC_ARM64",
                            distribution = Some("TEMURIN"),
                            url = "https://example.com/temurin-17.0.2-aarch64-mac.tar.gz",
                            visible = true,
                            checksums = mapOf("SHA-256" to "abc123")
                        )
                    )
            }
        }

        should("find non-Java row by exact match with NULL distribution") {
            // given: non-Java row with distribution=NULL (Business Rule 1 of postgres_version_repository)
            PostgresVersionSupport.setupVersion(
                database,
                Version(
                    candidate = "groovy",
                    version = "4.0.0",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url = "https://example.com/groovy-4.0.0.zip",
                    visible = true
                )
            )

            // when: caller queries with no distribution
            val result = repository.findByQuery("groovy", "4.0.0", None, Platform.Universal)

            // then: row is returned with distribution=None
            result shouldBeRightAnd { versionOption ->
                versionOption ==
                    Some(
                        Version(
                            candidate = "groovy",
                            version = "4.0.0",
                            platform = "UNIVERSAL",
                            distribution = None,
                            url = "https://example.com/groovy-4.0.0.zip",
                            visible = true
                        )
                    )
            }
        }

        should("return None when no matching row exists") {
            // given: empty table

            // when: querying for a row that does not exist
            val result = repository.findByQuery("nonexistent", "1.0.0", None, Platform.LinuxX64)

            // then: None is returned
            result shouldBeRightAnd { versionOption -> versionOption.isNone() }
        }

        should("return None when Java row exists but caller asks for a different distribution") {
            // given: Java row with TEMURIN distribution
            PostgresVersionSupport.setupVersion(
                database,
                Version(
                    candidate = "java",
                    version = "21.0.1",
                    platform = "LINUX_X64",
                    distribution = Some("TEMURIN"),
                    url = "https://example.com/temurin-21.0.1.tar.gz"
                )
            )

            // when: caller queries with a different distribution (ZULU)
            val result = repository.findByQuery("java", "21.0.1", Some("ZULU"), Platform.LinuxX64)

            // then: distinct distributions never match — None is returned (Business Rule 6)
            result shouldBeRightAnd { versionOption -> versionOption.isNone() }
        }

        should("not return non-Java NULL-distribution row when caller passes Some") {
            // given: non-Java row with NULL distribution
            PostgresVersionSupport.setupVersion(
                database,
                Version(
                    candidate = "groovy",
                    version = "4.0.0",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url = "https://example.com/groovy-4.0.0.zip"
                )
            )

            // when: caller queries with Some distribution against the NULL row
            val result = repository.findByQuery("groovy", "4.0.0", Some("TEMURIN"), Platform.Universal)

            // then: IS NOT DISTINCT FROM 'TEMURIN' rejects NULL — None is returned
            result shouldBeRightAnd { versionOption -> versionOption.isNone() }
        }

        should("not return Java row with non-NULL distribution when caller passes None") {
            // given: Java row with TEMURIN distribution
            PostgresVersionSupport.setupVersion(
                database,
                Version(
                    candidate = "java",
                    version = "17.0.2",
                    platform = "MAC_ARM64",
                    distribution = Some("TEMURIN"),
                    url = "https://example.com/temurin-17.0.2-aarch64-mac.tar.gz"
                )
            )

            // when: caller omits distribution against a row whose distribution is non-NULL
            val result = repository.findByQuery("java", "17.0.2", None, Platform.DarwinARM64)

            // then: IS NULL rejects 'TEMURIN' — None is returned
            result shouldBeRightAnd { versionOption -> versionOption.isNone() }
        }

        should("populate all three checksums when md5, sha-256 and sha-512 are present") {
            // given: row with all three checksums populated
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

            // when: retrieving the row
            val result = repository.findByQuery("kotlin", "1.9.22", None, Platform.Universal)

            // then: all three checksums are surfaced
            result shouldBeRightAnd { versionOption ->
                versionOption.fold(
                    { fail("expected version row to be present") },
                    {
                        it.checksums ==
                            mapOf(
                                "MD5" to "md5val",
                                "SHA-256" to "sha256val",
                                "SHA-512" to "sha512val"
                            )
                    }
                )
            }
        }

        should("populate only the algorithms whose checksum columns are non-null") {
            // given: row with only sha-256 populated
            PostgresVersionSupport.setupVersion(
                database,
                Version(
                    candidate = "maven",
                    version = "3.9.6",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url = "https://example.com/maven-3.9.6.tar.gz",
                    checksums = mapOf("SHA-256" to "only256")
                )
            )

            // when: retrieving the row
            val result = repository.findByQuery("maven", "3.9.6", None, Platform.Universal)

            // then: only sha-256 is surfaced (Business Rule 8)
            result shouldBeRightAnd { versionOption ->
                versionOption.fold(
                    { fail("expected version row to be present") },
                    { it.checksums == mapOf("SHA-256" to "only256") }
                )
            }
        }

        should("return empty checksums when all checksum columns are NULL") {
            // given: row with no checksums populated
            PostgresVersionSupport.setupVersion(
                database,
                Version(
                    candidate = "gradle",
                    version = "8.5",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url = "https://example.com/gradle-8.5.zip"
                )
            )

            // when: retrieving the row
            val result = repository.findByQuery("gradle", "8.5", None, Platform.Universal)

            // then: checksums map is empty
            result shouldBeRightAnd { versionOption ->
                versionOption.fold(
                    { fail("expected version row to be present") },
                    { it.checksums == emptyMap<String, String>() }
                )
            }
        }

        should("return rows where visible=false (visibility filtering is sdkman-state's responsibility)") {
            // given: row stored with visible=false
            PostgresVersionSupport.setupVersion(
                database,
                Version(
                    candidate = "scala",
                    version = "3.4.0",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url = "https://example.com/scala-3.4.0.zip",
                    visible = false
                )
            )

            // when: retrieving the row
            val result = repository.findByQuery("scala", "3.4.0", None, Platform.Universal)

            // then: row is returned with visible=false — broker is read-through
            result shouldBeRightAnd { versionOption ->
                versionOption.fold(
                    { fail("expected version row to be present") },
                    { !it.visible }
                )
            }
        }
    })
