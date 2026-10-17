package com.railsathi.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.railsathi.app.domain.model.AppError
import com.railsathi.app.domain.model.DataResult
import com.railsathi.app.domain.model.SavedTrain
import com.railsathi.app.domain.model.SearchHistoryItem
import com.railsathi.app.domain.model.Train
import com.railsathi.app.domain.repository.SavedTrainsRepository
import com.railsathi.app.domain.usecase.SearchTrainsUseCase
import com.railsathi.app.domain.usecase.Validators
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Saved-data half of the home screen. */
data class LocalHomeState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val favourites: List<SavedTrain> = emptyList(),
    val history: List<SearchHistoryItem> = emptyList(),
)

/** Search half of the home screen. */
data class HomeSearchState(
    val query: String = "",
    val queryError: String? = null,
    val isSearching: Boolean = false,
    /** null = no name search has been run. */
    val results: List<Train>? = null,
    val error: AppError? = null,
)

data class HomeUiState(
    val local: LocalHomeState = LocalHomeState(),
    val search: HomeSearchState = HomeSearchState(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedTrains: SavedTrainsRepository,
    private val searchTrains: SearchTrainsUseCase,
) : ViewModel() {

    private val reload = MutableStateFlow(0)
    private val searchState = MutableStateFlow(HomeSearchState())
    private var searchJob: Job? = null

    private val localState = reload.flatMapLatest {
        combine(
            savedTrains.observeFavourites(),
            savedTrains.observeHistory(HISTORY_ON_HOME),
        ) { favourites, history ->
            LocalHomeState(isLoading = false, hasError = false, favourites = favourites, history = history)
        }.catch { emit(LocalHomeState(isLoading = false, hasError = true)) }
    }

    val uiState: StateFlow<HomeUiState> = combine(localState, searchState) { local, search ->
        HomeUiState(local, search)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(value: String) {
        // A search still in flight belongs to the old text. Cancel it so it can neither leave the
        // spinner stuck nor land its results under a query the user has since changed.
        searchJob?.cancel()
        searchState.update {
            it.copy(query = value, queryError = null, isSearching = false, results = null, error = null)
        }
    }

    /**
     * A 5-digit query opens that train's live status directly via [onOpenTrain]. Anything else is
     * treated as a train name and searched through the data service.
     */
    fun onSearchSubmit(onOpenTrain: (String) -> Unit) {
        val query = searchState.value.query.trim()
        if (query.isEmpty()) {
            searchState.update { it.copy(queryError = "Enter a train number or name.") }
            return
        }
        if (query.all { it.isDigit() }) {
            if (Validators.isValidTrainNumber(query)) {
                onOpenTrain(query)
            } else {
                searchState.update { it.copy(queryError = "Train numbers have 5 digits.") }
            }
            return
        }
        if (query.length < SearchTrainsUseCase.MIN_QUERY_LENGTH) {
            // Input problem, not a failed search: show it under the field instead of an error
            // card with a Retry button that could only fail the same way.
            searchState.update {
                it.copy(queryError = "Type at least ${SearchTrainsUseCase.MIN_QUERY_LENGTH} characters.")
            }
            return
        }
        searchJob?.cancel()
        searchState.update { it.copy(isSearching = true, results = null, error = null, queryError = null) }
        searchJob = viewModelScope.launch {
            when (val result = searchTrains(query)) {
                is DataResult.Success ->
                    searchState.update { it.copy(isSearching = false, results = result.data) }
                is DataResult.Failure ->
                    searchState.update { it.copy(isSearching = false, error = result.error) }
            }
        }
    }

    fun retryLoadSaved() {
        reload.update { it + 1 }
    }

    fun removeFavourite(trainNumber: String) {
        viewModelScope.launch { savedTrains.removeFavourite(trainNumber) }
    }

    fun clearHistory() {
        viewModelScope.launch { savedTrains.clearHistory() }
    }

    private companion object {
        const val HISTORY_ON_HOME = 5
    }
}
