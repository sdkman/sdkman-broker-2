package io.sdkman.broker.application.service

import arrow.core.Some
import io.sdkman.broker.adapter.primary.rest.AuditContext
import io.sdkman.broker.domain.model.Audit
import io.sdkman.broker.domain.model.Platform
import io.sdkman.broker.domain.model.Version
import kotlinx.datetime.Clock
import java.util.UUID

data class AuditCommand(
    val candidate: String,
    val version: String,
    val platform: Platform,
    val actualDist: String,
    val versionEntity: Version,
    val auditContext: AuditContext
) {
    fun toAudit(): Audit =
        Audit(
            id = Some(UUID.randomUUID()),
            command = "install",
            candidate = candidate,
            version = version,
            platform = platform.persistentId,
            dist = actualDist,
            vendor = versionEntity.vendor,
            host = auditContext.host,
            agent = auditContext.agent,
            timestamp = Clock.System.now()
        )
}
