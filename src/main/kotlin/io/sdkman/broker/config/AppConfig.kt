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
    val serverPort: Int
    val serverHost: String
}

class DefaultAppConfig : AppConfig {
    private val config: Config = ConfigFactory.load()

    // MongoDB settings
    override val mongodbDatabase: String = config.getStringOrDefault("mongodb.database", "sdkman")
    override val mongodbHost: String = config.getStringOrDefault("mongodb.host", "127.0.0.1")
    override val mongodbPort: String = config.getStringOrDefault("mongodb.port", "27017")
    override val mongodbUsername: Option<String> = config.getOptionString("mongodb.username")
    override val mongodbPassword: Option<String> = config.getOptionString("mongodb.password")

    // Server settings
    override val serverPort: Int = config.getIntOrDefault("server.port", 8080)
    override val serverHost: String = config.getStringOrDefault("server.host", "127.0.0.1")
}
