package io.sdkman.broker.domain.model

import arrow.core.None
import arrow.core.Option

data class Version(
    val candidate: String,
    val version: String,
    val platform: String,
    val url: String,
    val vendor: Option<String> = None,
    val visible: Boolean = true,
    val checksums: Map<String, String> = emptyMap()
) {
    fun resolveActualDistribution(sourcePlatform: Platform): String =
        if (platform == Platform.Universal.persistentId) {
            Platform.Universal.persistentId
        } else {
            sourcePlatform.persistentId
        }
}
