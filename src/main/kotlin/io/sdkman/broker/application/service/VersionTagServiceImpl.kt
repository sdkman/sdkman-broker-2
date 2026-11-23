package io.sdkman.broker.application.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.sdkman.broker.domain.model.Platform
import io.sdkman.broker.domain.model.VersionTagError
import io.sdkman.broker.domain.repository.VersionTagRepository
import io.sdkman.broker.domain.service.VersionTagService

class VersionTagServiceImpl(
    private val versionTagRepository: VersionTagRepository
) : VersionTagService {
    override fun findVersionByTag(
        candidate: String,
        tag: String,
        platformCode: String
    ): Either<VersionTagError, String> =
        either {
            val platform =
                Platform
                    .fromCode(platformCode)
                    .toEither { VersionTagError.InvalidPlatform(platformCode) }
                    .bind()
            val tagNotFound = VersionTagError.TagNotFound(candidate, tag, platform.auditId)
            ensure(candidate in CLI_CANDIDATES) { tagNotFound }
            versionTagRepository
                .findTaggedVersion(candidate, tag, platform)
                .bind()
                .toEither { tagNotFound }
                .bind()
        }

    companion object {
        private val CLI_CANDIDATES = setOf("sdkcli", "sdkrs")
    }
}
