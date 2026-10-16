package com.railsathi.app.data

import com.railsathi.app.data.remote.api.ApiMode
import com.railsathi.app.data.remote.dto.AvailabilityResponseDto
import com.railsathi.app.data.remote.dto.PnrResponseDto
import com.railsathi.app.data.remote.dto.TrainDto
import com.railsathi.app.data.remote.dto.TrainSearchResponseDto
import com.railsathi.app.data.repository.AvailabilityRepositoryImpl
import com.railsathi.app.data.repository.PnrRepositoryImpl
import com.railsathi.app.data.repository.TrainSearchRepositoryImpl
import com.railsathi.app.domain.model.AvailabilityQuery
import com.railsathi.app.domain.model.DataResult
import com.railsathi.app.domain.model.ErrorType
import com.railsathi.app.testutil.FakeRailApi
import com.railsathi.app.testutil.FixedTimeProvider
import com.railsathi.app.testutil.httpException
import com.railsathi.app.testutil.ioException
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AvailabilityAndPnrRepositoryTest {

    private lateinit var api: FakeRailApi
    private val time = FixedTimeProvider()
    private val real = ApiMode.from("https://api.example.com")

    private fun query(from: String = "NDLS", to: String = "BPL", cls: String? = null) =
        AvailabilityQuery(from, to, LocalDate.of(2026, 10, 17), cls, "GN")

    @Before
    fun setUp() {
        api = FakeRailApi()
    }

    // ---- Availability ----

    @Test
    fun availability_success_mapsTrains() = runTest {
        val repo = AvailabilityRepositoryImpl(api, time, real)

        val result = repo.search(query()) as DataResult.Success

        assertEquals(1, result.data.trains.size)
        assertEquals("AVAILABLE-0042", result.data.trains[0].classes[0].statusText)
        assertEquals(false, result.data.isSample)
    }

    @Test
    fun availability_invalidInputs_failBeforeAnyNetworkCall() = runTest {
        val repo = AvailabilityRepositoryImpl(api, time, real)
        api.availabilityResult = Result.failure(IllegalStateException("must not be called"))

        listOf(
            query(from = ""),
            query(to = "B"),
            query(from = "NDLS", to = "ndls"),
            query(cls = "ZZ"),
        ).forEach { q ->
            val result = repo.search(q)
            assertTrue("expected failure for $q", result is DataResult.Failure)
            assertEquals(ErrorType.INVALID_INPUT, (result as DataResult.Failure).error.type)
        }
    }

    @Test
    fun availability_emptyTrainList_isSuccessWithNoTrains() = runTest {
        api.availabilityResult = Result.success(AvailabilityResponseDto(trains = emptyList()))
        val result = AvailabilityRepositoryImpl(api, time, real).search(query()) as DataResult.Success
        assertTrue(result.data.trains.isEmpty())
    }

    @Test
    fun availability_completelyEmptyBody_isAlsoAnEmptyResult() = runTest {
        api.availabilityResult = Result.success(AvailabilityResponseDto())
        val result = AvailabilityRepositoryImpl(api, time, real).search(query()) as DataResult.Success
        assertTrue(result.data.trains.isEmpty())
    }

    @Test
    fun availability_failures_areMapped() = runTest {
        val repo = AvailabilityRepositoryImpl(api, time, real)

        api.availabilityResult = Result.failure(ioException())
        assertEquals(ErrorType.NETWORK, (repo.search(query()) as DataResult.Failure).error.type)

        api.availabilityResult = Result.failure(httpException(429))
        assertEquals(ErrorType.RATE_LIMITED, (repo.search(query()) as DataResult.Failure).error.type)

        api.availabilityResult = Result.failure(httpException(500))
        assertEquals(ErrorType.SERVER, (repo.search(query()) as DataResult.Failure).error.type)
    }

    @Test
    fun availability_mockMode_isTaggedAsSample() = runTest {
        val result = AvailabilityRepositoryImpl(api, time, ApiMode.MOCK).search(query()) as DataResult.Success
        assertTrue(result.data.isSample)
    }

    // ---- PNR ----

    @Test
    fun pnr_invalidNumbers_failWithoutCallingTheApi() = runTest {
        val repo = PnrRepositoryImpl(api, time, real)

        listOf("", "123", "12345678901", "12345abcde").forEach { pnr ->
            val result = repo.getStatus(pnr) as DataResult.Failure
            assertEquals(ErrorType.INVALID_INPUT, result.error.type)
        }
        assertNull(api.lastPnrRequest)
    }

    @Test
    fun pnr_success_mapsPassengers_andSendsPnrInRequestBody() = runTest {
        val repo = PnrRepositoryImpl(api, time, real)

        val result = repo.getStatus(" 1234567890 ") as DataResult.Success

        assertEquals("1234567890", api.lastPnrRequest?.pnr)
        assertEquals(1, result.data.passengers.size)
        assertEquals("B2", result.data.passengers[0].coach)
    }

    @Test
    fun pnr_emptyResponse_isNoData() = runTest {
        api.pnrResult = Result.success(PnrResponseDto())
        val result = PnrRepositoryImpl(api, time, real).getStatus("1234567890") as DataResult.Failure
        assertEquals(ErrorType.NO_DATA, result.error.type)
    }

    @Test
    fun pnr_notFound_andRateLimit_areDistinguished() = runTest {
        val repo = PnrRepositoryImpl(api, time, real)

        api.pnrResult = Result.failure(httpException(404))
        assertEquals(ErrorType.NOT_FOUND, (repo.getStatus("1234567890") as DataResult.Failure).error.type)

        api.pnrResult = Result.failure(httpException(429))
        assertEquals(ErrorType.RATE_LIMITED, (repo.getStatus("1234567890") as DataResult.Failure).error.type)

        api.pnrResult = Result.failure(ioException())
        assertEquals(ErrorType.NETWORK, (repo.getStatus("1234567890") as DataResult.Failure).error.type)
    }

    // ---- Train search ----

    @Test
    fun trainSearch_mapsResults_andDropsEntriesWithoutNumbers() = runTest {
        api.searchResult = Result.success(
            TrainSearchResponseDto(listOf(TrainDto("12951", "Sample Rajdhani"), TrainDto(null, "No number"))),
        )

        val result = TrainSearchRepositoryImpl(api).search("raj") as DataResult.Success

        assertEquals(listOf("12951"), result.data.map { it.number })
    }

    @Test
    fun trainSearch_emptyBody_isEmptyList() = runTest {
        api.searchResult = Result.success(TrainSearchResponseDto())
        val result = TrainSearchRepositoryImpl(api).search("zzz") as DataResult.Success
        assertTrue(result.data.isEmpty())
    }
}
