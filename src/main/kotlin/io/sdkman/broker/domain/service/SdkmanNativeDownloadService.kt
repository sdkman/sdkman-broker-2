package io.sdkman.broker.domain.service

import arrow.core.Either
import io.sdkman.broker.domain.model.VersionError

interface SdkmanNativeDownloadService {
    fun downloadNativeCli(
        command: String,
        version: String,
        platformCode: String
    ): Either<VersionError, NativeDownloadInfo>
}

// TODO: Remove this in favour of `DownloadInfo`
data class NativeDownloadInfo(
    val downloadUrl: String,
    val archiveType: String = "zip"
)
