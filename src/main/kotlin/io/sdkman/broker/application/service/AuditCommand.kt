package io.sdkman.broker.application.service

import arrow.core.Some
import arrow.core.getOrElse
import io.sdkman.broker.adapter.primary.rest.AuditContext
import io.sdkman.broker.domain.model.Audit
import io.sdkman.broker.domain.model.Platform
import io.sdkman.broker.domain.model.Version
import kotlinx.datetime.Clock
import java.util.UUID

data class AuditCommand(
    val versionEntity: Version,
    val clientPlatform: Platform,
    val auditContext: AuditContext
) {
    fun toAudit(): Audit =
        Audit(
            id = Some(UUID.randomUUID()),
            command = "install",
            candidate = versionEntity.candidate,
            version =
                versionEntity.vendor
                    .map { vendor -> versionEntity.version.removeSuffix("-$vendor") }
                    .getOrElse { versionEntity.version },
            platform = clientPlatform.auditId,
            dist = versionEntity.resolveActualDistribution(clientPlatform),
            vendor = versionEntity.vendor,
            host = auditContext.host,
            agent = auditContext.agent,
            timestamp = Clock.System.now()
        )

    private fun Version.resolveActualDistribution(sourcePlatform: Platform): String =
        when (this.platform) {
            Platform.Universal.persistentId -> Platform.Universal.auditId
            else -> sourcePlatform.auditId
        }
}
