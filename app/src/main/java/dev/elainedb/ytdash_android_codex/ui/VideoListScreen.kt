package dev.elainedb.ytdash_android_codex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.elainedb.ytdash_android_codex.domain.model.FilterOptions
import dev.elainedb.ytdash_android_codex.domain.model.SortOption
import dev.elainedb.ytdash_android_codex.domain.model.Video
import dev.elainedb.ytdash_android_codex.presentation.VideoListUiState
import dev.elainedb.ytdash_android_codex.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen(
    uiState: VideoListUiState,
    filterOptions: FilterOptions,
    sortOption: SortOption,
    availableChannels: List<String>,
    availableCountries: List<String>,
    onRefresh: () -> Unit,
    onViewMap: () -> Unit,
    onFilterApply: (String?, String?) -> Unit,
    onSortApply: (SortOption) -> Unit,
    onClearFilters: () -> Unit,
    onLogout: () -> Unit,
    onOpenVideo: (String) -> Unit
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YouTube Dashboard") },
                actions = {
                    Button(onClick = onLogout) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = onRefresh) { Text("Refresh") }
                Button(onClick = onViewMap) { Text("View Map") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = { showFilterDialog = true }) { Text("Filter") }
                Button(onClick = { showSortDialog = true }) { Text("Sort") }
                if (filterOptions.hasActiveFilters) {
                    Button(onClick = onClearFilters) { Text("Clear") }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            when (uiState) {
                VideoListUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                VideoListUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No videos found.")
                    }
                }
                is VideoListUiState.Error -> {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is VideoListUiState.Success -> {
                    val showingText = if (filterOptions.hasActiveFilters) {
                        "Showing ${uiState.videos.size} of ${uiState.totalCount} videos"
                    } else {
                        "Showing ${uiState.totalCount} videos"
                    }
                    Text(showingText, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.videos, key = { it.id }) { video ->
                            VideoRow(video = video, onOpenVideo = onOpenVideo)
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            availableChannels = availableChannels,
            availableCountries = availableCountries,
            selectedChannel = filterOptions.channelName,
            selectedCountry = filterOptions.country,
            onDismiss = { showFilterDialog = false },
            onApply = { channel, country ->
                showFilterDialog = false
                onFilterApply(channel, country)
            }
        )
    }

    if (showSortDialog) {
        SortDialog(
            selectedOption = sortOption,
            onDismiss = { showSortDialog = false },
            onApply = { option ->
                showSortDialog = false
                onSortApply(option)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoRow(video: Video, onOpenVideo: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onOpenVideo(video.id) }
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier
                    .size(120.dp, 90.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(video.title, fontWeight = FontWeight.Bold)
                Text(video.channelName)
                Text("Published ${DateFormatter.toDisplayDate(video.publishedAt)}")
                video.recordingDate?.let {
                    Text("Recorded ${DateFormatter.toDisplayDate(it)}")
                }
                val locationLine = buildString {
                    if (!video.locationCity.isNullOrBlank()) append(video.locationCity)
                    if (!video.locationCountry.isNullOrBlank()) {
                        if (isNotBlank()) append(", ")
                        append(video.locationCountry)
                    }
                }
                if (locationLine.isNotBlank()) {
                    Text(locationLine)
                }
                if (video.locationLatitude != null && video.locationLongitude != null) {
                    Text("GPS ${video.locationLatitude}, ${video.locationLongitude}")
                }
            }
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
