package io.sdkman.broker.adapter.secondary.persistence

import io.kotest.core.spec.style.ShouldSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.sdkman.broker.domain.repository.HealthCheckFailure
import io.sdkman.broker.support.shouldBeLeftAnd
import io.sdkman.broker.support.shouldBeRightAnd
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource

class PostgresHealthRepositorySpec : ShouldSpec({

    context("PostgresHealthRepository") {

        context("checkConnectivity") {

            should("return success when database connection and query are successful") {
                val dataSource = mockk<DataSource>()
                val connection = mockk<Connection>()
                val preparedStatement = mockk<PreparedStatement>()
                val resultSet = mockk<ResultSet>()

                every { dataSource.connection } returns connection
                every { connection.prepareStatement("SELECT 1") } returns preparedStatement
                every { preparedStatement.executeQuery() } returns resultSet
                every { resultSet.next() } returns true
                every { resultSet.getInt(1) } returns 1
                every { connection.close() } returns Unit
                every { preparedStatement.close() } returns Unit
                every { resultSet.close() } returns Unit

                val repository = PostgresHealthRepository(dataSource)
                val result = repository.checkConnectivity()

                result.shouldBeRightAnd { true }

                verify {
                    dataSource.connection
                    connection.prepareStatement("SELECT 1")
                    preparedStatement.executeQuery()
                    resultSet.next()
                    resultSet.getInt(1)
                }
            }

            should("return ConnectionFailure when connection cannot be established") {
                val dataSource = mockk<DataSource>()
                val connectionException = SQLException("Connection timeout")

                every { dataSource.connection } throws connectionException

                val repository = PostgresHealthRepository(dataSource)
                val result = repository.checkConnectivity()

                result.shouldBeLeftAnd { error: HealthCheckFailure ->
                    error is HealthCheckFailure.ConnectionFailure
                }

                verify { dataSource.connection }
            }

            should("return ConnectionFailure for connection-related SQL exceptions") {
                val dataSource = mockk<DataSource>()
                val connection = mockk<Connection>()
                val connectionException = SQLException("Connection refused")

                every { dataSource.connection } returns connection
                every { connection.prepareStatement("SELECT 1") } throws connectionException
                every { connection.close() } returns Unit

                val repository = PostgresHealthRepository(dataSource)
                val result = repository.checkConnectivity()

                result.shouldBeLeftAnd { error: HealthCheckFailure ->
                    error is HealthCheckFailure.ConnectionFailure
                }
            }

            // TODO: this scenario is so unlikely that we will remove the test
            should("return QueryFailure when SQL query fails") {
                val dataSource = mockk<DataSource>()
                val connection = mockk<Connection>()
                val preparedStatement = mockk<PreparedStatement>()
                val queryException = SQLException("Invalid SQL syntax")

                every { dataSource.connection } returns connection
                every { connection.prepareStatement("SELECT 1") } returns preparedStatement
                every { preparedStatement.executeQuery() } throws queryException
                every { connection.close() } returns Unit
                every { preparedStatement.close() } returns Unit

                val repository = PostgresHealthRepository(dataSource)
                val result = repository.checkConnectivity()

                result.shouldBeLeftAnd { error: HealthCheckFailure ->
                    error is HealthCheckFailure.QueryFailure
                }
            }

            // TODO: this scenario is so unlikely that we will remove the test
            should("return QueryFailure when result set has no results") {
                val dataSource = mockk<DataSource>()
                val connection = mockk<Connection>()
                val preparedStatement = mockk<PreparedStatement>()
                val resultSet = mockk<ResultSet>()

                every { dataSource.connection } returns connection
                every { connection.prepareStatement("SELECT 1") } returns preparedStatement
                every { preparedStatement.executeQuery() } returns resultSet
                every { resultSet.next() } returns false
                every { connection.close() } returns Unit
                every { preparedStatement.close() } returns Unit
                every { resultSet.close() } returns Unit

                val repository = PostgresHealthRepository(dataSource)
                val result = repository.checkConnectivity()

                result.shouldBeLeftAnd { error: HealthCheckFailure ->
                    error is HealthCheckFailure.QueryFailure
                }
            }

            // TODO: this scenario is so unlikely that we will remove the test
            should("return QueryFailure when result set returns unexpected value") {
                val dataSource = mockk<DataSource>()
                val connection = mockk<Connection>()
                val preparedStatement = mockk<PreparedStatement>()
                val resultSet = mockk<ResultSet>()

                every { dataSource.connection } returns connection
                every { connection.prepareStatement("SELECT 1") } returns preparedStatement
                every { preparedStatement.executeQuery() } returns resultSet
                every { resultSet.next() } returns true
                every { resultSet.getInt(1) } returns 0
                every { connection.close() } returns Unit
                every { preparedStatement.close() } returns Unit
                every { resultSet.close() } returns Unit

                val repository = PostgresHealthRepository(dataSource)
                val result = repository.checkConnectivity()

                result.shouldBeLeftAnd { error: HealthCheckFailure ->
                    error is HealthCheckFailure.QueryFailure
                }
            }

            // TODO: this scenario is so unlikely that we will remove the test
            xshould("properly close resources even when exceptions occur") {
                val dataSource = mockk<DataSource>()
                val connection = mockk<Connection>()
                val preparedStatement = mockk<PreparedStatement>()
                val queryException = SQLException("Database error")

                every { dataSource.connection } returns connection
                every { connection.prepareStatement("SELECT 1") } returns preparedStatement
                every { preparedStatement.executeQuery() } throws queryException
                every { connection.close() } returns Unit
                every { preparedStatement.close() } returns Unit

                val repository = PostgresHealthRepository(dataSource)
                repository.checkConnectivity()

                verify {
                    connection.close()
                    preparedStatement.close()
                }
            }
        }
    }
})
