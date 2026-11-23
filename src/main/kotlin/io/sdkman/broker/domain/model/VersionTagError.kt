package io.sdkman.broker.domain.model

sealed class VersionTagError {
    data class DatabaseError(
        val cause: Throwable
    ) : VersionTagError()

    data class InvalidPlatform(
        val platform: String
    ) : VersionTagError()

    data class TagNotFound(
        val candidate: String,
        val tag: String,
        val platform: String
    ) : VersionTagError()
}
