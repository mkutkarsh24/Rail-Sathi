package com.railsathi.app.domain.repository

import com.railsathi.app.domain.model.AvailabilityQuery
import com.railsathi.app.domain.model.AvailabilityResult
import com.railsathi.app.domain.model.DataResult
import com.railsathi.app.domain.model.PnrStatus
import com.railsathi.app.domain.model.RunningStatus
import com.railsathi.app.domain.model.SavedTrain
import com.railsathi.app.domain.model.SearchHistoryItem
import com.railsathi.app.domain.model.Train
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface RunningStatusRepository {
    /** Fetches fresh status. On failure the result carries the last saved copy, if there is one. */
    suspend fun getRunningStatus(trainNumber: String, date: LocalDate): DataResult<RunningStatus>

    /** Epoch-millis of the last successful refresh per train number. */
    fun observeLastRefreshTimes(): Flow<Map<String, Long>>
}

interface AvailabilityRepository {
    suspend fun search(query: AvailabilityQuery): DataResult<AvailabilityResult>
}

interface PnrRepository {
    /** The PNR is sent to the data service and is never stored or logged on the device. */
    suspend fun getStatus(pnr: String): DataResult<PnrStatus>
}

interface TrainSearchRepository {
    suspend fun search(query: String): DataResult<List<Train>>
}

interface SavedTrainsRepository {
    fun observeFavourites(): Flow<List<SavedTrain>>
    fun observeIsFavourite(trainNumber: String): Flow<Boolean>
    suspend fun addFavourite(train: Train)
    suspend fun removeFavourite(trainNumber: String)

    fun observeHistory(limit: Int = DEFAULT_HISTORY_LIMIT): Flow<List<SearchHistoryItem>>
    suspend fun recordSearch(train: Train)
    suspend fun removeHistoryItem(trainNumber: String)
    suspend fun clearHistory()

    companion object {
        const val DEFAULT_HISTORY_LIMIT = 20
        const val MAX_HISTORY_ROWS = 50
    }
}
