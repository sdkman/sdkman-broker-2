package io.sdkman.broker.domain.model

import arrow.core.Option
import kotlinx.datetime.Instant
import java.util.UUID

data class Audit(
    val id: Option<UUID>,
    val command: String,
    val candidate: String,
    val version: String,
    val platform: String,
    val vendor: Option<String>,
    val host: String,
    val agent: String,
    val dist: String,
    val timestamp: Instant
)
