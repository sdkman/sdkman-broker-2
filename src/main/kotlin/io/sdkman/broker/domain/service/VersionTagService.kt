package io.sdkman.broker.domain.service

import arrow.core.Either
import io.sdkman.broker.domain.model.VersionTagError

interface VersionTagService {
    fun findVersionByTag(
        candidate: String,
        tag: String,
        platformCode: String
    ): Either<VersionTagError, String>
}
