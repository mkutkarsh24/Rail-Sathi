package com.railsathi.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.railsathi.app.data.local.dao.CachedTrainStatusDao
import com.railsathi.app.data.local.dao.SavedTrainDao
import com.railsathi.app.data.local.dao.TrainSearchHistoryDao
import com.railsathi.app.data.local.entity.CachedTrainStatusEntity
import com.railsathi.app.data.local.entity.SavedTrainEntity
import com.railsathi.app.data.local.entity.TrainSearchHistoryEntity

@Database(
    entities = [
        SavedTrainEntity::class,
        TrainSearchHistoryEntity::class,
        CachedTrainStatusEntity::class,
    ],
    version = RailSathiDatabase.VERSION,
    exportSchema = true,
)
abstract class RailSathiDatabase : RoomDatabase() {
    abstract fun savedTrainDao(): SavedTrainDao
    abstract fun searchHistoryDao(): TrainSearchHistoryDao
    abstract fun cachedTrainStatusDao(): CachedTrainStatusDao

    companion object {
        const val VERSION = 1
        const val NAME = "railsathi.db"
    }
}

/**
 * Schema migrations. Version 1 is the first release, so nothing to migrate yet.
 *
 * When the schema changes: bump [RailSathiDatabase.VERSION], add a `Migration(old, new)` here, and
 * include it in [ALL]. Never use destructive migration: it would wipe the user's favourites.
 * The exported schema JSON in app/schemas/ should be committed so migrations can be tested.
 */
object Migrations {
    val ALL: Array<Migration> = emptyArray()
}
