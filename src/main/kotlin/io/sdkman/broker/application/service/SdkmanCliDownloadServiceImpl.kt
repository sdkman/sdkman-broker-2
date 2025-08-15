package io.sdkman.broker.application.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.sdkman.broker.domain.model.DownloadInfo
import io.sdkman.broker.domain.model.Platform
import io.sdkman.broker.domain.model.VersionError
import io.sdkman.broker.domain.service.SdkmanCliDownloadService

class SdkmanCliDownloadServiceImpl : SdkmanCliDownloadService {
    private val validCommands = setOf("install", "selfupdate")
    private val githubReleasesUrl = "https://github.com/sdkman/sdkman-cli/releases/download"

    override fun downloadSdkmanCli(
        command: String,
        version: String,
        platformCode: String
    ): Either<VersionError, DownloadInfo> =
        validateCommand(command)
            .flatMap { validateVersion(version) }
            .flatMap { validatePlatform(platformCode) }
            .map { constructDownloadInfo(version) }

    private fun validateCommand(command: String): Either<VersionError, String> =
        if (command in validCommands) {
            command.right()
        } else {
            VersionError.InvalidCommand(command).left()
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

    private fun constructDownloadInfo(version: String): DownloadInfo {
        val tag = if (version.startsWith("latest+")) "latest" else version
        val filename = "sdkman-cli-$version.zip"
        return DownloadInfo(
            redirectUrl = "$githubReleasesUrl/$tag/$filename",
            archiveType = "zip"
        )
    }
}
