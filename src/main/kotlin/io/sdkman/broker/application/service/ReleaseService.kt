package io.sdkman.broker.application.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import java.util.Properties

interface ReleaseService {
    fun getRelease(): Either<ReleaseError, String>
}

class ReleaseServiceImpl(
    private val classLoader: ClassLoader = ReleaseServiceImpl::class.java.classLoader
) : ReleaseService {
    companion object {
        private const val RELEASE_PROPERTIES = "release.properties"
        private const val RELEASE_KEY = "release"
    }

    override fun getRelease(): Either<ReleaseError, String> =
        loadPropertiesFile()
            .flatMap { properties -> getReleaseFromProperties(properties) }

    private fun loadPropertiesFile(): Either<ReleaseError, Properties> =
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
        }.mapLeft { ReleaseError.ReleaseFileError(it) }

    private fun getReleaseFromProperties(properties: Properties): Either<ReleaseError, String> =
        properties.getProperty(RELEASE_KEY).toOption()
            .filter { it.isNotBlank() }
            .fold(
                {
                    ReleaseError.ReleaseFileError(
                        IllegalStateException("Release property not found in $RELEASE_PROPERTIES")
                    ).left()
                },
                { it.right() }
            )
}

sealed class ReleaseError {
    data class ReleaseFileError(val cause: Throwable) : ReleaseError()
}
