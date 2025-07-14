package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.None
import arrow.core.Some
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.sdkman.broker.domain.model.Audit
import io.sdkman.broker.domain.repository.PersistenceFailure
import io.sdkman.broker.support.PostgresTestListener
import io.sdkman.broker.support.shouldBeLeftAnd
import io.sdkman.broker.support.shouldBeRightAnd
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
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
            // TODO: Move this to a Postgres test support class
            val savedRecord =
                transaction(database) {
                    AuditTable.selectAll().where { AuditTable.id eq audit.id }.singleOrNull()
                }

            // TODO: introduce new reusable helper extension like shouldNotBeNullAnd { ... } for type T
            savedRecord.shouldNotBeNull()
            savedRecord.let {
                it[AuditTable.command] shouldBe audit.command
                it[AuditTable.candidate] shouldBe audit.candidate
                it[AuditTable.version] shouldBe audit.version
                it[AuditTable.platform] shouldBe audit.platform
                it[AuditTable.vendor] shouldBe audit.vendor.getOrNull()
                it[AuditTable.host] shouldBe audit.host
                it[AuditTable.agent] shouldBe audit.agent
                it[AuditTable.dist] shouldBe audit.dist
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
            // TODO: Move this to a Postgres test support class
            val savedRecord =
                transaction(database) {
                    AuditTable.selectAll().where { AuditTable.id eq audit.id }.singleOrNull()
                }

            // TODO: introduce new reusable helper extension like shouldNotBeNullAnd { ... } for type T
            savedRecord.shouldNotBeNull()
            savedRecord.let {
                it[AuditTable.vendor] shouldBe null
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

            result.shouldBeLeftAnd { error: PersistenceFailure ->
                // TODO: move this to a well-named helper method
                // TODO: check for an appropriate message
                error is PersistenceFailure.DatabaseConnectionFailure
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

            result.shouldBeLeftAnd { error: PersistenceFailure ->
                // TODO: move this to a well-named helper method
                error is PersistenceFailure.DatabaseConnectionFailure &&
                    // TODO: find a better way than toString() to retrieve the message
                    error.toString().contains("FATAL: password authentication failed for user \"$invalidUser\"")
            }
        }
    }
})
