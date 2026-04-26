package io.sdkman.broker.config

import arrow.core.Option
import arrow.core.toOption

enum class PersistenceBackend(
    val configValue: String
) {
    Mongo("mongo"),
    Postgres("postgres");

    companion object {
        private val configValueToBackend: Map<String, PersistenceBackend> =
            entries.associateBy { it.configValue }

        fun fromConfigValue(value: String): Option<PersistenceBackend> = configValueToBackend[value].toOption()

        val supportedValues: List<String> = entries.map { it.configValue }
    }
}
