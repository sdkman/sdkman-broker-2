package io.sdkman.broker.domain.model

import arrow.core.Option

enum class Command(val value: String) {
    INSTALL("install"),
    SELFUPDATE("selfupdate");

    companion object {
        private val commandMap =
            mapOf(
                "install" to INSTALL,
                "selfupdate" to SELFUPDATE
            )

        fun fromValue(value: String): Option<Command> = Option.fromNullable(commandMap[value])
    }
}
