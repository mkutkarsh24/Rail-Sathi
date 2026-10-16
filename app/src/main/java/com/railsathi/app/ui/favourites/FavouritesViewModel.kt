package com.railsathi.app.ui.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.railsathi.app.domain.model.SavedTrain
import com.railsathi.app.domain.repository.RunningStatusRepository
import com.railsathi.app.domain.repository.SavedTrainsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

data class FavouriteItem(
    val saved: SavedTrain,
    /** When this train's running status was last successfully refreshed and cached, if ever. */
    val lastRefreshMillis: Long?,
)

data class FavouritesUiState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val items: List<FavouriteItem> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FavouritesViewModel @Inject constructor(
    private val savedTrains: SavedTrainsRepository,
    statusRepository: RunningStatusRepository,
) : ViewModel() {

    private val reload = MutableStateFlow(0)

    val uiState: StateFlow<FavouritesUiState> = reload.flatMapLatest {
        combine(
            savedTrains.observeFavourites(),
            statusRepository.observeLastRefreshTimes(),
        ) { favourites, refreshTimes ->
            FavouritesUiState(
                isLoading = false,
                items = favourites.map { FavouriteItem(it, refreshTimes[it.train.number]) },
            )
        }.catch { emit(FavouritesUiState(isLoading = false, hasError = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FavouritesUiState())

    fun retry() {
        reload.update { it + 1 }
    }

    fun remove(trainNumber: String) {
        viewModelScope.launch { savedTrains.removeFavourite(trainNumber) }
    }
}
