package com.railsathi.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.railsathi.app.data.network.ConnectivityObserver
import com.railsathi.app.data.remote.api.ApiMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(
    connectivity: ConnectivityObserver,
    apiMode: ApiMode,
) : ViewModel() {

    val isSampleMode: Boolean = apiMode.isMock

    val isOnline: StateFlow<Boolean> = connectivity.isOnline.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )
}
