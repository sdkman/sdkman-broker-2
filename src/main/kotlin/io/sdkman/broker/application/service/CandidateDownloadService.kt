package io.sdkman.broker.application.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import io.sdkman.broker.adapter.primary.rest.AuditContext
import io.sdkman.broker.domain.model.ArchiveType
import io.sdkman.broker.domain.model.DownloadInfo
import io.sdkman.broker.domain.model.Platform
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.domain.model.VersionError
import io.sdkman.broker.domain.repository.AuditRepository
import io.sdkman.broker.domain.repository.VersionRepository
import org.slf4j.LoggerFactory

// TODO: move the interface to the domain.service package
interface CandidateDownloadService {
    fun downloadVersion(
        candidate: String,
        version: String,
        platformCode: String,
        auditContext: AuditContext
    ): Either<VersionError, DownloadInfo>
}

class CandidateDownloadServiceImpl(
    private val versionRepository: VersionRepository,
    private val auditRepository: AuditRepository
) : CandidateDownloadService {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun downloadVersion(
        candidate: String,
        version: String,
        platformCode: String,
        auditContext: AuditContext
    ): Either<VersionError, DownloadInfo> =
        either<VersionError, DownloadInfo> {
            val platform =
                Platform.fromCode(platformCode)
                    .toEither { VersionError.InvalidPlatform(platformCode) }.bind()
            val versionEntity = findVersionWithFallback(candidate, version, platform.persistentId).bind()
            val checksumHeaders =
                versionEntity.checksums.mapKeys { (algorithm, _) ->
                    "X-Sdkman-Checksum-${algorithm.uppercase()}"
                }
            createAuditEntry(
                AuditCommand(
                    versionEntity = versionEntity,
                    clientPlatform = platform,
                    auditContext = auditContext
                )
            )
            logger.info(
                "Downloading $candidate version $version; " +
                    "requested: ${platform.persistentId}; " +
                    "distributed: ${versionEntity.platform}"
            )
            val archiveType = ArchiveType.fromUrl(versionEntity.url).value
            val downloadInfo =
                DownloadInfo(
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
            .flatMap { platformSpecificOption ->
                platformSpecificOption.fold(
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
