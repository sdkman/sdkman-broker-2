package io.sdkman.broker.domain.model

/**
 * Represents the SDKMAN application status and version information.
 * This is the core domain model for health checks.
 */
data class App(
    val alive: String,
    val stableCliVersion: String,
    val betaCliVersion: String,
    val stableNativeCliVersion: String,
    val betaNativeCliVersion: String
) 