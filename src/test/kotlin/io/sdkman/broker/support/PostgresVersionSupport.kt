package io.sdkman.broker.support

import io.sdkman.broker.adapter.secondary.persistence.VersionsTable
import io.sdkman.broker.domain.model.Version
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

object PostgresVersionSupport {
    fun setupVersion(
        database: Database,
        version: Version
    ) {
        transaction(database) {
            VersionsTable.insert {
                it[candidate] = version.candidate
                it[VersionsTable.version] = version.version
                it[platform] = version.platform
                it[distribution] = version.distribution.getOrNull()
                it[url] = version.url
                it[visible] = version.visible
                it[md5Sum] = version.checksums["MD5"]
                it[sha256Sum] = version.checksums["SHA-256"]
                it[sha512Sum] = version.checksums["SHA-512"]
            }
        }
    }

    fun truncateVersions(database: Database) {
        transaction(database) {
            VersionsTable.deleteAll()
        }
    }
}
