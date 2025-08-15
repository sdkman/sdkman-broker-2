package io.sdkman.broker.domain.model

data class DownloadInfo(
    val redirectUrl: String,
    val archiveType: String,
    val checksumHeaders: Map<String, String> = emptyMap()
)
