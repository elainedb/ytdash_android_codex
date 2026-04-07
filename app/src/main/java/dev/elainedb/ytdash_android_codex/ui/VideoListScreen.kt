package dev.elainedb.ytdash_android_codex.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.elainedb.ytdash_android_codex.core.utils.DateUtils
import dev.elainedb.ytdash_android_codex.domain.model.Video
import dev.elainedb.ytdash_android_codex.presentation.videolist.FilterOptions
import dev.elainedb.ytdash_android_codex.presentation.videolist.SortOption
import dev.elainedb.ytdash_android_codex.presentation.videolist.VideoListUiState

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
    onApplyFilter: (FilterOptions) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onClearFilters: () -> Unit,
    onOpenVideo: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var showSortDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("YouTube Dash") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ControlsRow(
                onRefresh = onRefresh,
                onOpenMap = onOpenMap,
                onFilter = { showFilterDialog = true },
                onSort = { showSortDialog = true }
            )
            Spacer(modifier = Modifier.height(12.dp))
            SummaryRow(
                uiState = uiState,
                filterOptions = filterOptions,
                onClearFilters = onClearFilters
            )
            Spacer(modifier = Modifier.height(12.dp))
            when (uiState) {
                VideoListUiState.Loading -> LoadingState()
                VideoListUiState.Empty -> EmptyState()
                is VideoListUiState.Error -> ErrorState(uiState.message)
                is VideoListUiState.Success -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(uiState.videos, key = { it.id }) { video ->
                            VideoCard(video = video, onClick = { onOpenVideo(video.id) })
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            current = filterOptions,
            countries = availableCountries,
            channels = availableChannels,
            onDismiss = { showFilterDialog = false },
            onApply = {
                onApplyFilter(it)
                showFilterDialog = false
            }
        )
    }

    if (showSortDialog) {
        SortDialog(
            current = sortOption,
            onDismiss = { showSortDialog = false },
            onApply = {
                onSortChange(it)
                showSortDialog = false
            }
        )
    }
}

@Composable
private fun ControlsRow(
    onRefresh: () -> Unit,
    onOpenMap: () -> Unit,
    onFilter: () -> Unit,
    onSort: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SmallActionButton("Refresh", Icons.Default.Refresh, onRefresh)
        SmallActionButton("View Map", Icons.Default.Map, onOpenMap)
        SmallActionButton("Filter", Icons.Default.FilterAlt, onFilter)
        SmallActionButton("Sort", Icons.AutoMirrored.Filled.Sort, onSort)
    }
}

@Composable
private fun RowScope.SmallActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.size(6.dp))
        Text(label)
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun SummaryRow(
    uiState: VideoListUiState,
    filterOptions: FilterOptions,
    onClearFilters: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val summary = when (uiState) {
            is VideoListUiState.Success -> {
                if (filterOptions.channelName != null || filterOptions.country != null) {
                    "Showing ${uiState.videos.size} of ${uiState.totalCount} videos"
                } else {
                    "Total videos: ${uiState.totalCount}"
                }
            }
            else -> null
        }
        summary?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filterOptions.channelName?.let {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text(it) }
                )
            }
            filterOptions.country?.let {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text(it) }
                )
            }
            if (filterOptions.channelName != null || filterOptions.country != null) {
                OutlinedButton(onClick = onClearFilters) {
                    Text("Clear Filters")
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun VideoCard(
    video: Video,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(video.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(video.channelName, style = MaterialTheme.typography.bodyMedium)
            DateUtils.formatIsoDateTime(video.publishedAt)?.let {
                Text("Published $it", style = MaterialTheme.typography.bodySmall)
            }
            video.recordingDate?.let {
                DateUtils.formatIsoDate(it)?.let { value ->
                    Text("Recorded $value", style = MaterialTheme.typography.bodySmall)
                }
            }
            val locationLabel = listOfNotNull(video.locationCity, video.locationCountry).joinToString(", ")
            if (locationLabel.isNotBlank()) {
                Text(locationLabel, style = MaterialTheme.typography.bodySmall)
            }
            if (video.locationLatitude != null && video.locationLongitude != null) {
                Text(
                    "Lat ${"%.4f".format(video.locationLatitude)}, Lng ${"%.4f".format(video.locationLongitude)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (video.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    video.tags.take(8).forEach { tag ->
                        AssistChip(onClick = {}, label = { Text(tag) })
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No videos found.")
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun FilterDialog(
    current: FilterOptions,
    countries: List<String>,
    channels: List<String>,
    onDismiss: () -> Unit,
    onApply: (FilterOptions) -> Unit
) {
    var selectedChannel by remember(current) { mutableStateOf(current.channelName) }
    var selectedCountry by remember(current) { mutableStateOf(current.country) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Videos") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Channel", fontWeight = FontWeight.Bold)
                RadioGroup(
                    options = listOf<String?>(null) + channels,
                    current = selectedChannel,
                    label = { it ?: "All Channels" },
                    onSelect = { selectedChannel = it }
                )
                Text("Country", fontWeight = FontWeight.Bold)
                RadioGroup(
                    options = listOf<String?>(null) + countries,
                    current = selectedCountry,
                    label = { it ?: "All Countries" },
                    onSelect = { selectedCountry = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onApply(FilterOptions(selectedChannel, selectedCountry)) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SortDialog(
    current: SortOption,
    onDismiss: () -> Unit,
    onApply: (SortOption) -> Unit
) {
    var selectedOption by remember(current) { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort Videos") },
        text = {
            RadioGroup(
                options = SortOption.entries,
                current = selectedOption,
                label = {
                    when (it) {
                        SortOption.PUBLISHED_NEWEST -> "Publication Date (Newest First)"
                        SortOption.PUBLISHED_OLDEST -> "Publication Date (Oldest First)"
                        SortOption.RECORDING_NEWEST -> "Recording Date (Newest First)"
                        SortOption.RECORDING_OLDEST -> "Recording Date (Oldest First)"
                    }
                },
                onSelect = { selectedOption = it }
            )
        },
        confirmButton = {
            Button(onClick = { onApply(selectedOption) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun <T> RadioGroup(
    options: List<T>,
    current: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(option) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = option == current, onClick = { onSelect(option) })
                Text(label(option))
            }
        }
    }
}
