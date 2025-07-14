package io.sdkman.broker.domain.repository

import arrow.core.Either
import io.sdkman.broker.domain.model.Audit

interface AuditRepository {
    fun save(audit: Audit): Either<DatabaseFailure, Unit>
}
