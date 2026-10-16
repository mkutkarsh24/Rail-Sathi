package com.railsathi.app.data.repository

import com.railsathi.app.data.remote.api.ApiMode
import com.railsathi.app.data.remote.api.RailApi
import com.railsathi.app.data.remote.dto.PnrRequestDto
import com.railsathi.app.data.remote.dto.toDomain
import com.railsathi.app.domain.model.AppError
import com.railsathi.app.domain.model.AvailabilityQuery
import com.railsathi.app.domain.model.AvailabilityResult
import com.railsathi.app.domain.model.DataResult
import com.railsathi.app.domain.model.ErrorType
import com.railsathi.app.domain.model.PnrStatus
import com.railsathi.app.domain.model.Train
import com.railsathi.app.domain.repository.AvailabilityRepository
import com.railsathi.app.domain.repository.PnrRepository
import com.railsathi.app.domain.repository.TrainSearchRepository
import com.railsathi.app.domain.usecase.Validators
import com.railsathi.app.util.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seat availability is volatile, so it is never cached: showing yesterday's "AVAILABLE" would be
 * misleading. A result with zero trains is a successful, empty answer, not an error.
 */
@Singleton
class AvailabilityRepositoryImpl @Inject constructor(
    private val api: RailApi,
    private val time: TimeProvider,
    private val mode: ApiMode,
) : AvailabilityRepository {

    override suspend fun search(query: AvailabilityQuery): DataResult<AvailabilityResult> {
        val from = query.fromStation.trim().uppercase()
        val to = query.toStation.trim().uppercase()
        val invalid = Validators.stationError(from, "source")
            ?: Validators.stationError(to, "destination")
            ?: Validators.differentStationsError(from, to)
            ?: Validators.classError(query.travelClass)
        if (invalid != null) {
            return DataResult.Failure(AppError.of(ErrorType.INVALID_INPUT, invalid))
        }

        val outcome = apiCall {
            api.getAvailability(
                fromStation = from,
                toStation = to,
                date = query.date.toString(),
                travelClass = query.travelClass,
                quota = query.quota,
            )
        }
        val dto = outcome.getOrElse { return DataResult.Failure(it.toAppError()) }
        return DataResult.Success(dto.toDomain(fetchedAtMillis = time.now(), isSample = mode.isMock))
    }
}

/** PNR lookups are never cached or persisted, and the PNR is never logged. */
@Singleton
class PnrRepositoryImpl @Inject constructor(
    private val api: RailApi,
    private val time: TimeProvider,
    private val mode: ApiMode,
) : PnrRepository {

    override suspend fun getStatus(pnr: String): DataResult<PnrStatus> {
        val number = pnr.trim()
        Validators.pnrError(number)?.let {
            return DataResult.Failure(AppError.of(ErrorType.INVALID_INPUT, it))
        }

        val outcome = apiCall { api.getPnrStatus(PnrRequestDto(number)) }
        val dto = outcome.getOrElse { return DataResult.Failure(it.toAppError()) }
        val status = dto.toDomain(fetchedAtMillis = time.now(), isSample = mode.isMock)
            ?: return DataResult.Failure(AppError.of(ErrorType.NO_DATA))
        return DataResult.Success(status)
    }
}

@Singleton
class TrainSearchRepositoryImpl @Inject constructor(
    private val api: RailApi,
) : TrainSearchRepository {

    override suspend fun search(query: String): DataResult<List<Train>> {
        val outcome = apiCall { api.searchTrains(query.trim()) }
        val dto = outcome.getOrElse { return DataResult.Failure(it.toAppError()) }
        return DataResult.Success(dto.trains.orEmpty().mapNotNull { it.toDomain() })
    }
}
