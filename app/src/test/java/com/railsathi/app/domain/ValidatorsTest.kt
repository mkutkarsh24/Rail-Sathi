package com.railsathi.app.domain

import com.railsathi.app.domain.usecase.Validators
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    private val today = LocalDate.of(2026, 10, 16)

    @Test
    fun trainNumber_acceptsExactlyFiveDigits() {
        assertTrue(Validators.isValidTrainNumber("12951"))
        assertTrue(Validators.isValidTrainNumber(" 12951 "))
        assertFalse(Validators.isValidTrainNumber("1295"))
        assertFalse(Validators.isValidTrainNumber("129511"))
        assertFalse(Validators.isValidTrainNumber("1295a"))
        assertFalse(Validators.isValidTrainNumber(""))
    }

    @Test
    fun trainNumberError_messages() {
        assertEquals("Enter a train number.", Validators.trainNumberError(""))
        assertEquals("Enter a valid 5-digit train number.", Validators.trainNumberError("12"))
        assertNull(Validators.trainNumberError("12951"))
    }

    @Test
    fun pnr_requiresTenDigits() {
        assertTrue(Validators.isValidPnr("1234567890"))
        assertFalse(Validators.isValidPnr("123456789"))
        assertFalse(Validators.isValidPnr("12345678901"))
        assertFalse(Validators.isValidPnr("12345abcde"))
        assertEquals("Enter a valid 10-digit PNR number.", Validators.pnrError("123"))
        assertNull(Validators.pnrError("1234567890"))
    }

    @Test
    fun stationCode_isTwoToFiveLetters() {
        assertTrue(Validators.isValidStationCode("NDLS"))
        assertTrue(Validators.isValidStationCode("bpl"))
        assertFalse(Validators.isValidStationCode("N"))
        assertFalse(Validators.isValidStationCode("NDLS12"))
        assertFalse(Validators.isValidStationCode("ND1"))
        assertNotNull(Validators.stationError("", "source"))
        assertNotNull(Validators.stationError("1", "source"))
        assertNull(Validators.stationError("NDLS", "source"))
    }

    @Test
    fun date_mustBeIsoAndWithinWindow() {
        assertNull(Validators.dateError("2026-10-16", today))
        assertNull(Validators.dateError("2026-10-17", today))
        assertEquals("Enter the date as YYYY-MM-DD.", Validators.dateError("16/10/2026", today))
        assertEquals("The journey date can't be in the past.", Validators.dateError("2026-10-15", today))
        assertEquals("That date is too far ahead.", Validators.dateError("2027-12-31", today))
        assertNull(Validators.dateError(today.plusDays(Validators.MAX_DAYS_AHEAD).toString(), today))
        assertNotNull(Validators.dateError(today.plusDays(Validators.MAX_DAYS_AHEAD + 1).toString(), today))
    }

    @Test
    fun date_maxPastDaysAllowsRecentDates_forLiveStatus() {
        assertNull(Validators.dateError("2026-10-15", today, maxPastDays = 1))
        assertEquals(
            "That date is too far in the past.",
            Validators.dateError("2026-10-14", today, maxPastDays = 1),
        )
    }

    @Test
    fun travelClass_nullMeansAnyAndUnknownIsRejected() {
        assertNull(Validators.classError(null))
        assertNull(Validators.classError("3A"))
        assertNotNull(Validators.classError("XX"))
    }

    @Test
    fun sameStations_areRejectedIgnoringCase() {
        assertNotNull(Validators.differentStationsError("ndls", "NDLS"))
        assertNull(Validators.differentStationsError("NDLS", "BPL"))
    }
}
