package io.sdkman.broker.application.service

import arrow.core.Either
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.sdkman.broker.adapter.primary.rest.AuditContext
import io.sdkman.broker.domain.model.ArchiveType
import io.sdkman.broker.domain.model.Audit
import io.sdkman.broker.domain.model.Platform
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.domain.model.VersionError
import io.sdkman.broker.domain.repository.AuditRepository
import io.sdkman.broker.domain.repository.VersionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
)

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
    private val auditScope = CoroutineScope(Dispatchers.IO)

    override fun downloadVersion(
        candidate: String,
        version: String,
        platformCode: String,
        auditContext: AuditContext
    ): Either<VersionError, DownloadInfo> =
        Platform.fromCode(platformCode)
            .toEither { VersionError.InvalidPlatform(platformCode) }
            .flatMap { platform ->
                findVersionWithFallback(candidate, version, platform.persistentId)
                    .map { versionEntity ->
                        val checksumHeaders =
                            versionEntity.checksums.mapKeys { (algorithm, _) ->
                                "X-Sdkman-Checksum-${algorithm.uppercase()}"
                            }
                        val archiveType = ArchiveType.fromUrl(versionEntity.url).value

                        // TODO: Move this behaviour as a well-named method on Version
                        val actualDist =
                            if (versionEntity.platform == Platform.Universal.persistentId) {
                                Platform.Universal.persistentId
                            } else {
                                platform.persistentId
                            }

                        createAuditEntry(
                            AuditCommand(candidate, version, platform, actualDist, versionEntity, auditContext)
                        )

                        DownloadInfo(
                            redirectUrl = versionEntity.url,
                            checksumHeaders = checksumHeaders,
                            archiveType = archiveType
                        )
                    }
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

    private fun createAuditEntry(auditCommand: AuditCommand) {
        auditScope.launch {
            // TODO: move conversion logic to the AuditCommand as toAudit() method
            val audit =
                Audit(
                    id = Option.fromNullable(UUID.randomUUID()),
                    command = "install",
                    candidate = auditCommand.candidate,
                    version = auditCommand.version,
                    platform = auditCommand.platform.persistentId,
                    dist = auditCommand.actualDist,
                    vendor = auditCommand.versionEntity.vendor,
                    host = auditCommand.auditContext.host,
                    agent = auditCommand.auditContext.agent,
                    timestamp = Clock.System.now()
                )

            auditRepository.save(audit)
                .mapLeft { failure ->
                    logger.error(
                        "Failed to save audit entry for download: {}",
                        failure.exception.message,
                        failure.exception
                    )
                }
        }
    }
}
