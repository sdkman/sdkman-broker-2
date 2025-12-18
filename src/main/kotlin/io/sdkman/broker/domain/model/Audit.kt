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
    val dist: String,
    val distribution: Option<String>,
    val host: Option<String>,
    val agent: Option<String>,
    val timestamp: Instant
)
