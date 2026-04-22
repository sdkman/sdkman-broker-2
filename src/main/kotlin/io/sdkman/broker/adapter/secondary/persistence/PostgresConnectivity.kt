package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.sdkman.broker.config.AppConfig
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class PostgresConnectivity(
    private val config: AppConfig
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun dataSource(): DataSource {
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

    private fun createDataSource(connectionString: String): DataSource =
        with(HikariConfig()) {
            jdbcUrl = connectionString
            config.postgresUsername.map { username = it }
            config.postgresPassword.map { password = it }
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 30000
            idleTimeout = 600000
            maxLifetime = 1800000
            HikariDataSource(this)
        }

    companion object {
        private const val SSL_MODE_DISABLE = "disable"
    }
}
