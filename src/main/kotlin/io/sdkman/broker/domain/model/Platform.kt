package io.sdkman.broker.domain.model

import arrow.core.Option

sealed class Platform(val code: String, val persistentId: String) {
    data object LinuxX64 : Platform("linuxx64", "LINUX_64")

    data object LinuxARM64 : Platform("linuxarm64", "LINUX_ARM64")

    data object LinuxX32 : Platform("linuxx32", "LINUX_32")

    data object DarwinX64 : Platform("darwinx64", "MAC_OSX")

    data object DarwinARM64 : Platform("darwinarm64", "MAC_ARM64")

    data object WindowsX64 : Platform("windowsx64", "WINDOWS_64")

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
