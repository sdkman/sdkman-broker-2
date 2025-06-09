package io.sdkman.broker.application.service

import arrow.core.Either
import io.sdkman.broker.domain.model.VersionError

data class DownloadResponse(
    val redirectUrl: String,
    val checksumHeaders: Map<String, String>,
    val archiveType: String
)

interface VersionService {
    fun downloadVersion(
        candidate: String,
        version: String,
        platform: String
    ): Either<VersionError, DownloadResponse>
}
