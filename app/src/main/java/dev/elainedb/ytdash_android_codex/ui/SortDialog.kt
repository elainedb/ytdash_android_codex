package dev.elainedb.ytdash_android_codex.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import dev.elainedb.ytdash_android_codex.domain.model.SortOption

@Composable
fun SortDialog(
    selectedOption: SortOption,
    onDismiss: () -> Unit,
    onApply: (SortOption) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Sort Videos") },
        text = {
            androidx.compose.foundation.layout.Column {
                SortOption.entries.forEach { option ->
                    Row(modifier = androidx.compose.ui.Modifier.fillMaxWidth()) {
                        RadioButton(
                            selected = selectedOption == option,
                            onClick = { onApply(option) }
                        )
                        Text(option.label)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
