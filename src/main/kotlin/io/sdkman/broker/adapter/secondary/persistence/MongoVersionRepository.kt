package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.firstOrNone
import arrow.core.getOrElse
import arrow.core.toOption
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import io.sdkman.broker.adapter.secondary.persistence.MongoVersionRepository.Companion.CANDIDATE_FIELD
import io.sdkman.broker.adapter.secondary.persistence.MongoVersionRepository.Companion.PLATFORM_FIELD
import io.sdkman.broker.adapter.secondary.persistence.MongoVersionRepository.Companion.URL_FIELD
import io.sdkman.broker.adapter.secondary.persistence.MongoVersionRepository.Companion.VENDOR_FIELD
import io.sdkman.broker.adapter.secondary.persistence.MongoVersionRepository.Companion.VERSION_FIELD
import io.sdkman.broker.adapter.secondary.persistence.MongoVersionRepository.Companion.VISIBLE_FIELD
import io.sdkman.broker.domain.model.JavaDistribution
import io.sdkman.broker.domain.model.Platform
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.domain.model.VersionError
import io.sdkman.broker.domain.repository.VersionRepository
import org.bson.Document

class MongoVersionRepository(
    private val database: MongoDatabase
) : VersionRepository {
    companion object {
        private const val COLLECTION_NAME = "versions"
        const val CANDIDATE_FIELD = "candidate"
        const val VERSION_FIELD = "version"
        const val PLATFORM_FIELD = "platform"
        const val URL_FIELD = "url"
        const val VENDOR_FIELD = "vendor"
        const val VISIBLE_FIELD = "visible"
        const val CHECKSUMS_FIELD = "checksums"
    }

    override fun findByQuery(
        candidate: String,
        version: String,
        distribution: Option<String>,
        platform: Platform
    ): Either<VersionError, Option<Version>> =
        Either
            .catch {
                distribution.fold(
                    ifEmpty = { findVerbatim(candidate, version, platform.persistentId) },
                    ifSome = { enumName -> findByDistribution(candidate, version, enumName, platform.persistentId) }
                )
            }.mapLeft { error -> VersionError.DatabaseError(error) }

    private fun findVerbatim(
        candidate: String,
        version: String,
        platformId: String
    ): Option<Version> =
        database
            .getCollection(COLLECTION_NAME)
            .find(
                Filters.and(
                    Filters.eq(CANDIDATE_FIELD, candidate),
                    Filters.eq(VERSION_FIELD, version),
                    Filters.eq(PLATFORM_FIELD, platformId)
                )
            ).firstOrNone()
            .map { it.toVersion() }

    private fun findByDistribution(
        candidate: String,
        strippedVersion: String,
        distributionEnumName: String,
        platformId: String
    ): Option<Version> =
        JavaDistribution.shortCodeFor(distributionEnumName).fold(
            ifEmpty = { None },
            ifSome = { shortCode ->
                findVerbatim(candidate, "$strippedVersion-$shortCode", platformId)
                    .map { it.copy(version = strippedVersion, distribution = Some(distributionEnumName)) }
            }
        )
}

private fun Document.toVersion(): Version {
    val checksums =
        this
            .get(MongoVersionRepository.CHECKSUMS_FIELD, Document::class.java)
            .toOption()
            .map { checksumDoc ->
                checksumDoc
                    .mapKeys { it.key }
                    .mapValues { it.value.toString() }
            }.getOrElse { emptyMap() }

    return Version(
        candidate = this.getString(CANDIDATE_FIELD),
        version = this.getString(VERSION_FIELD),
        platform = this.getString(PLATFORM_FIELD),
        url = this.getString(URL_FIELD),
        distribution = this.getString(VENDOR_FIELD).toOption(),
        visible = this.getBoolean(VISIBLE_FIELD).toOption().getOrElse { true },
        checksums = checksums
    )
}
