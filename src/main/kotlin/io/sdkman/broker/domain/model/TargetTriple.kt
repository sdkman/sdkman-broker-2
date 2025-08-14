package io.sdkman.broker.domain.model

import arrow.core.Option

enum class TargetTriple(val triple: String) {
    LINUX_X64("x86_64-unknown-linux-gnu"),
    LINUX_ARM64("aarch64-unknown-linux-gnu"),
    LINUX_X32("i686-unknown-linux-gnu"),
    DARWIN_X64("x86_64-apple-darwin"),
    DARWIN_ARM64("aarch64-apple-darwin"),
    WINDOWS_X64("x86_64-pc-windows-msvc");

    companion object {
        private val platformToTargetTripleMap =
            mapOf(
                Platform.LinuxX64 to LINUX_X64,
                Platform.LinuxARM64 to LINUX_ARM64,
                Platform.LinuxX32 to LINUX_X32,
                Platform.DarwinX64 to DARWIN_X64,
                Platform.DarwinARM64 to DARWIN_ARM64,
                Platform.WindowsX64 to WINDOWS_X64
            )

        fun fromPlatform(platform: Platform): Option<TargetTriple> =
            Option.fromNullable(platformToTargetTripleMap[platform])
    }
}
