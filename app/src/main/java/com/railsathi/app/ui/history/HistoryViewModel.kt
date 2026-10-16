package com.railsathi.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.railsathi.app.domain.model.SearchHistoryItem
import com.railsathi.app.domain.repository.SavedTrainsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val items: List<SearchHistoryItem> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val savedTrains: SavedTrainsRepository,
) : ViewModel() {

    private val reload = MutableStateFlow(0)

    val uiState: StateFlow<HistoryUiState> = reload.flatMapLatest {
        savedTrains.observeHistory(SavedTrainsRepository.MAX_HISTORY_ROWS)
            .map { HistoryUiState(isLoading = false, items = it) }
            .catch { emit(HistoryUiState(isLoading = false, hasError = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun retry() {
        reload.update { it + 1 }
    }

    fun remove(trainNumber: String) {
        viewModelScope.launch { savedTrains.removeHistoryItem(trainNumber) }
    }

    fun clearAll() {
        viewModelScope.launch { savedTrains.clearHistory() }
    }
}
