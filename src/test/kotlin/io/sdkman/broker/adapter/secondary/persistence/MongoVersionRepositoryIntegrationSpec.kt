package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.None
import arrow.core.Some
import io.kotest.assertions.fail
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.support.MongoSupport.setupVersion
import io.sdkman.broker.support.MongoTestListener
import io.sdkman.broker.support.shouldBeRightAnd

class MongoVersionRepositoryIntegrationSpec : ShouldSpec({
    listener(MongoTestListener)

    val repository = MongoVersionRepository(MongoTestListener.database)


    should("find version by exact candidate, version, and platform match") {
        // given: version record in database
        setupVersion(
            Version(
                candidate = "java",
                version = "17.0.2-tem",
                platform = "DarwinARM64",
                url = "https://example.com/java-17.0.2-arm64.tar.gz",
                vendor = Some("tem"),
                visible = true,
                checksums = mapOf("SHA-256" to "abc123")
            )
        )

        // when: searching for exact match
        val result = repository.findByQuery("java", "17.0.2-tem", "DarwinARM64")

        // then: version is found
        result shouldBeRightAnd { versionOption ->
            versionOption ==
                Some(
                    Version(
                        candidate = "java",
                        version = "17.0.2-tem",
                        platform = "DarwinARM64",
                        url = "https://example.com/java-17.0.2-arm64.tar.gz",
                        vendor = Some("tem"),
                        visible = true,
                        checksums = mapOf("SHA-256" to "abc123")
                    )
                )
        }
    }

    should("return None when no matching version found") {
        // given: empty database

        // when: searching for non-existent version
        val result = repository.findByQuery("nonexistent", "1.0.0", "LinuxX64")

        // then: None is returned
        result shouldBeRightAnd { versionOption ->
            versionOption.isNone()
        }
    }

    should("return None when candidate matches but version does not") {
        // given: Java candidate with different version
        setupVersion(
            Version(
                candidate = "java",
                version = "11.0.2-tem",
                platform = "LinuxX64",
                url = "https://example.com/java-11.0.2.tar.gz",
                vendor = None
            )
        )

        // when: searching for different version
        val result = repository.findByQuery("java", "17.0.2-tem", "LinuxX64")

        // then: None is returned
        result shouldBeRightAnd { versionOption ->
            versionOption.isNone()
        }
    }

    should("return None when candidate and version match but platform does not") {
        // given: Java version for different platform
        setupVersion(
            Version(
                candidate = "java",
                version = "17.0.2-tem",
                platform = "WindowsX64",
                url = "https://example.com/java-17.0.2-windows.zip",
                vendor = None
            )
        )

        // when: searching for different platform
        val result = repository.findByQuery("java", "17.0.2-tem", "LinuxX64")

        // then: None is returned
        result shouldBeRightAnd { versionOption ->
            versionOption.isNone()
        }
    }

    should("handle version with no vendor field") {
        // given: version without vendor
        setupVersion(
            Version(
                candidate = "groovy",
                version = "4.0.0",
                platform = "UNIVERSAL",
                url = "https://example.com/groovy-4.0.0.zip",
                vendor = None,
                visible = true
            )
        )

        // when: retrieving version
        val result = repository.findByQuery("groovy", "4.0.0", "UNIVERSAL")

        // then: vendor is None
        result shouldBeRightAnd { versionOption ->
            versionOption.fold(
                { fail("version field should not be empty") },
                { it.vendor.isNone() }
            )
        }
    }

    should("handle version with no visible field defaulting to true") {
        // given: version without visible field
        setupVersion(
            Version(
                candidate = "kotlin",
                version = "1.5.31",
                platform = "UNIVERSAL",
                url = "https://example.com/kotlin-1.5.31.zip",
                vendor = None
            )
        )

        // when: retrieving version
        val result = repository.findByQuery("kotlin", "1.5.31", "UNIVERSAL")

        // then: visible defaults to true
        result shouldBeRightAnd { versionOption ->
            versionOption.fold(
                { fail("version field should not be empty") },
                { it.visible shouldBe true }
            )
        }
    }

    should("handle version with no checksums field") {
        // given: version without checksums
        setupVersion(
            Version(
                candidate = "gradle",
                version = "7.0",
                platform = "UNIVERSAL",
                url = "https://example.com/gradle-7.0.zip",
                vendor = None
            )
        )

        // when: retrieving version
        val result = repository.findByQuery("gradle", "7.0", "UNIVERSAL")

        // then: checksums is empty map
        result shouldBeRightAnd { versionOption ->
            versionOption.fold(
                { fail("version field should not be empty") },
                { it.checksums == emptyMap<String, String>() }
            )
        }
    }

    should("handle multiple checksum algorithms") {
        // given: version with multiple checksums
        setupVersion(
            Version(
                candidate = "maven",
                version = "3.8.1",
                platform = "UNIVERSAL",
                url = "https://example.com/maven-3.8.1.tar.gz",
                vendor = None,
                checksums = mapOf(
                    "SHA-256" to "sha256value",
                    "SHA-1" to "sha1value",
                    "MD5" to "md5value"
                )
            )
        )

        // when: retrieving version
        val result = repository.findByQuery("maven", "3.8.1", "UNIVERSAL")

        // then: all checksums are present
        result shouldBeRightAnd { versionOption ->
            versionOption.fold(
                { fail("version field should not be empty") },
                {
                    it.checksums ==
                        mapOf(
                            "SHA-256" to "sha256value",
                            "SHA-1" to "sha1value",
                            "MD5" to "md5value"
                        )
                }
            )
        }
    }
})
