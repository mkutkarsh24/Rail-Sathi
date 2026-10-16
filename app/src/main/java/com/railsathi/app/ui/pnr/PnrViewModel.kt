package com.railsathi.app.ui.pnr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.railsathi.app.domain.model.AppError
import com.railsathi.app.domain.model.DataResult
import com.railsathi.app.domain.model.PnrStatus
import com.railsathi.app.domain.usecase.GetPnrStatusUseCase
import com.railsathi.app.domain.usecase.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Held in memory only. The PNR and the result are never saved to disk or written to logs. */
data class PnrUiState(
    val pnrInput: String = "",
    val inputError: String? = null,
    val isLoading: Boolean = false,
    val status: PnrStatus? = null,
    val error: AppError? = null,
)

@HiltViewModel
class PnrViewModel @Inject constructor(
    private val getPnrStatus: GetPnrStatusUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PnrUiState())
    val uiState: StateFlow<PnrUiState> = _uiState.asStateFlow()

    private var job: Job? = null

    fun onPnrChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(10)
        _uiState.update { it.copy(pnrInput = digits, inputError = null) }
    }

    fun submit() {
        val pnr = _uiState.value.pnrInput
        val error = Validators.pnrError(pnr)
        if (error != null) {
            _uiState.update { it.copy(inputError = error) }
            return
        }
        job?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null, status = null, inputError = null) }
        job = viewModelScope.launch {
            when (val result = getPnrStatus(pnr)) {
                is DataResult.Success -> _uiState.update { it.copy(isLoading = false, status = result.data) }
                is DataResult.Failure -> _uiState.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }

    fun clear() {
        job?.cancel()
        _uiState.value = PnrUiState()
    }
}
