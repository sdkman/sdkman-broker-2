package io.sdkman.broker.application.service

data class DownloadInfo(
    val redirectUrl: String,
    val checksumHeaders: Map<String, String>,
    val archiveType: String
)
