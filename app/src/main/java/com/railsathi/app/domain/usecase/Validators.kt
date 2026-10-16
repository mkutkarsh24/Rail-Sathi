package com.railsathi.app.domain.usecase

import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Pure input validation. Each `...Error` function returns a user-facing message or null if valid. */
object Validators {
    const val MAX_DAYS_AHEAD = 120L

    val TRAVEL_CLASSES = listOf("SL", "3A", "2A", "1A", "CC", "EC", "3E", "2S")
    val QUOTAS = listOf("GN", "TQ", "PT", "LD", "SS")

    private val trainNumberRegex = Regex("^\\d{5}$")
    private val pnrRegex = Regex("^\\d{10}$")
    private val stationCodeRegex = Regex("^[A-Za-z]{2,5}$")

    fun isValidTrainNumber(value: String): Boolean = trainNumberRegex.matches(value.trim())

    fun isValidPnr(value: String): Boolean = pnrRegex.matches(value.trim())

    fun isValidStationCode(value: String): Boolean = stationCodeRegex.matches(value.trim())

    fun parseDate(value: String): LocalDate? = try {
        LocalDate.parse(value.trim())
    } catch (e: DateTimeParseException) {
        null
    }

    fun trainNumberError(value: String): String? = when {
        value.isBlank() -> "Enter a train number."
        !isValidTrainNumber(value) -> "Enter a valid 5-digit train number."
        else -> null
    }

    fun pnrError(value: String): String? = when {
        value.isBlank() -> "Enter your 10-digit PNR number."
        !isValidPnr(value) -> "Enter a valid 10-digit PNR number."
        else -> null
    }

    fun stationError(value: String, label: String): String? = when {
        value.isBlank() -> "Enter the $label station code."
        !isValidStationCode(value) -> "Use the 2–5 letter station code, e.g. NDLS."
        else -> null
    }

    /**
     * @param maxPastDays how many days before [today] are still acceptable. Bookings need 0, but a
     * train that left yesterday can still be running today, so live status allows 1.
     */
    fun dateError(value: String, today: LocalDate, maxPastDays: Long = 0): String? {
        val date = parseDate(value) ?: return "Enter the date as YYYY-MM-DD."
        return when {
            date.isBefore(today.minusDays(maxPastDays)) ->
                if (maxPastDays == 0L) "The journey date can't be in the past." else "That date is too far in the past."
            date.isAfter(today.plusDays(MAX_DAYS_AHEAD)) -> "That date is too far ahead."
            else -> null
        }
    }

    fun classError(selected: String?): String? =
        if (selected != null && selected !in TRAVEL_CLASSES) "Unsupported travel class." else null

    fun differentStationsError(from: String, to: String): String? =
        if (from.trim().equals(to.trim(), ignoreCase = true)) {
            "Destination must differ from the source."
        } else {
            null
        }
}
