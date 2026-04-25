package io.sdkman.broker.config

import arrow.core.Option
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
    val persistenceBackend: PersistenceBackend
    val serverPort: Int
    val serverHost: String
}

class DefaultAppConfig(
    private val config: Config = ConfigFactory.load()
) : AppConfig {
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

    // Persistence backend selector — fails fast at construction on invalid values.
    override val persistenceBackend: PersistenceBackend =
        PersistenceBackend.fromValue(config.getStringOrDefault("persistence.backend", "mongo"))

    // Server settings
    override val serverPort: Int = config.getIntOrDefault("server.port", 8080)
    override val serverHost: String = config.getStringOrDefault("server.host", "127.0.0.1")
}
