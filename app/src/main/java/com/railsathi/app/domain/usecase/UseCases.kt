package com.railsathi.app.domain.usecase

import com.railsathi.app.domain.model.AppError
import com.railsathi.app.domain.model.AvailabilityQuery
import com.railsathi.app.domain.model.AvailabilityResult
import com.railsathi.app.domain.model.DataResult
import com.railsathi.app.domain.model.ErrorType
import com.railsathi.app.domain.model.PnrStatus
import com.railsathi.app.domain.model.RunningStatus
import com.railsathi.app.domain.model.Train
import com.railsathi.app.domain.repository.AvailabilityRepository
import com.railsathi.app.domain.repository.PnrRepository
import com.railsathi.app.domain.repository.RunningStatusRepository
import com.railsathi.app.domain.repository.SavedTrainsRepository
import com.railsathi.app.domain.repository.TrainSearchRepository
import java.time.LocalDate
import javax.inject.Inject

/** Fetches running status and, on success, records the train in the search history. */
class GetRunningStatusUseCase @Inject constructor(
    private val statusRepository: RunningStatusRepository,
    private val savedTrains: SavedTrainsRepository,
) {
    suspend operator fun invoke(trainNumber: String, date: LocalDate): DataResult<RunningStatus> {
        val result = statusRepository.getRunningStatus(trainNumber.trim(), date)
        if (result is DataResult.Success) {
            savedTrains.recordSearch(Train(result.data.trainNumber, result.data.trainName))
        }
        return result
    }
}

class SearchAvailabilityUseCase @Inject constructor(
    private val repository: AvailabilityRepository,
) {
    suspend operator fun invoke(query: AvailabilityQuery): DataResult<AvailabilityResult> =
        repository.search(
            query.copy(
                fromStation = query.fromStation.trim().uppercase(),
                toStation = query.toStation.trim().uppercase(),
            ),
        )
}

class GetPnrStatusUseCase @Inject constructor(
    private val repository: PnrRepository,
) {
    suspend operator fun invoke(pnr: String): DataResult<PnrStatus> = repository.getStatus(pnr.trim())
}

class SearchTrainsUseCase @Inject constructor(
    private val repository: TrainSearchRepository,
) {
    suspend operator fun invoke(query: String): DataResult<List<Train>> {
        val trimmed = query.trim()
        if (trimmed.length < MIN_QUERY_LENGTH) {
            return DataResult.Failure(
                AppError.of(ErrorType.INVALID_INPUT, "Type at least $MIN_QUERY_LENGTH characters."),
            )
        }
        return repository.search(trimmed)
    }

    companion object {
        const val MIN_QUERY_LENGTH = 2
    }
}

class ToggleFavouriteUseCase @Inject constructor(
    private val savedTrains: SavedTrainsRepository,
) {
    suspend operator fun invoke(train: Train, isCurrentlyFavourite: Boolean) {
        if (isCurrentlyFavourite) {
            savedTrains.removeFavourite(train.number)
        } else {
            savedTrains.addFavourite(train)
        }
    }
}
