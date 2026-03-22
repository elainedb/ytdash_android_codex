package dev.elainedb.ytdash_android_codex.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.elainedb.ytdash_android_codex.viewmodel.VideoListViewModel

@Composable
fun VideoListRoute(
    viewModel: VideoListViewModel,
    onOpenMap: () -> Unit,
    onLogout: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.filterOptions.collectAsStateWithLifecycle()
    val sort by viewModel.sortOption.collectAsStateWithLifecycle()
    val countries by viewModel.availableCountries.collectAsStateWithLifecycle()
    val channels by viewModel.availableChannels.collectAsStateWithLifecycle()

    VideoListScreen(
        uiState = uiState,
        currentFilter = filter,
        currentSort = sort,
        availableChannels = channels,
        availableCountries = countries,
        onRefresh = viewModel::refresh,
        onOpenMap = onOpenMap,
        onApplyFilter = viewModel::applyFilter,
        onApplySort = viewModel::applySorting,
        onLogout = onLogout,
    )
}
