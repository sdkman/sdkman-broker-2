package io.sdkman.broker.domain.model

sealed class VersionError {
    data class VersionNotFound(val candidate: String, val version: String, val platform: String) : VersionError()

    data class InvalidPlatform(val platform: String) : VersionError()

    data class DatabaseError(val cause: Throwable) : VersionError()
}
