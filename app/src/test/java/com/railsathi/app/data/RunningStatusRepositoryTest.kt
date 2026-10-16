package com.railsathi.app.data

import com.google.gson.Gson
import com.railsathi.app.data.remote.api.ApiMode
import com.railsathi.app.data.remote.dto.RunningStatusDto
import com.railsathi.app.data.repository.RunningStatusRepositoryImpl
import com.railsathi.app.domain.model.DataResult
import com.railsathi.app.domain.model.ErrorType
import com.railsathi.app.testutil.FakeCachedTrainStatusDao
import com.railsathi.app.testutil.FakeRailApi
import com.railsathi.app.testutil.FixedTimeProvider
import com.railsathi.app.testutil.TestData
import com.railsathi.app.testutil.httpException
import com.railsathi.app.testutil.ioException
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RunningStatusRepositoryTest {

    private val date = LocalDate.of(2026, 10, 16)
    private lateinit var api: FakeRailApi
    private lateinit var dao: FakeCachedTrainStatusDao
    private lateinit var time: FixedTimeProvider
    private lateinit var repository: RunningStatusRepositoryImpl

    @Before
    fun setUp() {
        api = FakeRailApi()
        dao = FakeCachedTrainStatusDao()
        time = FixedTimeProvider()
        repository = RunningStatusRepositoryImpl(api, dao, Gson(), time, ApiMode.from("https://api.example.com"))
    }

    @Test
    fun invalidTrainNumber_failsWithoutCallingTheApi() = runTest {
        val result = repository.getRunningStatus("12", date)

        assertTrue(result is DataResult.Failure)
        assertEquals(ErrorType.INVALID_INPUT, (result as DataResult.Failure).error.type)
        assertEquals(0, api.runningStatusCalls)
    }

    @Test
    fun success_returnsFreshData_andCachesItWithRefreshTime() = runTest {
        val result = repository.getRunningStatus("12951", date)

        assertTrue(result is DataResult.Success)
        val status = (result as DataResult.Success).data
        assertFalse(status.isFromCache)
        assertFalse(status.isSample)
        assertEquals(time.nowMillis, status.fetchedAtMillis)

        val cached = dao.get("12951")
        assertNotNull(cached)
        assertEquals(time.nowMillis, cached!!.lastSuccessfulRefresh)
        assertEquals(mapOf("12951" to time.nowMillis), repository.observeLastRefreshTimes().first())
    }

    @Test
    fun networkFailure_afterASuccess_returnsCachedCopyMarkedAsCached() = runTest {
        repository.getRunningStatus("12951", date)
        val firstFetchTime = time.nowMillis
        time.nowMillis += 3_600_000L
        api.runningStatusResult = Result.failure(ioException())

        val result = repository.getRunningStatus("12951", date)

        assertTrue(result is DataResult.Failure)
        result as DataResult.Failure
        assertEquals(ErrorType.NETWORK, result.error.type)
        val cached = result.cached
        assertNotNull(cached)
        assertTrue(cached!!.isFromCache)
        assertEquals(firstFetchTime, cached.fetchedAtMillis) // shows when it was *last updated*, not now
        assertEquals("Agra Cantt", cached.lastReportedStation)
        assertEquals(3, cached.stops.size)
    }

    @Test
    fun failureWithNothingCached_hasNoCachedData() = runTest {
        api.runningStatusResult = Result.failure(ioException())

        val result = repository.getRunningStatus("12951", date) as DataResult.Failure

        assertNull(result.cached)
        assertEquals(ErrorType.NETWORK, result.error.type)
    }

    @Test
    fun notFound_andRateLimit_areMappedAndStillOfferCache() = runTest {
        repository.getRunningStatus("12951", date)

        api.runningStatusResult = Result.failure(httpException(404))
        val notFound = repository.getRunningStatus("12951", date) as DataResult.Failure
        assertEquals(ErrorType.NOT_FOUND, notFound.error.type)
        assertNotNull(notFound.cached)

        api.runningStatusResult = Result.failure(httpException(429))
        val limited = repository.getRunningStatus("12951", date) as DataResult.Failure
        assertEquals(ErrorType.RATE_LIMITED, limited.error.type)
    }

    @Test
    fun emptyApiResponse_isReportedAsNoData_andIsNotCached() = runTest {
        api.runningStatusResult = Result.success(RunningStatusDto())

        val result = repository.getRunningStatus("12951", date) as DataResult.Failure

        assertEquals(ErrorType.NO_DATA, result.error.type)
        assertNull(dao.get("12951"))
    }

    @Test
    fun corruptCache_isIgnoredRatherThanCrashing() = runTest {
        dao.upsert(
            com.railsathi.app.data.local.entity.CachedTrainStatusEntity(
                trainNumber = "12951",
                trainName = "x",
                journeyDate = "2026-10-16",
                payloadJson = "{ not json",
                lastSuccessfulRefresh = 1L,
                isSample = false,
            ),
        )
        api.runningStatusResult = Result.failure(ioException())

        val result = repository.getRunningStatus("12951", date) as DataResult.Failure

        assertNull(result.cached)
    }

    @Test
    fun mockMode_tagsDataAsSample_andKeepsTheTagInTheCache() = runTest {
        val mockRepo = RunningStatusRepositoryImpl(api, dao, Gson(), time, ApiMode.MOCK)
        api.runningStatusResult = Result.success(TestData.runningStatusDto())

        val fresh = (mockRepo.getRunningStatus("12951", date) as DataResult.Success).data
        assertTrue(fresh.isSample)

        api.runningStatusResult = Result.failure(ioException())
        val cached = (mockRepo.getRunningStatus("12951", date) as DataResult.Failure).cached!!
        assertTrue(cached.isSample)
    }
}
