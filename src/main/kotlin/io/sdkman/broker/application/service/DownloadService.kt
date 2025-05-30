package io.sdkman.broker.application.service

import arrow.core.Either
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.fold
import arrow.core.left
import arrow.core.right
import io.sdkman.broker.domain.error.RepositoryError
import io.sdkman.broker.domain.model.PlatformMapper
import io.sdkman.broker.domain.model.ValidationError
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.domain.repository.VersionRepository
import io.sdkman.broker.domain.util.ArchiveType

data class DownloadRequest(
    val candidate: String,
    val version: String,
    val platform: String
)

data class DownloadResponse(
    val url: String,
    val checksums: Map<String, String>,
    val archiveType: ArchiveType,
    val resolvedPlatform: String
)

sealed class DownloadError {
    data class InvalidPlatform(val platform: String) : DownloadError()
    data class CandidateNotFound(val candidate: String, val version: String) : DownloadError()
    data class PlatformNotFound(val candidate: String, val version: String, val platform: String) : DownloadError()
    data class SystemError(val message: String) : DownloadError()
}

interface DownloadService {
    fun resolveDownload(request: DownloadRequest): Either<DownloadError, DownloadResponse>
}

class DownloadServiceImpl(
    private val versionRepository: VersionRepository
) : DownloadService {

    private companion object {
        const val UNIVERSAL_PLATFORM = "UNIVERSAL"
        
        // Checksum algorithm priority (SHA-256 highest, MD5 lowest)
        private val CHECKSUM_PRIORITY = listOf("sha256", "sha512", "sha384", "sha224", "sha1", "md5")
    }

    override fun resolveDownload(request: DownloadRequest): Either<DownloadError, DownloadResponse> =
        validatePlatform(request.platform)
            .flatMap { normalizedPlatform ->
                findVersionWithPlatformFallback(request.candidate, request.version, normalizedPlatform)
            }
            .map { version ->
                DownloadResponse(
                    url = version.url,
                    checksums = prioritizeChecksums(version.checksums ?: emptyMap()),
                    archiveType = ArchiveType.fromUrl(version.url),
                    resolvedPlatform = version.platform
                )
            }

    private fun validatePlatform(platform: String): Either<DownloadError, String> =
        PlatformMapper.validateAndNormalize(platform)
            .fold(
                { error ->
                    when (error) {
                        is ValidationError.InvalidPlatform -> DownloadError.InvalidPlatform(error.platform).left()
                    }
                },
                { normalizedPlatform -> normalizedPlatform.right() }
            )

    private fun findVersionWithPlatformFallback(
        candidate: String,
        version: String,
        normalizedPlatform: String
    ): Either<DownloadError, Version> =
        // First try exact platform match
        findExactPlatformMatch(candidate, version, normalizedPlatform)
            .flatMap { exactMatch ->
                exactMatch.fold(
                    { 
                        // If no exact match, try UNIVERSAL fallback
                        findUniversalFallback(candidate, version)
                    },
                    { version -> version.right() }
                )
            }

    private fun findExactPlatformMatch(
        candidate: String,
        version: String,
        platform: String
    ): Either<DownloadError, Option<Version>> =
        versionRepository.findByCandidateVersionPlatform(candidate, version, platform)
            .fold(
                { repositoryError ->
                    when (repositoryError) {
                        is RepositoryError.NotFound -> DownloadError.CandidateNotFound(candidate, version).left()
                        is RepositoryError.DatabaseError -> DownloadError.SystemError(repositoryError.cause.message ?: "Database error").left()
                    }
                },
                { option -> option.right() }
            )

    private fun findUniversalFallback(
        candidate: String,
        version: String
    ): Either<DownloadError, Version> =
        versionRepository.findByCandidateVersionPlatform(candidate, version, UNIVERSAL_PLATFORM)
            .fold(
                { repositoryError ->
                    when (repositoryError) {
                        is RepositoryError.NotFound -> DownloadError.CandidateNotFound(candidate, version).left()
                        is RepositoryError.DatabaseError -> DownloadError.SystemError(repositoryError.cause.message ?: "Database error").left()
                    }
                },
                { universalOption ->
                    universalOption.fold(
                        { DownloadError.PlatformNotFound(candidate, version, "No platform-specific or UNIVERSAL version found").left() },
                        { version -> version.right() }
                    )
                }
            )

    private fun prioritizeChecksums(checksums: Map<String, String>): Map<String, String> =
        checksums.toList()
            .sortedBy { (algorithm, _) ->
                CHECKSUM_PRIORITY.indexOf(algorithm.lowercase()).takeIf { it != -1 } ?: Int.MAX_VALUE
            }
            .toMap()
}