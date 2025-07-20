package io.sdkman.broker.domain.model

import arrow.core.Option

sealed class Platform(val code: String, val persistentId: String, val auditId: String) {
    data object LinuxX64 : Platform("linuxx64", "LINUX_64", "LINUX_X64")

    data object LinuxARM64 : Platform("linuxarm64", "LINUX_ARM64", "LINUX_ARM64")

    data object LinuxX32 : Platform("linuxx32", "LINUX_32", "LINUX_X32")

    data object LinuxARM32HF : Platform("linuxarm32hf", "LINUX_ARM32HF", "LINUX_ARM32HF")

    data object LinuxARM32SF : Platform("linuxarm32sf", "LINUX_ARM32SF", "LINUX_ARM32SF")

    data object DarwinX64 : Platform("darwinx64", "MAC_OSX", "MAC_X64")

    data object DarwinARM64 : Platform("darwinarm64", "MAC_ARM64", "MAC_ARM64")

    data object WindowsX64 : Platform("windowsx64", "WINDOWS_64", "WINDOWS_X64")

    data object Exotic : Platform("exotic", "EXOTIC", "EXOTIC")

    data object Universal : Platform("universal", "UNIVERSAL", "UNIVERSAL")

    companion object {
        private val platformMap =
            mapOf(
                "linuxx64" to LinuxX64,
                "linuxarm64" to LinuxARM64,
                "linuxx32" to LinuxX32,
                "linuxarm32hf" to LinuxARM32HF,
                "linuxarm32sf" to LinuxARM32SF,
                "darwinx64" to DarwinX64,
                "darwinarm64" to DarwinARM64,
                "windowsx64" to WindowsX64,
                "exotic" to Exotic,
                "universal" to Universal
            )

        fun fromCode(code: String): Option<Platform> = Option.fromNullable(platformMap[code.lowercase()])
    }
}
