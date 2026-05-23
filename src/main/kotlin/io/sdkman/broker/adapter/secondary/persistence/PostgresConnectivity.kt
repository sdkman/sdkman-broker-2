package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.sdkman.broker.config.AppConfig
import org.slf4j.LoggerFactory

class PostgresConnectivity(
    private val config: AppConfig
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun dataSource(): HikariDataSource {
        val result =
            Either.catch {
                val connectionString = buildConnectionString()
                logger.info("Building postgres connection string: $connectionString")
                createDataSource(connectionString)
            }

        return result.fold(
            { exception ->
                logger.error("Failed to connect to PostgreSQL: ${exception.message}", exception)
                throw IllegalStateException("Failed to connect to PostgreSQL", exception)
            },
            { dataSource -> dataSource }
        )
    }

    fun buildConnectionString(): String {
        val base = "jdbc:postgresql://${config.postgresHost}:${config.postgresPort}/${config.postgresDatabase}"
        return if (config.postgresSslMode == SSL_MODE_DISABLE) base else "$base?sslmode=${config.postgresSslMode}"
    }

    private fun createDataSource(connectionString: String): HikariDataSource =
        with(HikariConfig()) {
            jdbcUrl = connectionString
            config.postgresUsername.map { username = it }
            config.postgresPassword.map { password = it }
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = config.postgresPoolMaxSize
            minimumIdle = config.postgresPoolMinIdle
            connectionTimeout = config.postgresPoolConnectionTimeoutMs
            maxLifetime = config.postgresPoolMaxLifetimeMs
            idleTimeout = config.postgresPoolIdleTimeoutMs
            poolName = POOL_NAME
            // Boot even when Postgres is unreachable; the outage surfaces through
            // /meta/health rather than aborting startup (spec Business Rule 4).
            initializationFailTimeout = BOOT_WITHOUT_DB
            HikariDataSource(this)
        }

    companion object {
        private const val SSL_MODE_DISABLE = "disable"
        private const val POOL_NAME = "sdkman-broker-pool"
        private const val BOOT_WITHOUT_DB = -1L
    }
}
