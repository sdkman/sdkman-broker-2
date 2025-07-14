package io.sdkman.broker.support

import arrow.core.Option
import arrow.core.toOption
import io.sdkman.broker.adapter.secondary.persistence.AuditTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
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
}
