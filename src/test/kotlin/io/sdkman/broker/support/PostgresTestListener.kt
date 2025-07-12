package io.sdkman.broker.support

import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec
import org.testcontainers.containers.PostgreSQLContainer
import java.io.PrintWriter
import java.sql.DriverManager
import java.util.logging.Logger
import javax.sql.DataSource

object PostgresTestListener : TestListener {
    // Create a single static container for all tests
    private val postgresContainer: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer("postgres:15-alpine")
            .withDatabaseName("sdkman")
            .withUsername("testuser")
            .withPassword("testpass")
            .apply {
                start()
            }
    }

    val host: String by lazy { postgresContainer.host }
    val port: Int by lazy { postgresContainer.getMappedPort(5432) }
    val databaseName: String by lazy { postgresContainer.databaseName }
    val username: String by lazy { postgresContainer.username }
    val password: String by lazy { postgresContainer.password }

    override suspend fun beforeSpec(spec: Spec) {
        // Ensure container is started
        postgresContainer

        // Set up environment for tests
        setSystemProperties()
    }

    // Sets system properties for tests using TypeSafe Config format
    private fun setSystemProperties() {
        // Lower case with dots (format used by AppConfig)
        System.setProperty("postgres.host", host)
        System.setProperty("postgres.port", port.toString())
        System.setProperty("postgres.database", databaseName)
        System.setProperty("postgres.username", username)
        System.setProperty("postgres.password", password)
    }

    fun jdbcUrl(): String = "jdbc:postgresql://$host:$port/$databaseName"

    /*
     * Create DataSource with default test container credentials
     */
    val dataSource: DataSource by lazy {
        createDataSource(username, password)
    }

    /*
     * Create DataSource with custom credentials
     */
    fun createDataSource(
        username: String,
        password: String
    ): DataSource = createDataSource(jdbcUrl(), username, password)

    /*
     * Create DataSource with custom URL and credentials
     */
    fun createDataSource(
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

            override fun getLogWriter() = throw UnsupportedOperationException("Can't get LogWriter")

            override fun setLogWriter(out: PrintWriter?) = Unit

            override fun getLoginTimeout() = 0

            override fun setLoginTimeout(seconds: Int) = Unit

            override fun getParentLogger() = Logger.getLogger("")

            override fun <T : Any?> unwrap(iface: Class<T>?) = throw UnsupportedOperationException("Can't unwrap")

            override fun isWrapperFor(iface: Class<*>?) = false
        }
    }
}
