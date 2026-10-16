package com.railsathi.app.data.remote.dto

import com.railsathi.app.domain.model.AvailabilityKind
import com.railsathi.app.domain.model.AvailabilityResult
import com.railsathi.app.domain.model.ClassAvailability
import com.railsathi.app.domain.model.PassengerStatus
import com.railsathi.app.domain.model.PnrStatus
import com.railsathi.app.domain.model.RunningStatus
import com.railsathi.app.domain.model.StationStop
import com.railsathi.app.domain.model.StopState
import com.railsathi.app.domain.model.Train
import com.railsathi.app.domain.model.TrainAvailability
import java.time.LocalDate
import java.time.OffsetDateTime

private fun String?.toOffsetDateTime(): OffsetDateTime? =
    this?.let { runCatching { OffsetDateTime.parse(it.trim()) }.getOrNull() }

private fun String?.toLocalDate(): LocalDate? =
    this?.let { runCatching { LocalDate.parse(it.trim()) }.getOrNull() }

private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/**
 * Returns null when the response carries no usable train identity, which the repository reports as
 * "no data". Missing optional fields stay null; nothing is estimated here.
 */
fun RunningStatusDto.toDomain(
    fetchedAtMillis: Long,
    isFromCache: Boolean,
    isSample: Boolean,
    fallbackDate: LocalDate,
): RunningStatus? {
    val number = trainNumber.cleaned() ?: return null
    return RunningStatus(
        trainNumber = number,
        trainName = trainName.cleaned() ?: "Train $number",
        journeyDate = journeyDate.toLocalDate() ?: fallbackDate,
        statusText = statusText.cleaned(),
        lastReportedStation = lastReportedStation.cleaned(),
        lastReportedAt = lastReportedAt.toOffsetDateTime(),
        delayMinutes = delayMinutes,
        expectedDestinationArrival = expectedDestinationArrival.toOffsetDateTime(),
        stops = stops.orEmpty().mapNotNull { it.toDomain() },
        providerUpdatedAt = lastUpdatedAt.toOffsetDateTime(),
        fetchedAtMillis = fetchedAtMillis,
        isFromCache = isFromCache,
        isSample = isSample,
    )
}

fun StopDto.toDomain(): StationStop? {
    val code = stationCode.cleaned()
    val name = stationName.cleaned()
    if (code == null && name == null) return null
    return StationStop(
        stationCode = code ?: name.orEmpty(),
        stationName = name ?: code.orEmpty(),
        scheduledArrival = scheduledArrival.toOffsetDateTime(),
        estimatedArrival = estimatedArrival.toOffsetDateTime(),
        scheduledDeparture = scheduledDeparture.toOffsetDateTime(),
        estimatedDeparture = estimatedDeparture.toOffsetDateTime(),
        delayMinutes = delayMinutes,
        state = when (state?.trim()?.uppercase()) {
            "PASSED", "DEPARTED" -> StopState.PASSED
            "CURRENT", "AT_STATION" -> StopState.CURRENT
            "UPCOMING" -> StopState.UPCOMING
            else -> StopState.UNKNOWN
        },
    )
}

fun AvailabilityResponseDto.toDomain(fetchedAtMillis: Long, isSample: Boolean): AvailabilityResult =
    AvailabilityResult(
        trains = trains.orEmpty().mapNotNull { it.toDomain() },
        providerUpdatedAt = lastUpdatedAt.toOffsetDateTime(),
        fetchedAtMillis = fetchedAtMillis,
        isSample = isSample,
    )

fun TrainAvailabilityDto.toDomain(): TrainAvailability? {
    val number = trainNumber.cleaned() ?: return null
    return TrainAvailability(
        train = Train(number, trainName.cleaned() ?: "Train $number"),
        fromStationCode = fromStationCode.cleaned(),
        toStationCode = toStationCode.cleaned(),
        departure = departure.toOffsetDateTime(),
        arrival = arrival.toOffsetDateTime(),
        durationMinutes = durationMinutes,
        classes = classes.orEmpty().mapNotNull { it.toDomain() },
    )
}

fun ClassAvailabilityDto.toDomain(): ClassAvailability? {
    val cls = travelClass.cleaned() ?: return null
    val status = statusText.cleaned()
    return ClassAvailability(
        travelClass = cls,
        statusText = status ?: "Not reported",
        kind = if (status == null) AvailabilityKind.OTHER else classifyAvailability(status),
        fareRupees = fareRupees,
    )
}

/** Only drives colour. The text the user reads is always the provider's own wording. */
fun classifyAvailability(statusText: String): AvailabilityKind {
    val s = statusText.trim().uppercase()
    return when {
        s.startsWith("AVAILABLE") || s.startsWith("AVL") -> AvailabilityKind.AVAILABLE
        s.startsWith("RAC") -> AvailabilityKind.RAC
        s.contains("REGRET") || s.contains("NOT AVAILABLE") -> AvailabilityKind.NOT_AVAILABLE
        s.contains("WL") || s.contains("WAITING") -> AvailabilityKind.WAITLIST
        else -> AvailabilityKind.OTHER
    }
}

fun PnrResponseDto.toDomain(fetchedAtMillis: Long, isSample: Boolean): PnrStatus? {
    val passengerList = passengers.orEmpty().map {
        PassengerStatus(
            serial = it.serial,
            bookingStatus = it.bookingStatus.cleaned(),
            currentStatus = it.currentStatus.cleaned(),
            coach = it.coach.cleaned(),
            berth = it.berth.cleaned(),
        )
    }
    if (trainNumber.cleaned() == null && passengerList.isEmpty()) return null
    return PnrStatus(
        trainNumber = trainNumber.cleaned(),
        trainName = trainName.cleaned(),
        journeyDate = journeyDate.toLocalDate(),
        boardingStation = boardingStation.cleaned(),
        destinationStation = destinationStation.cleaned(),
        travelClass = travelClass.cleaned(),
        chartPrepared = chartPrepared,
        passengers = passengerList,
        providerUpdatedAt = lastUpdatedAt.toOffsetDateTime(),
        fetchedAtMillis = fetchedAtMillis,
        isSample = isSample,
    )
}

fun TrainDto.toDomain(): Train? {
    val n = number.cleaned() ?: return null
    return Train(n, name.cleaned() ?: "Train $n")
}
