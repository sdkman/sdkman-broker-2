package io.sdkman.broker.application.service

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.should
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.sdkman.broker.support.beLeftAnd
import io.sdkman.broker.support.shouldBeRight
import java.io.ByteArrayInputStream

class VersionServiceSpec : ShouldSpec({

    should("successfully return version when properties file exists with valid version") {
        // given: a mocked classloader that returns a valid properties file
        val mockClassLoader = mockk<ClassLoader>()
        val propertiesContent = "version=1.2.3"
        val inputStream = ByteArrayInputStream(propertiesContent.toByteArray())

        every {
            mockClassLoader.getResourceAsStream("version.properties")
        } returns inputStream

        val versionService = VersionServiceImpl(mockClassLoader)

        // when: getting the version
        val result = versionService.getVersion()

        // then: version is returned successfully
        result shouldBeRight "1.2.3"
    }

    should("return error when properties file exists but version key is missing") {
        // given: a mocked classloader that returns a properties file without version
        val mockClassLoader = mockk<ClassLoader>()
        val propertiesContent = "someOtherKey=value"
        val inputStream = ByteArrayInputStream(propertiesContent.toByteArray())

        every {
            mockClassLoader.getResourceAsStream("version.properties")
        } returns inputStream

        val versionService = VersionServiceImpl(mockClassLoader)

        // when: getting the version
        val result = versionService.getVersion()

        // then: version file error is returned
        result should
            beLeftAnd<VersionError, String> { error ->
                error.shouldBeInstanceOf<VersionError.VersionFileError>()
                error.cause.message?.contains("Version property not found") ?: false
            }
    }

    should("return error when properties file exists but version value is blank") {
        // given: a mocked classloader that returns a properties file with blank version
        val mockClassLoader = mockk<ClassLoader>()
        val propertiesContent = "version="
        val inputStream = ByteArrayInputStream(propertiesContent.toByteArray())

        every {
            mockClassLoader.getResourceAsStream("version.properties")
        } returns inputStream

        val versionService = VersionServiceImpl(mockClassLoader)

        // when: getting the version
        val result = versionService.getVersion()

        // then: version file error is returned
        result should
            beLeftAnd<VersionError, String> { error ->
                error.shouldBeInstanceOf<VersionError.VersionFileError>()
                error.cause.message?.contains("Version property not found") ?: false
            }
    }

    should("return error when properties file cannot be loaded") {
        // given: a mocked classloader that returns null for the resource
        val mockClassLoader = mockk<ClassLoader>()

        every {
            mockClassLoader.getResourceAsStream("version.properties")
        } returns null

        val versionService = VersionServiceImpl(mockClassLoader)

        // when: getting the version
        val result = versionService.getVersion()

        // then: version file error is returned
        result should
            beLeftAnd<VersionError, String> { error ->
                error.shouldBeInstanceOf<VersionError.VersionFileError>()
                error.cause.message?.contains("Could not load") ?: false
            }
    }
})
