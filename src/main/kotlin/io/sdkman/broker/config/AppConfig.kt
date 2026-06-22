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
    override val mongodbDatabase: String = config.getString("mongodb.database")
    override val mongodbHost: String = config.getString("mongodb.host")
    override val mongodbPort: String = config.getString("mongodb.port")
    override val mongodbUsername: Option<String> = config.getOptionString("mongodb.username")
    override val mongodbPassword: Option<String> = config.getOptionString("mongodb.password")
    override val mongodbAuthMechanism: Option<String> = config.getOptionString("mongodb.authmechanism")

    // Postgres settings
    override val postgresDatabase: String = config.getString("postgres.database")
    override val postgresHost: String = config.getString("postgres.host")
    override val postgresPort: String = config.getString("postgres.port")
    override val postgresUsername: Option<String> = config.getOptionString("postgres.username")
    override val postgresPassword: Option<String> = config.getOptionString("postgres.password")
    override val postgresSslMode: String = config.getString("postgres.sslmode")

    // Postgres connection pool (HikariCP) settings
    override val postgresPoolMaxSize: Int = config.getInt("postgres.pool.maxSize")
    override val postgresPoolMinIdle: Int = config.getInt("postgres.pool.minIdle")
    override val postgresPoolConnectionTimeoutMs: Long =
        config.getLong("postgres.pool.connectionTimeoutMs")
    override val postgresPoolMaxLifetimeMs: Long =
        config.getLong("postgres.pool.maxLifetimeMs")
    override val postgresPoolIdleTimeoutMs: Long =
        config.getLong("postgres.pool.idleTimeoutMs")

    // Server settings
    override val serverPort: Int = config.getInt("server.port")
    override val serverHost: String = config.getString("server.host")

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
