package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.toOption
import io.sdkman.broker.domain.model.Audit
import io.sdkman.broker.domain.repository.AuditRepository
import io.sdkman.broker.domain.repository.PersistenceFailure
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
    val platform: Column<String> = text("platform")
    val vendor: Column<String?> = text("vendor").nullable()
    val host: Column<String> = text("host")
    val agent: Column<String> = text("agent")
    val dist: Column<String> = text("dist")
    val timestamp = timestamp("timestamp")

    override val primaryKey = PrimaryKey(id)
}

class PostgresAuditRepository(private val dataSource: DataSource) : AuditRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val database: Database by lazy { Database.connect(dataSource) }

    override fun save(audit: Audit): Either<PersistenceFailure, Unit> =
        Either.catch {
            transaction(database) {
                AuditTable.insert {
                    //TODO: allow for this value to be optional, allowing auto-generation of the PK in the database
                    it[id] = audit.id
                    it[command] = audit.command
                    it[candidate] = audit.candidate
                    it[version] = audit.version
                    it[platform] = audit.platform
                    it[vendor] = audit.vendor.getOrElse { null }
                    it[host] = audit.host
                    it[agent] = audit.agent
                    it[dist] = audit.dist
                    it[timestamp] = audit.timestamp
                }
                Unit
            }
        }
            .mapLeft { exception ->
                //TODO: extract this to an appropriately named extension method on Throwable for reuse and conciseness
                logger.error("Failed to save audit record: {}", exception.message, exception)
                val errorMessage = exception.message.toOption().getOrElse { "" }
                when {
                    errorMessage.contains("connect", ignoreCase = true) ->
                        PersistenceFailure.DatabaseConnectionFailure(exception)

                    else -> PersistenceFailure.QueryExecutionFailure(exception)
                }
            }
}
