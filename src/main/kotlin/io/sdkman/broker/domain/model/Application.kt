package io.sdkman.broker.domain.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right

data class Application private constructor(
    val alive: AliveStatus
) {
    companion object {
        fun of(alive: String): Either<ApplicationError, Application> =
            AliveStatus.of(alive).map { status ->
                Application(status)
            }
    }
}

@JvmInline
value class AliveStatus private constructor(val value: String) {
    companion object {
        private const val OK_STATUS = "OK"
        
        fun of(value: String): Either<ApplicationError, AliveStatus> =
            if (value == OK_STATUS) {
                AliveStatus(value).right()
            } else {
                ApplicationError.InvalidAliveStatus(value).left()
            }
    }
}

sealed class ApplicationError {
    data class InvalidAliveStatus(val status: String) : ApplicationError()
    data class ApplicationNotFound(val message: String = "Application record not found") : ApplicationError()
    data class SystemError(val cause: Throwable) : ApplicationError()
} 
