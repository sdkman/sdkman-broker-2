package io.sdkman.broker.application.service

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.should
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.sdkman.broker.support.beLeftAnd
import io.sdkman.broker.support.shouldBeRight
import java.io.ByteArrayInputStream

class MetaReleaseServiceSpec : ShouldSpec({

    should("successfully return release when properties file exists with valid release") {
        // given: a mocked classloader that returns a valid properties file
        val mockClassLoader = mockk<ClassLoader>()
        val propertiesContent = "release=1.2.3"
        val inputStream = ByteArrayInputStream(propertiesContent.toByteArray())

        every {
            mockClassLoader.getResourceAsStream("release.properties")
        } returns inputStream

        val releaseService = MetaReleaseServiceImpl(mockClassLoader)

        // when: getting the release
        val result = releaseService.getReleaseVersion()

        // then: release is returned successfully
        result shouldBeRight "1.2.3"
    }

    should("return error when properties file exists but release key is missing") {
        // given: a mocked classloader that returns a properties file without release
        val mockClassLoader = mockk<ClassLoader>()
        val propertiesContent = "someOtherKey=value"
        val inputStream = ByteArrayInputStream(propertiesContent.toByteArray())

        every {
            mockClassLoader.getResourceAsStream("release.properties")
        } returns inputStream

        val releaseService = MetaReleaseServiceImpl(mockClassLoader)

        // when: getting the release
        val result = releaseService.getReleaseVersion()

        // then: release file error is returned
        result should
            beLeftAnd { error ->
                error.shouldBeInstanceOf<MetaReleaseError>()
                error.message.contains("Release property not found")
            }
    }

    should("return error when properties file exists but release value is blank") {
        // given: a mocked classloader that returns a properties file with blank release
        val mockClassLoader = mockk<ClassLoader>()
        val propertiesContent = "release="
        val inputStream = ByteArrayInputStream(propertiesContent.toByteArray())

        every {
            mockClassLoader.getResourceAsStream("release.properties")
        } returns inputStream

        val releaseService = MetaReleaseServiceImpl(mockClassLoader)

        // when: getting the release
        val result = releaseService.getReleaseVersion()

        // then: release file error is returned
        result should
            beLeftAnd<MetaReleaseError, String> { error ->
                error.shouldBeInstanceOf<MetaReleaseError>()
                error.message.contains("Release property not found")
            }
    }

    should("return error when properties file cannot be loaded") {
        // given: a mocked classloader that returns null for the resource
        val mockClassLoader = mockk<ClassLoader>()

        every {
            mockClassLoader.getResourceAsStream("release.properties")
        } returns null

        val releaseService = MetaReleaseServiceImpl(mockClassLoader)

        // when: getting the release
        val result = releaseService.getReleaseVersion()

        // then: release file error is returned
        result should
            beLeftAnd { error ->
                error.shouldBeInstanceOf<MetaReleaseError>()
                error.message.contains("Could not load")
            }
    }
})
