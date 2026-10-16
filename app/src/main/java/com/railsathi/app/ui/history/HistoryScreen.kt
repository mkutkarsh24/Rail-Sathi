package com.railsathi.app.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.railsathi.app.ui.components.BackTitleBar
import com.railsathi.app.ui.components.EmptyState
import com.railsathi.app.ui.components.ErrorState
import com.railsathi.app.ui.components.LoadingState
import com.railsathi.app.ui.components.TrainRow
import com.railsathi.app.util.Formatters

@Composable
fun HistoryRoute(
    onBack: () -> Unit,
    onOpenStatus: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(
        state = state,
        onBack = onBack,
        onOpenStatus = onOpenStatus,
        onRemove = viewModel::remove,
        onClearAll = viewModel::clearAll,
        onRetry = viewModel::retry,
    )
}

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onBack: () -> Unit,
    onOpenStatus: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        BackTitleBar(
            title = "Search history",
            onBack = onBack,
            actions = {
                if (state.items.isNotEmpty()) {
                    TextButton(onClick = onClearAll) { Text("Clear all") }
                }
            },
        )
        when {
            state.isLoading -> LoadingState(message = "Loading history…")
            state.hasError -> ErrorState("Couldn't load your search history.", onRetry = onRetry)
            state.items.isEmpty() -> EmptyState(
                title = "No search history",
                message = "Trains you look up will be listed here.",
            )
            else -> LazyColumn {
                items(state.items, key = { it.train.number }) { item ->
                    TrainRow(
                        train = item.train,
                        supporting = Formatters.deviceDateTime(item.searchedAtMillis),
                        onClick = { onOpenStatus(item.train.number) },
                        trailing = {
                            IconButton(onClick = { onRemove(item.train.number) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove ${item.train.name} from history")
                            }
                        },
                    )
                }
            }
        }
    }
}
