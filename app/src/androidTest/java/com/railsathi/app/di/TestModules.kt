package com.railsathi.app.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.railsathi.app.data.local.dao.CachedTrainStatusDao
import com.railsathi.app.data.local.dao.SavedTrainDao
import com.railsathi.app.data.local.dao.TrainSearchHistoryDao
import com.railsathi.app.data.local.database.RailSathiDatabase
import com.railsathi.app.data.remote.api.ApiMode
import com.railsathi.app.data.remote.api.MockRailApi
import com.railsathi.app.data.remote.api.RailApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Instrumented tests always use the built-in sample API (with no artificial delay) and an
 * in-memory database, whatever RAIL_API_BASE_URL is set to. They never touch the network or the
 * user's real saved data.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [NetworkModule::class])
object TestNetworkModule {
    @Provides
    @Singleton
    fun provideApiMode(): ApiMode = ApiMode.MOCK

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideRailApi(): RailApi = MockRailApi(latencyMillis = 0L)
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DatabaseModule::class])
object TestDatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RailSathiDatabase =
        Room.inMemoryDatabaseBuilder(context, RailSathiDatabase::class.java).build()

    @Provides
    fun provideSavedTrainDao(db: RailSathiDatabase): SavedTrainDao = db.savedTrainDao()

    @Provides
    fun provideSearchHistoryDao(db: RailSathiDatabase): TrainSearchHistoryDao = db.searchHistoryDao()

    @Provides
    fun provideCachedTrainStatusDao(db: RailSathiDatabase): CachedTrainStatusDao =
        db.cachedTrainStatusDao()
}
