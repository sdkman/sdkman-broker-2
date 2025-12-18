package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import io.sdkman.broker.domain.model.Audit
import io.sdkman.broker.domain.repository.AuditRepository
import io.sdkman.broker.domain.repository.DatabaseFailure
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

object AuditTable : Table("audit") {
    val id: Column<UUID> = uuid("id")
    val command: Column<String> = text("command")
    val candidate: Column<String> = text("candidate")
    val version: Column<String> = text("version")
    val clientPlatform: Column<String> = text("client_platform")
    val candidatePlatform: Column<String> = text("candidate_platform")
    val distribution: Column<String?> = text("distribution").nullable()
    val host: Column<String?> = text("host").nullable()
    val agent: Column<String?> = text("agent").nullable()
    val timestamp = timestamp("timestamp")

    override val primaryKey = PrimaryKey(id)
}

class PostgresAuditRepository(private val dataSource: DataSource) : AuditRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val database: Database by lazy { Database.connect(dataSource) }

    override fun save(audit: Audit): Either<DatabaseFailure, Unit> =
        Either.catch {
            transaction(database) {
                AuditTable.insert {
                    audit.id.map { auditId -> it[id] = auditId }
                    it[command] = audit.command
                    it[candidate] = audit.candidate
                    it[version] = audit.version
                    it[clientPlatform] = audit.clientPlatform
                    it[candidatePlatform] = audit.candidatePlatform
                    it[distribution] = audit.distribution.getOrNull()
                    it[host] = audit.host.getOrNull()
                    it[agent] = audit.agent.getOrNull()
                    it[timestamp] = audit.timestamp
                }
                Unit
            }
        }
            .mapLeft { exception ->
                logger.error("Failed to save audit record: {}", exception.message, exception)
                exception.toDatabaseFailure()
            }
}
