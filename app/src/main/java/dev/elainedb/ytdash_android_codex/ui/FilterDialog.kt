package dev.elainedb.ytdash_android_codex.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun FilterDialog(
    availableChannels: List<String>,
    availableCountries: List<String>,
    selectedChannel: String?,
    selectedCountry: String?,
    onDismiss: () -> Unit,
    onApply: (String?, String?) -> Unit
) {
    var channel by remember(selectedChannel) { mutableStateOf(selectedChannel) }
    var country by remember(selectedCountry) { mutableStateOf(selectedCountry) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onApply(channel, country) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Filter Videos") },
        text = {
            Column {
                Text("Channel")
                FilterRadioOption("All Channels", channel == null) { channel = null }
                availableChannels.forEach { item ->
                    FilterRadioOption(item, channel == item) { channel = item }
                }
                Text("Country")
                FilterRadioOption("All Countries", country == null) { country = null }
                availableCountries.forEach { item ->
                    FilterRadioOption(item, country == item) { country = item }
                }
            }
        }
    )
}

@Composable
private fun FilterRadioOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(text = label)
    }
}
