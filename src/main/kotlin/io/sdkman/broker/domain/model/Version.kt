package io.sdkman.broker.domain.model

import arrow.core.None
import arrow.core.Option

data class Version(
    val candidate: String,
    val version: String,
    val platform: String,
    val url: String,
    val vendor: Option<String> = None,
    val visible: Boolean = true,
    val checksums: Map<String, String> = emptyMap()
)

sealed class Platform(val code: String, val normalizedId: String) {
    data object LinuxX64 : Platform("linuxx64", "LinuxX64")

    data object LinuxARM64 : Platform("linuxarm64", "LinuxARM64")

    data object LinuxX32 : Platform("linuxx32", "LinuxX32")

    data object DarwinX64 : Platform("darwinx64", "DarwinX64")

    data object DarwinARM64 : Platform("darwinarm64", "DarwinARM64")

    data object WindowsX64 : Platform("windowsx64", "WindowsX64")

    data object Exotic : Platform("exotic", "Exotic")

    data object Universal : Platform("universal", "UNIVERSAL")

    companion object {
        private val platformMap =
            mapOf(
                "linuxx64" to LinuxX64,
                "linuxarm64" to LinuxARM64,
                "linuxx32" to LinuxX32,
                "darwinx64" to DarwinX64,
                "darwinarm64" to DarwinARM64,
                "windowsx64" to WindowsX64,
                "exotic" to Exotic,
                "universal" to Universal
            )

        fun fromCode(code: String): Option<Platform> = Option.fromNullable(platformMap[code.lowercase()])
    }
}

sealed class ArchiveType(val value: String) {
    data object Zip : ArchiveType("zip")

    data object TarGz : ArchiveType("tar.gz")

    companion object {
        fun fromUrl(url: String): ArchiveType =
            when {
                url.endsWith(".zip") -> Zip
                url.endsWith(".tar.gz") || url.endsWith(".tgz") -> TarGz
                else -> Zip // Default fallback
            }
    }
}

sealed class VersionError {
    data class VersionNotFound(val candidate: String, val version: String, val platform: String) : VersionError()

    data class InvalidPlatform(val platform: String) : VersionError()

    data class DatabaseError(val cause: Throwable) : VersionError()
}
