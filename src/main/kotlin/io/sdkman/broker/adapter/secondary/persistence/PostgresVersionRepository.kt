package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import arrow.core.Option
import arrow.core.firstOrNone
import arrow.core.toOption
import io.sdkman.broker.domain.model.JavaDistribution
import io.sdkman.broker.domain.model.Platform
import io.sdkman.broker.domain.model.Version
import io.sdkman.broker.domain.model.VersionError
import io.sdkman.broker.domain.repository.VersionRepository
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.append
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

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

private class DistributionMatches(
    private val value: Option<String>
) : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append(VersionsTable.distribution, " IS NOT DISTINCT FROM ")
        value.fold(
            ifEmpty = { queryBuilder.append("NULL") },
            ifSome = { value ->
                queryBuilder.registerArgument(VersionsTable.distribution.columnType, value)
            }
        )
    }
}

class PostgresVersionRepository(
    private val database: Database
) : VersionRepository {
    override fun findByQuery(
        candidate: String,
        version: String,
        distribution: Option<JavaDistribution>,
        platform: Platform
    ): Either<VersionError, Option<Version>> =
        Either
            .catch {
                transaction(database) {
                    VersionsTable
                        .selectAll()
                        .where {
                            (VersionsTable.candidate eq candidate) and
                                (VersionsTable.version eq version) and
                                (VersionsTable.platform eq platform.auditId) and
                                DistributionMatches(distribution.map { it.name })
                        }.limit(1)
                        .firstOrNone()
                        .map { it.toVersion() }
                }
            }.mapLeft { error -> VersionError.DatabaseError(error) }
}

private fun ResultRow.toVersion(): Version =
    Version(
        candidate = this[VersionsTable.candidate],
        version = this[VersionsTable.version],
        platform = this[VersionsTable.platform],
        distribution = this[VersionsTable.distribution].toOption(),
        url = this[VersionsTable.url],
        visible = this[VersionsTable.visible],
        checksums = this.toChecksums()
    )

private fun ResultRow.toChecksums(): Map<String, String> =
    listOfNotNull(
        this[VersionsTable.md5Sum]?.let { ALGO_MD5 to it },
        this[VersionsTable.sha256Sum]?.let { ALGO_SHA_256 to it },
        this[VersionsTable.sha512Sum]?.let { ALGO_SHA_512 to it }
    ).toMap()

private const val ALGO_MD5 = "MD5"
private const val ALGO_SHA_256 = "SHA-256"
private const val ALGO_SHA_512 = "SHA-512"
