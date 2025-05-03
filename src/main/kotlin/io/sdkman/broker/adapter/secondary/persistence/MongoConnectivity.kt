package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.isSome
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.MongoDatabase
import io.sdkman.broker.config.AppConfig

class MongoConnectivity(private val config: AppConfig) {

    //TODO: Implement appropriate error handling using arrow.core.Either
    fun database(): MongoDatabase {
        val connectionString = buildConnectionString()
        val mongoClient = MongoClient(MongoClientURI(connectionString))
        return mongoClient.getDatabase(config.mongodbDatabase)
    }

    private fun buildConnectionString(): String {
        val host = config.mongodbHost.getOrElse { "localhost" }
        val port = config.mongodbPort.getOrElse { "27017" }
        
        return when {
            // If username and password are provided, use authenticated connection
            //TODO: use a  monadic for comprehension to extract values. both username and password must be present
            config.mongodbUsername.isSome() && config.mongodbPassword.isSome() -> {
                val username = config.mongodbUsername.getOrElse { "" }
                val password = config.mongodbPassword.getOrElse { "" }
                val authMechanism = if (isProductionEnvironment(host)) "?authMechanism=SCRAM-SHA-1" else ""
                "mongodb://$username:$password@$host:$port/${config.mongodbDatabase}$authMechanism"
            }
            // Otherwise use simple connection
            else -> "mongodb://$host:$port/${config.mongodbDatabase}"
        }
    }

    //TODO: no CI variable, infer the environment from the host
    private fun isProductionEnvironment(host: String): Boolean {
        // Consider it a production environment if it's not localhost and not running in CI
        val isLocalhost = host == "localhost"
        val isCiEnvironment = Option.fromNullable(System.getenv("CI")).isSome()
        
        return !isLocalhost && !isCiEnvironment
    }
} 