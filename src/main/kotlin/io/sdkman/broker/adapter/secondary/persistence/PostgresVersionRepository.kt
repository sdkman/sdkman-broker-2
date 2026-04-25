package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import arrow.core.Option
import arrow.core.toOption
import io.sdkman.broker.domain.model.Platform
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.domain.model.VersionError
import io.sdkman.broker.domain.repository.VersionRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import javax.sql.DataSource

object VersionsTable : Table("versions") {
    val candidate = text("candidate")
    val version = text("version")
    val distribution = text("distribution").nullable()
    val platform = text("platform")
    val visible = bool("visible")
    val url = text("url")
    val md5Sum = text("md5_sum").nullable()
    val sha256Sum = text("sha_256_sum").nullable()
    val sha512Sum = text("sha_512_sum").nullable()
}

class PostgresVersionRepository(
    private val dataSource: DataSource
) : VersionRepository {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val database: Database by lazy { Database.connect(dataSource) }

    override fun findByQuery(
        candidate: String,
        version: String,
        distribution: Option<String>,
        platform: Platform
    ): Either<VersionError, Option<Version>> =
        Either
            .catch {
                transaction(database) {
                    runQuery(candidate, version, distribution, platform)
                }
            }.mapLeft { exception ->
                logger.error("Failed to read version: {}", exception.message, exception)
                VersionError.DatabaseError(exception)
            }

    private fun runQuery(
        candidate: String,
        version: String,
        distribution: Option<String>,
        platform: Platform
    ): Option<Version> =
        VersionsTable
            .selectAll()
            .where { matchOn(candidate, version, distribution, platform) }
            .limit(1)
            .singleOrNull()
            .toOption()
            .map { it.toVersion() }

    private fun SqlExpressionBuilder.matchOn(
        candidate: String,
        version: String,
        distribution: Option<String>,
        platform: Platform
    ): Op<Boolean> {
        val base =
            (VersionsTable.candidate eq candidate) and
                (VersionsTable.version eq version) and
                (VersionsTable.platform eq platform.auditId)
        return distribution.fold(
            ifEmpty = { base and VersionsTable.distribution.isNull() },
            ifSome = { value -> base and (VersionsTable.distribution eq value) }
        )
    }
}

private fun ResultRow.toVersion(): Version =
    Version(
        candidate = this[VersionsTable.candidate],
        version = this[VersionsTable.version],
        platform = this[VersionsTable.platform],
        distribution = this[VersionsTable.distribution].toOption(),
        url = this[VersionsTable.url],
        visible = this[VersionsTable.visible],
        checksums = extractChecksums()
    )

private fun ResultRow.extractChecksums(): Map<String, String> =
    listOf(
        "MD5" to this[VersionsTable.md5Sum].toOption(),
        "SHA-256" to this[VersionsTable.sha256Sum].toOption(),
        "SHA-512" to this[VersionsTable.sha512Sum].toOption()
    ).flatMap { (algorithm, optionalValue) ->
        optionalValue.fold(
            ifEmpty = { emptyList() },
            ifSome = { listOf(algorithm to it) }
        )
    }.toMap()
