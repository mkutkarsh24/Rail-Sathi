package com.railsathi.app.ui.availability

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.railsathi.app.domain.model.AppError
import com.railsathi.app.domain.model.AvailabilityQuery
import com.railsathi.app.domain.model.AvailabilityResult
import com.railsathi.app.domain.model.DataResult
import com.railsathi.app.domain.usecase.SearchAvailabilityUseCase
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

data class AvailabilityUiState(
    val fromInput: String = "",
    val toInput: String = "",
    val dateInput: String = "",
    /** null = any class. */
    val selectedClass: String? = null,
    val selectedQuota: String = "GN",
    val fromError: String? = null,
    val toError: String? = null,
    val dateError: String? = null,
    val isLoading: Boolean = false,
    val result: AvailabilityResult? = null,
    val error: AppError? = null,
    /** True once a search has been submitted, so "no trains" is not shown before the first search. */
    val hasSearched: Boolean = false,
)

@HiltViewModel
class AvailabilityViewModel @Inject constructor(
    private val searchAvailability: SearchAvailabilityUseCase,
    private val time: TimeProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AvailabilityUiState(dateInput = time.today().toString()))
    val uiState: StateFlow<AvailabilityUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onFromChange(value: String) {
        _uiState.update { it.copy(fromInput = value.filter(Char::isLetter).take(5).uppercase(), fromError = null) }
    }

    fun onToChange(value: String) {
        _uiState.update { it.copy(toInput = value.filter(Char::isLetter).take(5).uppercase(), toError = null) }
    }

    fun onDateChange(value: String) {
        _uiState.update { it.copy(dateInput = value, dateError = null) }
    }

    fun onQuickDate(daysFromToday: Int) {
        onDateChange(time.today().plusDays(daysFromToday.toLong()).toString())
    }

    fun onClassSelected(travelClass: String?) {
        _uiState.update { it.copy(selectedClass = travelClass) }
    }

    fun onQuotaSelected(quota: String) {
        _uiState.update { it.copy(selectedQuota = quota) }
    }

    fun search() {
        val s = _uiState.value
        val fromError = Validators.stationError(s.fromInput, "source")
        val toError = Validators.stationError(s.toInput, "destination")
            ?: if (fromError == null) Validators.differentStationsError(s.fromInput, s.toInput) else null
        val dateError = Validators.dateError(s.dateInput, time.today())
        if (fromError != null || toError != null || dateError != null) {
            _uiState.update { it.copy(fromError = fromError, toError = toError, dateError = dateError) }
            return
        }
        val date = Validators.parseDate(s.dateInput) ?: return

        searchJob?.cancel()
        _uiState.update {
            it.copy(isLoading = true, error = null, result = null, hasSearched = true, fromError = null, toError = null, dateError = null)
        }
        searchJob = viewModelScope.launch {
            val query = AvailabilityQuery(
                fromStation = s.fromInput,
                toStation = s.toInput,
                date = date,
                travelClass = s.selectedClass,
                quota = s.selectedQuota,
            )
            when (val result = searchAvailability(query)) {
                is DataResult.Success -> _uiState.update { it.copy(isLoading = false, result = result.data, error = null) }
                is DataResult.Failure -> _uiState.update { it.copy(isLoading = false, result = null, error = result.error) }
            }
        }
    }
}
