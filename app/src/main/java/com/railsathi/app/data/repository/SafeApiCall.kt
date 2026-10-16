package com.railsathi.app.data.repository

import com.google.gson.JsonParseException
import com.railsathi.app.domain.model.AppError
import com.railsathi.app.domain.model.ErrorType
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.HttpException

/** Runs a network call, capturing failures as a [Result] but always re-throwing cancellation. */
internal suspend fun <T> apiCall(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

/** Maps a failure to a user-presentable [AppError]. Never includes request URLs or bodies. */
internal fun Throwable.toAppError(): AppError = when (this) {
    is HttpException -> when (code()) {
        400, 422 -> AppError.of(ErrorType.INVALID_INPUT)
        401, 403 -> AppError.of(ErrorType.UNAUTHORIZED)
        404 -> AppError.of(ErrorType.NOT_FOUND)
        429 -> AppError.of(ErrorType.RATE_LIMITED)
        in 500..599 -> AppError.of(ErrorType.SERVER)
        else -> AppError.of(ErrorType.UNKNOWN)
    }
    is JsonParseException -> AppError.of(ErrorType.PARSE)
    is IOException -> AppError.of(ErrorType.NETWORK)
    else -> AppError.of(ErrorType.UNKNOWN)
}
