package io.sdkman.broker.domain.model

data class Version(
    val candidate: String,
    val version: String,
    val platform: String,
    val url: String,
    val vendor: String? = null,
    val visible: Boolean? = true,
    val checksums: Map<String, String>? = null
)