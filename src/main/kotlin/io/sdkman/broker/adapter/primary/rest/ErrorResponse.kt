package io.sdkman.broker.adapter.primary.rest

import kotlinx.serialization.Serializable

/**
 * The single error-body shape returned for every non-2xx Broker response: a stable,
 * machine-readable [error] code (SCREAMING_SNAKE_CASE) and a human-readable [message].
 * 5xx bodies carry a deliberately generic message; 4xx bodies may name the offending client input.
 */
@Serializable
data class ErrorResponse(
    val error: String,
    val message: String
)

const val INTERNAL_ERROR_CODE = "INTERNAL_ERROR"
const val INTERNAL_ERROR_MESSAGE = "An internal error occurred while processing the request"

fun internalErrorResponse(): ErrorResponse = ErrorResponse(INTERNAL_ERROR_CODE, INTERNAL_ERROR_MESSAGE)
