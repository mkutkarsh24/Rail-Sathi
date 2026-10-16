package com.railsathi.app.domain.model

enum class ErrorType {
    INVALID_INPUT,
    NOT_FOUND,
    NO_DATA,
    RATE_LIMITED,
    NETWORK,
    SERVER,
    UNAUTHORIZED,
    PARSE,
    UNKNOWN,
}

data class AppError(
    val type: ErrorType,
    val message: String,
) {
    companion object {
        fun of(type: ErrorType, message: String? = null): AppError =
            AppError(type, message ?: defaultMessage(type))

        fun defaultMessage(type: ErrorType): String = when (type) {
            ErrorType.INVALID_INPUT -> "Please check your input and try again."
            ErrorType.NOT_FOUND -> "No record was found for that request."
            ErrorType.NO_DATA -> "The data provider returned no information for this request."
            ErrorType.RATE_LIMITED -> "Too many requests. Please wait a minute and try again."
            ErrorType.NETWORK -> "Can't reach the server. Check your internet connection."
            ErrorType.SERVER -> "The data service is having problems. Please try again later."
            ErrorType.UNAUTHORIZED ->
                "The app isn't authorised to use the data service. Check the app configuration."
            ErrorType.PARSE -> "The data service sent a response the app couldn't understand."
            ErrorType.UNKNOWN -> "Something went wrong. Please try again."
        }
    }
}

/**
 * Result of a repository call. A [Failure] may still carry [Failure.cached] data (for example the
 * last saved running status) so the UI can show it, clearly marked as saved, alongside the error.
 */
sealed interface DataResult<out T> {
    data class Success<out T>(val data: T) : DataResult<T>
    data class Failure<out T>(val error: AppError, val cached: T? = null) : DataResult<T>
}
