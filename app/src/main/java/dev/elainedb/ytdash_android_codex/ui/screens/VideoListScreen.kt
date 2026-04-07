package dev.elainedb.ytdash_android_codex.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.elainedb.ytdash_android_codex.MapActivity
import dev.elainedb.ytdash_android_codex.domain.model.FilterOptions
import dev.elainedb.ytdash_android_codex.domain.model.SortOption
import dev.elainedb.ytdash_android_codex.domain.model.Video
import dev.elainedb.ytdash_android_codex.presentation.model.VideoListUiState
import dev.elainedb.ytdash_android_codex.ui.components.FilterDialog
import dev.elainedb.ytdash_android_codex.ui.components.SortDialog

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun VideoListScreen(
    uiState: VideoListUiState,
    filterOptions: FilterOptions,
    sortOption: SortOption,
    availableCountries: List<String>,
    availableChannels: List<String>,
    onRefresh: () -> Unit,
    onFilterApply: (FilterOptions) -> Unit,
    onSortApply: (SortOption) -> Unit,
    onClearFilters: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YouTube Dashboard") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onRefresh) { Text("Refresh") }
                Button(onClick = { context.startActivity(MapActivity.newIntent(context)) }) { Text("View Map") }
                Button(onClick = { showFilterDialog = true }) { Text("Filter") }
                Button(onClick = { showSortDialog = true }) { Text("Sort") }
            }

            if (filterOptions.channelName != null || filterOptions.country != null) {
                TextButton(onClick = onClearFilters) {
                    Text("Clear Filters")
                }
            }

            when (uiState) {
                VideoListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
                }
                VideoListUiState.Empty -> {
                    Text("No videos found.", modifier = Modifier.padding(top = 24.dp))
                }
                is VideoListUiState.Error -> {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
                is VideoListUiState.Success -> {
                    val filteredCount = uiState.videos.size
                    val summary = if (filteredCount == uiState.totalCount) {
                        "Showing $filteredCount videos"
                    } else {
                        "Showing $filteredCount of ${uiState.totalCount} videos"
                    }
                    Text(summary, modifier = Modifier.padding(vertical = 12.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.videos, key = { it.id }) { video ->
                            VideoCard(video = video, onClick = { openVideo(context, video.id) })
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            initialValue = filterOptions,
            channels = availableChannels,
            countries = availableCountries,
            onApply = {
                showFilterDialog = false
                onFilterApply(it)
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    if (showSortDialog) {
        SortDialog(
            currentOption = sortOption,
            onApply = {
                showSortDialog = false
                onSortApply(it)
            },
            onDismiss = { showSortDialog = false }
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun VideoCard(video: Video, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
            Text(video.title, style = MaterialTheme.typography.titleMedium)
            Text(video.channelName, style = MaterialTheme.typography.bodyMedium)
            Text(video.publishedAt.take(10), style = MaterialTheme.typography.bodySmall)

            if (video.tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    video.tags.take(6).forEach { tag ->
                        Text(
                            text = tag,
                            modifier = Modifier
                                .background(Color(0xFFE7E0EC), MaterialTheme.shapes.small)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            val locationText = buildString {
                val cityCountry = listOfNotNull(video.locationCity, video.locationCountry).joinToString(", ")
                if (cityCountry.isNotBlank()) append(cityCountry)
                if (video.locationLatitude != null && video.locationLongitude != null) {
                    if (isNotBlank()) append(" • ")
                    append("${video.locationLatitude}, ${video.locationLongitude}")
                }
            }
            if (locationText.isNotBlank()) {
                Text(locationText, modifier = Modifier.padding(top = 8.dp))
            }
            video.recordingDate?.let {
                Text("Recorded: ${it.take(10)}", modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

private fun openVideo(context: android.content.Context, videoId: String) {
    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
    runCatching { context.startActivity(appIntent) }
        .onFailure { context.startActivity(webIntent) }
}
