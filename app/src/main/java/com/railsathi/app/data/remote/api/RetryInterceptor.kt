package com.railsathi.app.data.remote.api

import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Retries idempotent GET requests that fail with a connection error or a transient 502/503/504,
 * with a short linear back-off. Other methods (POST) and other status codes (including 429, which
 * should not be hammered) are never retried.
 */
class RetryInterceptor(
    private val maxRetries: Int = 2,
    private val backoffMillis: Long = 300L,
    private val sleeper: (Long) -> Unit = { Thread.sleep(it) },
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET") return chain.proceed(request)

        var attempt = 0
        while (true) {
            try {
                val response = chain.proceed(request)
                if (response.code !in RETRYABLE_CODES || attempt >= maxRetries) return response
                response.close()
            } catch (e: IOException) {
                if (chain.call().isCanceled() || attempt >= maxRetries) throw e
            }
            attempt++
            sleeper(backoffMillis * attempt)
        }
    }

    private companion object {
        val RETRYABLE_CODES = setOf(502, 503, 504)
    }
}
