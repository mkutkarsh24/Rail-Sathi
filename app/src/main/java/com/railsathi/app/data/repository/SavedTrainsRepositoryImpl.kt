package com.railsathi.app.data.repository

import com.railsathi.app.data.local.dao.SavedTrainDao
import com.railsathi.app.data.local.dao.TrainSearchHistoryDao
import com.railsathi.app.data.local.entity.SavedTrainEntity
import com.railsathi.app.data.local.entity.TrainSearchHistoryEntity
import com.railsathi.app.domain.model.SavedTrain
import com.railsathi.app.domain.model.SearchHistoryItem
import com.railsathi.app.domain.model.Train
import com.railsathi.app.domain.repository.SavedTrainsRepository
import com.railsathi.app.util.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SavedTrainsRepositoryImpl @Inject constructor(
    private val savedDao: SavedTrainDao,
    private val historyDao: TrainSearchHistoryDao,
    private val time: TimeProvider,
) : SavedTrainsRepository {

    override fun observeFavourites(): Flow<List<SavedTrain>> =
        savedDao.observeAll().map { rows ->
            rows.map { SavedTrain(Train(it.trainNumber, it.trainName), it.savedAt) }
        }

    override fun observeIsFavourite(trainNumber: String): Flow<Boolean> =
        savedDao.observeIsSaved(trainNumber)

    override suspend fun addFavourite(train: Train) {
        savedDao.upsert(SavedTrainEntity(train.number, train.name, time.now()))
    }

    override suspend fun removeFavourite(trainNumber: String) {
        savedDao.delete(trainNumber)
    }

    override fun observeHistory(limit: Int): Flow<List<SearchHistoryItem>> =
        historyDao.observeRecent(limit).map { rows ->
            rows.map { SearchHistoryItem(Train(it.trainNumber, it.trainName), it.searchedAt) }
        }

    override suspend fun recordSearch(train: Train) {
        historyDao.upsert(TrainSearchHistoryEntity(train.number, train.name, time.now()))
        historyDao.trimTo(SavedTrainsRepository.MAX_HISTORY_ROWS)
    }

    override suspend fun removeHistoryItem(trainNumber: String) {
        historyDao.delete(trainNumber)
    }

    override suspend fun clearHistory() {
        historyDao.clear()
    }
}
