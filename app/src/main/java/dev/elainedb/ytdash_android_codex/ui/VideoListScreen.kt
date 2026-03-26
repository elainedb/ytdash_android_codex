@file:OptIn(ExperimentalLayoutApi::class)

package dev.elainedb.ytdash_android_codex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.elainedb.ytdash_android_codex.R
import dev.elainedb.ytdash_android_codex.model.Video
import dev.elainedb.ytdash_android_codex.viewmodel.FilterOptions
import dev.elainedb.ytdash_android_codex.viewmodel.SortOption
import dev.elainedb.ytdash_android_codex.viewmodel.VideoListUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VideoListScreen(
    uiState: VideoListUiState,
    filterOptions: FilterOptions,
    sortOption: SortOption,
    availableCountries: List<String>,
    availableChannels: List<String>,
    onRefresh: () -> Unit,
    onOpenMap: () -> Unit,
    onApplyFilter: (String?, String?) -> Unit,
    onApplySorting: (SortOption) -> Unit,
    onClearFilters: () -> Unit,
    onLogout: () -> Unit,
    onVideoClick: (Video) -> Unit
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.video_list_title)) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.logout))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ControlRow(
                onRefresh = onRefresh,
                onOpenMap = onOpenMap,
                onFilter = { showFilterDialog = true },
                onSort = { showSortDialog = true }
            )
            Spacer(modifier = Modifier.height(12.dp))
            VideoCountHeader(uiState = uiState, filterOptions = filterOptions)
            Spacer(modifier = Modifier.height(8.dp))

            when (uiState) {
                VideoListUiState.Empty -> EmptyState(text = stringResource(R.string.no_videos), onRefresh = onRefresh)
                is VideoListUiState.Error -> EmptyState(text = uiState.message, onRefresh = onRefresh)
                VideoListUiState.Loading -> LoadingState()
                is VideoListUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.videos, key = { it.id }) { video ->
                            VideoCard(video = video, onClick = { onVideoClick(video) })
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            currentFilter = filterOptions,
            availableChannels = availableChannels,
            availableCountries = availableCountries,
            onApply = { channel, country ->
                onApplyFilter(channel, country)
                showFilterDialog = false
            },
            onClear = {
                onClearFilters()
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    if (showSortDialog) {
        SortDialog(
            currentSortOption = sortOption,
            onApply = {
                onApplySorting(it)
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }
}

@Composable
private fun ControlRow(
    onRefresh: () -> Unit,
    onOpenMap: () -> Unit,
    onFilter: () -> Unit,
    onSort: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onRefresh, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.refresh)) }
        Button(onClick = onOpenMap, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.view_map)) }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onFilter, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.filter)) }
        Button(onClick = onSort, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.sort)) }
    }
}

@Composable
private fun VideoCountHeader(uiState: VideoListUiState, filterOptions: FilterOptions) {
    if (uiState !is VideoListUiState.Success) {
        return
    }

    val hasFilters = filterOptions.channelName != null || filterOptions.country != null
    val label = if (hasFilters) {
        stringResource(R.string.showing_count, uiState.videos.size, uiState.totalCount)
    } else {
        stringResource(R.string.total_videos, uiState.totalCount)
    }
    Text(text = label, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun VideoCard(video: Video, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(video.title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(video.channelName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            MetadataLine(label = stringResource(R.string.publication_date), value = video.publishedAt.take(10))
            video.recordingDate?.take(10)?.let {
                MetadataLine(label = stringResource(R.string.recording_date), value = it)
            }
            formatLocation(video)?.let {
                MetadataLine(label = stringResource(R.string.location), value = it)
            }
            if (video.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    video.tags.take(6).forEach { tag ->
                        AssistChip(onClick = onClick, label = { Text(tag) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataLine(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(12.dp))
        Text(stringResource(R.string.loading_videos))
    }
}

@Composable
private fun EmptyState(text: String, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = text)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRefresh) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
fun FilterDialog(
    currentFilter: FilterOptions,
    availableChannels: List<String>,
    availableCountries: List<String>,
    onApply: (String?, String?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedChannel by remember(currentFilter) { mutableStateOf(currentFilter.channelName) }
    var selectedCountry by remember(currentFilter) { mutableStateOf(currentFilter.country) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.filter_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.filter))
                FilterGroup(
                    entries = listOf(null) + availableChannels,
                    selected = selectedChannel,
                    allLabel = stringResource(R.string.all_channels),
                    onSelected = { selectedChannel = it }
                )
                FilterGroup(
                    entries = listOf(null) + availableCountries,
                    selected = selectedCountry,
                    allLabel = stringResource(R.string.all_countries),
                    onSelected = { selectedCountry = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(selectedChannel, selectedCountry) }) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text(stringResource(R.string.clear_filters)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}

@Composable
private fun FilterGroup(
    entries: List<String?>,
    selected: String?,
    allLabel: String,
    onSelected: (String?) -> Unit
) {
    Column {
        entries.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(option) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected == option, onClick = { onSelected(option) })
                Text(option ?: allLabel)
            }
        }
    }
}

@Composable
fun SortDialog(
    currentSortOption: SortOption,
    onApply: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember(currentSortOption) { mutableStateOf(currentSortOption) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sort_dialog_title)) },
        text = {
            Column {
                SortOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = option },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOption == option,
                            onClick = { selectedOption = option }
                        )
                        Text(stringResource(option.labelRes))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(selectedOption) }) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

internal fun formatLocation(video: Video): String? {
    val place = listOfNotNull(video.locationCity, video.locationCountry).joinToString(", ")
    val coordinates = if (video.locationLatitude != null && video.locationLongitude != null) {
        "(${video.locationLatitude}, ${video.locationLongitude})"
    } else {
        null
    }
    return listOfNotNull(place.takeIf { it.isNotBlank() }, coordinates).joinToString(" ").ifBlank { null }
}
