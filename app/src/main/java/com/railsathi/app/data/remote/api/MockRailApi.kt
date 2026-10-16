package com.railsathi.app.data.remote.api

import com.railsathi.app.data.remote.dto.AvailabilityResponseDto
import com.railsathi.app.data.remote.dto.ClassAvailabilityDto
import com.railsathi.app.data.remote.dto.PassengerDto
import com.railsathi.app.data.remote.dto.PnrRequestDto
import com.railsathi.app.data.remote.dto.PnrResponseDto
import com.railsathi.app.data.remote.dto.RunningStatusDto
import com.railsathi.app.data.remote.dto.StopDto
import com.railsathi.app.data.remote.dto.TrainAvailabilityDto
import com.railsathi.app.data.remote.dto.TrainDto
import com.railsathi.app.data.remote.dto.TrainSearchResponseDto
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/**
 * SAMPLE DATA ONLY. Used when RAIL_API_BASE_URL is not configured.
 *
 * Nothing here reflects real train movements, seats or bookings: the numbers are generated from
 * the requested date and the current clock so the UI can be developed and tested without
 * credentials. The repositories tag everything produced by this class as "sample", and the UI
 * labels it as such.
 *
 * Special inputs that exercise error states:
 *  - train numbers 00000 and 99999 -> not found / rate limited
 *  - PNRs starting with 0          -> not found; PNR 9999999999 -> rate limited
 *  - availability class "1A"       -> empty result ("no trains")
 */
class MockRailApi(
    private val latencyMillis: Long = 400L,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : RailApi {

    private val isoFormat = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val ist = ZoneOffset.ofHoursMinutes(5, 30)

    private val sampleTrains = listOf(
        TrainDto("12951", "Mumbai Central - New Delhi Rajdhani"),
        TrainDto("12301", "Howrah - New Delhi Rajdhani"),
        TrainDto("12002", "New Delhi - Bhopal Shatabdi"),
        TrainDto("12627", "Karnataka Express"),
        TrainDto("12723", "Telangana Express"),
    )

    private val route = listOf(
        "NDLS" to "New Delhi",
        "MTJ" to "Mathura Jn",
        "AGC" to "Agra Cantt",
        "GWL" to "Gwalior",
        "JHS" to "Jhansi Jn",
        "BPL" to "Bhopal Jn",
    )

    override suspend fun getRunningStatus(trainNumber: String, date: String): RunningStatusDto {
        delay(latencyMillis)
        if (trainNumber == "00000") throw httpError(404)
        if (trainNumber == "99999") throw httpError(429)
        val train = sampleTrains.firstOrNull { it.number == trainNumber } ?: throw httpError(404)

        val journeyDate = runCatching { LocalDate.parse(date) }.getOrElse { LocalDate.now(ist) }
        val now = Instant.ofEpochMilli(clock()).atOffset(ist)
        val start = OffsetDateTime.of(journeyDate, LocalTime.of(6, 0), ist)
        val delayMinutes = 12

        val stops = route.mapIndexed { index, (code, name) ->
            val scheduled = start.plusMinutes(index * 105L)
            val state = when {
                index < 2 -> "PASSED"
                index == 2 -> "CURRENT"
                else -> "UPCOMING"
            }
            StopDto(
                stationCode = code,
                stationName = name,
                scheduledArrival = if (index == 0) null else scheduled.format(isoFormat),
                estimatedArrival = if (index == 0) null else scheduled.plusMinutes(delayMinutes.toLong()).format(isoFormat),
                scheduledDeparture = if (index == route.lastIndex) null else scheduled.plusMinutes(5).format(isoFormat),
                estimatedDeparture = if (index == route.lastIndex) null else scheduled.plusMinutes(5L + delayMinutes).format(isoFormat),
                delayMinutes = delayMinutes,
                state = state,
            )
        }
        return RunningStatusDto(
            trainNumber = train.number,
            trainName = train.name,
            journeyDate = journeyDate.toString(),
            statusText = "Running (sample data)",
            lastReportedStation = "Agra Cantt",
            lastReportedAt = now.minusMinutes(5).format(isoFormat),
            delayMinutes = delayMinutes,
            expectedDestinationArrival = start.plusMinutes((route.lastIndex * 105L) + delayMinutes).format(isoFormat),
            stops = stops,
            lastUpdatedAt = now.format(isoFormat),
        )
    }

    override suspend fun getAvailability(
        fromStation: String,
        toStation: String,
        date: String,
        travelClass: String?,
        quota: String?,
    ): AvailabilityResponseDto {
        delay(latencyMillis)
        val now = Instant.ofEpochMilli(clock()).atOffset(ist)
        if (travelClass == "1A") {
            return AvailabilityResponseDto(trains = emptyList(), lastUpdatedAt = now.format(isoFormat))
        }
        val journeyDate = runCatching { LocalDate.parse(date) }.getOrElse { LocalDate.now(ist) }
        val departureBase = OffsetDateTime.of(journeyDate, LocalTime.of(16, 35), ist)

        val allClasses = listOf(
            ClassAvailabilityDto("SL", "AVAILABLE-0042", 740),
            ClassAvailabilityDto("3A", "GNWL 14/WL 9", 1980),
            ClassAvailabilityDto("2A", "RAC 6", 2830),
            ClassAvailabilityDto("CC", "REGRET", 1450),
        )
        val classes = if (travelClass == null) allClasses else allClasses.filter { it.travelClass == travelClass }

        val trains = sampleTrains.take(3).mapIndexed { index, train ->
            val departure = departureBase.plusMinutes(index * 95L)
            val duration = 600 + index * 35
            TrainAvailabilityDto(
                trainNumber = train.number,
                trainName = train.name,
                fromStationCode = fromStation,
                toStationCode = toStation,
                departure = departure.format(isoFormat),
                arrival = departure.plusMinutes(duration.toLong()).format(isoFormat),
                durationMinutes = duration,
                classes = classes,
            )
        }
        return AvailabilityResponseDto(trains = trains, lastUpdatedAt = now.format(isoFormat))
    }

    override suspend fun getPnrStatus(request: PnrRequestDto): PnrResponseDto {
        delay(latencyMillis)
        if (request.pnr.startsWith("0")) throw httpError(404)
        if (request.pnr == "9999999999") throw httpError(429)
        val now = Instant.ofEpochMilli(clock()).atOffset(ist)
        val train = sampleTrains.first()
        return PnrResponseDto(
            trainNumber = train.number,
            trainName = train.name,
            journeyDate = now.toLocalDate().plusDays(3).toString(),
            boardingStation = "New Delhi (NDLS)",
            destinationStation = "Bhopal Jn (BPL)",
            travelClass = "3A",
            chartPrepared = false,
            passengers = listOf(
                PassengerDto(1, "CNF/B2/21", "CNF", "B2", "21 Lower"),
                PassengerDto(2, "WL 3", "RAC 11", null, null),
            ),
            lastUpdatedAt = now.format(isoFormat),
        )
    }

    override suspend fun searchTrains(query: String): TrainSearchResponseDto {
        delay(latencyMillis / 2)
        val q = query.trim().lowercase()
        val matches = sampleTrains.filter {
            it.number.orEmpty().contains(q) || it.name.orEmpty().lowercase().contains(q)
        }
        return TrainSearchResponseDto(trains = matches)
    }

    private fun httpError(code: Int): HttpException =
        HttpException(Response.error<Any>(code, "".toResponseBody(null)))
}
