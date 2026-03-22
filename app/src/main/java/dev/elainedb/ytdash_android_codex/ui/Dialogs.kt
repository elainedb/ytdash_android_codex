package dev.elainedb.ytdash_android_codex.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.elainedb.ytdash_android_codex.R
import dev.elainedb.ytdash_android_codex.model.SortOption

@Composable
fun FilterDialog(
    channels: List<String>,
    countries: List<String>,
    initialChannel: String?,
    initialCountry: String?,
    onDismiss: () -> Unit,
    onApply: (String?, String?) -> Unit,
) {
    val selectedChannel = remember(initialChannel) { mutableStateOf(initialChannel) }
    val selectedCountry = remember(initialCountry) { mutableStateOf(initialCountry) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onApply(selectedChannel.value, selectedCountry.value) }
            ) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(stringResource(R.string.filter_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterSection(
                    title = stringResource(R.string.channel_filter_label),
                    options = listOf(null to stringResource(R.string.all_channels)) + channels.map { it to it },
                    selected = selectedChannel.value,
                    onSelect = { selectedChannel.value = it }
                )
                FilterSection(
                    title = stringResource(R.string.country_filter_label),
                    options = listOf(null to stringResource(R.string.all_countries)) + countries.map { it to it },
                    selected = selectedCountry.value,
                    onSelect = { selectedCountry.value = it }
                )
            }
        }
    )
}

@Composable
fun SortDialog(
    initialSortOption: SortOption,
    onDismiss: () -> Unit,
    onApply: (SortOption) -> Unit,
) {
    val selected = remember(initialSortOption) { mutableStateOf(initialSortOption) }
    val options = listOf(
        SortOption.PUBLICATION_DATE_DESC to stringResource(R.string.publication_newest),
        SortOption.PUBLICATION_DATE_ASC to stringResource(R.string.publication_oldest),
        SortOption.RECORDING_DATE_DESC to stringResource(R.string.recording_newest),
        SortOption.RECORDING_DATE_ASC to stringResource(R.string.recording_oldest),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onApply(selected.value) }) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(stringResource(R.string.sort_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (option, label) ->
                    FilterOptionRow(
                        label = label,
                        selected = selected.value == option,
                        onClick = { selected.value = option }
                    )
                }
            }
        }
    )
}

@Composable
private fun FilterSection(
    title: String,
    options: List<Pair<String?, String>>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title)
        options.forEach { (value, label) ->
            FilterOptionRow(
                label = label,
                selected = selected == value,
                onClick = { onSelect(value) }
            )
        }
    }
}

@Composable
private fun FilterOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(top = 12.dp))
    }
}
