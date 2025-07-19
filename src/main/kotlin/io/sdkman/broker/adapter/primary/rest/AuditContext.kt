package io.sdkman.broker.adapter.primary.rest

import arrow.core.Option

data class AuditContext(
    val host: Option<String>,
    val agent: Option<String>
)
