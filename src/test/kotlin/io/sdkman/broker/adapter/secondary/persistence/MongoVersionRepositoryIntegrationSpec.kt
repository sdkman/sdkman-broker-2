package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Some
import io.kotest.assertions.fail
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.support.MongoTestListener
import io.sdkman.broker.support.shouldBeRightAnd
import org.bson.Document

class MongoVersionRepositoryIntegrationSpec : ShouldSpec({
    listener(MongoTestListener)

    val repository = MongoVersionRepository(MongoTestListener.database)

    // TODO: move this fixture helper into a `MongoSupport` helper object under the test `support` package
    val versionsCollection = MongoTestListener.database.getCollection("versions")

    should("find version by exact candidate, version, and platform match") {
        // given: version record in database
        // TODO: Use new MongoSupport helper with `setupVersion` method for this MongoDB fixture
        val versionDoc =
            Document().apply {
                put("candidate", "java")
                put("version", "17.0.2-tem")
                put("platform", "DarwinARM64")
                put("url", "https://example.com/java-17.0.2-arm64.tar.gz")
                put("vendor", "tem")
                put("visible", true)
                put("checksums", Document("SHA-256", "abc123"))
            }
        versionsCollection.insertOne(versionDoc)

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
        // TODO: Use new MongoSupport helper with `setupVersion` method for this MongoDB fixture
        val versionDoc =
            Document().apply {
                put("candidate", "java")
                put("version", "11.0.2-tem")
                put("platform", "LinuxX64")
                put("url", "https://example.com/java-11.0.2.tar.gz")
            }
        versionsCollection.insertOne(versionDoc)

        // when: searching for different version
        val result = repository.findByQuery("java", "17.0.2-tem", "LinuxX64")

        // then: None is returned
        result shouldBeRightAnd { versionOption ->
            versionOption.isNone()
        }
    }

    should("return None when candidate and version match but platform does not") {
        // given: Java version for different platform
        // TODO: Use new MongoSupport helper with `setupVersion` method for this MongoDB fixture
        val versionDoc =
            Document().apply {
                put("candidate", "java")
                put("version", "17.0.2-tem")
                put("platform", "WindowsX64")
                put("url", "https://example.com/java-17.0.2-windows.zip")
            }
        versionsCollection.insertOne(versionDoc)

        // when: searching for different platform
        val result = repository.findByQuery("java", "17.0.2-tem", "LinuxX64")

        // then: None is returned
        result shouldBeRightAnd { versionOption ->
            versionOption.isNone()
        }
    }

    should("handle version with no vendor field") {
        // given: version without vendor
        // TODO: Use new MongoSupport helper with `setupVersion` method for this MongoDB fixture
        val versionDoc =
            Document().apply {
                put("candidate", "groovy")
                put("version", "4.0.0")
                put("platform", "UNIVERSAL")
                put("url", "https://example.com/groovy-4.0.0.zip")
                put("visible", true)
            }
        versionsCollection.insertOne(versionDoc)

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
        // TODO: Use new MongoSupport helper with `setupVersion` method for this MongoDB fixture
        val versionDoc =
            Document().apply {
                put("candidate", "kotlin")
                put("version", "1.5.31")
                put("platform", "UNIVERSAL")
                put("url", "https://example.com/kotlin-1.5.31.zip")
            }
        versionsCollection.insertOne(versionDoc)

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
        // TODO: Use new MongoSupport helper with `setupVersion` method for this MongoDB fixture
        val versionDoc =
            Document().apply {
                put("candidate", "gradle")
                put("version", "7.0")
                put("platform", "UNIVERSAL")
                put("url", "https://example.com/gradle-7.0.zip")
            }
        versionsCollection.insertOne(versionDoc)

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
        // TODO: Use new MongoSupport helper with `setupVersion` method for this MongoDB fixture
        val versionDoc =
            Document().apply {
                put("candidate", "maven")
                put("version", "3.8.1")
                put("platform", "UNIVERSAL")
                put("url", "https://example.com/maven-3.8.1.tar.gz")
                put(
                    "checksums",
                    Document().apply {
                        put("SHA-256", "sha256value")
                        put("SHA-1", "sha1value")
                        put("MD5", "md5value")
                    }
                )
            }
        versionsCollection.insertOne(versionDoc)

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
