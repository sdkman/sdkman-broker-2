package io.sdkman.broker.domain.service

import arrow.core.Either
import io.sdkman.broker.domain.model.VersionError

// TODO: rename this to `SdkmanNativeDownloadService`. also rename all associated variable names.
interface NativeDownloadService {
    fun downloadNativeCli(
        command: String,
        version: String,
        platformCode: String
    ): Either<VersionError, NativeDownloadInfo>
}

data class NativeDownloadInfo(
    val downloadUrl: String,
    val archiveType: String = "zip"
)
