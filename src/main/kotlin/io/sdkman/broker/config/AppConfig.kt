package io.sdkman.broker.config

import arrow.core.Option
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

class AppConfig {
    private val config: Config = ConfigFactory.load()

    // MongoDB settings
    val mongodbDatabase: String = config.getString("mongodb.database")
    val mongodbHost: String = config.getString("mongodb.host")
    val mongodbPort: String = config.getString("mongodb.port")
    val mongodbUsername: Option<String> = Option.fromNullable(config.getString("mongodb.username"))
    val mongodbPassword: Option<String> = Option.fromNullable(config.getString("mongodb.password"))

    // Server settings
    val serverPort: Int = config.getInt("server.port")
    val serverHost: String = config.getString("server.host")
}