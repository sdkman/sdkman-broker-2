package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.*
import com.mongodb.MongoException
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import io.sdkman.broker.adapter.secondary.persistence.MongoVersionRepository.Companion.CANDIDATE_FIELD
import io.sdkman.broker.adapter.secondary.persistence.MongoVersionRepository.Companion.PLATFORM_FIELD
import io.sdkman.broker.adapter.secondary.persistence.MongoVersionRepository.Companion.URL_FIELD
import io.sdkman.broker.adapter.secondary.persistence.MongoVersionRepository.Companion.VENDOR_FIELD
import io.sdkman.broker.adapter.secondary.persistence.MongoVersionRepository.Companion.VERSION_FIELD
import io.sdkman.broker.adapter.secondary.persistence.MongoVersionRepository.Companion.VISIBLE_FIELD
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.domain.model.VersionError
import io.sdkman.broker.domain.repository.VersionRepository
import org.bson.Document

class MongoVersionRepository(private val database: MongoDatabase) : VersionRepository {
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
        platform: String
    ): Either<VersionError, Option<Version>> =
        Either.catch {
            val filter =
                Filters.and(
                    Filters.eq(CANDIDATE_FIELD, candidate),
                    Filters.eq(VERSION_FIELD, version),
                    Filters.eq(PLATFORM_FIELD, platform)
                )

            database.getCollection(COLLECTION_NAME)
                .find(filter)
                .firstOrNone()
                .map { it.toVersion() }
        }.mapLeft { error ->
            when (error) {
                is MongoException -> VersionError.DatabaseError(error)
                else -> VersionError.DatabaseError(error)
            }
        }

}

private fun Document.toVersion(): Version {
    val checksums =
        this.get(MongoVersionRepository.CHECKSUMS_FIELD, Document::class.java)
            .toOption()
            .map { checksumDoc ->
                checksumDoc.mapKeys { it.key }
                    .mapValues { it.value.toString() }
            }
            .getOrElse { emptyMap() }

    return Version(
        candidate = this.getString(CANDIDATE_FIELD),
        version = this.getString(VERSION_FIELD),
        platform = this.getString(PLATFORM_FIELD),
        url = this.getString(URL_FIELD),
        vendor = this.getString(VENDOR_FIELD).toOption(),
        visible = this.getBoolean(VISIBLE_FIELD).toOption().getOrElse { true },
        checksums = checksums
    )
}
