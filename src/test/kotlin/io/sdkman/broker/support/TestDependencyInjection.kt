package io.sdkman.broker.support

import arrow.core.Either
import arrow.core.left
import io.sdkman.broker.adapter.secondary.persistence.MongoApplicationRepository
import io.sdkman.broker.adapter.secondary.persistence.MongoVersionRepository
import io.sdkman.broker.adapter.secondary.persistence.PostgresAuditRepository
import io.sdkman.broker.adapter.secondary.persistence.PostgresHealthRepository
import io.sdkman.broker.application.service.CandidateDownloadServiceImpl
import io.sdkman.broker.application.service.MetaHealthServiceImpl
import io.sdkman.broker.application.service.MetaReleaseServiceImpl
import io.sdkman.broker.application.service.SdkmanCliDownloadServiceImpl
import io.sdkman.broker.application.service.SdkmanNativeDownloadServiceImpl
import io.sdkman.broker.domain.model.Audit
import io.sdkman.broker.domain.repository.AuditRepository
import io.sdkman.broker.domain.repository.DatabaseFailure
import javax.sql.DataSource

// Dependency injection for tests
// Uses the shared MongoTestListener and PostgresTestListener to provide consistent database access across all tests
object TestDependencyInjection {
    // Use the database from MongoTestListener directly
    val database by lazy { MongoTestListener.database }

    val postgresDataSource by lazy { PostgresTestListener.dataSource }

    fun postgresDataSource(
        username: String,
        password: String
    ): DataSource = PostgresTestListener.createDataSource(username, password)

    val applicationRepository by lazy {
        MongoApplicationRepository(database)
    }

    val versionRepository by lazy {
        MongoVersionRepository(database)
    }

    val postgresHealthRepository by lazy {
        PostgresHealthRepository(postgresDataSource)
    }

    val postgresHealthRepositoryInvalidCredentials by lazy {
        PostgresHealthRepository(postgresDataSource("invalid", "invalid"))
    }

    val auditRepository by lazy {
        PostgresAuditRepository(postgresDataSource)
    }

    val failingAuditRepository by lazy {
        object : AuditRepository {
            override fun save(audit: Audit): Either<DatabaseFailure, Unit> =
                DatabaseFailure.ConnectionFailure(RuntimeException()).left()
        }
    }

    val healthService by lazy {
        MetaHealthServiceImpl(applicationRepository, postgresHealthRepository)
    }

    val healthServiceInvalidCredentials by lazy {
        MetaHealthServiceImpl(applicationRepository, postgresHealthRepositoryInvalidCredentials)
    }

    val metaService by lazy {
        MetaReleaseServiceImpl()
    }

    val versionService by lazy {
        CandidateDownloadServiceImpl(versionRepository, auditRepository)
    }

    val versionServiceWithBrokenAuditRepo by lazy {
        CandidateDownloadServiceImpl(versionRepository, failingAuditRepository)
    }

    val sdkmanCliDownloadService by lazy {
        SdkmanCliDownloadServiceImpl()
    }

    val sdkmanNativeDownloadService by lazy {
        SdkmanNativeDownloadServiceImpl()
    }
}
