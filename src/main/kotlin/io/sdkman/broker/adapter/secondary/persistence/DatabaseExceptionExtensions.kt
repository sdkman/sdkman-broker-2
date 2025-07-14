package io.sdkman.broker.adapter.secondary.persistence

import arrow.core.getOrElse
import arrow.core.toOption
import io.sdkman.broker.domain.repository.DatabaseFailure

fun Throwable.toDatabaseFailure(): DatabaseFailure {
    val errorMessage = message.toOption().getOrElse { "" }
    return when {
        errorMessage.contains("connect", ignoreCase = true) ->
            DatabaseFailure.ConnectionFailure(this)
        else -> DatabaseFailure.QueryExecutionFailure(this)
    }
}
