package io.sdkman.broker.config

import arrow.core.Option
import arrow.core.getOrElse
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

interface AppConfig {
    val mongodbHost: String
    val mongodbPort: String
    val mongodbDatabase: String
    val mongodbUsername: Option<String>
    val mongodbPassword: Option<String>
    val mongodbAuthMechanism: Option<String>
    val postgresHost: String
    val postgresPort: String
    val postgresDatabase: String
    val postgresUsername: Option<String>
    val postgresPassword: Option<String>
    val postgresSslMode: String
    val postgresPoolMaxSize: Int
    val postgresPoolMinIdle: Int
    val postgresPoolConnectionTimeoutMs: Long
    val postgresPoolMaxLifetimeMs: Long
    val postgresPoolIdleTimeoutMs: Long
    val serverPort: Int
    val serverHost: String
    val persistenceBackend: PersistenceBackend
}

class DefaultAppConfig : AppConfig {
    private val config: Config = ConfigFactory.load()

    // MongoDB settings
    override val mongodbDatabase: String = config.getStringOrDefault("mongodb.database", "sdkman")
    override val mongodbHost: String = config.getStringOrDefault("mongodb.host", "127.0.0.1")
    override val mongodbPort: String = config.getStringOrDefault("mongodb.port", "27017")
    override val mongodbUsername: Option<String> = config.getOptionString("mongodb.username")
    override val mongodbPassword: Option<String> = config.getOptionString("mongodb.password")
    override val mongodbAuthMechanism: Option<String> = config.getOptionString("mongodb.authmechanism")

    // Postgres settings
    override val postgresDatabase: String = config.getStringOrDefault("postgres.database", "sdkman")
    override val postgresHost: String = config.getStringOrDefault("postgres.host", "127.0.0.1")
    override val postgresPort: String = config.getStringOrDefault("postgres.port", "5432")
    override val postgresUsername: Option<String> = config.getOptionString("postgres.username")
    override val postgresPassword: Option<String> = config.getOptionString("postgres.password")
    override val postgresSslMode: String = config.getStringOrDefault("postgres.sslmode", "disable")

    // Postgres connection pool (HikariCP) settings
    override val postgresPoolMaxSize: Int = config.getIntOrDefault("postgres.pool.maxSize", 20)
    override val postgresPoolMinIdle: Int = config.getIntOrDefault("postgres.pool.minIdle", 2)
    override val postgresPoolConnectionTimeoutMs: Long =
        config.getLongOrDefault("postgres.pool.connectionTimeoutMs", 5_000L)
    override val postgresPoolMaxLifetimeMs: Long =
        config.getLongOrDefault("postgres.pool.maxLifetimeMs", 1_800_000L)
    override val postgresPoolIdleTimeoutMs: Long =
        config.getLongOrDefault("postgres.pool.idleTimeoutMs", 600_000L)

    // Server settings
    override val serverPort: Int = config.getIntOrDefault("server.port", 8080)
    override val serverHost: String = config.getStringOrDefault("server.host", "127.0.0.1")

    // Persistence backend selector
    override val persistenceBackend: PersistenceBackend =
        config
            .getOptionString("persistence.backend")
            .flatMap { PersistenceBackend.fromConfigValue(it) }
            .getOrElse {
                throw IllegalArgumentException(
                    "PERSISTENCE_BACKEND value not found or incorrect. Supported values: mongo, postgres."
                )
            }
}
