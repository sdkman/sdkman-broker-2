package io.sdkman.broker.domain.repository

import arrow.core.Either
import arrow.core.Option
import io.sdkman.broker.domain.model.Platform
import io.sdkman.broker.domain.model.VersionTagError

interface VersionTagRepository {
    fun findTaggedVersion(
        candidate: String,
        tag: String,
        platform: Platform
    ): Either<VersionTagError, Option<String>>
}
