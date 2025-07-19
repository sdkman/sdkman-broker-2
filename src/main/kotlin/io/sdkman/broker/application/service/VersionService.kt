package io.sdkman.broker.application.service

import arrow.core.Either
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import io.sdkman.broker.adapter.primary.rest.AuditContext
import io.sdkman.broker.domain.model.ArchiveType
import io.sdkman.broker.domain.model.Audit
import io.sdkman.broker.domain.model.Platform
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.domain.model.VersionError
import io.sdkman.broker.domain.repository.AuditRepository
import io.sdkman.broker.domain.repository.VersionRepository
import kotlinx.datetime.Clock
import org.slf4j.LoggerFactory
import java.util.UUID

data class DownloadInfo(
    val redirectUrl: String,
    val checksumHeaders: Map<String, String>,
    val archiveType: String
)

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
            id = Option.fromNullable(UUID.randomUUID()),
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

interface VersionService {
    fun downloadVersion(
        candidate: String,
        version: String,
        platformCode: String,
        auditContext: AuditContext
    ): Either<VersionError, DownloadInfo>
}

class VersionServiceImpl(
    private val versionRepository: VersionRepository,
    private val auditRepository: AuditRepository
) : VersionService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun downloadVersion(
        candidate: String,
        version: String,
        platformCode: String,
        auditContext: AuditContext
    ): Either<VersionError, DownloadInfo> =
        either<VersionError, DownloadInfo> {
            val platform =
                Platform.fromCode(platformCode).toEither { VersionError.InvalidPlatform(platformCode) }.bind()
            val versionEntity = findVersionWithFallback(candidate, version, platform.persistentId).bind()
            val checksumHeaders =
                versionEntity.checksums.mapKeys { (algorithm, _) ->
                    "X-Sdkman-Checksum-${algorithm.uppercase()}"
                }
            val archiveType = ArchiveType.fromUrl(versionEntity.url).value
            val actualDist = versionEntity.resolveActualDistribution(platform)
            createAuditEntry(AuditCommand(candidate, version, platform, actualDist, versionEntity, auditContext))
            val downloadInfo = DownloadInfo(
                redirectUrl = versionEntity.url,
                checksumHeaders = checksumHeaders,
                archiveType = archiveType
            )
            return downloadInfo.right()
        }

    private fun findVersionWithFallback(
        candidate: String,
        version: String,
        platformId: String
    ): Either<VersionError, Version> =
        versionRepository.findByQuery(candidate, version, platformId)
            .flatMap { versionOption ->
                versionOption.fold(
                    { // Try UNIVERSAL fallback
                        versionRepository.findByQuery(candidate, version, Platform.Universal.persistentId)
                            .flatMap { universalOption ->
                                universalOption.fold(
                                    { VersionError.VersionNotFound(candidate, version, platformId).left() },
                                    { it.right() }
                                )
                            }
                    },
                    { it.right() }
                )
            }

    private fun createAuditEntry(auditCommand: AuditCommand): Either<Unit, Unit> =
        auditRepository.save(auditCommand.toAudit())
            .mapLeft { failure ->
                logger.error(
                    "Failed to save audit entry for download: {}",
                    failure.exception.message,
                    failure.exception
                )
            }
}
