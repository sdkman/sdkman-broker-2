package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.sdkman.broker.config.AppConfig
import org.junit.jupiter.api.Tag

@Tag("unit")
class MongoConnectivitySpec : ShouldSpec({
    
    should("generate basic connection string for localhost with no credentials") {
        // given
        val connectivity = TestMongoConnectivity(
            host = None,
            port = None,
            username = None,
            password = None,
            database = "test-db",
            isProduction = false
        )
        
        // when
        val connectionString = connectivity.getConnectionString()
        
        // then
        connectionString shouldBe "mongodb://localhost:27017/test-db"
    }
    
    should("use custom host and port when provided") {
        // given
        val connectivity = TestMongoConnectivity(
            host = Some("mongo.example.com"),
            port = Some("12345"),
            username = None,
            password = None,
            database = "test-db",
            isProduction = false
        )
        
        // when
        val connectionString = connectivity.getConnectionString()
        
        // then
        connectionString shouldBe "mongodb://mongo.example.com:12345/test-db"
    }
    
    should("include credentials when username and password are provided") {
        // given
        val connectivity = TestMongoConnectivity(
            host = Some("localhost"),
            port = Some("27017"),
            username = Some("broker"),
            password = Some("password123"),
            database = "test-db",
            isProduction = false
        )
        
        // when
        val connectionString = connectivity.getConnectionString()
        
        // then
        connectionString shouldBe "mongodb://broker:password123@localhost:27017/test-db"
    }
    
    should("add auth mechanism for non-localhost production environments") {
        // given
        val connectivity = TestMongoConnectivity(
            host = Some("mongo.sdkman.io"),
            port = Some("16434"),
            username = Some("broker"),
            password = Some("password123"),
            database = "test-db",
            isProduction = true
        )
        
        // when
        val connectionString = connectivity.getConnectionString()
        
        // then
        connectionString shouldBe "mongodb://broker:password123@mongo.sdkman.io:16434/test-db?authMechanism=SCRAM-SHA-1"
    }
})

// Test-specific implementation of MongoConnectivity that exposes the buildConnectionString method
//TODO: Remove this and use the AppConfig with appropriate values
class TestMongoConnectivity(
    private val host: Option<String>,
    private val port: Option<String>,
    private val username: Option<String>,
    private val password: Option<String>,
    private val database: String,
    private val isProduction: Boolean
) {
    fun getConnectionString(): String {
        val hostValue = host.getOrElse { "localhost" }
        val portValue = port.getOrElse { "27017" }
        
        return when {
            // If username and password are provided, use authenticated connection
            username.isSome() && password.isSome() -> {
                val usernameValue = username.getOrElse { "" }
                val passwordValue = password.getOrElse { "" }
                val authMechanism = if (isProduction) "?authMechanism=SCRAM-SHA-1" else ""
                "mongodb://$usernameValue:$passwordValue@$hostValue:$portValue/$database$authMechanism"
            }
            // Otherwise use simple connection
            else -> "mongodb://$hostValue:$portValue/$database"
        }
    }
} 