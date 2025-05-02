package io.sdkman.broker.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

class AppConfig {
    private val config: Config = ConfigFactory.load()
    
    // MongoDB settings
    val mongodbUri: String = config.getString("mongodb.uri")
    val mongodbDatabase: String = config.getString("mongodb.database")
    
    // Server settings
    val serverPort: Int = config.getInt("server.port")
    val serverHost: String = config.getString("server.host")
} 
