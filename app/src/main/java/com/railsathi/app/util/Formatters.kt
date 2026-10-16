package com.railsathi.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    private val ist: ZoneId = ZoneId.of("Asia/Kolkata")
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
    private val dateTimeFormat = DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.ENGLISH)
    private val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

    const val MISSING = "—"

    /** Train times are shown in IST. */
    fun time(value: OffsetDateTime?): String =
        value?.atZoneSameInstant(ist)?.format(timeFormat) ?: MISSING

    fun dateTime(value: OffsetDateTime?): String =
        value?.atZoneSameInstant(ist)?.format(dateTimeFormat) ?: MISSING

    fun date(value: LocalDate?): String = value?.format(dateFormat) ?: MISSING

    /** "Last updated" stamps are shown in the device's own time zone. */
    fun deviceDateTime(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateTimeFormat)

    fun delay(minutes: Int?): String = when {
        minutes == null -> "Delay not reported"
        minutes == 0 -> "On time"
        minutes > 0 -> "$minutes min late"
        else -> "${-minutes} min early"
    }

    fun duration(minutes: Int?): String {
        if (minutes == null || minutes < 0) return MISSING
        val hours = minutes / 60
        val rest = minutes % 60
        return if (hours == 0) "${rest}m" else "${hours}h ${rest}m"
    }

    fun rupees(amount: Int?): String? = amount?.let { "₹$it" }
}
