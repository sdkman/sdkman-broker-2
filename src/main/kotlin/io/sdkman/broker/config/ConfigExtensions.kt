package io.sdkman.broker.config

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.none
import com.typesafe.config.Config

/**
 * Get an optional string value from config
 */
fun Config.getOptionString(path: String): Option<String> =
    Either
        .catch {
            if (!hasPath(path) || getIsNull(path)) {
                none()
            } else {
                Option.fromNullable(getString(path))
            }
        }.getOrElse { none() }
