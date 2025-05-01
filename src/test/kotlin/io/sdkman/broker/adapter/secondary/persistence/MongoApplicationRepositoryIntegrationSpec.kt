package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.mongodb.MongoClient
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.sdkman.broker.domain.model.Application
import io.sdkman.broker.domain.repository.RepositoryError
import org.bson.Document
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

/**
 * Integration test for the MongoDB implementation of ApplicationRepository.
 */
//TODO: Use a MongoTestListener instead to hand off MongoDB interaction to the listener
class MongoApplicationRepositoryIntegrationSpec : ShouldSpec() {
    private val mongoContainer = MongoDBContainer(DockerImageName.parse("mongo:3.2"))
        .apply { start() }
    
    private val mongoClient = MongoClient(
        mongoContainer.host,
        mongoContainer.firstMappedPort
    )
    
    private val database = mongoClient.getDatabase("sdkman")
    private val applicationCollection = database.getCollection("application")
    
    private val repository = MongoApplicationRepository(database)
    
    private fun resetDatabase() {
        applicationCollection.drop()
    }
    
    override suspend fun beforeTest(testCase: TestCase) {
        resetDatabase()
    }
    
    override suspend fun afterSpec(spec: io.kotest.core.spec.Spec) {
        mongoClient.close()
        mongoContainer.stop()
    }
    
    init {
        should("return application when record exists with valid alive status") {
            // given
            applicationCollection.insertOne(Document("alive", "OK"))
            
            // when
            val result = repository.findApplication()
            
            // then
            result.isRight() shouldBe true
            result.fold(
                { error -> throw RuntimeException("Repository error: $error") },
                { applicationOption ->
                    applicationOption shouldBe Some(app)
                }
            )
        }
        
        should("return None when application record does not exist") {
            // given: empty database
            
            // when
            val result = repository.findApplication()
            
            // then
            result.isRight() shouldBe true
            result.fold(
                { error -> throw RuntimeException("Repository error: $error") },
                { applicationOption ->
                    applicationOption shouldBe None
                }
            )
        }
        
        should("return an error when application record has invalid alive status") {
            // given
            applicationCollection.insertOne(Document("alive", "NOT_OK"))
            
            // when
            val result = repository.findApplication()
            
            // then
            //TODO: Write Either matchers for Kotest
            result.isLeft() shouldBe true
            result.fold(
                { error -> error.shouldBeTypeOf<RepositoryError.DatabaseError>() },
                { fail("Expected Left but got Right with $it") }
            )
        }
    }
    
    companion object {
        private val app = Application.of("OK").fold(
            { error -> throw IllegalStateException("Failed to create test application: $error") },
            { it }
        )
    }
    
    private fun fail(message: String): Nothing {
        throw AssertionError(message)
    }
} 