package com.railsathi.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.railsathi.app.data.local.entity.CachedTrainStatusEntity
import com.railsathi.app.data.local.entity.SavedTrainEntity
import com.railsathi.app.data.local.entity.TrainSearchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedTrainDao {
    @Query("SELECT * FROM saved_trains ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<SavedTrainEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_trains WHERE trainNumber = :trainNumber)")
    fun observeIsSaved(trainNumber: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SavedTrainEntity)

    @Query("DELETE FROM saved_trains WHERE trainNumber = :trainNumber")
    suspend fun delete(trainNumber: String)
}

@Dao
interface TrainSearchHistoryDao {
    @Query("SELECT * FROM train_search_history ORDER BY searchedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TrainSearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TrainSearchHistoryEntity)

    @Query("DELETE FROM train_search_history WHERE trainNumber = :trainNumber")
    suspend fun delete(trainNumber: String)

    @Query("DELETE FROM train_search_history")
    suspend fun clear()

    /** Keeps only the [keep] most recent rows. */
    @Query(
        "DELETE FROM train_search_history WHERE trainNumber NOT IN " +
            "(SELECT trainNumber FROM train_search_history ORDER BY searchedAt DESC LIMIT :keep)",
    )
    suspend fun trimTo(keep: Int)
}

/** Projection used to show "last refreshed" next to favourites without loading cached payloads. */
data class RefreshTime(
    val trainNumber: String,
    val lastSuccessfulRefresh: Long,
)

@Dao
interface CachedTrainStatusDao {
    @Query("SELECT * FROM cached_train_status WHERE trainNumber = :trainNumber")
    suspend fun get(trainNumber: String): CachedTrainStatusEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedTrainStatusEntity)

    @Query("SELECT trainNumber, lastSuccessfulRefresh FROM cached_train_status")
    fun observeRefreshTimes(): Flow<List<RefreshTime>>

    @Query("DELETE FROM cached_train_status WHERE lastSuccessfulRefresh < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM cached_train_status")
    suspend fun clear()
}
