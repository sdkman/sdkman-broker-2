package io.sdkman.broker.adapter.secondary.persistence

import io.kotest.core.spec.style.ShouldSpec
import io.sdkman.broker.domain.repository.HealthCheckFailure
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.shouldBeLeftAnd
import io.sdkman.broker.support.shouldBeRightAnd
import org.junit.jupiter.api.Tag

@Tag("integration")
class PostgresHealthRepositoryIntegrationSpec : ShouldSpec() {
    init {
        listeners(PostgresTestListener)

        val repository = PostgresHealthRepository(PostgresTestListener.dataSource)

        context("PostgresHealthRepository Integration Tests") {

            should("successfully connect to PostgreSQL container and execute health check") {
                val result = repository.checkConnectivity()

                result.shouldBeRightAnd { true }
            }

            should("return ConnectionFailure when connecting to invalid database URL") {
                val invalidDataSource =
                    PostgresTestListener.createDataSource(
                        "jdbc:postgresql://invalid-host:5432/invalid-db",
                        "invalid-user",
                        "invalid-password"
                    )
                val invalidRepository = PostgresHealthRepository(invalidDataSource)

                val result = invalidRepository.checkConnectivity()

                result.shouldBeLeftAnd { error: HealthCheckFailure ->
                    error is HealthCheckFailure.ConnectionFailure
                }
            }

            should("return ConnectionFailure when connecting with invalid credentials") {
                val invalidDataSource = PostgresTestListener.createDataSource("invalid-user", "invalid-password")
                val invalidRepository = PostgresHealthRepository(invalidDataSource)

                val result = invalidRepository.checkConnectivity()

                result.shouldBeLeftAnd { error: HealthCheckFailure ->
                    error.toString().contains("FATAL: password authentication failed for user \"invalid-user\"")
                }
            }
        }
    }
}
