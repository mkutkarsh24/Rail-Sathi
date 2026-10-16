package com.railsathi.app.data

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.railsathi.app.data.remote.dto.AvailabilityResponseDto
import com.railsathi.app.data.remote.dto.PnrResponseDto
import com.railsathi.app.data.remote.dto.RunningStatusDto
import com.railsathi.app.data.remote.dto.classifyAvailability
import com.railsathi.app.data.remote.dto.toDomain
import com.railsathi.app.domain.model.AvailabilityKind
import com.railsathi.app.domain.model.StopState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoParsingTest {

    private val gson = Gson()
    private val fallbackDate = LocalDate.of(2026, 10, 16)

    private fun resource(name: String): String =
        requireNotNull(javaClass.classLoader?.getResourceAsStream("mock/$name")) { "missing $name" }
            .bufferedReader().use { it.readText() }

    @Test
    fun runningStatus_fullResponse_mapsEveryField() {
        val dto = gson.fromJson(resource("running_status.json"), RunningStatusDto::class.java)
        val status = dto.toDomain(fetchedAtMillis = 5L, isFromCache = false, isSample = false, fallbackDate = fallbackDate)

        assertNotNull(status)
        status!!
        assertEquals("12951", status.trainNumber)
        assertEquals("Sample Rajdhani", status.trainName)
        assertEquals(12, status.delayMinutes)
        assertEquals("Agra Cantt", status.lastReportedStation)
        assertNotNull(status.expectedDestinationArrival)
        assertNotNull(status.providerUpdatedAt)
        assertEquals(3, status.stops.size)
        assertEquals(1, status.previousStops.size)
        assertEquals("AGC", status.currentStop?.stationCode)
        assertEquals(1, status.upcomingStops.size)
        assertEquals("Bhopal Jn", status.destinationName)
        assertFalse(status.isFromCache)
        assertFalse(status.isSample)
    }

    @Test
    fun runningStatus_partialResponse_leavesMissingFieldsNullInsteadOfInventingThem() {
        val dto = gson.fromJson(resource("running_status_partial.json"), RunningStatusDto::class.java)
        val status = dto.toDomain(fetchedAtMillis = 5L, isFromCache = false, isSample = false, fallbackDate = fallbackDate)!!

        assertEquals("Train 12951", status.trainName) // placeholder name only, no invented data
        assertNull(status.statusText) // blank text is treated as missing
        assertNull(status.delayMinutes)
        assertNull(status.providerUpdatedAt) // unparseable timestamp is dropped, not guessed
        assertNull(status.expectedDestinationArrival)
        assertEquals(fallbackDate, status.journeyDate)
        // The stop with neither code nor name is dropped; the unknown state is kept as UNKNOWN.
        assertEquals(1, status.stops.size)
        assertEquals(StopState.UNKNOWN, status.stops[0].state)
        assertEquals("AGC", status.stops[0].stationName)
    }

    @Test
    fun runningStatus_emptyObject_isNoData() {
        val dto = gson.fromJson(resource("empty_object.json"), RunningStatusDto::class.java)
        assertNull(dto.toDomain(0L, isFromCache = false, isSample = false, fallbackDate = fallbackDate))
    }

    @Test
    fun runningStatus_staleDetection_usesProviderTimestamp() {
        val dto = gson.fromJson(resource("running_status.json"), RunningStatusDto::class.java)
        val status = dto.toDomain(0L, false, false, fallbackDate)!!
        val providerMillis = status.providerUpdatedAt!!.toInstant().toEpochMilli()

        assertFalse(status.isStale(providerMillis + 10 * 60_000L))
        assertTrue(status.isStale(providerMillis + 31 * 60_000L))
    }

    @Test
    fun availability_mapsClassesAndKeepsProviderWording() {
        val dto = gson.fromJson(resource("availability.json"), AvailabilityResponseDto::class.java)
        val result = dto.toDomain(fetchedAtMillis = 1L, isSample = true)

        assertTrue(result.isSample)
        assertEquals(1, result.trains.size) // the entry without a train number is dropped
        val classes = result.trains[0].classes
        assertEquals(listOf("SL", "3A", "2A", "CC", "EC"), classes.map { it.travelClass })
        assertEquals("AVAILABLE-0042", classes[0].statusText)
        assertEquals(AvailabilityKind.AVAILABLE, classes[0].kind)
        assertEquals(AvailabilityKind.WAITLIST, classes[1].kind)
        assertEquals(AvailabilityKind.RAC, classes[2].kind)
        assertEquals(AvailabilityKind.NOT_AVAILABLE, classes[3].kind)
        assertEquals("Not reported", classes[4].statusText)
        assertEquals(AvailabilityKind.OTHER, classes[4].kind)
        assertNull(classes[2].fareRupees)
        assertEquals(1980, classes[1].fareRupees)
    }

    @Test
    fun availability_emptyTrainList_isAnEmptyResultNotAnError() {
        val dto = gson.fromJson(resource("availability_empty.json"), AvailabilityResponseDto::class.java)
        assertTrue(dto.toDomain(1L, false).trains.isEmpty())
    }

    @Test
    fun classifyAvailability_coversCommonProviderWording() {
        assertEquals(AvailabilityKind.AVAILABLE, classifyAvailability("AVAILABLE-0012"))
        assertEquals(AvailabilityKind.AVAILABLE, classifyAvailability("available 5"))
        assertEquals(AvailabilityKind.WAITLIST, classifyAvailability("GNWL 5/WL 3"))
        assertEquals(AvailabilityKind.WAITLIST, classifyAvailability("RLWL 2"))
        assertEquals(AvailabilityKind.RAC, classifyAvailability("RAC 2"))
        assertEquals(AvailabilityKind.NOT_AVAILABLE, classifyAvailability("REGRET"))
        assertEquals(AvailabilityKind.OTHER, classifyAvailability("Train departed"))
    }

    @Test
    fun pnr_fullResponse_mapsPassengers() {
        val dto = gson.fromJson(resource("pnr.json"), PnrResponseDto::class.java)
        val status = dto.toDomain(fetchedAtMillis = 1L, isSample = false)!!

        assertEquals("12951", status.trainNumber)
        assertEquals(LocalDate.of(2026, 10, 20), status.journeyDate)
        assertEquals(false, status.chartPrepared)
        assertEquals(2, status.passengers.size)
        assertEquals("B2", status.passengers[0].coach)
        assertNull(status.passengers[1].coach)
        assertEquals("RAC 11", status.passengers[1].currentStatus)
    }

    @Test
    fun pnr_emptyObject_isNoData() {
        val dto = gson.fromJson(resource("empty_object.json"), PnrResponseDto::class.java)
        assertNull(dto.toDomain(1L, false))
    }

    @Test(expected = JsonSyntaxException::class)
    fun malformedJson_throwsParseException() {
        gson.fromJson("{ this is not json", RunningStatusDto::class.java)
    }
}
