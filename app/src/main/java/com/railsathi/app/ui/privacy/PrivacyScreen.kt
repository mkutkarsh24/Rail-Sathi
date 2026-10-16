package com.railsathi.app.ui.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.railsathi.app.ui.components.BackTitleBar
import com.railsathi.app.ui.components.SectionTitle

/** Summarises PRIVACY_POLICY.md in the repository root. If you change one, update the other. */
private val sections = listOf(
    "What this app does" to
        "RailSathi shows train running status, seat availability and PNR status. It gets that " +
        "information from a railway data service configured by the app's publisher.",
    "Data stored on your device" to
        "Your favourite trains, your train search history, and the last successful running status " +
        "for trains you looked up (so it can be shown offline, marked with its update time). " +
        "This data stays on your device. You can remove favourites and clear history in the app, " +
        "or clear everything by clearing the app's storage in Android settings.",
    "Data sent over the network" to
        "The train numbers, station codes, dates and PNR numbers you enter are sent over HTTPS " +
        "to the data service to get results. Your PNR is sent in the request body, is never " +
        "saved on your device, and is never written to logs.",
    "What the app does not collect" to
        "No account, name, phone number, email address, location, contacts, advertising ID or " +
        "analytics. The app requests only internet and network-state permissions.",
    "Passenger information" to
        "PNR results are held in memory while you view them and discarded afterwards. They are " +
        "not saved or cached.",
    "Data providers" to
        "The data service you connect to has its own terms and privacy practices. The app " +
        "publisher is responsible for choosing a provider whose terms permit this use.",
    "Sample data mode" to
        "Builds without a configured data service show clearly labelled sample data that does " +
        "not describe real trains or bookings.",
)

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        BackTitleBar(title = "Privacy", onBack = onBack)
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            sections.forEach { (title, body) ->
                SectionTitle(title)
                Text(body, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
