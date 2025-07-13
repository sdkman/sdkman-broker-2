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
    val postgresHost: String
    val postgresPort: String
    val postgresDatabase: String
    val postgresUsername: Option<String>
    val postgresPassword: Option<String>
    val serverPort: Int
    val serverHost: String
    val flywayUrl: String
    val flywayUsername: String
    val flywayPassword: String
}

class DefaultAppConfig : AppConfig {
    private val config: Config = ConfigFactory.load()

    // MongoDB settings
    override val mongodbDatabase: String = config.getStringOrDefault("mongodb.database", "sdkman")
    override val mongodbHost: String = config.getStringOrDefault("mongodb.host", "127.0.0.1")
    override val mongodbPort: String = config.getStringOrDefault("mongodb.port", "27017")
    override val mongodbUsername: Option<String> = config.getOptionString("mongodb.username")
    override val mongodbPassword: Option<String> = config.getOptionString("mongodb.password")

    // Postgres settings
    override val postgresDatabase: String = config.getStringOrDefault("postgres.database", "sdkman")
    override val postgresHost: String = config.getStringOrDefault("postgres.host", "127.0.0.1")
    override val postgresPort: String = config.getStringOrDefault("postgres.port", "5432")
    override val postgresUsername: Option<String> = config.getOptionString("postgres.username")
    override val postgresPassword: Option<String> = config.getOptionString("postgres.password")

    // Server settings
    override val serverPort: Int = config.getIntOrDefault("server.port", 8080)
    override val serverHost: String = config.getStringOrDefault("server.host", "127.0.0.1")

    // Flyway settings - use postgres credentials
    override val flywayUrl: String = "jdbc:postgresql://$postgresHost:$postgresPort/$postgresDatabase"
    override val flywayUsername: String = postgresUsername.getOrElse { "postgres" }
    override val flywayPassword: String = postgresPassword.getOrElse { "postgres" }
}
