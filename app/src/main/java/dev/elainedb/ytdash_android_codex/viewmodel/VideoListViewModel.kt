package dev.elainedb.ytdash_android_codex.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.elainedb.ytdash_android_codex.model.FilterOptions
import dev.elainedb.ytdash_android_codex.model.SortOption
import dev.elainedb.ytdash_android_codex.model.Video
import dev.elainedb.ytdash_android_codex.repository.YouTubeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class VideoListUiState {
    data object Loading : VideoListUiState()
    data object Empty : VideoListUiState()
    data class Success(val videos: List<Video>, val totalCount: Int) : VideoListUiState()
    data class Error(val message: String) : VideoListUiState()
}

class VideoListViewModel(
    private val repository: YouTubeRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<VideoListUiState>(VideoListUiState.Loading)
    val uiState: StateFlow<VideoListUiState> = _uiState

    private val _filterOptions = MutableStateFlow(FilterOptions())
    val filterOptions: StateFlow<FilterOptions> = _filterOptions

    private val _sortOption = MutableStateFlow(SortOption.PUBLICATION_DATE_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption

    val availableCountries: StateFlow<List<String>> = repository.getDistinctCountries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableChannels: StateFlow<List<String>> = repository.getDistinctChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val totalCount: StateFlow<Int> = repository.getTotalVideoCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        observeVideoChanges()
        loadVideos()
    }

    fun refreshVideos() {
        viewModelScope.launch {
            _uiState.value = VideoListUiState.Loading
            repository.refreshVideos()
                .onFailure { _uiState.value = VideoListUiState.Error(it.message ?: "Failed to refresh videos.") }
        }
    }

    fun applyFilter(channelName: String?, country: String?) {
        _filterOptions.value = FilterOptions(
            channelName = channelName,
            country = country,
        )
    }

    fun applySorting(sortOption: SortOption) {
        _sortOption.value = sortOption
    }

    fun clearFilters() {
        _filterOptions.value = FilterOptions()
    }

    private fun loadVideos() {
        viewModelScope.launch {
            repository.getLatestVideos()
                .onFailure {
                    _uiState.value = VideoListUiState.Error(it.message ?: "Failed to load videos.")
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeVideoChanges() {
        viewModelScope.launch {
            combine(_filterOptions, _sortOption) { filterOptions, sortOption ->
                filterOptions to sortOption
            }.flatMapLatest { (filterOptions, sortOption) ->
                repository.observeVideos(filterOptions, sortOption)
            }.collectLatest { videos ->
                _uiState.value = when {
                    videos.isEmpty() -> VideoListUiState.Empty
                    else -> VideoListUiState.Success(videos, totalCount.value)
                }
            }
        }
    }

    class Factory(
        private val repository: YouTubeRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VideoListViewModel(repository) as T
        }
    }
}
