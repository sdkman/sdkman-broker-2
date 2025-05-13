package io.sdkman.broker.application.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.util.Properties

interface VersionService {
    fun getVersion(): Either<VersionError, String>
}

class VersionServiceImpl : VersionService {
    companion object {
        private const val VERSION_PROPERTIES = "version.properties"
        private const val VERSION_KEY = "version"
    }

    override fun getVersion(): Either<VersionError, String> =
        Either.catch {
            val properties = Properties()
            //TODO: Use an Option instead of nullables!!!
            javaClass.classLoader.getResourceAsStream(VERSION_PROPERTIES)?.use { stream ->
                properties.load(stream)
                val version = properties.getProperty(VERSION_KEY)
                if (version.isNullOrBlank()) {
                    throw IllegalStateException("Version property not found in $VERSION_PROPERTIES")
                }
                version
            } ?: throw IllegalStateException("Could not load $VERSION_PROPERTIES")
        }.mapLeft { throwable ->
            VersionError.VersionFileError(throwable)
        }
}

sealed class VersionError {
    data class VersionFileError(val cause: Throwable) : VersionError()
}
