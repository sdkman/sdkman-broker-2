package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import arrow.core.Option
import arrow.core.toOption
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import io.sdkman.broker.domain.error.RepositoryError
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.domain.repository.VersionRepository
import org.bson.Document

class MongoVersionRepository(private val database: MongoDatabase) : VersionRepository {

    private companion object {
        const val VERSIONS_COLLECTION = "versions"
    }

    override fun findByCandidateVersionPlatform(
        candidate: String,
        version: String,
        platform: String
    ): Either<RepositoryError, Option<Version>> =
        Either.catch {
            database.getCollection(VERSIONS_COLLECTION)
                .find(
                    Filters.and(
                        Filters.eq("candidate", candidate),
                        Filters.eq("version", version),
                        Filters.eq("platform", platform)
                    )
                )
                .firstOrNull()
                ?.toVersion()
                .toOption()
        }.mapLeft { e ->
            RepositoryError.DatabaseError(e)
        }

    override fun findByCandidateVersion(
        candidate: String,
        version: String
    ): Either<RepositoryError, List<Version>> =
        Either.catch {
            database.getCollection(VERSIONS_COLLECTION)
                .find(
                    Filters.and(
                        Filters.eq("candidate", candidate),
                        Filters.eq("version", version)
                    )
                )
                .map { it.toVersion() }
                .toList()
        }.mapLeft { e ->
            RepositoryError.DatabaseError(e)
        }

    private fun Document.toVersion(): Version =
        Version(
            candidate = getString("candidate"),
            version = getString("version"),
            platform = getString("platform"),
            url = getString("url"),
            vendor = getString("vendor"),
            visible = getBoolean("visible"),
            checksums = get("checksums", Document::class.java)?.let { checksumDoc ->
                checksumDoc.toMap().mapValues { it.value.toString() }
            }
        )
}