package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.flatMap
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.MongoDatabase
import io.sdkman.broker.config.AppConfig
import org.slf4j.LoggerFactory

class MongoConnectivity(private val config: AppConfig) {
    
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    fun database(): MongoDatabase {
        val result = Either.catch {
            val connectionString = buildConnectionString()
            val mongoClient = MongoClient(MongoClientURI(connectionString))
            mongoClient.getDatabase(config.mongodbDatabase)
        }
        
        return result.fold(
            { exception ->
                logger.error("Failed to connect to MongoDB: ${exception.message}", exception)
                throw RuntimeException("Failed to connect to MongoDB", exception)
            },
            { database -> database }
        )
    }

    private fun buildConnectionString(): String {
        val host = config.mongodbHost
        val port = config.mongodbPort
        
        val credentials = config.mongodbUsername.flatMap { username ->
            config.mongodbPassword.map { password ->
                val authMechanism = if (isProductionEnvironment(host)) "?authMechanism=SCRAM-SHA-1" else ""
                "mongodb://$username:$password@$host:$port/${config.mongodbDatabase}$authMechanism"
            }
        }
        
        return credentials.getOrElse {
            "mongodb://$host:$port/${config.mongodbDatabase}"
        }
    }

    //TODO: no CI variable, infer the environment from the host
    private fun isProductionEnvironment(host: String): Boolean {
        val ciEnv = Option.fromNullable(System.getenv("CI"))
        
        return host != "localhost" && ciEnv.isEmpty()
    }
} 