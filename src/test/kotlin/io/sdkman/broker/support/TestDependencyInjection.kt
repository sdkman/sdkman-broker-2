package io.sdkman.broker.support

import io.sdkman.broker.adapter.secondary.persistence.MongoApplicationRepository
import io.sdkman.broker.adapter.secondary.persistence.MongoVersionRepository
import io.sdkman.broker.adapter.secondary.persistence.PostgresHealthRepository
import io.sdkman.broker.application.service.HealthServiceImpl
import io.sdkman.broker.application.service.ReleaseServiceImpl
import io.sdkman.broker.application.service.VersionServiceImpl
import io.sdkman.broker.config.DefaultAppConfig
import java.io.PrintWriter
import java.sql.DriverManager
import java.util.logging.Logger
import javax.sql.DataSource

// Dependency injection for tests
// Uses the shared MongoTestListener and PostgresTestListener to provide consistent database access across all tests
object TestDependencyInjection {
    // Use the shared containers from test listeners
    val config by lazy { DefaultAppConfig() }

    // Use the database from MongoTestListener directly
    val database by lazy { MongoTestListener.database }

    // Use PostgreSQL from PostgresTestListener
    val postgresDataSource by lazy {
        // Create a DataSource that uses the test container connection
        val jdbcUrl = PostgresTestListener.jdbcUrl()
        // TODO: move this to PostgresTestListener, find a cleaner approach that aligns with current project patterns
        object : DataSource {
            override fun getConnection() =
                DriverManager.getConnection(jdbcUrl, PostgresTestListener.username, PostgresTestListener.password)

            override fun getConnection(
                username: String?,
                password: String?
            ) = DriverManager.getConnection(jdbcUrl, username, password)

            override fun getLogWriter() = throw UnsupportedOperationException("Can't get LogWriter")

            override fun setLogWriter(out: PrintWriter?) = Unit

            override fun getLoginTimeout() = 0

            override fun setLoginTimeout(seconds: Int) = Unit

            override fun getParentLogger() = Logger.getLogger("")

            override fun <T : Any?> unwrap(iface: Class<T>?) = throw UnsupportedOperationException("Can't unwrap")

            override fun isWrapperFor(iface: Class<*>?) = false
        }
    }

    fun postgresDataSource(
        username: String,
        password: String
    ): DataSource {
        // Create a DataSource that uses the test container connection
        val jdbcUrl = PostgresTestListener.jdbcUrl()
        // TODO: move this to PostgresTestListener, find a cleaner approach that aligns with current project patterns
        return object : DataSource {
            override fun getConnection() = DriverManager.getConnection(jdbcUrl, username, password)

            override fun getConnection(
                username: String?,
                password: String?
            ) = DriverManager.getConnection(jdbcUrl, username, password)

            override fun getLogWriter() = throw UnsupportedOperationException("Can't get LogWriter")

            override fun setLogWriter(out: PrintWriter?) = Unit

            override fun getLoginTimeout() = 0

            override fun setLoginTimeout(seconds: Int) = Unit

            override fun getParentLogger() = Logger.getLogger("")

            override fun <T : Any?> unwrap(iface: Class<T>?) = throw UnsupportedOperationException("Can't unwrap")

            override fun isWrapperFor(iface: Class<*>?) = false
        }
    }

    val applicationRepository by lazy {
        MongoApplicationRepository(database)
    }

    val versionRepository by lazy {
        MongoVersionRepository(database)
    }

    val postgresHealthRepository by lazy {
        PostgresHealthRepository(postgresDataSource)
    }

    val postgresHealthRepositoryInvalidCredentials by lazy {
        PostgresHealthRepository(postgresDataSource("invalid", "invalid"))
    }

    val healthService by lazy {
        HealthServiceImpl(applicationRepository, postgresHealthRepository)
    }

    val healthServiceInvalidCredentials by lazy {
        HealthServiceImpl(applicationRepository, postgresHealthRepositoryInvalidCredentials)
    }

    val releaseService by lazy {
        ReleaseServiceImpl()
    }

    val versionService by lazy {
        VersionServiceImpl(versionRepository)
    }
}
