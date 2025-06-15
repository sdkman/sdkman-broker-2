package io.sdkman.broker.application.service

import arrow.core.Either
import io.sdkman.broker.domain.model.VersionError

data class DownloadResponse(
    val redirectUrl: String,
    val checksumHeaders: Map<String, String>,
    val archiveType: String
)

interface VersionService {
    fun downloadVersion(
        candidate: String,
        version: String,
        platform: String
    ): Either<VersionError, DownloadResponse>
}

class VersionServiceImpl(
    private val versionRepository: VersionRepository
) : VersionService {
    override fun downloadVersion(
        candidate: String,
        version: String,
        platform: String
    ): Either<VersionError, DownloadResponse> =
        Platform.fromCode(platform)
            .toEither { VersionError.InvalidPlatform(platform) }
            .flatMap { platformObj ->
                findVersionWithFallback(candidate, version, platformObj.normalizedId)
                    .map { versionEntity ->
                        val checksumHeaders =
                            versionEntity.checksums.mapKeys { (algorithm, _) ->
                                "X-Sdkman-Checksum-${algorithm.uppercase()}"
                            }
                        val archiveType = ArchiveType.fromUrl(versionEntity.url).value

                        DownloadResponse(
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
                        versionRepository.findByQuery(candidate, version, Platform.Universal.normalizedId)
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
}
