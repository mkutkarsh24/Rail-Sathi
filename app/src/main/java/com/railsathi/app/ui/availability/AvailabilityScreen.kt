package com.railsathi.app.ui.availability

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.railsathi.app.domain.model.AvailabilityKind
import com.railsathi.app.domain.model.AvailabilityResult
import com.railsathi.app.domain.model.ClassAvailability
import com.railsathi.app.domain.model.TrainAvailability
import com.railsathi.app.domain.usecase.Validators
import com.railsathi.app.ui.components.BannerTone
import com.railsathi.app.ui.components.EmptyState
import com.railsathi.app.ui.components.ErrorState
import com.railsathi.app.ui.components.InfoBanner
import com.railsathi.app.ui.components.LoadingState
import com.railsathi.app.ui.components.QuickDateChips
import com.railsathi.app.ui.components.ScreenTitle
import com.railsathi.app.ui.components.SectionTitle
import com.railsathi.app.util.Formatters

@Composable
fun AvailabilityRoute(viewModel: AvailabilityViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AvailabilityScreen(
        state = state,
        onFromChange = viewModel::onFromChange,
        onToChange = viewModel::onToChange,
        onDateChange = viewModel::onDateChange,
        onQuickDate = viewModel::onQuickDate,
        onClassSelected = viewModel::onClassSelected,
        onQuotaSelected = viewModel::onQuotaSelected,
        onSearch = viewModel::search,
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AvailabilityScreen(
    state: AvailabilityUiState,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onQuickDate: (Int) -> Unit,
    onClassSelected: (String?) -> Unit,
    onQuotaSelected: (String) -> Unit,
    onSearch: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenTitle("Seat availability") }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.fromInput,
                    onValueChange = onFromChange,
                    label = { Text("From (code)") },
                    placeholder = { Text("NDLS") },
                    singleLine = true,
                    isError = state.fromError != null,
                    supportingText = state.fromError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.toInput,
                    onValueChange = onToChange,
                    label = { Text("To (code)") },
                    placeholder = { Text("BPL") },
                    singleLine = true,
                    isError = state.toError != null,
                    supportingText = state.toError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                )
            }
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
                keyboardActions = KeyboardActions(onDone = { onSearch() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item { QuickDateChips(onSelectDaysFromToday = onQuickDate) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle("Class")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.selectedClass == null,
                        onClick = { onClassSelected(null) },
                        label = { Text("Any") },
                    )
                    Validators.TRAVEL_CLASSES.forEach { cls ->
                        FilterChip(
                            selected = state.selectedClass == cls,
                            onClick = { onClassSelected(cls) },
                            label = { Text(cls) },
                        )
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionTitle("Quota")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Validators.QUOTAS.forEach { quota ->
                        FilterChip(
                            selected = state.selectedQuota == quota,
                            onClick = { onQuotaSelected(quota) },
                            label = { Text(quota) },
                        )
                    }
                }
                Text(
                    "Class and quota options depend on what your data provider supports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Button(onClick = onSearch, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                Text("Check availability")
            }
        }

        when {
            state.isLoading -> item { LoadingState(message = "Checking availability…") }
            state.error != null -> item { ErrorState(message = state.error.message, onRetry = onSearch) }
            state.result != null -> resultItems(state.result)
            !state.hasSearched -> item {
                EmptyState(
                    title = "Find a train",
                    message = "Enter source and destination station codes and a date to see availability as reported by your data provider.",
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.resultItems(result: AvailabilityResult) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (result.isSample) {
                InfoBanner("SAMPLE DATA: these are not real seat counts.", tone = BannerTone.INFO)
            }
            Text(
                "Updated ${result.providerUpdatedAt?.let { Formatters.dateTime(it) + " (provider)" } ?: Formatters.deviceDateTime(result.fetchedAtMillis) + " (received)"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (result.trains.isEmpty()) {
        item {
            EmptyState(
                title = "No trains found",
                message = "The provider returned no trains for these stations, date and filters.",
            )
        }
    } else {
        items(result.trains, key = { it.train.number }) { TrainAvailabilityCard(it) }
    }
}

@Composable
private fun TrainAvailabilityCard(item: TrainAvailability) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.train.name, style = MaterialTheme.typography.titleMedium)
            Text("#${item.train.number}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${item.fromStationCode ?: "—"} ${Formatters.time(item.departure)}  →  " +
                    "${item.toStationCode ?: "—"} ${Formatters.time(item.arrival)}  ·  ${Formatters.duration(item.durationMinutes)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (item.classes.isEmpty()) {
                Text("No class information from the provider.", style = MaterialTheme.typography.bodySmall)
            } else {
                item.classes.forEach { ClassRow(it) }
            }
        }
    }
}

@Composable
private fun ClassRow(item: ClassAvailability) {
    val color = when (item.kind) {
        AvailabilityKind.AVAILABLE -> MaterialTheme.colorScheme.tertiary
        AvailabilityKind.RAC -> MaterialTheme.colorScheme.secondary
        AvailabilityKind.WAITLIST -> MaterialTheme.colorScheme.secondary
        AvailabilityKind.NOT_AVAILABLE -> MaterialTheme.colorScheme.error
        AvailabilityKind.OTHER -> Color.Unspecified
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(item.travelClass, style = MaterialTheme.typography.titleSmall)
        Text(item.statusText, style = MaterialTheme.typography.bodyMedium, color = color)
        Text(Formatters.rupees(item.fareRupees) ?: "Fare n/a", style = MaterialTheme.typography.bodyMedium)
    }
}
