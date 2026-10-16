package com.railsathi.app.testutil

import com.railsathi.app.data.local.dao.CachedTrainStatusDao
import com.railsathi.app.data.local.dao.RefreshTime
import com.railsathi.app.data.local.entity.CachedTrainStatusEntity
import com.railsathi.app.data.remote.api.RailApi
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
import com.railsathi.app.domain.model.SavedTrain
import com.railsathi.app.domain.model.SearchHistoryItem
import com.railsathi.app.domain.model.Train
import com.railsathi.app.domain.repository.SavedTrainsRepository
import com.railsathi.app.util.TimeProvider
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import retrofit2.HttpException
import retrofit2.Response

/** Replaces Dispatchers.Main so ViewModels using viewModelScope can run in JVM tests. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

class FixedTimeProvider(
    var nowMillis: Long = 1_760_000_000_000L,
    var today: LocalDate = LocalDate.of(2026, 10, 16),
) : TimeProvider {
    override fun now(): Long = nowMillis
    override fun today(): LocalDate = today
}

fun httpException(code: Int): HttpException =
    HttpException(Response.error<Any>(code, "".toResponseBody(null)))

fun ioException(): IOException = IOException("simulated network failure")

/** Configurable fake. Set the `*Result` property for the call you want to control. */
class FakeRailApi : RailApi {
    var runningStatusResult: Result<RunningStatusDto> = Result.success(TestData.runningStatusDto())
    var availabilityResult: Result<AvailabilityResponseDto> = Result.success(TestData.availabilityDto())
    var pnrResult: Result<PnrResponseDto> = Result.success(TestData.pnrDto())
    var searchResult: Result<TrainSearchResponseDto> = Result.success(
        TrainSearchResponseDto(listOf(TrainDto("12951", "Sample Rajdhani"))),
    )

    var runningStatusCalls = 0
    var lastPnrRequest: PnrRequestDto? = null

    override suspend fun getRunningStatus(trainNumber: String, date: String): RunningStatusDto {
        runningStatusCalls++
        return runningStatusResult.getOrThrow()
    }

    override suspend fun getAvailability(
        fromStation: String,
        toStation: String,
        date: String,
        travelClass: String?,
        quota: String?,
    ): AvailabilityResponseDto = availabilityResult.getOrThrow()

    override suspend fun getPnrStatus(request: PnrRequestDto): PnrResponseDto {
        lastPnrRequest = request
        return pnrResult.getOrThrow()
    }

    override suspend fun searchTrains(query: String): TrainSearchResponseDto = searchResult.getOrThrow()
}

class FakeCachedTrainStatusDao : CachedTrainStatusDao {
    private val rows = MutableStateFlow<Map<String, CachedTrainStatusEntity>>(emptyMap())

    override suspend fun get(trainNumber: String): CachedTrainStatusEntity? = rows.value[trainNumber]

    override suspend fun upsert(entity: CachedTrainStatusEntity) {
        rows.value = rows.value + (entity.trainNumber to entity)
    }

    override fun observeRefreshTimes(): Flow<List<RefreshTime>> =
        rows.map { map -> map.values.map { RefreshTime(it.trainNumber, it.lastSuccessfulRefresh) } }

    override suspend fun deleteOlderThan(before: Long) {
        rows.value = rows.value.filterValues { it.lastSuccessfulRefresh >= before }
    }

    override suspend fun clear() {
        rows.value = emptyMap()
    }
}

class FakeSavedTrainsRepository : SavedTrainsRepository {
    val favourites = MutableStateFlow<List<SavedTrain>>(emptyList())
    val history = MutableStateFlow<List<SearchHistoryItem>>(emptyList())
    private var clock = 1L

    override fun observeFavourites(): Flow<List<SavedTrain>> = favourites

    override fun observeIsFavourite(trainNumber: String): Flow<Boolean> =
        favourites.map { list -> list.any { it.train.number == trainNumber } }

    override suspend fun addFavourite(train: Train) {
        favourites.value = favourites.value.filterNot { it.train.number == train.number } +
            SavedTrain(train, clock++)
    }

    override suspend fun removeFavourite(trainNumber: String) {
        favourites.value = favourites.value.filterNot { it.train.number == trainNumber }
    }

    override fun observeHistory(limit: Int): Flow<List<SearchHistoryItem>> =
        history.map { it.take(limit) }

    override suspend fun recordSearch(train: Train) {
        history.value = listOf(SearchHistoryItem(train, clock++)) +
            history.value.filterNot { it.train.number == train.number }
    }

    override suspend fun removeHistoryItem(trainNumber: String) {
        history.value = history.value.filterNot { it.train.number == trainNumber }
    }

    override suspend fun clearHistory() {
        history.value = emptyList()
    }
}

object TestData {
    fun runningStatusDto(
        number: String? = "12951",
        name: String? = "Sample Rajdhani",
        updatedAt: String? = "2026-10-16T10:00:00+05:30",
    ) = RunningStatusDto(
        trainNumber = number,
        trainName = name,
        journeyDate = "2026-10-16",
        statusText = "Running",
        lastReportedStation = "Agra Cantt",
        lastReportedAt = "2026-10-16T09:55:00+05:30",
        delayMinutes = 12,
        expectedDestinationArrival = "2026-10-16T16:42:00+05:30",
        stops = listOf(
            StopDto("NDLS", "New Delhi", null, null, "2026-10-16T06:05:00+05:30", "2026-10-16T06:05:00+05:30", 0, "PASSED"),
            StopDto("AGC", "Agra Cantt", "2026-10-16T08:00:00+05:30", "2026-10-16T08:12:00+05:30", "2026-10-16T08:05:00+05:30", "2026-10-16T08:17:00+05:30", 12, "CURRENT"),
            StopDto("BPL", "Bhopal Jn", "2026-10-16T16:30:00+05:30", "2026-10-16T16:42:00+05:30", null, null, 12, "UPCOMING"),
        ),
        lastUpdatedAt = updatedAt,
    )

    fun availabilityDto() = AvailabilityResponseDto(
        trains = listOf(
            TrainAvailabilityDto(
                trainNumber = "12951",
                trainName = "Sample Rajdhani",
                fromStationCode = "NDLS",
                toStationCode = "BPL",
                departure = "2026-10-17T16:35:00+05:30",
                arrival = "2026-10-18T02:35:00+05:30",
                durationMinutes = 600,
                classes = listOf(
                    ClassAvailabilityDto("SL", "AVAILABLE-0042", 740),
                    ClassAvailabilityDto("3A", "GNWL 14/WL 9", 1980),
                ),
            ),
        ),
        lastUpdatedAt = "2026-10-16T10:00:00+05:30",
    )

    fun pnrDto() = PnrResponseDto(
        trainNumber = "12951",
        trainName = "Sample Rajdhani",
        journeyDate = "2026-10-20",
        boardingStation = "NDLS",
        destinationStation = "BPL",
        travelClass = "3A",
        chartPrepared = false,
        passengers = listOf(PassengerDto(1, "CNF/B2/21", "CNF", "B2", "21")),
        lastUpdatedAt = "2026-10-16T10:00:00+05:30",
    )
}
