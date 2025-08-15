package io.sdkman.broker.domain.service

import arrow.core.Either
import io.sdkman.broker.adapter.primary.rest.AuditContext
import io.sdkman.broker.domain.model.DownloadInfo
import io.sdkman.broker.domain.model.VersionError

interface CandidateDownloadService {
    fun downloadVersion(
        candidate: String,
        version: String,
        platformCode: String,
        auditContext: AuditContext
    ): Either<VersionError, DownloadInfo>
}
