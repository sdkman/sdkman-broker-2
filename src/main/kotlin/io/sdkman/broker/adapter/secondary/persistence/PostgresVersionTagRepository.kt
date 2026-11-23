package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.Either
import arrow.core.Option
import arrow.core.firstOrNone
import io.sdkman.broker.domain.model.Platform
import io.sdkman.broker.domain.model.VersionTagError
import io.sdkman.broker.domain.repository.VersionTagRepository
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object VersionTagsTable : Table("version_tags") {
    val id = integer("id").autoIncrement()
    val candidate = text("candidate")
    val tag = text("tag")
    val distribution = text("distribution").nullable()
    val platform = text("platform")
    val versionId = integer("version_id")

    override val primaryKey = PrimaryKey(id)
}

class PostgresVersionTagRepository(
    private val database: Database
) : VersionTagRepository {
    override fun findTaggedVersion(
        candidate: String,
        tag: String,
        platform: Platform
    ): Either<VersionTagError, Option<String>> =
        Either
            .catch {
                transaction(database) {
                    VersionTagsTable
                        .join(
                            VersionsTable,
                            JoinType.INNER,
                            additionalConstraint = { VersionTagsTable.versionId eq VersionsTable.id }
                        ).selectAll()
                        .where {
                            (VersionTagsTable.candidate eq candidate) and
                                (VersionTagsTable.tag eq tag) and
                                (VersionTagsTable.platform eq platform.auditId) and
                                VersionsTable.distribution.isNull()
                        }.limit(1)
                        .firstOrNone()
                        .map { it[VersionsTable.version] }
                }
            }.mapLeft { error -> VersionTagError.DatabaseError(error) }
}
