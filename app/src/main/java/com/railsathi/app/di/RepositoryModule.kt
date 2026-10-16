package com.railsathi.app.di

import com.railsathi.app.data.repository.AvailabilityRepositoryImpl
import com.railsathi.app.data.repository.PnrRepositoryImpl
import com.railsathi.app.data.repository.RunningStatusRepositoryImpl
import com.railsathi.app.data.repository.SavedTrainsRepositoryImpl
import com.railsathi.app.data.repository.TrainSearchRepositoryImpl
import com.railsathi.app.domain.repository.AvailabilityRepository
import com.railsathi.app.domain.repository.PnrRepository
import com.railsathi.app.domain.repository.RunningStatusRepository
import com.railsathi.app.domain.repository.SavedTrainsRepository
import com.railsathi.app.domain.repository.TrainSearchRepository
import com.railsathi.app.util.SystemTimeProvider
import com.railsathi.app.util.TimeProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRunningStatusRepository(impl: RunningStatusRepositoryImpl): RunningStatusRepository

    @Binds
    @Singleton
    abstract fun bindAvailabilityRepository(impl: AvailabilityRepositoryImpl): AvailabilityRepository

    @Binds
    @Singleton
    abstract fun bindPnrRepository(impl: PnrRepositoryImpl): PnrRepository

    @Binds
    @Singleton
    abstract fun bindTrainSearchRepository(impl: TrainSearchRepositoryImpl): TrainSearchRepository

    @Binds
    @Singleton
    abstract fun bindSavedTrainsRepository(impl: SavedTrainsRepositoryImpl): SavedTrainsRepository

    @Binds
    @Singleton
    abstract fun bindTimeProvider(impl: SystemTimeProvider): TimeProvider
}
