package com.railsathi.app.data.repository

import com.google.gson.Gson
import com.railsathi.app.data.local.dao.CachedTrainStatusDao
import com.railsathi.app.data.local.entity.CachedTrainStatusEntity
import com.railsathi.app.data.remote.api.ApiMode
import com.railsathi.app.data.remote.api.RailApi
import com.railsathi.app.data.remote.dto.RunningStatusDto
import com.railsathi.app.data.remote.dto.toDomain
import com.railsathi.app.domain.model.AppError
import com.railsathi.app.domain.model.DataResult
import com.railsathi.app.domain.model.ErrorType
import com.railsathi.app.domain.model.RunningStatus
import com.railsathi.app.domain.repository.RunningStatusRepository
import com.railsathi.app.domain.usecase.Validators
import com.railsathi.app.util.TimeProvider
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RunningStatusRepositoryImpl @Inject constructor(
    private val api: RailApi,
    private val cacheDao: CachedTrainStatusDao,
    private val gson: Gson,
    private val time: TimeProvider,
    private val mode: ApiMode,
) : RunningStatusRepository {

    override suspend fun getRunningStatus(
        trainNumber: String,
        date: LocalDate,
    ): DataResult<RunningStatus> {
        val number = trainNumber.trim()
        if (!Validators.isValidTrainNumber(number)) {
            return DataResult.Failure(
                AppError.of(ErrorType.INVALID_INPUT, "Enter a valid 5-digit train number."),
            )
        }

        val outcome = apiCall { api.getRunningStatus(number, date.toString()) }
        val dto = outcome.getOrElse { throwable ->
            return DataResult.Failure(throwable.toAppError(), loadCached(number))
        }

        val now = time.now()
        val status = dto.toDomain(
            fetchedAtMillis = now,
            isFromCache = false,
            isSample = mode.isMock,
            fallbackDate = date,
        ) ?: return DataResult.Failure(AppError.of(ErrorType.NO_DATA), loadCached(number))

        cacheDao.upsert(
            CachedTrainStatusEntity(
                trainNumber = status.trainNumber,
                trainName = status.trainName,
                journeyDate = status.journeyDate.toString(),
                payloadJson = gson.toJson(dto),
                lastSuccessfulRefresh = now,
                isSample = mode.isMock,
            ),
        )
        return DataResult.Success(status)
    }

    override fun observeLastRefreshTimes(): Flow<Map<String, Long>> =
        cacheDao.observeRefreshTimes().map { rows ->
            rows.associate { it.trainNumber to it.lastSuccessfulRefresh }
        }

    /** The last saved copy for this train, marked as cached, or null if none (or unreadable). */
    private suspend fun loadCached(trainNumber: String): RunningStatus? {
        val entity = cacheDao.get(trainNumber) ?: return null
        val dto = try {
            gson.fromJson(entity.payloadJson, RunningStatusDto::class.java)
        } catch (e: Exception) {
            null
        } ?: return null
        val fallbackDate = runCatching { LocalDate.parse(entity.journeyDate) }.getOrElse { time.today() }
        return dto.toDomain(
            fetchedAtMillis = entity.lastSuccessfulRefresh,
            isFromCache = true,
            isSample = entity.isSample,
            fallbackDate = fallbackDate,
        )
    }
}
