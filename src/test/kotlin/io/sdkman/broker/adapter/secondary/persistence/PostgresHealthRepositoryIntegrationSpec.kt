package io.sdkman.broker.adapter.secondary.persistence

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.sdkman.broker.domain.repository.HealthCheckFailure
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.shouldBeLeftAnd
import io.sdkman.broker.support.shouldBeRightAnd
import org.junit.jupiter.api.Tag
import java.sql.DriverManager

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

            // TODO: remove redundant test
            should("work with actual PostgreSQL connection from container") {
                val jdbcUrl = PostgresTestListener.jdbcUrl()

                // Verify container is running and accessible
                val connection =
                    DriverManager.getConnection(
                        jdbcUrl,
                        PostgresTestListener.username,
                        PostgresTestListener.password
                    )

                connection.use { conn ->
                    val statement = conn.prepareStatement("SELECT 1")
                    val resultSet = statement.executeQuery()
                    resultSet.next()
                    val result = resultSet.getInt(1)

                    // Verify direct database access works
                    result shouldBe 1
                }

                // Now test through our repository
                val repositoryResult = repository.checkConnectivity()
                repositoryResult.shouldBeRightAnd { true }
            }
        }
    }
}
