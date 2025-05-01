package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.flatMap
import arrow.core.right
import com.mongodb.MongoException
import com.mongodb.client.MongoDatabase
import io.sdkman.broker.domain.model.Application
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
        findDocument()
            .flatMap { documentOption ->
                documentOption.fold(
                    { None.right() },
                    { doc -> processDocument(doc) }
                )
            }
    
    private fun findDocument(): Either<RepositoryError, Option<Document>> =
        Either.catch {
            Option.fromNullable(
                database.getCollection(COLLECTION_NAME)
                    .find()
                    .first()
            )
        }.mapLeft { error ->
            when (error) {
                is MongoException -> RepositoryError.ConnectionError(error)
                else -> RepositoryError.DatabaseError(error)
            }
        }

    private fun processDocument(document: Document): Either<RepositoryError, Option<Application>> =
        Either.catch { document.getString(ALIVE_FIELD) }
            .mapLeft { error -> RepositoryError.DatabaseError(error) }
            .flatMap { aliveStatus -> 
                Application.of(aliveStatus)
                    .mapLeft { error -> RepositoryError.DatabaseError(IllegalStateException("Invalid application record: $error")) }
                    .map { app -> Some(app) }
            }
} 