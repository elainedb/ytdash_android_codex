package dev.elainedb.ytdash_android_codex.presentation.videolist

import dev.elainedb.ytdash_android_codex.domain.model.Video

sealed class VideoListUiState {
    data object Loading : VideoListUiState()
    data object Empty : VideoListUiState()
    data class Success(
        val videos: List<Video>,
        val totalCount: Int
    ) : VideoListUiState()
    data class Error(val message: String) : VideoListUiState()
}
