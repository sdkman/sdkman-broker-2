package io.sdkman.broker.application.service

import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.some
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.sdkman.broker.domain.error.RepositoryError
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.domain.repository.VersionRepository
import io.sdkman.broker.domain.util.ArchiveType

class DownloadServiceSpec : ShouldSpec({
    val mockRepository = mockk<VersionRepository>()
    val service = DownloadServiceImpl(mockRepository)

    context("resolveDownload") {

        should("return download response for exact platform match") {
            // Given
            val version = Version(
                candidate = "java",
                version = "17.0.2-tem",
                platform = "LinuxX64",
                url = "https://example.com/java.tar.gz",
                vendor = "tem",
                checksums = mapOf("sha256" to "abc123", "sha1" to "def456")
            )
            every { mockRepository.findByCandidateVersionPlatform("java", "17.0.2-tem", "LinuxX64") } returns version.some().right()

            val request = DownloadRequest("java", "17.0.2-tem", "linuxx64")

            // When
            val result = service.resolveDownload(request)

            // Then
            result shouldBe DownloadResponse(
                url = "https://example.com/java.tar.gz",
                checksums = mapOf("sha256" to "abc123", "sha1" to "def456"),
                archiveType = ArchiveType.TAR_GZ,
                resolvedPlatform = "LinuxX64"
            ).right()
        }

        should("return download response using UNIVERSAL fallback when platform-specific not found") {
            // Given
            val universalVersion = Version(
                candidate = "groovy",
                version = "4.0.0",
                platform = "UNIVERSAL",
                url = "https://example.com/groovy.zip",
                checksums = mapOf("sha256" to "universal123")
            )
            every { mockRepository.findByCandidateVersionPlatform("groovy", "4.0.0", "LinuxX64") } returns none<Version>().right()
            every { mockRepository.findByCandidateVersionPlatform("groovy", "4.0.0", "UNIVERSAL") } returns universalVersion.some().right()

            val request = DownloadRequest("groovy", "4.0.0", "linuxx64")

            // When
            val result = service.resolveDownload(request)

            // Then
            result shouldBe DownloadResponse(
                url = "https://example.com/groovy.zip",
                checksums = mapOf("sha256" to "universal123"),
                archiveType = ArchiveType.ZIP,
                resolvedPlatform = "UNIVERSAL"
            ).right()
        }

        should("prefer exact platform match over UNIVERSAL") {
            // Given
            val platformVersion = Version(
                candidate = "kotlin",
                version = "1.8.0",
                platform = "LinuxX64",
                url = "https://example.com/kotlin-linux.zip",
                checksums = mapOf("sha256" to "platform123")
            )
            every { mockRepository.findByCandidateVersionPlatform("kotlin", "1.8.0", "LinuxX64") } returns platformVersion.some().right()

            val request = DownloadRequest("kotlin", "1.8.0", "linuxx64")

            // When
            val result = service.resolveDownload(request)

            // Then
            result shouldBe DownloadResponse(
                url = "https://example.com/kotlin-linux.zip",
                checksums = mapOf("sha256" to "platform123"),
                archiveType = ArchiveType.ZIP,
                resolvedPlatform = "LinuxX64"
            ).right()
        }

        should("return InvalidPlatform error for invalid platform") {
            // Given
            val request = DownloadRequest("java", "17.0.2", "invalidplatform")

            // When
            val result = service.resolveDownload(request)

            // Then
            result shouldBe DownloadError.InvalidPlatform("invalidplatform").left()
        }

        should("return PlatformNotFound error when neither platform-specific nor UNIVERSAL version exists") {
            // Given
            every { mockRepository.findByCandidateVersionPlatform("nonexistent", "1.0.0", "LinuxX64") } returns none<Version>().right()
            every { mockRepository.findByCandidateVersionPlatform("nonexistent", "1.0.0", "UNIVERSAL") } returns none<Version>().right()

            val request = DownloadRequest("nonexistent", "1.0.0", "linuxx64")

            // When
            val result = service.resolveDownload(request)

            // Then
            result shouldBe DownloadError.PlatformNotFound("nonexistent", "1.0.0", "No platform-specific or UNIVERSAL version found").left()
        }

        should("return SystemError for database errors") {
            // Given
            every { mockRepository.findByCandidateVersionPlatform("java", "17.0.2", "LinuxX64") } returns RepositoryError.DatabaseError(RuntimeException("Connection failed")).left()

            val request = DownloadRequest("java", "17.0.2", "linuxx64")

            // When
            val result = service.resolveDownload(request)

            // Then
            result shouldBe DownloadError.SystemError("Connection failed").left()
        }

        should("prioritize checksums correctly") {
            // Given
            val version = Version(
                candidate = "gradle",
                version = "7.6",
                platform = "UNIVERSAL",
                url = "https://example.com/gradle.zip",
                checksums = mapOf(
                    "md5" to "md5hash",
                    "sha1" to "sha1hash", 
                    "sha256" to "sha256hash",
                    "sha512" to "sha512hash"
                )
            )
            every { mockRepository.findByCandidateVersionPlatform("gradle", "7.6", "LinuxX64") } returns none<Version>().right()
            every { mockRepository.findByCandidateVersionPlatform("gradle", "7.6", "UNIVERSAL") } returns version.some().right()

            val request = DownloadRequest("gradle", "7.6", "linuxx64")

            // When
            val result = service.resolveDownload(request)

            // Then
            result.fold(
                { error -> throw AssertionError("Expected success but got error: $error") },
                { response ->
                    val checksumKeys = response.checksums.keys.toList()
                    checksumKeys[0] shouldBe "sha256" // highest priority
                    checksumKeys[1] shouldBe "sha512"
                    checksumKeys[2] shouldBe "sha1"
                    checksumKeys[3] shouldBe "md5" // lowest priority
                }
            )
        }

        should("handle missing checksums gracefully") {
            // Given
            val version = Version(
                candidate = "maven",
                version = "3.9.0",
                platform = "UNIVERSAL",
                url = "https://example.com/maven.tar.gz",
                checksums = null
            )
            every { mockRepository.findByCandidateVersionPlatform("maven", "3.9.0", "LinuxX64") } returns none<Version>().right()
            every { mockRepository.findByCandidateVersionPlatform("maven", "3.9.0", "UNIVERSAL") } returns version.some().right()

            val request = DownloadRequest("maven", "3.9.0", "linuxx64")

            // When
            val result = service.resolveDownload(request)

            // Then
            result shouldBe DownloadResponse(
                url = "https://example.com/maven.tar.gz",
                checksums = emptyMap(),
                archiveType = ArchiveType.TAR_GZ,
                resolvedPlatform = "UNIVERSAL"
            ).right()
        }

        should("detect archive types correctly") {
            val testCases = listOf(
                "https://example.com/file.zip" to ArchiveType.ZIP,
                "https://example.com/file.tar.gz" to ArchiveType.TAR_GZ,
                "https://example.com/file.tgz" to ArchiveType.TAR_GZ,
                "https://example.com/file.tar.bz2" to ArchiveType.TBZ2,
                "https://example.com/file.tar.xz" to ArchiveType.XZ,
                "https://example.com/file.unknown" to ArchiveType.ZIP // default
            )

            testCases.forEach { (url, expectedType) ->
                // Given
                val version = Version(
                    candidate = "test",
                    version = "1.0.0",
                    platform = "UNIVERSAL",
                    url = url
                )
                every { mockRepository.findByCandidateVersionPlatform("test", "1.0.0", "LinuxX64") } returns none<Version>().right()
                every { mockRepository.findByCandidateVersionPlatform("test", "1.0.0", "UNIVERSAL") } returns version.some().right()

                val request = DownloadRequest("test", "1.0.0", "linuxx64")

                // When
                val result = service.resolveDownload(request)

                // Then
                result.fold(
                    { error -> throw AssertionError("Expected success but got error: $error") },
                    { response ->
                        response.archiveType shouldBe expectedType
                    }
                )
            }
        }
    }
})