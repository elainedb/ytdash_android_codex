package dev.elainedb.ytdash_android_codex.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.elainedb.ytdash_android_codex.model.Video
import dev.elainedb.ytdash_android_codex.repository.FilterOptions
import dev.elainedb.ytdash_android_codex.repository.SortOption
import dev.elainedb.ytdash_android_codex.ui.components.FilterDialog
import dev.elainedb.ytdash_android_codex.ui.components.SortDialog
import dev.elainedb.ytdash_android_codex.utils.DateUtils
import dev.elainedb.ytdash_android_codex.utils.openVideo
import dev.elainedb.ytdash_android_codex.viewmodel.VideoListUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen(
    uiState: VideoListUiState,
    currentFilter: FilterOptions,
    currentSort: SortOption,
    availableChannels: List<String>,
    availableCountries: List<String>,
    onRefresh: () -> Unit,
    onOpenMap: () -> Unit,
    onApplyFilter: (FilterOptions) -> Unit,
    onApplySort: (SortOption) -> Unit,
    onLogout: () -> Unit,
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YT Dash") },
                actions = {
                    Button(onClick = onLogout) { Text("Logout") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefresh) { Text("Refresh") }
                Button(onClick = onOpenMap) { Text("View Map") }
                Button(onClick = { showFilterDialog = true }) { Text("Filter") }
                Button(onClick = { showSortDialog = true }) { Text("Sort") }
            }

            when (uiState) {
                VideoListUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                VideoListUiState.Empty -> {
                    Text("No videos available.")
                }

                is VideoListUiState.Error -> {
                    Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
                }

                is VideoListUiState.Success -> {
                    if (currentFilter.channelName != null || currentFilter.country != null) {
                        Text("Showing ${uiState.videos.size} of ${uiState.totalCount} videos")
                    } else {
                        Text("Showing ${uiState.totalCount} videos")
                    }
                    VideoList(videos = uiState.videos)
                }
            }
        }

        if (showFilterDialog) {
            FilterDialog(
                currentFilter = currentFilter,
                channels = availableChannels,
                countries = availableCountries,
                onDismiss = { showFilterDialog = false },
                onApply = {
                    showFilterDialog = false
                    onApplyFilter(it)
                }
            )
        }

        if (showSortDialog) {
            SortDialog(
                currentSort = currentSort,
                onDismiss = { showSortDialog = false },
                onApply = {
                    showSortDialog = false
                    onApplySort(it)
                }
            )
        }
    }
}

@Composable
private fun VideoList(videos: List<Video>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(videos, key = { it.id }) { video ->
            VideoCard(video = video)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoCard(video: Video) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { openVideo(context, video.id) }
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier.fillMaxWidth()
            )
            Text(video.title, style = MaterialTheme.typography.titleMedium)
            Text(video.channelName, style = MaterialTheme.typography.bodyMedium)
            Text(DateUtils.formatIsoDate(video.publishedAt), style = MaterialTheme.typography.bodySmall)
            if (video.tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    video.tags.take(6).forEach { tag ->
                        AssistChip(onClick = {}, label = { Text(tag) })
                    }
                }
            }
            val locationParts = listOfNotNull(video.locationCity, video.locationCountry)
            if (locationParts.isNotEmpty() || (video.locationLatitude != null && video.locationLongitude != null)) {
                Text(
                    buildString {
                        if (locationParts.isNotEmpty()) {
                            append(locationParts.joinToString(", "))
                        }
                        if (video.locationLatitude != null && video.locationLongitude != null) {
                            if (isNotEmpty()) append(" ")
                            append("(${video.locationLatitude}, ${video.locationLongitude})")
                        }
                    }
                )
            }
            if (!video.recordingDate.isNullOrBlank()) {
                Text("Recorded ${DateUtils.formatIsoDate(video.recordingDate)}")
            }
        }
    }
}
