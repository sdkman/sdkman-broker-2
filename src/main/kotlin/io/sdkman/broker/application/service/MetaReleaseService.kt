package io.sdkman.broker.application.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import java.util.Properties

interface MetaReleaseService {
    fun getReleaseVersion(): Either<MetaError, String>
}

class MetaReleaseServiceImpl(
    private val classLoader: ClassLoader = MetaReleaseServiceImpl::class.java.classLoader
) : MetaReleaseService {
    companion object {
        private const val RELEASE_PROPERTIES = "release.properties"
        private const val RELEASE_KEY = "release"
    }

    override fun getReleaseVersion(): Either<MetaError, String> =
        loadPropertiesFile()
            .flatMap { properties -> getReleaseFromProperties(properties) }

    private fun loadPropertiesFile(): Either<MetaError, Properties> =
        Either.catch {
            val properties = Properties()
            classLoader.getResourceAsStream(RELEASE_PROPERTIES).toOption()
                .fold(
                    { throw IllegalStateException("Could not load $RELEASE_PROPERTIES") },
                    { stream ->
                        stream.use { properties.load(it) }
                        properties
                    }
                )
        }.mapLeft { MetaError.MetaFileError(it) }

    private fun getReleaseFromProperties(properties: Properties): Either<MetaError, String> =
        properties.getProperty(RELEASE_KEY).toOption()
            .filter { it.isNotBlank() }
            .map { it.right() }
            .getOrElse {
                MetaError.MetaFileError(IllegalStateException("Release property not found in $RELEASE_PROPERTIES"))
                    .left()
            }
}

sealed class MetaError {
    data class MetaFileError(val cause: Throwable) : MetaError()
}
