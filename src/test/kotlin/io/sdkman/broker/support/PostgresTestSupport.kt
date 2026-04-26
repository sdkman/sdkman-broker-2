package io.sdkman.broker.support

import arrow.core.None
import arrow.core.Option
import arrow.core.toOption
import io.sdkman.broker.adapter.secondary.persistence.AuditTable
import io.sdkman.broker.adapter.secondary.persistence.VersionsTable
import io.sdkman.broker.domain.model.Version
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

private const val ALGO_MD5 = "MD5"
private const val ALGO_SHA_256 = "SHA-256"
private const val ALGO_SHA_512 = "SHA-512"

object PostgresTestSupport {
    fun setupVersion(
        database: Database,
        version: Version
    ) {
        transaction(database) {
            VersionsTable.insert {
                it[candidate] = version.candidate
                it[VersionsTable.version] = version.version
                it[distribution] = version.distribution.getOrNull()
                it[platform] = version.platform
                it[visible] = version.visible
                it[url] = version.url
                it[md5Sum] = version.checksums[ALGO_MD5]
                it[sha256Sum] = version.checksums[ALGO_SHA_256]
                it[sha512Sum] = version.checksums[ALGO_SHA_512]
            }
        }
    }

    fun clearVersions(database: Database) {
        transaction(database) {
            VersionsTable.deleteAll()
        }
    }

    fun readSavedAuditRecord(
        database: Database,
        id: UUID
    ): Option<ResultRow> =
        transaction(database) {
            AuditTable
                .selectAll()
                .where { AuditTable.id eq id }
                .singleOrNull()
                .toOption()
        }

    fun readSavedAuditRecordByVersion(
        database: Database,
        candidate: String,
        version: String,
        platform: String,
        vendor: Option<String> = None
    ): Option<ResultRow> =
        transaction(database) {
            AuditTable
                .selectAll()
                .where {
                    val baseCondition =
                        (AuditTable.candidate eq candidate) and
                            (AuditTable.version eq version) and
                            (AuditTable.clientPlatform eq platform)

                    vendor.fold(
                        ifEmpty = { baseCondition },
                        ifSome = { vendorValue -> baseCondition and (AuditTable.distribution eq vendorValue) }
                    )
                }.singleOrNull()
                .toOption()
        }
}
