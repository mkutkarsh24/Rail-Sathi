package com.railsathi.app.data.remote.api

/**
 * Decides whether the app talks to a real backend or uses the built-in sample data.
 *
 * A real backend is used only when a valid https:// base URL is configured. Anything else
 * (blank, http://, garbage) falls back to the clearly-labelled sample mode, so the app can never
 * silently send traffic over cleartext.
 */
data class ApiMode(
    val baseUrl: String,
    val isMock: Boolean,
) {
    companion object {
        val MOCK = ApiMode(baseUrl = "", isMock = true)

        fun from(configuredBaseUrl: String): ApiMode {
            val trimmed = configuredBaseUrl.trim()
            if (!trimmed.startsWith("https://", ignoreCase = true) || trimmed.length <= "https://".length) {
                return MOCK
            }
            val normalised = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
            return ApiMode(baseUrl = normalised, isMock = false)
        }
    }
}
