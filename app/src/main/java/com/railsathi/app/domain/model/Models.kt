package com.railsathi.app.domain.model

import java.time.LocalDate
import java.time.OffsetDateTime

data class Train(
    val number: String,
    val name: String,
)

data class SavedTrain(
    val train: Train,
    val savedAtMillis: Long,
)

data class SearchHistoryItem(
    val train: Train,
    val searchedAtMillis: Long,
)

// ---------------------------------------------------------------------------------------------
// Live running status
// ---------------------------------------------------------------------------------------------

enum class StopState { PASSED, CURRENT, UPCOMING, UNKNOWN }

data class StationStop(
    val stationCode: String,
    val stationName: String,
    val scheduledArrival: OffsetDateTime?,
    val estimatedArrival: OffsetDateTime?,
    val scheduledDeparture: OffsetDateTime?,
    val estimatedDeparture: OffsetDateTime?,
    val delayMinutes: Int?,
    val state: StopState,
)

/**
 * Running status exactly as reported by the data provider. Fields the provider did not send are
 * null; nothing is estimated or inferred on the device.
 */
data class RunningStatus(
    val trainNumber: String,
    val trainName: String,
    val journeyDate: LocalDate,
    val statusText: String?,
    val lastReportedStation: String?,
    val lastReportedAt: OffsetDateTime?,
    val delayMinutes: Int?,
    val expectedDestinationArrival: OffsetDateTime?,
    val stops: List<StationStop>,
    /** Timestamp supplied by the provider for this data, if any. */
    val providerUpdatedAt: OffsetDateTime?,
    /** When this device last successfully received it from the provider. */
    val fetchedAtMillis: Long,
    val isFromCache: Boolean,
    /** True when the data came from the built-in sample (mock) data source. */
    val isSample: Boolean,
) {
    val previousStops: List<StationStop> get() = stops.filter { it.state == StopState.PASSED }
    val currentStop: StationStop? get() = stops.firstOrNull { it.state == StopState.CURRENT }
    val upcomingStops: List<StationStop> get() = stops.filter { it.state == StopState.UPCOMING }
    val destinationName: String? get() = stops.lastOrNull()?.stationName

    /** True when the provider's own timestamp is older than [thresholdMinutes]. */
    fun isStale(nowMillis: Long, thresholdMinutes: Long = STALE_AFTER_MINUTES): Boolean {
        val updated = providerUpdatedAt ?: return false
        val ageMillis = nowMillis - updated.toInstant().toEpochMilli()
        return ageMillis > thresholdMinutes * 60_000L
    }

    companion object {
        const val STALE_AFTER_MINUTES = 30L
    }
}

// ---------------------------------------------------------------------------------------------
// Seat availability
// ---------------------------------------------------------------------------------------------

data class AvailabilityQuery(
    val fromStation: String,
    val toStation: String,
    val date: LocalDate,
    val travelClass: String?,
    val quota: String?,
)

/** Only used to pick a colour / icon. The text shown to the user is always the provider's own. */
enum class AvailabilityKind { AVAILABLE, WAITLIST, RAC, NOT_AVAILABLE, OTHER }

data class ClassAvailability(
    val travelClass: String,
    val statusText: String,
    val kind: AvailabilityKind,
    val fareRupees: Int?,
)

data class TrainAvailability(
    val train: Train,
    val fromStationCode: String?,
    val toStationCode: String?,
    val departure: OffsetDateTime?,
    val arrival: OffsetDateTime?,
    val durationMinutes: Int?,
    val classes: List<ClassAvailability>,
)

data class AvailabilityResult(
    val trains: List<TrainAvailability>,
    val providerUpdatedAt: OffsetDateTime?,
    val fetchedAtMillis: Long,
    val isSample: Boolean,
)

// ---------------------------------------------------------------------------------------------
// PNR
// ---------------------------------------------------------------------------------------------

data class PassengerStatus(
    val serial: Int?,
    val bookingStatus: String?,
    val currentStatus: String?,
    val coach: String?,
    val berth: String?,
)

/** Deliberately contains no PNR number and no passenger names; it is never written to disk. */
data class PnrStatus(
    val trainNumber: String?,
    val trainName: String?,
    val journeyDate: LocalDate?,
    val boardingStation: String?,
    val destinationStation: String?,
    val travelClass: String?,
    val chartPrepared: Boolean?,
    val passengers: List<PassengerStatus>,
    val providerUpdatedAt: OffsetDateTime?,
    val fetchedAtMillis: Long,
    val isSample: Boolean,
)
