package com.railsathi.app.ui.runningstatus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.railsathi.app.domain.model.RunningStatus
import com.railsathi.app.domain.model.StationStop
import com.railsathi.app.domain.model.StopState
import com.railsathi.app.ui.components.BannerTone
import com.railsathi.app.ui.components.EmptyState
import com.railsathi.app.ui.components.ErrorState
import com.railsathi.app.ui.components.FavouriteToggle
import com.railsathi.app.ui.components.InfoBanner
import com.railsathi.app.ui.components.LoadingState
import com.railsathi.app.ui.components.QuickDateChips
import com.railsathi.app.ui.components.ScreenTitle
import com.railsathi.app.ui.components.SectionTitle
import com.railsathi.app.util.Formatters

@Composable
fun RunningStatusRoute(
    initialTrain: String?,
    viewModel: RunningStatusViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Runs once per distinct incoming train number, and not again after a configuration change.
    var handled by rememberSaveable(initialTrain) { mutableStateOf(false) }
    LaunchedEffect(initialTrain) {
        if (!initialTrain.isNullOrBlank() && !handled) {
            handled = true
            viewModel.openTrain(initialTrain)
        }
    }

    RunningStatusScreen(
        state = state,
        onTrainNumberChange = viewModel::onTrainNumberChange,
        onDateChange = viewModel::onDateChange,
        onQuickDate = viewModel::onQuickDate,
        onSubmit = viewModel::submit,
        onToggleFavourite = viewModel::onToggleFavourite,
    )
}

@Composable
fun RunningStatusScreen(
    state: RunningStatusUiState,
    onTrainNumberChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onQuickDate: (Int) -> Unit,
    onSubmit: () -> Unit,
    onToggleFavourite: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenTitle("Live running status") }

        item {
            OutlinedTextField(
                value = state.trainNumberInput,
                onValueChange = onTrainNumberChange,
                label = { Text("Train number") },
                placeholder = { Text("e.g. 12951") },
                singleLine = true,
                isError = state.trainNumberError != null,
                supportingText = state.trainNumberError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = state.dateInput,
                onValueChange = onDateChange,
                label = { Text("Journey date (YYYY-MM-DD)") },
                singleLine = true,
                isError = state.dateError != null,
                supportingText = state.dateError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { QuickDateChips(onSelectDaysFromToday = onQuickDate) }
        item {
            Button(
                onClick = onSubmit,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Get status") }
        }

        when {
            state.isLoading && state.status == null -> item { LoadingState(message = "Fetching running status…") }

            state.status != null -> {
                val status = state.status
                item {
                    StatusBanners(state = state, status = status)
                }
                item {
                    StatusSummaryCard(
                        status = status,
                        isFavourite = state.isFavourite,
                        isRefreshing = state.isLoading,
                        onRefresh = onSubmit,
                        onToggleFavourite = onToggleFavourite,
                    )
                }
                if (status.stops.isEmpty()) {
                    item {
                        Text(
                            "The provider did not send the station list for this train.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    stopSection("Previous stations", status.previousStops)
                    stopSection("Current station", listOfNotNull(status.currentStop))
                    stopSection("Upcoming stations", status.upcomingStops)
                    stopSection("Other stations", status.stops.filter { it.state == StopState.UNKNOWN })
                }
            }

            state.error != null -> item {
                ErrorState(message = state.error.message, onRetry = onSubmit)
            }

            else -> item {
                EmptyState(
                    title = "Where is my train?",
                    message = "Enter a train number and journey date to see its current running status.",
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.stopSection(
    title: String,
    stops: List<StationStop>,
) {
    if (stops.isEmpty()) return
    item(key = "header_$title") { SectionTitle(title) }
    items(stops, key = { "${title}_${it.stationCode}" }) { stop -> StopRow(stop) }
}

@Composable
private fun StatusBanners(state: RunningStatusUiState, status: RunningStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (status.isSample) {
            InfoBanner("SAMPLE DATA: not a real train position.", tone = BannerTone.INFO)
        }
        if (state.error != null) {
            InfoBanner(
                text = "${state.error.message} Showing saved data from ${Formatters.deviceDateTime(status.fetchedAtMillis)}.",
                tone = BannerTone.WARNING,
            )
        } else if (status.isFromCache) {
            InfoBanner(
                text = "Saved data from ${Formatters.deviceDateTime(status.fetchedAtMillis)}.",
                tone = BannerTone.WARNING,
            )
        }
        if (state.isStale) {
            InfoBanner(
                text = "This update may be out of date. The provider last reported at " +
                    "${Formatters.dateTime(status.providerUpdatedAt)}. Try refreshing.",
                tone = BannerTone.WARNING,
            )
        }
    }
}

@Composable
private fun StatusSummaryCard(
    status: RunningStatus,
    isFavourite: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onToggleFavourite: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(status.trainName, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "#${status.trainNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row {
                    IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh running status")
                    }
                    FavouriteToggle(
                        isFavourite = isFavourite,
                        onToggle = onToggleFavourite,
                        trainName = status.trainName,
                    )
                }
            }
            HorizontalDivider()
            DetailLine("Status", status.statusText ?: "Not reported")
            DetailLine(
                "Last reported station",
                listOfNotNull(
                    status.lastReportedStation ?: "Not reported",
                    status.lastReportedAt?.let { "at ${Formatters.time(it)}" },
                ).joinToString(" "),
            )
            DetailLine("Delay", Formatters.delay(status.delayMinutes))
            DetailLine(
                "Expected at destination",
                listOfNotNull(
                    status.destinationName,
                    status.expectedDestinationArrival?.let { Formatters.dateTime(it) } ?: "not reported",
                ).joinToString(": "),
            )
            DetailLine("Journey date", Formatters.date(status.journeyDate))
            DetailLine(
                "Last updated",
                status.providerUpdatedAt?.let { "${Formatters.dateTime(it)} (provider)" }
                    ?: "${Formatters.deviceDateTime(status.fetchedAtMillis)} (received)",
            )
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun StopRow(stop: StationStop) {
    val stateLabel = when (stop.state) {
        StopState.PASSED -> "Departed"
        StopState.CURRENT -> "Current"
        StopState.UPCOMING -> "Upcoming"
        StopState.UNKNOWN -> "Status unknown"
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${stop.stationName} (${stop.stationCode})", style = MaterialTheme.typography.titleSmall)
                Text(stateLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (stop.scheduledArrival != null || stop.estimatedArrival != null) {
                Text(
                    "Arrival: scheduled ${Formatters.time(stop.scheduledArrival)}, expected ${Formatters.time(stop.estimatedArrival)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (stop.scheduledDeparture != null || stop.estimatedDeparture != null) {
                Text(
                    "Departure: scheduled ${Formatters.time(stop.scheduledDeparture)}, expected ${Formatters.time(stop.estimatedDeparture)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (stop.delayMinutes != null) {
                Text(Formatters.delay(stop.delayMinutes), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
