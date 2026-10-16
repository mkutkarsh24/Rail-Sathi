package com.railsathi.app.ui.pnr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.railsathi.app.domain.model.PassengerStatus
import com.railsathi.app.domain.model.PnrStatus
import com.railsathi.app.ui.components.BannerTone
import com.railsathi.app.ui.components.EmptyState
import com.railsathi.app.ui.components.ErrorState
import com.railsathi.app.ui.components.InfoBanner
import com.railsathi.app.ui.components.LoadingState
import com.railsathi.app.ui.components.ScreenTitle
import com.railsathi.app.ui.components.SectionTitle
import com.railsathi.app.util.Formatters

@Composable
fun PnrRoute(viewModel: PnrViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PnrScreen(
        state = state,
        onPnrChange = viewModel::onPnrChange,
        onSubmit = viewModel::submit,
        onClear = viewModel::clear,
    )
}

@Composable
fun PnrScreen(
    state: PnrUiState,
    onPnrChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenTitle("PNR status") }
        item {
            OutlinedTextField(
                value = state.pnrInput,
                onValueChange = onPnrChange,
                label = { Text("10-digit PNR number") },
                singleLine = true,
                isError = state.inputError != null,
                supportingText = state.inputError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onSubmit, enabled = !state.isLoading, modifier = Modifier.weight(1f)) {
                    Text("Check PNR")
                }
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("Clear") }
            }
        }
        item {
            Text(
                "Your PNR is sent to the data service to fetch the status. It is not saved on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            state.isLoading -> item { LoadingState(message = "Checking PNR…") }
            state.error != null -> item { ErrorState(message = state.error.message, onRetry = onSubmit) }
            state.status != null -> {
                val status = state.status
                item { PnrSummary(status) }
                item { SectionTitle("Passengers") }
                if (status.passengers.isEmpty()) {
                    item {
                        Text(
                            "The provider did not return passenger details.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    itemsIndexed(status.passengers) { index, passenger -> PassengerCard(index, passenger) }
                }
            }
            else -> item {
                EmptyState(
                    title = "Check a booking",
                    message = "Enter your 10-digit PNR to see its current status.",
                )
            }
        }
    }
}

@Composable
private fun PnrSummary(status: PnrStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (status.isSample) {
            InfoBanner("SAMPLE DATA: this is not a real booking.", tone = BannerTone.INFO)
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    listOfNotNull(status.trainNumber, status.trainName).joinToString(" · ").ifEmpty { "Train not reported" },
                    style = MaterialTheme.typography.titleMedium,
                )
                HorizontalDivider()
                Line("Journey date", Formatters.date(status.journeyDate))
                Line("Boarding", status.boardingStation ?: "Not reported")
                Line("Destination", status.destinationStation ?: "Not reported")
                Line("Class", status.travelClass ?: "Not reported")
                Line(
                    "Chart",
                    when (status.chartPrepared) {
                        true -> "Chart prepared"
                        false -> "Chart not prepared yet"
                        null -> "Not reported"
                    },
                )
                Line(
                    "Last updated",
                    status.providerUpdatedAt?.let { "${Formatters.dateTime(it)} (provider)" }
                        ?: "${Formatters.deviceDateTime(status.fetchedAtMillis)} (received)",
                )
            }
        }
    }
}

@Composable
private fun PassengerCard(index: Int, passenger: PassengerStatus) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Passenger ${passenger.serial ?: (index + 1)}", style = MaterialTheme.typography.titleSmall)
            Line("Booking status", passenger.bookingStatus ?: "Not reported")
            Line("Current status", passenger.currentStatus ?: "Not reported")
            Line("Coach / berth", listOfNotNull(passenger.coach, passenger.berth).joinToString(" / ").ifEmpty { "Not allotted yet" })
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
