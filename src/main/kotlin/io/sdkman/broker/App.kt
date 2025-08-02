package io.sdkman.broker

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.sdkman.broker.adapter.primary.rest.downloadRoutes
import io.sdkman.broker.adapter.primary.rest.metaRoutes
import io.sdkman.broker.adapter.secondary.persistence.MongoApplicationRepository
import io.sdkman.broker.adapter.secondary.persistence.MongoConnectivity
import io.sdkman.broker.adapter.secondary.persistence.MongoVersionRepository
import io.sdkman.broker.adapter.secondary.persistence.PostgresAuditRepository
import io.sdkman.broker.adapter.secondary.persistence.PostgresConnectivity
import io.sdkman.broker.adapter.secondary.persistence.PostgresHealthRepository
import io.sdkman.broker.application.service.CandidateDownloadService
import io.sdkman.broker.application.service.CandidateDownloadServiceImpl
import io.sdkman.broker.application.service.HealthService
import io.sdkman.broker.application.service.HealthServiceImpl
import io.sdkman.broker.application.service.ReleaseService
import io.sdkman.broker.application.service.ReleaseServiceImpl
import io.sdkman.broker.config.DefaultAppConfig
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

object App {
    @JvmStatic
    fun main(args: Array<String>) {
        val config = DefaultAppConfig()

        // Create MongoDB connection
        val mongoConnectivity = MongoConnectivity(config)
        val database = mongoConnectivity.database()

        // Create Postgres connection
        val postgresConnectivity = PostgresConnectivity(config)
        val postgresDataSource = postgresConnectivity.dataSource()

        // Initialize repositories
        val applicationRepository = MongoApplicationRepository(database)
        val versionRepository = MongoVersionRepository(database)
        val postgresHealthRepository = PostgresHealthRepository(postgresDataSource)
        val auditRepository = PostgresAuditRepository(postgresDataSource)

        // Initialize services
        val healthService = HealthServiceImpl(applicationRepository, postgresHealthRepository)
        val releaseService = ReleaseServiceImpl()
        val versionService = CandidateDownloadServiceImpl(versionRepository, auditRepository)

        // Start Ktor server
        embeddedServer(Netty, port = config.serverPort, host = config.serverHost) {
            configureApp(healthService, releaseService, versionService)
        }.start(wait = true)
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureApp(
    healthService: HealthService,
    releaseService: ReleaseService,
    candidateDownloadService: CandidateDownloadService
) {
    // Install plugins
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            }
        )
    }

    // Configure routes
    metaRoutes(healthService, releaseService)
    downloadRoutes(candidateDownloadService)
}
