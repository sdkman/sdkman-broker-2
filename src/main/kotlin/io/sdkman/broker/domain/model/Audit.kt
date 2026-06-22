package io.sdkman.broker.domain.model

import arrow.core.Option
import java.util.UUID
import kotlin.time.Instant

data class Audit(
    val id: Option<UUID>,
    val command: String,
    val candidate: String,
    val version: String,
    val clientPlatform: String,
    val candidatePlatform: String,
    val distribution: Option<String>,
    val host: Option<String>,
    val agent: Option<String>,
    val timestamp: Instant
)
