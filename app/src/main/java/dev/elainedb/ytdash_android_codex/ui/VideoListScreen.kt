package dev.elainedb.ytdash_android_codex.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.elainedb.ytdash_android_codex.MapActivity
import dev.elainedb.ytdash_android_codex.R
import dev.elainedb.ytdash_android_codex.model.FilterOptions
import dev.elainedb.ytdash_android_codex.model.SortOption
import dev.elainedb.ytdash_android_codex.model.Video
import dev.elainedb.ytdash_android_codex.utils.DateUtils
import dev.elainedb.ytdash_android_codex.utils.VideoIntentHelper
import dev.elainedb.ytdash_android_codex.viewmodel.VideoListUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VideoListScreen(
    uiState: VideoListUiState,
    filterOptions: FilterOptions,
    sortOption: SortOption,
    availableCountries: List<String>,
    availableChannels: List<String>,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onApplyFilter: (String?, String?) -> Unit,
    onApplySort: (SortOption) -> Unit,
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showFilterDialog) {
        FilterDialog(
            channels = availableChannels,
            countries = availableCountries,
            initialChannel = filterOptions.channelName,
            initialCountry = filterOptions.country,
            onDismiss = { showFilterDialog = false },
            onApply = { channel, country ->
                onApplyFilter(channel, country)
                showFilterDialog = false
            }
        )
    }

    if (showSortDialog) {
        SortDialog(
            initialSortOption = sortOption,
            onDismiss = { showSortDialog = false },
            onApply = { selectedSort ->
                onApplySort(selectedSort)
                showSortDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    Button(onClick = onLogout) {
                        Text(stringResource(R.string.logout))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefresh) { Text(stringResource(R.string.refresh)) }
                Button(onClick = { context.startActivity(MapActivity.newIntent(context)) }) {
                    Text(stringResource(R.string.view_map))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showFilterDialog = true }) { Text(stringResource(R.string.filter)) }
                Button(onClick = { showSortDialog = true }) { Text(stringResource(R.string.sort)) }
            }

            when (uiState) {
                VideoListUiState.Loading -> {
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

                VideoListUiState.Empty -> {
                    Text(stringResource(R.string.no_videos_found))
                }

                is VideoListUiState.Error -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = uiState.message,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onRefresh) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }

                is VideoListUiState.Success -> {
                    val filtersActive = filterOptions.channelName != null || filterOptions.country != null
                    val countText = if (filtersActive) {
                        stringResource(
                            R.string.video_count_filtered,
                            uiState.videos.size,
                            uiState.totalCount
                        )
                    } else {
                        stringResource(R.string.video_count_all, uiState.totalCount)
                    }

                    Text(
                        text = countText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.videos, key = { it.id }) { video ->
                            VideoCard(video = video) {
                                VideoIntentHelper.openVideo(context, video.id)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoCard(
    video: Video,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            Text(video.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(video.channelName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${stringResource(R.string.publication_date_label)}: ${DateUtils.toDisplayDate(video.publishedAt).orEmpty()}",
                style = MaterialTheme.typography.bodySmall
            )

            if (video.tags.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.tags_label), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        video.tags.forEach { tag ->
                            AssistChip(onClick = {}, label = { Text(tag) })
                        }
                    }
                }
            }

            val location = buildLocationLabel(video)
            if (location != null) {
                Text(
                    text = "${stringResource(R.string.location_label)}: $location",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            DateUtils.toDisplayDate(video.recordingDate)?.let { recordingDate ->
                Text(
                    text = "${stringResource(R.string.recording_date_label)}: $recordingDate",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun buildLocationLabel(video: Video): String? {
    val parts = listOfNotNull(video.locationCity, video.locationCountry).joinToString(", ")
    val coordinates = if (video.locationLatitude != null && video.locationLongitude != null) {
        "${video.locationLatitude}, ${video.locationLongitude}"
    } else {
        null
    }

    return listOfNotNull(parts.ifBlank { null }, coordinates).joinToString(" | ").ifBlank { null }
}
