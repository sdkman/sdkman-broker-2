package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.None
import arrow.core.Some
import io.kotest.core.spec.style.ShouldSpec
import io.sdkman.broker.domain.model.Application
import io.sdkman.broker.domain.repository.RepositoryError
import io.sdkman.broker.support.MongoSupport
import io.sdkman.broker.support.MongoTestListener
import io.sdkman.broker.support.shouldBeLeftAnd
import io.sdkman.broker.support.shouldBeRight

class MongoApplicationRepositoryIntegrationSpec : ShouldSpec({
    listener(MongoTestListener)

    val repository = MongoApplicationRepository(MongoTestListener.database)

    should("return application when record exists with valid alive status") {
        // given
        MongoSupport.setupValidAppRecord()

        // when
        val result = repository.findApplication()

        // then
        Application.of("OK").fold(
            { error -> throw RuntimeException("Failed to create test application: $error") },
            { expectedApp -> result shouldBeRight Some(expectedApp) }
        )
    }

    should("return None when application record does not exist") {
        // given: empty database (MongoTestListener.resetDatabase() is called automatically)

        // when
        val result = repository.findApplication()

        // then
        result shouldBeRight None
    }

    should("return an error when application record has invalid alive status") {
        // given
        MongoSupport.setupInvalidAppRecord()

        // when
        val result = repository.findApplication()

        // then
        result shouldBeLeftAnd { it is RepositoryError.DatabaseError }
    }
})
