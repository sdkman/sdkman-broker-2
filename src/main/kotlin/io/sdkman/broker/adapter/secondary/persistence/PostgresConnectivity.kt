package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.sdkman.broker.config.AppConfig
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class PostgresConnectivity(private val config: AppConfig) {
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
                throw RuntimeException("Failed to connect to PostgreSQL", exception)
            },
            { dataSource -> dataSource }
        )
    }

    fun buildConnectionString(): String {
        val host = config.postgresHost
        val port = config.postgresPort
        val database = config.postgresDatabase
        val simpleConnectionString = "jdbc:postgresql://$host:$port/$database"

        if (!isProductionEnvironment(host)) {
            return simpleConnectionString
        }

        val credentialConnectionString: Option<String> =
            config.postgresUsername.flatMap { username ->
                config.postgresPassword.map { password ->
                    "jdbc:postgresql://$username:$password@$host:$port/$database?sslmode=require"
                }
            }

        return credentialConnectionString.getOrElse { simpleConnectionString }
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

    fun isProductionEnvironment(host: String): Boolean = host != "localhost" && host != "127.0.0.1"
}
