package com.railsathi.app.ui.favourites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.railsathi.app.ui.components.EmptyState
import com.railsathi.app.ui.components.ErrorState
import com.railsathi.app.ui.components.LoadingState
import com.railsathi.app.ui.components.ScreenTitle
import com.railsathi.app.ui.components.TrainRow
import com.railsathi.app.util.Formatters

@Composable
fun FavouritesRoute(
    onOpenStatus: (String) -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: FavouritesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FavouritesScreen(
        state = state,
        onOpenStatus = onOpenStatus,
        onOpenHistory = onOpenHistory,
        onRemove = viewModel::remove,
        onRetry = viewModel::retry,
    )
}

@Composable
fun FavouritesScreen(
    state: FavouritesUiState,
    onOpenStatus: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onRemove: (String) -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ScreenTitle("Favourite trains")
                TextButton(onClick = onOpenHistory) { Text("Search history") }
            }
        }
        item {
            Text(
                "Tap a train to open its status. If you're offline, the last saved status is shown, marked with when it was updated.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            state.isLoading -> item { LoadingState(message = "Loading favourites…") }
            state.hasError -> item { ErrorState("Couldn't load your favourites.", onRetry = onRetry) }
            state.items.isEmpty() -> item {
                EmptyState(
                    title = "No favourites yet",
                    message = "Open a train's live status and tap the heart to save it.",
                )
            }
            else -> items(state.items, key = { it.saved.train.number }) { item ->
                TrainRow(
                    train = item.saved.train,
                    supporting = item.lastRefreshMillis
                        ?.let { "Status saved ${Formatters.deviceDateTime(it)}" }
                        ?: "No saved status yet",
                    onClick = { onOpenStatus(item.saved.train.number) },
                    trailing = {
                        IconButton(onClick = { onRemove(item.saved.train.number) }) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Remove ${item.saved.train.name} from favourites",
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    },
                )
            }
        }
    }
}
