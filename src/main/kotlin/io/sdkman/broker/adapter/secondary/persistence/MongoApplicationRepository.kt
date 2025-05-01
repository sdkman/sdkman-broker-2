package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.flatMap
import com.mongodb.MongoException
import com.mongodb.client.MongoDatabase
import io.sdkman.broker.domain.model.Application
import io.sdkman.broker.domain.model.ApplicationError
import io.sdkman.broker.domain.repository.ApplicationRepository
import io.sdkman.broker.domain.repository.RepositoryError
import org.bson.Document

/**
 * MongoDB implementation of the ApplicationRepository interface.
 */
class MongoApplicationRepository(private val database: MongoDatabase) : ApplicationRepository {

    companion object {
        private const val COLLECTION_NAME = "application"
        private const val ALIVE_FIELD = "alive"
    }

    override fun findApplication(): Either<RepositoryError, Option<Application>> =
        Either.catch {
            val collection = database.getCollection(COLLECTION_NAME)
            val document = collection.find().first()
            
            //TODO: Use Arrow Option instead!!!
            document?.let { doc ->
                //TODO: Use Arrow Either instead!!!
                try {
                    val aliveValue = doc.getString(ALIVE_FIELD)
                    Application.of(aliveValue).fold(
                        { error ->
                            throw IllegalStateException("Invalid application record: $error")
                        },
                        { app -> Some(app) }
                    )
                } catch (e: Exception) {
                    throw when (e) {
                        is IllegalStateException -> e
                        else -> IllegalStateException("Error processing document: ${e.message}", e)
                    }
                }
            } ?: None
        }.mapLeft { error ->
            when (error) {
                is MongoException -> RepositoryError.ConnectionError(error)
                is IllegalStateException -> RepositoryError.DatabaseError(error)
                else -> RepositoryError.DatabaseError(error)
            }
        }
} 