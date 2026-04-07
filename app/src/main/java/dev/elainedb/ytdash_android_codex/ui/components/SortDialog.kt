package dev.elainedb.ytdash_android_codex.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.elainedb.ytdash_android_codex.domain.model.SortOption

@Composable
fun SortDialog(
    currentOption: SortOption,
    onApply: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedOption = remember { mutableStateOf(currentOption) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort Videos") },
        text = {
            Column {
                SortOption.entries.forEach { option ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.selectable(
                            selected = selectedOption.value == option,
                            onClick = { selectedOption.value = option }
                        )
                    ) {
                        RadioButton(
                            selected = selectedOption.value == option,
                            onClick = { selectedOption.value = option }
                        )
                        Text(option.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(selectedOption.value) }) {
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
