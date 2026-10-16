package com.railsathi.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.railsathi.app.ui.components.EmptyState
import com.railsathi.app.ui.components.ErrorState
import com.railsathi.app.ui.components.LoadingState
import com.railsathi.app.ui.components.RailLogo
import com.railsathi.app.ui.components.ScreenTitle
import com.railsathi.app.ui.components.SectionTitle
import com.railsathi.app.ui.components.TrainRow
import com.railsathi.app.util.Formatters

@Composable
fun HomeRoute(
    onOpenStatus: (String) -> Unit,
    onOpenAvailability: () -> Unit,
    onOpenPnr: () -> Unit,
    onOpenPrivacy: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onSearch = { viewModel.onSearchSubmit(onOpenStatus) },
        onOpenTrain = onOpenStatus,
        onOpenStatusShortcut = { onOpenStatus("") },
        onOpenAvailability = onOpenAvailability,
        onOpenPnr = onOpenPnr,
        onOpenPrivacy = onOpenPrivacy,
        onRemoveFavourite = viewModel::removeFavourite,
        onClearHistory = viewModel::clearHistory,
        onRetryLoadSaved = viewModel::retryLoadSaved,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onOpenTrain: (String) -> Unit,
    onOpenStatusShortcut: () -> Unit,
    onOpenAvailability: () -> Unit,
    onOpenPnr: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onRemoveFavourite: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRetryLoadSaved: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RailLogo()
                    Spacer(Modifier.width(12.dp))
                    Column {
                        ScreenTitle("RailSathi")
                        Text(
                            "Indian Railway Companion",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onOpenPrivacy) {
                    Icon(Icons.Default.Lock, contentDescription = "Privacy policy")
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.search.query,
                onValueChange = onQueryChange,
                label = { Text("Train number or name") },
                singleLine = true,
                isError = state.search.queryError != null,
                supportingText = state.search.queryError?.let { { Text(it) } },
                trailingIcon = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search trains")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        when {
            state.search.isSearching -> item { LoadingState(message = "Searching trains…") }
            state.search.error != null -> item {
                ErrorState(message = state.search.error.message, onRetry = onSearch)
            }
            state.search.results != null -> {
                if (state.search.results.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No trains found",
                            message = "Try a different name, or search by the 5-digit train number.",
                        )
                    }
                } else {
                    item { SectionTitle("Search results") }
                    items(state.search.results, key = { "result_${it.number}" }) { train ->
                        TrainRow(train = train, onClick = { onOpenTrain(train.number) })
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShortcutCard("Live status", Icons.Default.Place, onOpenStatusShortcut, Modifier.weight(1f))
                ShortcutCard("Seat availability", Icons.Default.Search, onOpenAvailability, Modifier.weight(1f))
                ShortcutCard("PNR status", Icons.Default.Info, onOpenPnr, Modifier.weight(1f))
            }
        }

        item { SectionTitle("Favourite trains") }
        when {
            state.local.isLoading -> item { LoadingState(message = "Loading saved trains…") }
            state.local.hasError -> item {
                ErrorState(message = "Couldn't load your saved trains.", onRetry = onRetryLoadSaved)
            }
            state.local.favourites.isEmpty() -> item {
                EmptyState(
                    title = "No favourites yet",
                    message = "Open a train's live status and tap the heart to save it here.",
                )
            }
            else -> items(state.local.favourites, key = { "fav_${it.train.number}" }) { saved ->
                TrainRow(
                    train = saved.train,
                    onClick = { onOpenTrain(saved.train.number) },
                    trailing = {
                        IconButton(onClick = { onRemoveFavourite(saved.train.number) }) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Remove ${saved.train.name} from favourites",
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    },
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SectionTitle("Recently searched")
                if (state.local.history.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) { Text("Clear") }
                }
            }
        }
        when {
            state.local.isLoading || state.local.hasError -> Unit
            state.local.history.isEmpty() -> item {
                EmptyState(
                    title = "No recent searches",
                    message = "Trains you look up will appear here.",
                )
            }
            else -> items(state.local.history, key = { "hist_${it.train.number}" }) { item ->
                TrainRow(
                    train = item.train,
                    supporting = Formatters.deviceDateTime(item.searchedAtMillis),
                    onClick = { onOpenTrain(item.train.number) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShortcutCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
