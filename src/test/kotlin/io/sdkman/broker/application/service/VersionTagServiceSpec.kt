package io.sdkman.broker.application.service

import arrow.core.None
import arrow.core.Some
import arrow.core.left
import arrow.core.right
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.sdkman.broker.domain.model.Platform
import io.sdkman.broker.domain.model.VersionTagError
import io.sdkman.broker.domain.repository.VersionTagRepository
import io.sdkman.broker.support.shouldBeLeft
import io.sdkman.broker.support.shouldBeRight

class VersionTagServiceSpec :
    ShouldSpec({
        val mockVersionTagRepository = mockk<VersionTagRepository>()
        val underTest = VersionTagServiceImpl(mockVersionTagRepository)

        beforeEach {
            clearMocks(mockVersionTagRepository)
        }

        should("return the tagged version for a CLI candidate") {
            // given: sdkcli carries the stable tag on UNIVERSAL
            every {
                mockVersionTagRepository.findTaggedVersion("sdkcli", "stable", Platform.Universal)
            } returns Some("5.18.0").right()

            // when: looking up the stable version
            val result = underTest.findVersionByTag("sdkcli", "stable", "universal")

            // then: the stored version
            result shouldBeRight "5.18.0"
        }

        should("match the platform code without regard to case") {
            // given: sdkcli carries the latest tag on UNIVERSAL
            every {
                mockVersionTagRepository.findTaggedVersion("sdkcli", "latest", Platform.Universal)
            } returns Some("5.24.0").right()

            // when: looking up with an upper-case platform code
            val result = underTest.findVersionByTag("sdkcli", "latest", "UNIVERSAL")

            // then: the stored version
            result shouldBeRight "5.24.0"
        }

        should("return InvalidPlatform for an unknown platform code on a CLI candidate") {
            // when: looking up with a platform outside the download routes' set
            val result = underTest.findVersionByTag("sdkcli", "stable", "bogus")

            // then: InvalidPlatform carrying the code as given
            result shouldBeLeft VersionTagError.InvalidPlatform("bogus")
        }

        should("check the platform before the candidate") {
            // when: looking up a non-CLI candidate with an unknown platform
            val result = underTest.findVersionByTag("gradle", "stable", "bogus")

            // then: InvalidPlatform, not TagNotFound
            result shouldBeLeft VersionTagError.InvalidPlatform("bogus")
        }

        should("return TagNotFound and skip the repository for a non-CLI candidate") {
            // when: looking up a candidate that is neither sdkcli nor sdkrs
            val result = underTest.findVersionByTag("gradle", "stable", "universal")

            // then: TagNotFound
            result shouldBeLeft VersionTagError.TagNotFound("gradle", "stable", "UNIVERSAL")

            // and: the repository is never asked
            verify(exactly = 0) { mockVersionTagRepository.findTaggedVersion(any(), any(), any()) }
        }

        should("return TagNotFound when the repository finds no tagged version") {
            // given: no row carries the tag on that platform
            every {
                mockVersionTagRepository.findTaggedVersion("sdkcli", "nightly", Platform.Universal)
            } returns None.right()

            // when: looking up the missing tag
            val result = underTest.findVersionByTag("sdkcli", "nightly", "universal")

            // then: TagNotFound naming the platform's stored id
            result shouldBeLeft VersionTagError.TagNotFound("sdkcli", "nightly", "UNIVERSAL")
        }

        should("return DatabaseError when the repository fails") {
            // given: the repository fails
            val dbError = RuntimeException("Database connection failed")
            every {
                mockVersionTagRepository.findTaggedVersion("sdkrs", "stable", Platform.LinuxX64)
            } returns VersionTagError.DatabaseError(dbError).left()

            // when: looking up the version
            val result = underTest.findVersionByTag("sdkrs", "stable", "linuxx64")

            // then: DatabaseError propagated
            result shouldBeLeft VersionTagError.DatabaseError(dbError)
        }
    })
