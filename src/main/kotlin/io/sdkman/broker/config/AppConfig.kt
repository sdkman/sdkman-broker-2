package io.sdkman.broker.config

import arrow.core.Option
import arrow.core.toOption
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

class AppConfig {
    private val config: Config = ConfigFactory.load()

    // MongoDB settings
    val mongodbDatabase: String = config.getString("mongodb.database")
    //TODO: use typesafe config to read the environment variables
    val mongodbHost: Option<String> = Option.fromNullable(System.getenv("MONGODB_HOST"))
    val mongodbPort: Option<String> = Option.fromNullable(System.getenv("MONGODB_PORT"))
    val mongodbUsername: Option<String> = Option.fromNullable(System.getenv("MONGODB_USERNAME"))
    val mongodbPassword: Option<String> = Option.fromNullable(System.getenv("MONGODB_PASSWORD"))

    // Server settings
    val serverPort: Int = config.getInt("server.port")
    val serverHost: String = config.getString("server.host")
}
