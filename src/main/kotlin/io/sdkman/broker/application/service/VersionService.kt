package io.sdkman.broker.application.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import java.util.Properties

interface VersionService {
    fun getVersion(): Either<VersionError, String>
}

class VersionServiceImpl(
    private val classLoader: ClassLoader = VersionServiceImpl::class.java.classLoader
) : VersionService {
    companion object {
        private const val VERSION_PROPERTIES = "version.properties"
        private const val VERSION_KEY = "version"
    }

    override fun getVersion(): Either<VersionError, String> =
        loadPropertiesFile()
            .flatMap { properties -> getVersionFromProperties(properties) }

    private fun loadPropertiesFile(): Either<VersionError, Properties> =
        Either.catch {
            val properties = Properties()
            classLoader.getResourceAsStream(VERSION_PROPERTIES).toOption()
                .fold(
                    { throw IllegalStateException("Could not load $VERSION_PROPERTIES") },
                    { stream -> 
                        stream.use { properties.load(it) }
                        properties
                    }
                )
        }.mapLeft { VersionError.VersionFileError(it) }

    private fun getVersionFromProperties(properties: Properties): Either<VersionError, String> =
        properties.getProperty(VERSION_KEY).toOption()
            .filter { it.isNotBlank() }
            .fold(
                { VersionError.VersionFileError(IllegalStateException("Version property not found in $VERSION_PROPERTIES")).left() },
                { it.right() }
            )
}

sealed class VersionError {
    data class VersionFileError(val cause: Throwable) : VersionError()
}
