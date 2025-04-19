package io.sdkman.broker.infra.mongo

import arrow.core.Either
import arrow.core.left
import arrow.core.right
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
    override fun findApp(): Either<DomainError, App> {
        return try {
            val collection = database.getCollection<Document>("application")
            val doc = collection.findOne()
            
            if (doc == null) {
                DomainError.AppNotFound().left()
            } else {
                App(
                    alive = doc.getString("alive") ?: "",
                    stableCliVersion = doc.getString("stableCliVersion") ?: "",
                    betaCliVersion = doc.getString("betaCliVersion") ?: "",
                    stableNativeCliVersion = doc.getString("stableNativeCliVersion") ?: "",
                    betaNativeCliVersion = doc.getString("betaNativeCliVersion") ?: ""
                ).right()
            }
        } catch (e: Exception) {
            DomainError.RepositoryError(e).left()
        }
    }
} 