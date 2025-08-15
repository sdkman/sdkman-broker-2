package io.sdkman.broker.application.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.sdkman.broker.domain.model.Command
import io.sdkman.broker.domain.model.DownloadInfo
import io.sdkman.broker.domain.model.Platform
import io.sdkman.broker.domain.model.TargetTriple
import io.sdkman.broker.domain.model.VersionError
import io.sdkman.broker.domain.service.SdkmanNativeDownloadService

class SdkmanNativeDownloadServiceImpl : SdkmanNativeDownloadService {
    private val githubReleasesUrl = "https://github.com/sdkman/sdkman-cli-native/releases/download"

    override fun downloadNativeCli(
        command: String,
        version: String,
        platformCode: String
    ): Either<VersionError, DownloadInfo> =
        validateCommand(command)
            .flatMap { validateVersion(version) }
            .flatMap { validatedVersion -> validatePlatform(platformCode).map { validatedVersion to it } }
            .flatMap { (validatedVersion, platform) -> mapTargetTriple(platform).map { validatedVersion to it } }
            .map { (validatedVersion, targetTriple) -> constructDownloadInfo(validatedVersion, targetTriple) }

    private fun validateCommand(command: String): Either<VersionError, Command> =
        if (command.isBlank()) {
            VersionError.InvalidCommand("[empty/blank]").left()
        } else {
            Command.fromValue(command)
                .toEither { VersionError.InvalidCommand(command) }
        }

    private fun validateVersion(version: String): Either<VersionError, String> =
        if (version.isNotBlank()) {
            version.right()
        } else {
            VersionError.InvalidVersion("[empty/blank]").left()
        }

    private fun validatePlatform(platformCode: String): Either<VersionError, Platform> =
        if (platformCode.isBlank()) {
            VersionError.InvalidPlatform("[empty/blank]").left()
        } else {
            Platform.fromCode(platformCode)
                .toEither { VersionError.InvalidPlatform(platformCode) }
        }

    private fun mapTargetTriple(platform: Platform): Either<VersionError, TargetTriple> =
        when (platform) {
            Platform.Exotic -> VersionError.InvalidPlatform(platform.code).left()
            else ->
                TargetTriple.fromPlatform(platform)
                    .toEither { VersionError.InvalidPlatform(platform.code) }
        }

    private fun constructDownloadInfo(
        version: String,
        targetTriple: TargetTriple
    ): DownloadInfo {
        val filename = "sdkman-cli-native-$version-${targetTriple.triple}.zip"
        return DownloadInfo(
            redirectUrl = "$githubReleasesUrl/v$version/$filename",
            archiveType = "zip"
        )
    }
}
