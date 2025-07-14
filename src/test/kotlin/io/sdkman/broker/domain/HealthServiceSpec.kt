package io.sdkman.broker.domain

import arrow.core.Either
import arrow.core.None
import arrow.core.Some
import arrow.core.left
import arrow.core.right
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.every
import io.mockk.mockk
import io.sdkman.broker.application.service.DatabaseHealthStatus
import io.sdkman.broker.application.service.HealthCheckError
import io.sdkman.broker.application.service.HealthServiceImpl
import io.sdkman.broker.application.service.HealthStatus
import io.sdkman.broker.domain.model.Application
import io.sdkman.broker.domain.repository.ApplicationRepository
import io.sdkman.broker.domain.repository.DatabaseFailure
import io.sdkman.broker.domain.repository.HealthCheckSuccess
import io.sdkman.broker.domain.repository.HealthRepository
import io.sdkman.broker.domain.repository.RepositoryError
import io.sdkman.broker.support.shouldBeLeftAnd
import io.sdkman.broker.support.shouldBeRight

class HealthServiceSpec : ShouldSpec({
    val applicationOK =
        Application.of("OK").fold(
            { error -> throw IllegalStateException("Failed to create test application: $error") },
            { it }
        )

    context("HealthServiceImpl with dual database checks") {

        should("return DatabaseHealthStatus with both UP when both databases are healthy") {
            // given
            val mockApplicationRepo = mockk<ApplicationRepository>()
            val mockPostgresHealthRepo = mockk<HealthRepository>()

            every { mockApplicationRepo.findApplication() } returns Some(applicationOK).right()
            every { mockPostgresHealthRepo.checkConnectivity() } returns HealthCheckSuccess.right()

            val service = HealthServiceImpl(mockApplicationRepo, mockPostgresHealthRepo)

            // when
            val result = service.checkHealth()

            // then
            result shouldBeRight DatabaseHealthStatus(HealthStatus.UP, HealthStatus.UP)
        }

        should("return MongoDatabaseUnavailable error when MongoDB is down but PostgreSQL is up") {
            // given
            val mockApplicationRepo = mockk<ApplicationRepository>()
            val mockPostgresHealthRepo = mockk<HealthRepository>()

            every { mockApplicationRepo.findApplication() } returns None.right()
            every { mockPostgresHealthRepo.checkConnectivity() } returns HealthCheckSuccess.right()

            val service = HealthServiceImpl(mockApplicationRepo, mockPostgresHealthRepo)

            // when
            val result = service.checkHealth()

            // then
            result shouldBeLeftAnd { it is HealthCheckError.MongoDatabaseUnavailable }
        }

        should("return PostgresDatabaseUnavailable error when PostgreSQL is down but MongoDB is up") {
            // given
            val mockApplicationRepo = mockk<ApplicationRepository>()
            val mockPostgresHealthRepo = mockk<HealthRepository>()

            every { mockApplicationRepo.findApplication() } returns Some(applicationOK).right()
            every {
                mockPostgresHealthRepo.checkConnectivity()
            } returns DatabaseFailure.ConnectionFailure(RuntimeException("Connection failed")).left()

            val service = HealthServiceImpl(mockApplicationRepo, mockPostgresHealthRepo)

            // when
            val result = service.checkHealth()

            // then
            result shouldBeLeftAnd { it is HealthCheckError.PostgresDatabaseUnavailable }
        }

        should("return BothDatabasesUnavailable error when both databases are down") {
            // given
            val mockApplicationRepo = mockk<ApplicationRepository>()
            val mockPostgresHealthRepo = mockk<HealthRepository>()

            val dbError = RepositoryError.DatabaseError(RuntimeException("MongoDB error"))
            every { mockApplicationRepo.findApplication() } returns Either.Left(dbError)
            every {
                mockPostgresHealthRepo.checkConnectivity()
            } returns DatabaseFailure.QueryExecutionFailure(RuntimeException("PostgreSQL error")).left()

            val service = HealthServiceImpl(mockApplicationRepo, mockPostgresHealthRepo)

            // when
            val result = service.checkHealth()

            // then
            result shouldBeLeftAnd { it is HealthCheckError.BothDatabasesUnavailable }
        }

        should("return MongoDatabaseUnavailable when MongoDB has connection error") {
            // given
            val mockApplicationRepo = mockk<ApplicationRepository>()
            val mockPostgresHealthRepo = mockk<HealthRepository>()

            val connectionError = RepositoryError.ConnectionError(RuntimeException("Connection timeout"))
            every { mockApplicationRepo.findApplication() } returns Either.Left(connectionError)
            every { mockPostgresHealthRepo.checkConnectivity() } returns HealthCheckSuccess.right()

            val service = HealthServiceImpl(mockApplicationRepo, mockPostgresHealthRepo)

            // when
            val result = service.checkHealth()

            // then
            result shouldBeLeftAnd { it is HealthCheckError.MongoDatabaseUnavailable }
        }

        should("return PostgresDatabaseUnavailable when PostgreSQL has query failure") {
            // given
            val mockApplicationRepo = mockk<ApplicationRepository>()
            val mockPostgresHealthRepo = mockk<HealthRepository>()

            every { mockApplicationRepo.findApplication() } returns Some(applicationOK).right()
            every {
                mockPostgresHealthRepo.checkConnectivity()
            } returns DatabaseFailure.QueryExecutionFailure(RuntimeException("Query failed")).left()

            val service = HealthServiceImpl(mockApplicationRepo, mockPostgresHealthRepo)

            // when
            val result = service.checkHealth()

            // then
            result shouldBeLeftAnd { it is HealthCheckError.PostgresDatabaseUnavailable }
        }

        should("handle scenario where both databases have different error types") {
            // given
            val mockApplicationRepo = mockk<ApplicationRepository>()
            val mockPostgresHealthRepo = mockk<HealthRepository>()

            // MongoDB has a general database error, PostgreSQL has connection failure
            val dbError = RepositoryError.DatabaseError(RuntimeException("MongoDB query error"))
            every { mockApplicationRepo.findApplication() } returns Either.Left(dbError)
            every {
                mockPostgresHealthRepo.checkConnectivity()
            } returns DatabaseFailure.ConnectionFailure(RuntimeException("PostgreSQL connection error")).left()

            val service = HealthServiceImpl(mockApplicationRepo, mockPostgresHealthRepo)

            // when
            val result = service.checkHealth()

            // then
            result shouldBeLeftAnd { it is HealthCheckError.BothDatabasesUnavailable }
        }
    }
})
