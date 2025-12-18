package io.sdkman.broker.domain.model

import arrow.core.None
import arrow.core.Option

data class Version(
    val candidate: String,
    val version: String,
    val platform: String,
    val distribution: Option<String> = None,
    val url: String,
    val visible: Boolean = true,
    val checksums: Map<String, String> = emptyMap()
)
