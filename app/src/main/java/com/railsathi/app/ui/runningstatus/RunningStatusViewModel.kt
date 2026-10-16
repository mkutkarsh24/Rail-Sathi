package com.railsathi.app.ui.runningstatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.railsathi.app.domain.model.AppError
import com.railsathi.app.domain.model.DataResult
import com.railsathi.app.domain.model.RunningStatus
import com.railsathi.app.domain.model.Train
import com.railsathi.app.domain.repository.SavedTrainsRepository
import com.railsathi.app.domain.usecase.GetRunningStatusUseCase
import com.railsathi.app.domain.usecase.ToggleFavouriteUseCase
import com.railsathi.app.domain.usecase.Validators
import com.railsathi.app.util.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RunningStatusUiState(
    val trainNumberInput: String = "",
    val dateInput: String = "",
    val trainNumberError: String? = null,
    val dateError: String? = null,
    val isLoading: Boolean = false,
    /** Fresh data, or the last saved copy when [error] is also set. */
    val status: RunningStatus? = null,
    val error: AppError? = null,
    val isStale: Boolean = false,
    val isFavourite: Boolean = false,
)

@HiltViewModel
class RunningStatusViewModel @Inject constructor(
    private val getRunningStatus: GetRunningStatusUseCase,
    private val savedTrains: SavedTrainsRepository,
    private val toggleFavouriteUseCase: ToggleFavouriteUseCase,
    private val time: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RunningStatusUiState(dateInput = time.today().toString()))
    val uiState: StateFlow<RunningStatusUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var favouriteJob: Job? = null

    fun onTrainNumberChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(5)
        _uiState.update { it.copy(trainNumberInput = digits, trainNumberError = null) }
    }

    fun onDateChange(value: String) {
        _uiState.update { it.copy(dateInput = value, dateError = null) }
    }

    fun onQuickDate(daysFromToday: Int) {
        onDateChange(time.today().plusDays(daysFromToday.toLong()).toString())
    }

    /** Called when the screen is opened with a train number (e.g. from Home or Favourites). */
    fun openTrain(trainNumber: String) {
        onTrainNumberChange(trainNumber)
        _uiState.update { it.copy(dateInput = time.today().toString(), dateError = null) }
        submit()
    }

    fun submit() {
        val state = _uiState.value
        val numberError = Validators.trainNumberError(state.trainNumberInput)
        val dateError = Validators.dateError(
            state.dateInput,
            time.today(),
            maxPastDays = RUNNING_STATUS_MAX_PAST_DAYS,
        )
        if (numberError != null || dateError != null) {
            _uiState.update { it.copy(trainNumberError = numberError, dateError = dateError) }
            return
        }
        val date = Validators.parseDate(state.dateInput) ?: return
        val number = state.trainNumberInput

        loadJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                trainNumberError = null,
                dateError = null,
                // Drop the previous train's data if a different train was requested.
                status = it.status?.takeIf { s -> s.trainNumber == number },
            )
        }
        observeFavourite(number)

        loadJob = viewModelScope.launch {
            when (val result = getRunningStatus(number, date)) {
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        status = result.data,
                        error = null,
                        isStale = result.data.isStale(time.now()),
                    )
                }
                is DataResult.Failure -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        status = result.cached,
                        error = result.error,
                        isStale = false,
                    )
                }
            }
        }
    }

    fun onToggleFavourite() {
        val status = _uiState.value.status ?: return
        val wasFavourite = _uiState.value.isFavourite
        viewModelScope.launch {
            toggleFavouriteUseCase(Train(status.trainNumber, status.trainName), wasFavourite)
        }
    }

    private fun observeFavourite(trainNumber: String) {
        favouriteJob?.cancel()
        favouriteJob = viewModelScope.launch {
            savedTrains.observeIsFavourite(trainNumber).collect { isFav ->
                _uiState.update { it.copy(isFavourite = isFav) }
            }
        }
    }

    private companion object {
        /** A train that departed yesterday (overnight journeys) can still be running today. */
        const val RUNNING_STATUS_MAX_PAST_DAYS = 1L
    }
}
