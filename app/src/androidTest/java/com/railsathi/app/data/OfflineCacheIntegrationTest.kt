package com.railsathi.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.railsathi.app.data.local.database.RailSathiDatabase
import com.railsathi.app.data.remote.api.ApiMode
import com.railsathi.app.data.remote.api.MockRailApi
import com.railsathi.app.data.remote.api.RailApi
import com.railsathi.app.data.remote.dto.AvailabilityResponseDto
import com.railsathi.app.data.remote.dto.PnrRequestDto
import com.railsathi.app.data.remote.dto.PnrResponseDto
import com.railsathi.app.data.remote.dto.RunningStatusDto
import com.railsathi.app.data.remote.dto.TrainSearchResponseDto
import com.railsathi.app.data.repository.RunningStatusRepositoryImpl
import com.railsathi.app.domain.model.DataResult
import com.railsathi.app.domain.model.ErrorType
import com.railsathi.app.util.TimeProvider
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Offline behaviour end to end: real repository + real Room database. The first call succeeds
 * (sample API) and is cached; the second call "loses the network" and must fall back to the saved
 * copy, marked as cached and carrying the time it was last updated.
 */
@RunWith(AndroidJUnit4::class)
class OfflineCacheIntegrationTest {

    private lateinit var db: RailSathiDatabase
    private val date = LocalDate.of(2026, 10, 16)
    private val clock = object : TimeProvider {
        var now = 1_000_000L
        override fun now() = now
        override fun today(): LocalDate = date
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RailSathiDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun whenTheNetworkDrops_theLastSavedStatusIsShownAndMarkedAsCached() = runBlocking {
        val online = RunningStatusRepositoryImpl(
            MockRailApi(latencyMillis = 0L), db.cachedTrainStatusDao(), Gson(), clock, ApiMode.MOCK,
        )
        val fresh = online.getRunningStatus("12951", date) as DataResult.Success
        assertEquals(false, fresh.data.isFromCache)
        val firstFetch = clock.now

        clock.now += 3_600_000L
        val offline = RunningStatusRepositoryImpl(OfflineApi(), db.cachedTrainStatusDao(), Gson(), clock, ApiMode.MOCK)
        val result = offline.getRunningStatus("12951", date)

        assertTrue(result is DataResult.Failure)
        result as DataResult.Failure
        assertEquals(ErrorType.NETWORK, result.error.type)
        val cached = result.cached
        assertNotNull(cached)
        assertTrue(cached!!.isFromCache)
        assertEquals("12951", cached.trainNumber)
        assertEquals(firstFetch, cached.fetchedAtMillis) // when it was last updated, not "now"
        assertTrue(cached.stops.isNotEmpty())
    }

    @Test
    fun whenNothingWasEverCached_theFailureCarriesNoData() = runBlocking {
        val offline = RunningStatusRepositoryImpl(OfflineApi(), db.cachedTrainStatusDao(), Gson(), clock, ApiMode.MOCK)

        val result = offline.getRunningStatus("12951", date) as DataResult.Failure

        assertEquals(null, result.cached)
    }

    /** Every call fails the way a dead connection does. */
    private class OfflineApi : RailApi {
        override suspend fun getRunningStatus(trainNumber: String, date: String): RunningStatusDto =
            throw IOException("offline")

        override suspend fun getAvailability(
            fromStation: String,
            toStation: String,
            date: String,
            travelClass: String?,
            quota: String?,
        ): AvailabilityResponseDto = throw IOException("offline")

        override suspend fun getPnrStatus(request: PnrRequestDto): PnrResponseDto = throw IOException("offline")

        override suspend fun searchTrains(query: String): TrainSearchResponseDto = throw IOException("offline")
    }
}
