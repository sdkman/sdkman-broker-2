package io.sdkman.broker.adapter.primary.rest

import io.sdkman.broker.application.service.DownloadInfo

data class DownloadResponse(
    val redirectUrl: String,
    val checksumHeaders: Map<String, String>,
    val archiveType: String
) {
    companion object {
        fun from(downloadInfo: DownloadInfo): DownloadResponse =
            DownloadResponse(
                redirectUrl = downloadInfo.redirectUrl,
                checksumHeaders = downloadInfo.checksumHeaders,
                archiveType = downloadInfo.archiveType
            )
    }
}
