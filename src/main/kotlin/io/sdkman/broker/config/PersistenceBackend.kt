package io.sdkman.broker.config

import arrow.core.Option
import arrow.core.getOrElse

/**
 * Selects the persistence backend the broker reads version records from.
 *
 * Driven by the `PERSISTENCE_BACKEND` environment variable (default `mongo`).
 * Invalid values cause [fromValue] to throw [IllegalArgumentException], which
 * fails the application fast at startup per `specs/postgres_version_repository.md`.
 */
sealed class PersistenceBackend(
    val value: String
) {
    data object Mongo : PersistenceBackend("mongo")

    data object Postgres : PersistenceBackend("postgres")

    companion object {
        // Built lazily — eager initialisation triggers a sealed-class
        // circular-init gotcha when the first reference to this type is
        // `PersistenceBackend.Mongo` (or `Postgres`) rather than a companion
        // method. The eager `mapOf(Mongo.value to Mongo, …)` reads
        // `Mongo.INSTANCE` while the parent class's static init is still
        // running, yielding `NullPointerException` from inside the
        // companion's `<clinit>`.
        private val backends: Map<String, PersistenceBackend> by lazy {
            mapOf(
                Mongo.value to Mongo,
                Postgres.value to Postgres
            )
        }

        fun fromValue(value: String): PersistenceBackend =
            Option.fromNullable(backends[value.lowercase()]).getOrElse {
                throw IllegalArgumentException(
                    "Invalid PERSISTENCE_BACKEND value '$value'; expected one of: " +
                        backends.keys.joinToString(", ")
                )
            }
    }
}
