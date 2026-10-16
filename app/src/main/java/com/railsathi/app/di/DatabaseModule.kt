package com.railsathi.app.di

import android.content.Context
import androidx.room.Room
import com.railsathi.app.data.local.dao.CachedTrainStatusDao
import com.railsathi.app.data.local.dao.SavedTrainDao
import com.railsathi.app.data.local.dao.TrainSearchHistoryDao
import com.railsathi.app.data.local.database.Migrations
import com.railsathi.app.data.local.database.RailSathiDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RailSathiDatabase =
        Room.databaseBuilder(context, RailSathiDatabase::class.java, RailSathiDatabase.NAME)
            .addMigrations(*Migrations.ALL)
            .build()

    @Provides
    fun provideSavedTrainDao(db: RailSathiDatabase): SavedTrainDao = db.savedTrainDao()

    @Provides
    fun provideSearchHistoryDao(db: RailSathiDatabase): TrainSearchHistoryDao = db.searchHistoryDao()

    @Provides
    fun provideCachedTrainStatusDao(db: RailSathiDatabase): CachedTrainStatusDao =
        db.cachedTrainStatusDao()
}
