package dev.elainedb.ytdash_android_codex.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.elainedb.ytdash_android_codex.repository.FilterOptions
import dev.elainedb.ytdash_android_codex.repository.SortOption

@Composable
fun FilterDialog(
    currentFilter: FilterOptions,
    channels: List<String>,
    countries: List<String>,
    onDismiss: () -> Unit,
    onApply: (FilterOptions) -> Unit,
) {
    val selectedChannel = remember(currentFilter.channelName) { mutableStateOf(currentFilter.channelName) }
    val selectedCountry = remember(currentFilter.country) { mutableStateOf(currentFilter.country) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Videos") },
        text = {
            Column {
                Text("Channel")
                SelectableList(
                    options = listOf(null) + channels,
                    selected = selectedChannel.value,
                    allLabel = "All Channels",
                    onSelect = { selectedChannel.value = it }
                )
                Text("Country")
                SelectableList(
                    options = listOf(null) + countries,
                    selected = selectedCountry.value,
                    allLabel = "All Countries",
                    onSelect = { selectedCountry.value = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onApply(FilterOptions(selectedChannel.value, selectedCountry.value))
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SortDialog(
    currentSort: SortOption,
    onDismiss: () -> Unit,
    onApply: (SortOption) -> Unit,
) {
    val selected = remember(currentSort) { mutableStateOf(currentSort) }
    val labels = mapOf(
        SortOption.PUBLISHED_NEWEST to "Publication Date (Newest First)",
        SortOption.PUBLISHED_OLDEST to "Publication Date (Oldest First)",
        SortOption.RECORDING_NEWEST to "Recording Date (Newest First)",
        SortOption.RECORDING_OLDEST to "Recording Date (Oldest First)",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort Videos") },
        text = {
            Column {
                SortOption.entries.forEach { option ->
                    RowOption(
                        selected = selected.value == option,
                        label = labels.getValue(option),
                        onClick = { selected.value = option }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(selected.value) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SelectableList(
    options: List<String?>,
    selected: String?,
    allLabel: String,
    onSelect: (String?) -> Unit,
) {
    Column {
        options.forEach { option ->
            RowOption(
                selected = selected == option,
                label = option ?: allLabel,
                onClick = { onSelect(option) }
            )
        }
    }
}

@Composable
private fun RowOption(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label)
    }
}
