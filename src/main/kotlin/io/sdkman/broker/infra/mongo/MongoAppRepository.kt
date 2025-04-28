package io.sdkman.broker.infra.mongo

import arrow.core.Either
import arrow.core.flatten
import arrow.core.getOrElse
import arrow.core.toOption
import com.mongodb.client.MongoDatabase
import io.sdkman.broker.domain.error.DomainError
import io.sdkman.broker.domain.model.App
import io.sdkman.broker.domain.repository.AppRepository
import org.bson.Document
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection

/**
 * MongoDB implementation of the AppRepository
 */
class MongoAppRepository(private val database: MongoDatabase) : AppRepository {
    /**
     * Finds the single application record in the MongoDB collection
     */
    override fun findApp(): Either<DomainError, App> =
        Either.catch {
            database.getCollection<Document>("application").findOne().toOption()
                .toEither<DomainError> { DomainError.AppNotFound() }.map { doc ->
                    App(
                        alive = doc.getString("alive").toOption().getOrElse { "" },
                        stableCliVersion = doc.getString("stableCliVersion").toOption().getOrElse { "" },
                        betaCliVersion = doc.getString("betaCliVersion").toOption().getOrElse { "" },
                        stableNativeCliVersion = doc.getString("stableNativeCliVersion").toOption().getOrElse { "" },
                        betaNativeCliVersion = doc.getString("betaNativeCliVersion").toOption().getOrElse { "" },
                    )
                }
        }.mapLeft { e -> DomainError.RepositoryError(e) }.flatten()
}
