package io.sdkman.broker.domain.repository

import arrow.core.Either
import arrow.core.Option
import io.sdkman.broker.domain.error.RepositoryError
import io.sdkman.broker.domain.model.Version

interface VersionRepository {
    fun findByCandidateVersionPlatform(candidate: String, version: String, platform: String): Either<RepositoryError, Option<Version>>
    fun findByCandidateVersion(candidate: String, version: String): Either<RepositoryError, List<Version>>
}