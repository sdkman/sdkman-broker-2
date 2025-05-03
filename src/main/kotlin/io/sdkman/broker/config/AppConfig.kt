package io.sdkman.broker.config

import arrow.core.Option
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

class AppConfig {
    private val config: Config = ConfigFactory.load()

    // MongoDB settings
    val mongodbDatabase: String = config.getStringOrDefault("mongodb.database", "sdkman")
    val mongodbHost: String = config.getStringOrDefault("mongodb.host", "localhost")
    val mongodbPort: String = config.getStringOrDefault("mongodb.port", "27017")
    val mongodbUsername: Option<String> = config.getOptionString("mongodb.username")
    val mongodbPassword: Option<String> = config.getOptionString("mongodb.password")

    // Server settings
    val serverPort: Int = config.getIntOrDefault("server.port", 8080)
    val serverHost: String = config.getStringOrDefault("server.host", "0.0.0.0")
}