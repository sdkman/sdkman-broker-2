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
import io.sdkman.broker.application.service.CandidateDownloadServiceImpl
import io.sdkman.broker.application.service.MetaHealthService
import io.sdkman.broker.application.service.MetaHealthServiceImpl
import io.sdkman.broker.application.service.MetaReleaseService
import io.sdkman.broker.application.service.MetaReleaseServiceImpl
import io.sdkman.broker.application.service.SdkmanCliDownloadServiceImpl
import io.sdkman.broker.application.service.SdkmanNativeDownloadServiceImpl
import io.sdkman.broker.config.DefaultAppConfig
import io.sdkman.broker.domain.service.CandidateDownloadService
import io.sdkman.broker.domain.service.SdkmanCliDownloadService
import io.sdkman.broker.domain.service.SdkmanNativeDownloadService
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
        val metaHealthService = MetaHealthServiceImpl(applicationRepository, postgresHealthRepository)
        val metaReleaseService = MetaReleaseServiceImpl()
        val candidateDownloadService = CandidateDownloadServiceImpl(versionRepository, auditRepository)
        val sdkmanCliDownloadService = SdkmanCliDownloadServiceImpl()
        val sdkmanNativeDownloadService = SdkmanNativeDownloadServiceImpl()

        // Start Ktor server
        embeddedServer(Netty, port = config.serverPort, host = config.serverHost) {
            configureApp(
                metaHealthService,
                metaReleaseService,
                candidateDownloadService,
                sdkmanCliDownloadService,
                sdkmanNativeDownloadService
            )
        }.start(wait = true)
    }
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.configureApp(
    metaHealthService: MetaHealthService,
    metaReleaseService: MetaReleaseService,
    candidateDownloadService: CandidateDownloadService,
    sdkmanCliDownloadService: SdkmanCliDownloadService,
    sdkmanNativeDownloadService: SdkmanNativeDownloadService
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
    metaRoutes(metaHealthService, metaReleaseService)
    downloadRoutes(candidateDownloadService, sdkmanCliDownloadService, sdkmanNativeDownloadService)
}
