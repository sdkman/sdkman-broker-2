package io.sdkman.broker.support

import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
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

        // Run migrations once per container lifecycle
        runMigrations()
    }

    private fun runMigrations() {
        val flyway =
            Flyway
                .configure()
                .dataSource(jdbcUrl(), username, password)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .load()
        flyway.migrate()
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
    ): DataSource =
        PGSimpleDataSource().apply {
            setURL(jdbcUrl)
            this.user = username
            this.password = password
        }
}
