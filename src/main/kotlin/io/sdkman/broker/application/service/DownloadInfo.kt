package io.sdkman.broker.application.service

// TODO: This should live in the domain, not in the application layer
// TODO: Use this class as the return type for all download services
data class DownloadInfo(
    val redirectUrl: String,
    val archiveType: String,
    val checksumHeaders: Map<String, String> = emptyMap()
)
