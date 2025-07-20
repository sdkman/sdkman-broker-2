package io.sdkman.broker.support

import arrow.core.None
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

    fun readSavedAuditRecordByVersion(
        database: Database,
        candidate: String,
        version: String,
        platform: String,
        vendor: Option<String> = None
    ): Option<ResultRow> =
        transaction(database) {
            AuditTable.selectAll().where {
                val baseCondition =
                    (AuditTable.candidate eq candidate) and
                        (AuditTable.version eq version) and
                        (AuditTable.platform eq platform)

                vendor.fold(
                    ifEmpty = { baseCondition },
                    ifSome = { vendorValue -> baseCondition and (AuditTable.vendor eq vendorValue) }
                )
            }.singleOrNull().toOption()
        }
}
