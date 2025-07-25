package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.None
import arrow.core.Some
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
            val auditId = UUID.randomUUID()
            val audit =
                Audit(
                    id = Some(auditId),
                    command = "install",
                    candidate = "java",
                    version = "17.0.2-open",
                    platform = "linuxx64",
                    vendor = Some("openjdk"),
                    host = Some("test-host"),
                    agent = Some("test-agent"),
                    dist = "LINUX_X64",
                    timestamp = Clock.System.now()
                )

            val result = repository.save(audit)

            result.shouldBeRightAnd { true }

            // Verify the record was actually saved by querying the database
            val savedRecord = PostgresTestSupport.readSavedAuditRecord(database, auditId)

            savedRecord shouldBeSomeAnd { record ->
                record[AuditTable.command] shouldBe audit.command
                record[AuditTable.candidate] shouldBe audit.candidate
                record[AuditTable.version] shouldBe audit.version
                record[AuditTable.platform] shouldBe audit.platform
                record[AuditTable.vendor] shouldBe audit.vendor.getOrNull()
                record[AuditTable.host] shouldBe audit.host.getOrNull()
                record[AuditTable.agent] shouldBe audit.agent.getOrNull()
                record[AuditTable.dist] shouldBe audit.dist
            }
        }

        should("successfully save audit record with null vendor") {
            val auditId = UUID.randomUUID()
            val audit =
                Audit(
                    id = Some(auditId),
                    command = "install",
                    candidate = "java",
                    version = "17.0.2-open",
                    platform = "linux64",
                    vendor = None,
                    host = Some("test-host"),
                    agent = Some("test-agent"),
                    dist = "test-dist",
                    timestamp = Clock.System.now()
                )

            val result = repository.save(audit)

            result.shouldBeRightAnd { true }

            // Verify the record was actually saved by querying the database
            val savedRecord = PostgresTestSupport.readSavedAuditRecord(database, auditId)

            savedRecord shouldBeSomeAnd { record ->
                record[AuditTable.vendor] shouldBe null
            }
        }

        should("successfully save audit record with null host") {
            val auditId = UUID.randomUUID()
            val audit =
                Audit(
                    id = Some(auditId),
                    command = "install",
                    candidate = "java",
                    version = "17.0.2-open",
                    platform = "linux64",
                    vendor = Some("openjdk"),
                    host = None,
                    agent = Some("test-agent"),
                    dist = "test-dist",
                    timestamp = Clock.System.now()
                )

            val result = repository.save(audit)

            result.shouldBeRightAnd { true }

            val savedRecord = PostgresTestSupport.readSavedAuditRecord(database, auditId)

            savedRecord shouldBeSomeAnd { record ->
                record[AuditTable.host] shouldBe null
            }
        }

        should("successfully save audit record with null user agent") {
            val auditId = UUID.randomUUID()
            val audit =
                Audit(
                    id = Some(auditId),
                    command = "install",
                    candidate = "java",
                    version = "17.0.2-open",
                    platform = "linux64",
                    vendor = Some("openjdk"),
                    host = Some("test-host"),
                    agent = None,
                    dist = "test-dist",
                    timestamp = Clock.System.now()
                )

            val result = repository.save(audit)

            result.shouldBeRightAnd { true }

            val savedRecord = PostgresTestSupport.readSavedAuditRecord(database, auditId)

            savedRecord shouldBeSomeAnd { record ->
                record[AuditTable.agent] shouldBe null
            }
        }

        should("successfully save audit record with auto-generated ID") {
            val audit =
                Audit(
                    id = None,
                    command = "list",
                    candidate = "kotlin",
                    version = "1.9.22",
                    platform = "linux64",
                    vendor = Some("jetbrains"),
                    host = Some("test-host"),
                    agent = Some("test-agent"),
                    dist = "test-dist",
                    timestamp = Clock.System.now()
                )

            val result = repository.save(audit)

            result.shouldBeRightAnd { true }

            val savedRecord =
                PostgresTestSupport.readSavedAuditRecordByVersion(
                    database = database,
                    candidate = audit.candidate,
                    version = audit.version,
                    platform = audit.platform
                )

            savedRecord shouldBeSomeAnd { record ->
                record[AuditTable.id] shouldNotBe null
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
                    id = Some(UUID.randomUUID()),
                    command = "install",
                    candidate = "java",
                    version = "17.0.2-open",
                    platform = "linux64",
                    vendor = None,
                    host = Some("test-host"),
                    agent = Some("test-agent"),
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
                    id = Some(UUID.randomUUID()),
                    command = "install",
                    candidate = "java",
                    version = "17.0.2-open",
                    platform = "linux64",
                    vendor = None,
                    host = Some("test-host"),
                    agent = Some("test-agent"),
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
