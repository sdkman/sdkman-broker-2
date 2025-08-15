package io.sdkman.broker.domain.service

import arrow.core.Either
import io.sdkman.broker.domain.model.VersionError

interface SdkmanCliDownloadService {
    fun downloadSdkmanCli(
        command: String,
        version: String,
        platformCode: String
    ): Either<VersionError, SdkmanCliDownloadInfo>
}

// TODO: Remove this in favour of `DownloadInfo`
data class SdkmanCliDownloadInfo(
    val downloadUrl: String,
    val archiveType: String = "zip"
)
