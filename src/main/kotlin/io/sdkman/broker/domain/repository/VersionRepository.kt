package io.sdkman.broker.domain.repository

import arrow.core.Either
import arrow.core.Option
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.domain.model.VersionError

interface VersionRepository {
    fun findByQuery(
        candidate: String,
        version: String,
        platform: String
    ): Either<VersionError, Option<Version>>
}
