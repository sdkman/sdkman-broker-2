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

data class SdkmanCliDownloadInfo(
    val downloadUrl: String,
    val archiveType: String = "zip"
)
