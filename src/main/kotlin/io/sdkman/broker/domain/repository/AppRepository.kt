package io.sdkman.broker.domain.repository

import arrow.core.Either
import io.sdkman.broker.domain.error.DomainError
import io.sdkman.broker.domain.model.App

/**
 * Repository interface for accessing the application record
 */
interface AppRepository {
    /**
     * Finds the single application record
     *
     * @return Either a domain error or the App entity
     */
    fun findApp(): Either<DomainError, App>
}
