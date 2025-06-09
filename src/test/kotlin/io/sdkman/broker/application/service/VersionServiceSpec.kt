package io.sdkman.broker.application.service

import arrow.core.None
import arrow.core.Some
import arrow.core.left
import arrow.core.right
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.every
import io.mockk.mockk
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.domain.model.VersionError
import io.sdkman.broker.domain.repository.VersionRepository
import io.sdkman.broker.support.shouldBeLeft
import io.sdkman.broker.support.shouldBeRightAnd

class VersionServiceSpec : ShouldSpec({
    val mockRepository = mockk<VersionRepository>()
    val service = VersionServiceImpl(mockRepository)

    should("return download response for exact platform match") {
        // given: exact platform match exists
        val version =
            Version(
                candidate = "java",
                version = "17.0.2-tem",
                platform = "DarwinARM64",
                url = "https://example.com/java-17.0.2-arm64.tar.gz",
                vendor = Some("tem"),
                checksums = mapOf("SHA-256" to "abc123")
            )
        every { mockRepository.findByQuery("java", "17.0.2-tem", "DarwinARM64") } returns Some(version).right()

        // when: downloading version
        val result = service.downloadVersion("java", "17.0.2-tem", "darwinarm64")

        // then: download response with exact match
        result shouldBeRightAnd { response ->
            response.redirectUrl == "https://example.com/java-17.0.2-arm64.tar.gz" &&
                response.checksumHeaders == mapOf("X-Sdkman-Checksum-SHA-256" to "abc123") &&
                response.archiveType == "tar.gz"
        }
    }

    should("return download response for UNIVERSAL fallback when platform not found") {
        // given: no exact platform match but UNIVERSAL exists
        val universalVersion =
            Version(
                candidate = "groovy",
                version = "4.0.0",
                platform = "UNIVERSAL",
                url = "https://example.com/groovy-4.0.0.zip",
                checksums = mapOf("SHA-256" to "def456", "MD5" to "ghi789")
            )
        every { mockRepository.findByQuery("groovy", "4.0.0", "LinuxX64") } returns None.right()
        every {
            mockRepository.findByQuery("groovy", "4.0.0", "UNIVERSAL")
        } returns Some(universalVersion).right()

        // when: downloading version for Linux
        val result = service.downloadVersion("groovy", "4.0.0", "linuxx64")

        // then: download response with UNIVERSAL fallback
        result shouldBeRightAnd { response ->
            response.redirectUrl == "https://example.com/groovy-4.0.0.zip" &&
                response.checksumHeaders ==
                mapOf(
                    "X-Sdkman-Checksum-SHA-256" to "def456",
                    "X-Sdkman-Checksum-MD5" to "ghi789"
                ) &&
                response.archiveType == "zip"
        }
    }

    should("return InvalidPlatform error for invalid platform code") {
        // when: using invalid platform
        val result = service.downloadVersion("java", "17.0.2-tem", "invalidplatform")

        // then: InvalidPlatform error
        result shouldBeLeft VersionError.InvalidPlatform("invalidplatform")
    }

    should("return VersionNotFound error when no version exists") {
        // given: no version found for exact or UNIVERSAL
        every { mockRepository.findByQuery("nonexistent", "1.0.0", "LinuxX64") } returns None.right()
        every { mockRepository.findByQuery("nonexistent", "1.0.0", "UNIVERSAL") } returns None.right()

        // when: downloading non-existent version
        val result = service.downloadVersion("nonexistent", "1.0.0", "linuxx64")

        // then: VersionNotFound error
        result shouldBeLeft VersionError.VersionNotFound("nonexistent", "1.0.0", "LinuxX64")
    }

    should("return VersionNotFound error when platform not found and no UNIVERSAL fallback") {
        // given: no exact match and no UNIVERSAL fallback
        every { mockRepository.findByQuery("java", "17.0.2-tem", "LinuxX64") } returns None.right()
        every { mockRepository.findByQuery("java", "17.0.2-tem", "UNIVERSAL") } returns None.right()

        // when: downloading version for unsupported platform
        val result = service.downloadVersion("java", "17.0.2-tem", "linuxx64")

        // then: VersionNotFound error
        result shouldBeLeft VersionError.VersionNotFound("java", "17.0.2-tem", "LinuxX64")
    }

    should("return DatabaseError when repository fails") {
        // given: repository error
        val dbError = RuntimeException("Database connection failed")
        every {
            mockRepository.findByQuery("java", "17.0.2-tem", "DarwinARM64")
        } returns VersionError.DatabaseError(dbError).left()

        // when: downloading version
        val result = service.downloadVersion("java", "17.0.2-tem", "darwinarm64")

        // then: DatabaseError propagated
        result shouldBeLeft VersionError.DatabaseError(dbError)
    }

    should("detect archive type from URL extensions") {
        // given: versions with different URL extensions
        val zipVersion =
            Version(
                candidate = "gradle",
                version = "7.0",
                platform = "UNIVERSAL",
                url = "https://example.com/gradle-7.0.zip"
            )
        val tarGzVersion =
            Version(
                candidate = "maven",
                version = "3.8.1",
                platform = "UNIVERSAL",
                url = "https://example.com/maven-3.8.1.tar.gz"
            )
        val tgzVersion =
            Version(
                candidate = "ant",
                version = "1.10.12",
                platform = "UNIVERSAL",
                url = "https://example.com/ant-1.10.12.tgz"
            )

        every { mockRepository.findByQuery("gradle", "7.0", "UNIVERSAL") } returns Some(zipVersion).right()
        every { mockRepository.findByQuery("maven", "3.8.1", "UNIVERSAL") } returns Some(tarGzVersion).right()
        every { mockRepository.findByQuery("ant", "1.10.12", "UNIVERSAL") } returns Some(tgzVersion).right()

        // when: downloading different archive types
        val zipResult = service.downloadVersion("gradle", "7.0", "universal")
        val tarGzResult = service.downloadVersion("maven", "3.8.1", "universal")
        val tgzResult = service.downloadVersion("ant", "1.10.12", "universal")

        // then: correct archive types detected
        zipResult shouldBeRightAnd { response -> response.archiveType == "zip" }
        tarGzResult shouldBeRightAnd { response -> response.archiveType == "tar.gz" }
        tgzResult shouldBeRightAnd { response -> response.archiveType == "tar.gz" }
    }

    should("handle version with no checksums") {
        // given: version with no checksums
        val version =
            Version(
                candidate = "kotlin",
                version = "1.5.31",
                platform = "UNIVERSAL",
                url = "https://example.com/kotlin-1.5.31.zip",
                checksums = emptyMap()
            )
        every { mockRepository.findByQuery("kotlin", "1.5.31", "UNIVERSAL") } returns Some(version).right()

        // when: downloading version
        val result = service.downloadVersion("kotlin", "1.5.31", "universal")

        // then: empty checksum headers
        result shouldBeRightAnd { response ->
            response.checksumHeaders.isEmpty()
        }
    }

    should("convert checksum algorithm names to uppercase in headers") {
        // given: version with lowercase checksum algorithm names
        val version =
            Version(
                candidate = "scala",
                version = "2.13.8",
                platform = "UNIVERSAL",
                url = "https://example.com/scala-2.13.8.tgz",
                checksums =
                    mapOf(
                        "sha-256" to "abc123",
                        "md5" to "def456"
                    )
            )
        every { mockRepository.findByQuery("scala", "2.13.8", "UNIVERSAL") } returns Some(version).right()

        // when: downloading version
        val result = service.downloadVersion("scala", "2.13.8", "universal")

        // then: checksum headers use uppercase algorithm names
        result shouldBeRightAnd { response ->
            response.checksumHeaders ==
                mapOf(
                    "X-Sdkman-Checksum-SHA-256" to "abc123",
                    "X-Sdkman-Checksum-MD5" to "def456"
                )
        }
    }
})
