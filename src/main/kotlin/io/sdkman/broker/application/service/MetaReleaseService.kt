package io.sdkman.broker.application.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import java.util.Properties

interface MetaReleaseService {
    fun getReleaseVersion(): Either<MetaReleaseError, String>
}

class MetaReleaseServiceImpl(
    private val classLoader: ClassLoader = MetaReleaseServiceImpl::class.java.classLoader
) : MetaReleaseService {
    companion object {
        private const val RELEASE_PROPERTIES = "release.properties"
        private const val RELEASE_KEY = "release"
    }

    override fun getReleaseVersion(): Either<MetaReleaseError, String> =
        loadPropertiesFile()
            .flatMap { properties -> getReleaseFromProperties(properties) }

    private fun loadPropertiesFile(): Either<MetaReleaseError, Properties> =
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
        }.mapLeft { MetaReleaseError(it) }

    private fun getReleaseFromProperties(properties: Properties): Either<MetaReleaseError, String> =
        properties.getProperty(RELEASE_KEY).toOption()
            .filter { it.isNotBlank() }
            .map { it.right() }
            .getOrElse {
                MetaReleaseError(IllegalStateException("Release property not found in $RELEASE_PROPERTIES"))
                    .left()
            }
}

class MetaReleaseError(e: Throwable) : Throwable(e) {
    override val message: String
        get() = super.message ?: "An error occurred while retrieving the release version."
}
