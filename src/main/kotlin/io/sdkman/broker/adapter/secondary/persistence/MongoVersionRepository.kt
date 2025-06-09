package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.toOption
import com.mongodb.MongoException
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.domain.model.VersionError
import io.sdkman.broker.domain.repository.VersionRepository
import org.bson.Document

class MongoVersionRepository(private val database: MongoDatabase) : VersionRepository {
    companion object {
        private const val COLLECTION_NAME = "versions"
        private const val CANDIDATE_FIELD = "candidate"
        private const val VERSION_FIELD = "version"
        private const val PLATFORM_FIELD = "platform"
        private const val URL_FIELD = "url"
        private const val VENDOR_FIELD = "vendor"
        private const val VISIBLE_FIELD = "visible"
        private const val CHECKSUMS_FIELD = "checksums"
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
                .first()
                .toOption()
                .map { doc ->
                    // TODO: Call an extension method on Document
                    documentToVersion(doc)
                }
        }.mapLeft { error ->
            when (error) {
                is MongoException -> VersionError.DatabaseError(error)
                else -> VersionError.DatabaseError(error)
            }
        }

    // TODO: Turn this into an extension method on Document
    private fun documentToVersion(document: Document): Version {
        val checksums =
            document.get(CHECKSUMS_FIELD, Document::class.java)
                .toOption()
                .map { checksumDoc ->
                    checksumDoc.mapKeys { it.key }
                        .mapValues { it.value.toString() }
                }
                .getOrElse { emptyMap() }

        return Version(
            candidate = document.getString(CANDIDATE_FIELD),
            version = document.getString(VERSION_FIELD),
            platform = document.getString(PLATFORM_FIELD),
            url = document.getString(URL_FIELD),
            vendor = document.getString(VENDOR_FIELD).toOption(),
            visible = document.getBoolean(VISIBLE_FIELD).toOption().getOrElse { true },
            checksums = checksums
        )
    }
}
