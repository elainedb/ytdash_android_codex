package dev.elainedb.ytdash_android_codex.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.elainedb.ytdash_android_codex.domain.model.FilterOptions

@Composable
fun FilterDialog(
    initialValue: FilterOptions,
    channels: List<String>,
    countries: List<String>,
    onApply: (FilterOptions) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedChannel = remember { mutableStateOf(initialValue.channelName) }
    val selectedCountry = remember { mutableStateOf(initialValue.country) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Videos") },
        text = {
            Column {
                Text("Channel")
                RadioOption(
                    label = "All Channels",
                    selected = selectedChannel.value == null,
                    onSelected = { selectedChannel.value = null }
                )
                channels.forEach { channel ->
                    RadioOption(
                        label = channel,
                        selected = selectedChannel.value == channel,
                        onSelected = { selectedChannel.value = channel }
                    )
                }
                Text("Country")
                RadioOption(
                    label = "All Countries",
                    selected = selectedCountry.value == null,
                    onSelected = { selectedCountry.value = null }
                )
                countries.forEach { country ->
                    RadioOption(
                        label = country,
                        selected = selectedCountry.value == country,
                        onSelected = { selectedCountry.value = country }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(
                        FilterOptions(
                            channelName = selectedChannel.value,
                            country = selectedCountry.value
                        )
                    )
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RadioOption(
    label: String,
    selected: Boolean,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier.selectable(
            selected = selected,
            onClick = onSelected
        )
    ) {
        RadioButton(selected = selected, onClick = onSelected)
        Text(label)
    }
}
