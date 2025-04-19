package io.sdkman.broker.domain.error

/**
 * Sealed hierarchy for domain-level errors
 */
sealed class DomainError {
    /**
     * Error when the required App record is not found in the database
     */
    data class AppNotFound(val message: String = "Application record not found") : DomainError()
    
    /**
     * Error when the App record exists but is not in a healthy state
     */
    data class AppNotHealthy(val message: String = "Application is not healthy") : DomainError()
    
    /**
     * Error when there's a technical issue with the repository
     */
    data class RepositoryError(val cause: Throwable) : DomainError()
} 