package io.sdkman.broker.domain.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right

enum class Platform(val urlParameter: String, val normalizedId: String, val description: String) {
    LINUX_X64("linuxx64", "LinuxX64", "64-bit Linux"),
    LINUX_ARM64("linuxarm64", "LinuxARM64", "ARM64 Linux"),
    LINUX_X32("linuxx32", "LinuxX32", "32-bit Linux"),
    DARWIN_X64("darwinx64", "DarwinX64", "64-bit macOS (Intel)"),
    DARWIN_ARM64("darwinarm64", "DarwinARM64", "ARM64 macOS (Apple Silicon)"),
    WINDOWS_X64("windowsx64", "WindowsX64", "64-bit Windows"),
    EXOTIC("exotic", "Exotic", "Fallback for unsupported platforms");

    companion object {
        private val urlParameterMap = values().associateBy { it.urlParameter }
        private val normalizedIdMap = values().associateBy { it.normalizedId }

        fun fromUrlParameter(parameter: String): Either<ValidationError, Platform> =
            urlParameterMap[parameter]?.right() ?: ValidationError.InvalidPlatform(parameter).left()

        fun fromNormalizedId(id: String): Either<ValidationError, Platform> =
            normalizedIdMap[id]?.right() ?: ValidationError.InvalidPlatform(id).left()
    }
}

sealed class ValidationError {
    data class InvalidPlatform(val platform: String) : ValidationError()
}

object PlatformMapper {
    fun validateAndNormalize(urlParameter: String): Either<ValidationError, String> =
        Platform.fromUrlParameter(urlParameter).map { it.normalizedId }
}