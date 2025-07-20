package io.sdkman.broker.application.service

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.some
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.sdkman.broker.adapter.primary.rest.AuditContext
import io.sdkman.broker.domain.model.Audit
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.domain.model.VersionError
import io.sdkman.broker.domain.repository.AuditRepository
import io.sdkman.broker.domain.repository.VersionRepository
import io.sdkman.broker.support.shouldBeLeft
import io.sdkman.broker.support.shouldBeRightAnd

class VersionServiceSpec : ShouldSpec({
    val mockVersionRepository = mockk<VersionRepository>()
    val mockAuditRepository = mockk<AuditRepository>()
    val service = VersionServiceImpl(mockVersionRepository, mockAuditRepository)

    val testAuditContext =
        AuditContext(
            host = Option.fromNullable("192.168.1.100"),
            agent = Option.fromNullable("SDKMAN/5.19.0")
        )

    beforeEach {
        clearMocks(mockVersionRepository, mockAuditRepository)
    }

    should("return download response and record audit for specific platform match") {
        // given: exact platform match exists
        val platformSpecificVersion =
            Version(
                candidate = "java",
                version = "17.0.2-tem",
                platform = "MAC_ARM64",
                url = "https://example.com/java-17.0.2-arm64.tar.gz",
                vendor = Some("tem"),
                checksums = mapOf("SHA-256" to "abc123")
            )

        every {
            mockVersionRepository.findByQuery(
                "java",
                "17.0.2-tem",
                "MAC_ARM64"
            )
        } returns Some(platformSpecificVersion).right()
        every { mockAuditRepository.save(any()) } returns Unit.right()

        // when: downloading version
        val result = service.downloadVersion("java", "17.0.2-tem", "darwinarm64", testAuditContext)

        // then: download response with exact match
        result shouldBeRightAnd { response ->
            response.redirectUrl == "https://example.com/java-17.0.2-arm64.tar.gz" &&
                response.checksumHeaders == mapOf("X-Sdkman-Checksum-SHA-256" to "abc123") &&
                response.archiveType == "tar.gz"
        }

        // and: audit entry persisted
        val auditSlot = slot<Audit>()
        verify { mockAuditRepository.save(capture(auditSlot)) }
        auditSlot.captured.apply {
            command shouldBe "install"
            version shouldBe "17.0.2"
            candidate shouldBe "java"
            platform shouldBe "MAC_ARM64"
            dist shouldBe "MAC_ARM64"
            vendor shouldBe "tem".some()
            host shouldBe testAuditContext.host
            agent shouldBe testAuditContext.agent
        }
    }

    should("return download response and record audit for UNIVERSAL fallback when platform match not found") {
        // given: no exact platform match but UNIVERSAL exists
        val universalVersion =
            Version(
                candidate = "groovy",
                version = "4.0.0",
                platform = "UNIVERSAL",
                url = "https://example.com/groovy-4.0.0.zip",
                checksums = mapOf("SHA-256" to "def456", "MD5" to "ghi789")
            )
        every { mockVersionRepository.findByQuery("groovy", "4.0.0", "LINUX_64") } returns None.right()
        every {
            mockVersionRepository.findByQuery("groovy", "4.0.0", "UNIVERSAL")
        } returns Some(universalVersion).right()
        every { mockAuditRepository.save(any()) } returns Unit.right()

        // when: downloading version for Linux
        val result = service.downloadVersion("groovy", "4.0.0", "linuxx64", testAuditContext)

        // then: download response with UNIVERSAL fallback
        result shouldBeRightAnd { response ->
            response.redirectUrl == "https://example.com/groovy-4.0.0.zip" &&
                response.checksumHeaders ==
                mapOf(
                    "X-Sdkman-Checksum-SHA-256" to "def456",
                    "X-Sdkman-Checksum-MD5" to "ghi789"
                ) && response.archiveType == "zip"
        }

        // and: audit entry persisted
        val auditSlot = slot<Audit>()
        verify { mockAuditRepository.save(capture(auditSlot)) }
        auditSlot.captured.apply {
            command shouldBe "install"
            candidate shouldBe "groovy"
            version shouldBe "4.0.0"
            platform shouldBe "LINUX_64"
            dist shouldBe "UNIVERSAL"
            vendor shouldBe none()
            host shouldBe testAuditContext.host
            agent shouldBe testAuditContext.agent
        }
    }

    should("return InvalidPlatform error with no audit for invalid platform code") {
        // when: using invalid platform
        val result = service.downloadVersion("java", "17.0.2-tem", "invalidplatform", testAuditContext)

        // then: InvalidPlatform error
        result shouldBeLeft VersionError.InvalidPlatform("invalidplatform")

        // and: record no audit
        verify(exactly = 0) { mockAuditRepository.save(any()) }
    }

    should("return VersionNotFound error and record no audit when no version exists") {
        // given: no version found for exact or UNIVERSAL
        every { mockVersionRepository.findByQuery("nonexistent", "1.0.0", "LINUX_64") } returns None.right()
        every { mockVersionRepository.findByQuery("nonexistent", "1.0.0", "UNIVERSAL") } returns None.right()

        // when: downloading non-existent version
        val result = service.downloadVersion("nonexistent", "1.0.0", "linuxx64", testAuditContext)

        // then: VersionNotFound error
        result shouldBeLeft VersionError.VersionNotFound("nonexistent", "1.0.0", "LINUX_64")

        // and: record no audit
        verify(exactly = 0) { mockAuditRepository.save(any()) }
    }

    should("return VersionNotFound error and record no audit when platform not found and no UNIVERSAL fallback") {
        // given: no exact match and no UNIVERSAL fallback
        every { mockVersionRepository.findByQuery("java", "17.0.2-tem", "LINUX_64") } returns None.right()
        every { mockVersionRepository.findByQuery("java", "17.0.2-tem", "UNIVERSAL") } returns None.right()

        // when: downloading version for unsupported platform
        val result = service.downloadVersion("java", "17.0.2-tem", "linuxx64", testAuditContext)

        // then: VersionNotFound error
        result shouldBeLeft VersionError.VersionNotFound("java", "17.0.2-tem", "LINUX_64")

        // and: record no audit
        verify(exactly = 0) { mockAuditRepository.save(any()) }
    }

    should("return DatabaseError and record no audit when repository fails") {
        // given: repository error
        val dbError = RuntimeException("Database connection failed")
        every {
            mockVersionRepository.findByQuery("java", "17.0.2-tem", "MAC_ARM64")
        } returns VersionError.DatabaseError(dbError).left()

        // when: downloading version
        val result = service.downloadVersion("java", "17.0.2-tem", "darwinarm64", testAuditContext)

        // then: DatabaseError propagated
        result shouldBeLeft VersionError.DatabaseError(dbError)

        // and: record no audit
        verify(exactly = 0) { mockAuditRepository.save(any()) }
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

        every { mockVersionRepository.findByQuery("gradle", "7.0", "UNIVERSAL") } returns Some(zipVersion).right()
        every { mockVersionRepository.findByQuery("maven", "3.8.1", "UNIVERSAL") } returns Some(tarGzVersion).right()
        every { mockVersionRepository.findByQuery("ant", "1.10.12", "UNIVERSAL") } returns Some(tgzVersion).right()
        every { mockAuditRepository.save(any()) } returns Unit.right()

        // when: downloading different archive types
        val zipResult = service.downloadVersion("gradle", "7.0", "universal", testAuditContext)
        val tarGzResult = service.downloadVersion("maven", "3.8.1", "universal", testAuditContext)
        val tgzResult = service.downloadVersion("ant", "1.10.12", "universal", testAuditContext)

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
        every { mockVersionRepository.findByQuery("kotlin", "1.5.31", "UNIVERSAL") } returns Some(version).right()
        every { mockAuditRepository.save(any()) } returns Unit.right()

        // when: downloading version
        val result = service.downloadVersion("kotlin", "1.5.31", "universal", testAuditContext)

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
        every { mockVersionRepository.findByQuery("scala", "2.13.8", "UNIVERSAL") } returns Some(version).right()
        every { mockAuditRepository.save(any()) } returns Unit.right()

        // when: downloading version
        val result = service.downloadVersion("scala", "2.13.8", "universal", testAuditContext)

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
