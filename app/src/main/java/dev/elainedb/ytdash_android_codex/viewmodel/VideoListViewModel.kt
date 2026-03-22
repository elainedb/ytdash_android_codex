package dev.elainedb.ytdash_android_codex.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.elainedb.ytdash_android_codex.model.Video
import dev.elainedb.ytdash_android_codex.repository.FilterOptions
import dev.elainedb.ytdash_android_codex.repository.SortOption
import dev.elainedb.ytdash_android_codex.repository.YouTubeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
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

    private val _sortOption = MutableStateFlow(SortOption.PUBLISHED_NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption

    val availableCountries: StateFlow<List<String>> = repository.observeCountries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableChannels: StateFlow<List<String>> = repository.observeChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        observeVideoChanges()
        refreshIfNeeded()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = VideoListUiState.Loading
            repository.refreshVideos()
                .onFailure { _uiState.value = VideoListUiState.Error(it.message ?: "Unable to refresh videos.") }
        }
    }

    fun applyFilter(options: FilterOptions) {
        _filterOptions.value = options
    }

    fun applySorting(option: SortOption) {
        _sortOption.value = option
    }

    fun clearFilters() {
        _filterOptions.value = FilterOptions()
    }

    private fun refreshIfNeeded() {
        viewModelScope.launch {
            repository.getLatestVideos()
                .onFailure { _uiState.value = VideoListUiState.Error(it.message ?: "Unable to load videos.") }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeVideoChanges() {
        viewModelScope.launch {
            combine(_filterOptions, _sortOption) { filters, sort -> filters to sort }
                .flatMapLatest { (filters, sort) ->
                    repository.observeVideos(filters.channelName, filters.country, sort)
                }
                .collect { videos ->
                    val totalCount = repository.getTotalVideoCount()
                    _uiState.value = when {
                        videos.isEmpty() && totalCount == 0 -> VideoListUiState.Empty
                        videos.isEmpty() -> VideoListUiState.Error("No videos match the current filters.")
                        else -> VideoListUiState.Success(videos, totalCount)
                    }
                }
        }
    }
}

class VideoListViewModelFactory(
    private val repository: YouTubeRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VideoListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VideoListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
