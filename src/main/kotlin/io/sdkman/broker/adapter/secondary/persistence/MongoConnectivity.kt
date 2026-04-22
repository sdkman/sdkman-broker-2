package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.MongoDatabase
import io.sdkman.broker.config.AppConfig
import org.slf4j.LoggerFactory

class MongoConnectivity(
    private val config: AppConfig
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun database(): MongoDatabase {
        val result =
            Either.catch {
                val connectionString = buildConnectionString()
                val mongoClient = MongoClient(MongoClientURI(connectionString))
                mongoClient.getDatabase(config.mongodbDatabase)
            }

        return result.fold(
            { exception ->
                logger.error("Failed to connect to MongoDB: ${exception.message}", exception)
                throw IllegalStateException("Failed to connect to MongoDB", exception)
            },
            { database -> database }
        )
    }

    fun buildConnectionString(): String {
        val host = config.mongodbHost
        val port = config.mongodbPort
        val authSuffix = config.mongodbAuthMechanism.map { "?authMechanism=$it" }.getOrElse { "" }

        val credentials =
            config.mongodbUsername.flatMap { username ->
                config.mongodbPassword.map { password ->
                    "mongodb://$username:$password@$host:$port/${config.mongodbDatabase}$authSuffix"
                }
            }

        return credentials.getOrElse {
            "mongodb://$host:$port/${config.mongodbDatabase}"
        }
    }
}
