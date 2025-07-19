package io.sdkman.broker.support

import arrow.core.Option
import arrow.core.toOption
import io.sdkman.broker.adapter.secondary.persistence.AuditTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object PostgresTestSupport {
    fun readSavedAuditRecord(
        database: Database,
        id: UUID
    ): Option<ResultRow> =
        transaction(database) {
            AuditTable.selectAll().where { AuditTable.id eq id }.singleOrNull().toOption()
        }

    // TODO: Enhance this to also include optional `vendor`
    fun readSavedAuditRecordByVersion(
        database: Database,
        candidate: String,
        version: String,
        platform: String
    ): Option<ResultRow> =
        transaction(database) {
            AuditTable.selectAll().where {
                (AuditTable.candidate eq candidate) and
                    (AuditTable.version eq version) and
                    (AuditTable.platform eq platform)
            }.singleOrNull().toOption()
        }
}
