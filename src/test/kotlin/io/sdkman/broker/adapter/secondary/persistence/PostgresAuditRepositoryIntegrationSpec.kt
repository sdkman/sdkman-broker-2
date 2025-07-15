package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.None
import arrow.core.Some
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.sdkman.broker.domain.model.Audit
import io.sdkman.broker.domain.repository.DatabaseFailure
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.PostgresTestSupport
import io.sdkman.broker.support.shouldBeLeftAnd
import io.sdkman.broker.support.shouldBeRightAnd
import io.sdkman.broker.support.shouldBeSomeAnd
import io.sdkman.broker.support.shouldContainMessage
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.Tag
import java.util.UUID

@Tag("integration")
class PostgresAuditRepositoryIntegrationSpec : ShouldSpec({
    listeners(PostgresTestListener)

    val repository = PostgresAuditRepository(PostgresTestListener.dataSource)
    val database = Database.connect(PostgresTestListener.dataSource)

    context("PostgresAuditRepository Integration Tests") {

        should("successfully save audit record to PostgreSQL container") {
            val audit =
                Audit(
                    id = UUID.randomUUID(),
                    command = "install",
                    candidate = "java",
                    version = "17.0.2-open",
                    platform = "linuxx64",
                    vendor = Some("openjdk"),
                    host = "test-host",
                    agent = "test-agent",
                    dist = "LINUX_X64",
                    timestamp = Clock.System.now()
                )

            val result = repository.save(audit)

            result.shouldBeRightAnd { true }

            // Verify the record was actually saved by querying the database
            val savedRecord = PostgresTestSupport.readSavedAuditRecord(database, audit.id)

            savedRecord shouldBeSomeAnd { record ->
                record[AuditTable.command] shouldBe audit.command
                record[AuditTable.candidate] shouldBe audit.candidate
                record[AuditTable.version] shouldBe audit.version
                record[AuditTable.platform] shouldBe audit.platform
                record[AuditTable.vendor] shouldBe audit.vendor.getOrNull()
                record[AuditTable.host] shouldBe audit.host
                record[AuditTable.agent] shouldBe audit.agent
                record[AuditTable.dist] shouldBe audit.dist
            }
        }

        should("successfully save audit record with null vendor") {
            val audit =
                Audit(
                    id = UUID.randomUUID(),
                    command = "install",
                    candidate = "java",
                    version = "17.0.2-open",
                    platform = "linux64",
                    vendor = None,
                    host = "test-host",
                    agent = "test-agent",
                    dist = "test-dist",
                    timestamp = Clock.System.now()
                )

            val result = repository.save(audit)

            result.shouldBeRightAnd { true }

            // Verify the record was actually saved by querying the database
            val savedRecord = PostgresTestSupport.readSavedAuditRecord(database, audit.id)

            savedRecord shouldBeSomeAnd { record ->
                record[AuditTable.vendor] shouldBe null
            }
        }

        should("return DatabaseConnectionFailure when connecting to invalid database URL") {
            val invalidDataSource =
                PostgresTestListener.createDataSource(
                    "jdbc:postgresql://invalid-host:5432/invalid-db",
                    "invalid-user",
                    "invalid-password"
                )
            val invalidRepository = PostgresAuditRepository(invalidDataSource)

            val audit =
                Audit(
                    id = UUID.randomUUID(),
                    command = "install",
                    candidate = "java",
                    version = "17.0.2-open",
                    platform = "linux64",
                    vendor = None,
                    host = "test-host",
                    agent = "test-agent",
                    dist = "test-dist",
                    timestamp = Clock.System.now()
                )

            val result = invalidRepository.save(audit)

            result.shouldBeLeftAnd { error: DatabaseFailure ->
                error is DatabaseFailure.ConnectionFailure &&
                    error shouldContainMessage "The connection attempt failed"
            }
        }

        should("return DatabaseConnectionFailure when connecting with invalid credentials") {
            val invalidUser = "invalid-user"
            val invalidDataSource = PostgresTestListener.createDataSource(invalidUser, "invalid-password")
            val invalidRepository = PostgresAuditRepository(invalidDataSource)

            val audit =
                Audit(
                    id = UUID.randomUUID(),
                    command = "install",
                    candidate = "java",
                    version = "17.0.2-open",
                    platform = "linux64",
                    vendor = None,
                    host = "test-host",
                    agent = "test-agent",
                    dist = "test-dist",
                    timestamp = Clock.System.now()
                )

            val result = invalidRepository.save(audit)

            result.shouldBeLeftAnd { error: DatabaseFailure ->
                error is DatabaseFailure.QueryExecutionFailure &&
                    error shouldContainMessage "FATAL: password authentication failed" &&
                    error shouldContainMessage invalidUser
            }
        }
    }
})
