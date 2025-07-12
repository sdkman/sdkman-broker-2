package io.sdkman.broker.adapter.secondary.persistence

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.shouldBe
import io.sdkman.broker.domain.repository.HealthCheckFailure
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.shouldBeLeftAnd
import io.sdkman.broker.support.shouldBeRightAnd
import org.junit.jupiter.api.Tag
import java.sql.DriverManager
import javax.sql.DataSource

@Tag("integration")
class PostgresHealthRepositoryIntegrationSpec : ShouldSpec() {
    // TODO: Instantiate the repository normally as a field inside `init`
    private lateinit var repository: PostgresHealthRepository

    // TODO: Move this into the `PostgresTestListener` where before hooks belong
    override suspend fun beforeTest(testCase: TestCase) {
        val jdbcUrl = PostgresTestListener.jdbcUrl()
        val dataSource =
            createTestDataSource(
                jdbcUrl,
                PostgresTestListener.username,
                PostgresTestListener.password
            )
        repository = PostgresHealthRepository(dataSource)
    }

    // TODO: remove the init block when `beforeTest` and `repository` are refactored away
    init {
        listeners(PostgresTestListener)

        context("PostgresHealthRepository Integration Tests") {

            should("successfully connect to PostgreSQL container and execute health check") {
                val result = repository.checkConnectivity()

                result.shouldBeRightAnd { true }
            }

            should("return ConnectionFailure when connecting to invalid database URL") {
                val invalidDataSource =
                    createTestDataSource(
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
                val jdbcUrl = PostgresTestListener.jdbcUrl()
                val invalidDataSource =
                    createTestDataSource(
                        jdbcUrl,
                        "invalid-user",
                        "invalid-password"
                    )
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

    // TODO: find a cleaner way of obtaining a datasource
    private fun createTestDataSource(
        jdbcUrl: String,
        username: String,
        password: String
    ): DataSource {
        return object : DataSource {
            override fun getConnection() = DriverManager.getConnection(jdbcUrl, username, password)

            override fun getConnection(
                username: String?,
                password: String?
            ) = DriverManager.getConnection(jdbcUrl, username, password)

            override fun getLogWriter() = null

            override fun setLogWriter(out: java.io.PrintWriter?) = Unit

            override fun getLoginTimeout() = 0

            override fun setLoginTimeout(seconds: Int) = Unit

            override fun getParentLogger() = java.util.logging.Logger.getLogger("")

            override fun <T : Any?> unwrap(iface: Class<T>?) = throw UnsupportedOperationException()

            override fun isWrapperFor(iface: Class<*>?) = false
        }
    }
}
