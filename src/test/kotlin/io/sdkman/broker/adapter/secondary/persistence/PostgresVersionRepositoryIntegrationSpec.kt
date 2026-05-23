package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.None
import arrow.core.Some
import io.kotest.core.spec.style.ShouldSpec
import io.sdkman.broker.domain.model.JavaDistribution
import io.sdkman.broker.domain.model.Platform
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.PostgresTestSupport
import io.sdkman.broker.support.shouldBeRightAnd
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Tag

@Tag("integration")
class PostgresVersionRepositoryIntegrationSpec :
    ShouldSpec({
        register(PostgresTestListener)

        val database = Database.connect(PostgresTestListener.dataSource)
        val repository = PostgresVersionRepository(database)

        beforeTest { PostgresTestSupport.clearVersions(database) }

        should("find Java row by exact candidate, version, distribution and new-style platform") {
            // given: a TEMURIN row stored in the new-style schema
            PostgresTestSupport.setupVersion(
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

            // when: parsed (version, distribution) tuple is supplied
            val result =
                repository.findByQuery("java", "17.0.2", Some(JavaDistribution.TEMURIN), Platform.DarwinARM64)

            // then: row is returned with the full distribution name and new-style platform
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

        should("match a non-Java row with NULL distribution when distribution is None") {
            // given: a row with no distribution (Business Rule 1)
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "groovy",
                    version = "4.0.0",
                    platform = "LINUX_X64",
                    distribution = None,
                    url = "https://example.com/groovy-4.0.0.zip",
                    visible = true
                )
            )

            // when: caller omits distribution, query uses IS NOT DISTINCT FROM NULL
            val result = repository.findByQuery("groovy", "4.0.0", None, Platform.LinuxX64)

            // then: the NULL row is matched
            result shouldBeRightAnd { versionOption ->
                versionOption.fold({ false }, { it.distribution.isNone() })
            }
        }

        should("resolve a UNIVERSAL row directly when caller supplies Platform.Universal") {
            // given: a UNIVERSAL row (the only kind for non-Java multi-platform candidates)
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "groovy",
                    version = "4.0.0",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url = "https://example.com/groovy-4.0.0.zip"
                )
            )

            // when: querying with Platform.Universal (auditId == "UNIVERSAL")
            val result = repository.findByQuery("groovy", "4.0.0", None, Platform.Universal)

            // then: UNIVERSAL row is returned
            result shouldBeRightAnd { versionOption ->
                versionOption.fold({ false }, { it.platform == "UNIVERSAL" })
            }
        }

        should("not match a row whose distribution differs (TEMURIN request vs ZULU row)") {
            // given: only a ZULU UNIVERSAL row exists for java/21.0.1 (Business Rule 6)
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "java",
                    version = "21.0.1",
                    platform = "UNIVERSAL",
                    distribution = Some("ZULU"),
                    url = "https://example.com/zulu-21.0.1.tar.gz"
                )
            )

            // when: a TEMURIN request comes in for the same (candidate, version, platform)
            val result =
                repository.findByQuery("java", "21.0.1", Some(JavaDistribution.TEMURIN), Platform.Universal)

            // then: the ZULU row must NOT match — distribution preservation
            result shouldBeRightAnd { versionOption -> versionOption.isNone() }
        }

        should("not match a row whose distribution is NULL when caller supplies a distribution") {
            // given: only a NULL-distribution row exists for java/21.0.1 (defensive — see Business Rule 6)
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "java",
                    version = "21.0.1",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url = "https://example.com/java-21.0.1.tar.gz"
                )
            )

            // when: a TEMURIN request comes in
            val result =
                repository.findByQuery("java", "21.0.1", Some(JavaDistribution.TEMURIN), Platform.Universal)

            // then: NULL row must NOT satisfy a non-NULL distribution request
            result shouldBeRightAnd { versionOption -> versionOption.isNone() }
        }

        should("not match a NULL-distribution request against a row with a distribution") {
            // given: a TEMURIN row exists; caller is a non-Java request shape (None distribution)
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "groovy",
                    version = "4.0.0",
                    platform = "LINUX_X64",
                    distribution = Some("TEMURIN"),
                    url = "https://example.com/will-not-match.zip"
                )
            )

            // when: caller supplies None
            val result = repository.findByQuery("groovy", "4.0.0", None, Platform.LinuxX64)

            // then: the row with a non-NULL distribution must not match (NULL-safe equality)
            result shouldBeRightAnd { versionOption -> versionOption.isNone() }
        }

        should("project only non-null checksum columns into the checksums map") {
            // given: a row populating only SHA-256, leaving md5_sum and sha_512_sum NULL
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "groovy",
                    version = "4.0.0",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url = "https://example.com/groovy-4.0.0.zip",
                    checksums = mapOf("SHA-256" to "def456")
                )
            )

            // when: retrieving the row
            val result = repository.findByQuery("groovy", "4.0.0", None, Platform.Universal)

            // then: checksums contains only the SHA-256 entry — no MD5 or SHA-512 keys
            result shouldBeRightAnd { versionOption ->
                versionOption.fold({ false }, { it.checksums == mapOf("SHA-256" to "def456") })
            }
        }

        should("project all populated checksum columns when MD5, SHA-256 and SHA-512 are present") {
            // given: a row with every supported algorithm populated
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "groovy",
                    version = "4.0.1",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url = "https://example.com/groovy-4.0.1.zip",
                    checksums =
                        mapOf(
                            "MD5" to "md5val",
                            "SHA-256" to "sha256val",
                            "SHA-512" to "sha512val"
                        )
                )
            )

            // when: retrieving the row
            val result = repository.findByQuery("groovy", "4.0.1", None, Platform.Universal)

            // then: every algorithm is surfaced in the projection
            result shouldBeRightAnd { versionOption ->
                versionOption.fold(
                    { false },
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

        should("look up by Platform.auditId, not Platform.persistentId") {
            // given: row stored with the new-style identifier MAC_X64 (audit-style)
            // — DarwinX64.persistentId is the legacy "MAC_OSX"; auditId is "MAC_X64"
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "java",
                    version = "17.0.2",
                    platform = "MAC_X64",
                    distribution = Some("TEMURIN"),
                    url = "https://example.com/temurin-17.0.2-x64-mac.tar.gz"
                )
            )

            // when: querying with Platform.DarwinX64
            val result =
                repository.findByQuery("java", "17.0.2", Some(JavaDistribution.TEMURIN), Platform.DarwinX64)

            // then: the new-style MAC_X64 row matches (proving auditId is used)
            result shouldBeRightAnd { versionOption ->
                versionOption.fold({ false }, { it.platform == "MAC_X64" })
            }
        }

        should("return None when there is no matching row at all") {
            // given: empty versions table (cleared in beforeTest)

            // when: querying for any tuple
            val result = repository.findByQuery("nonexistent", "1.0.0", None, Platform.LinuxX64)

            // then: None is returned (no row, no error)
            result shouldBeRightAnd { versionOption -> versionOption.isNone() }
        }

        should("return None when candidate matches but version differs") {
            // given: a different version exists for the same candidate/platform
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "groovy",
                    version = "4.0.0",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url = "https://example.com/groovy-4.0.0.zip"
                )
            )

            // when: querying for a different version
            val result = repository.findByQuery("groovy", "5.0.0", None, Platform.Universal)

            // then: None is returned
            result shouldBeRightAnd { versionOption -> versionOption.isNone() }
        }

        should("expose distribution and platform as the verbatim full enum name and new-style identifier") {
            // given: a TEMURIN/MAC_ARM64 row
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "java",
                    version = "17.0.2",
                    platform = "MAC_ARM64",
                    distribution = Some("TEMURIN"),
                    url = "https://example.com/temurin-17.0.2-aarch64-mac.tar.gz"
                )
            )

            // when: retrieving the row
            val result =
                repository.findByQuery("java", "17.0.2", Some(JavaDistribution.TEMURIN), Platform.DarwinARM64)

            // then: the projection preserves the column values verbatim
            result shouldBeRightAnd { versionOption ->
                versionOption.fold({ false }, { it.distribution == Some("TEMURIN") && it.platform == "MAC_ARM64" })
            }
        }

        should("read the visible column into the Version") {
            // given: a row with visible = false
            PostgresTestSupport.setupVersion(
                database,
                Version(
                    candidate = "groovy",
                    version = "4.0.2",
                    platform = "UNIVERSAL",
                    distribution = None,
                    url = "https://example.com/groovy-4.0.2.zip",
                    visible = false
                )
            )

            // when: retrieving the row
            val result = repository.findByQuery("groovy", "4.0.2", None, Platform.Universal)

            // then: visible is reflected in the returned Version
            result shouldBeRightAnd { versionOption ->
                versionOption.fold({ false }, { !it.visible })
            }
        }
    })
