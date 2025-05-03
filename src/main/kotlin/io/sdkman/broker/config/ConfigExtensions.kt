package io.sdkman.broker.config

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.none
import com.typesafe.config.Config

/**
 * Get a string value from config with a default fallback
 */
fun Config.getStringOrDefault(
    path: String,
    default: String
): String =
    Either.catch {
        if (hasPath(path)) getString(path) else default
    }.getOrElse { default }

/**
 * Get an int value from config with a default fallback
 */
fun Config.getIntOrDefault(
    path: String,
    default: Int
): Int =
    Either.catch {
        if (hasPath(path)) getInt(path) else default
    }.getOrElse { default }

/**
 * Get an optional string value from config
 */
fun Config.getOptionString(path: String): Option<String> =
    Either.catch {
        if (!hasPath(path) || getIsNull(path)) {
            none()
        } else {
            Option.fromNullable(getString(path))
        }
    }.getOrElse { none() }
