package dev.elainedb.ytdash_android_codex.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.elainedb.ytdash_android_codex.R
import dev.elainedb.ytdash_android_codex.repository.YouTubeRepository
import dev.elainedb.ytdash_android_codex.model.Video
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FilterOptions(
    val channelName: String? = null,
    val country: String? = null
)

enum class SortOption(val labelRes: Int) {
    PUBLISHED_DESC(R.string.publication_newest),
    PUBLISHED_ASC(R.string.publication_oldest),
    RECORDING_DESC(R.string.recording_newest),
    RECORDING_ASC(R.string.recording_oldest)
}

sealed interface VideoListUiState {
    data object Loading : VideoListUiState
    data object Empty : VideoListUiState
    data class Success(val videos: List<Video>, val totalCount: Int) : VideoListUiState
    data class Error(val message: String) : VideoListUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class VideoListViewModel(
    private val repository: YouTubeRepository
) : ViewModel() {

    private val _filterOptions = MutableStateFlow(FilterOptions())
    val filterOptions: StateFlow<FilterOptions> = _filterOptions

    private val _sortOption = MutableStateFlow(SortOption.PUBLISHED_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption

    private val _uiState = MutableStateFlow<VideoListUiState>(VideoListUiState.Loading)
    val uiState: StateFlow<VideoListUiState> = _uiState

    val availableCountries = repository.getDistinctCountries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableChannels = repository.getDistinctChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val totalVideoCount = repository.getTotalVideoCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        observeVideoChanges()
        loadVideos()
    }

    fun loadVideos() {
        viewModelScope.launch {
            _uiState.value = VideoListUiState.Loading
            repository.getLatestVideos()
                .onFailure { _uiState.value = VideoListUiState.Error(it.message ?: "Unable to load videos.") }
        }
    }

    fun refreshVideos() {
        viewModelScope.launch {
            _uiState.value = VideoListUiState.Loading
            repository.refreshVideos()
                .onFailure { _uiState.value = VideoListUiState.Error(it.message ?: "Unable to refresh videos.") }
        }
    }

    fun applyFilter(channelName: String?, country: String?) {
        _filterOptions.value = FilterOptions(
            channelName = channelName,
            country = country
        )
    }

    fun applySorting(sortOption: SortOption) {
        _sortOption.value = sortOption
    }

    fun clearFilters() {
        _filterOptions.value = FilterOptions()
    }

    private fun observeVideoChanges() {
        viewModelScope.launch {
            combine(
                _filterOptions,
                _sortOption
            ) { filters, sort ->
                filters to sort
            }.flatMapLatest { (filters, sort) ->
                repository.observeVideos(filters, sort)
            }.collectLatest { videos ->
                val totalCount = totalVideoCount.value
                _uiState.value = when {
                    videos.isEmpty() -> VideoListUiState.Empty
                    else -> VideoListUiState.Success(videos, totalCount)
                }
            }
        }
    }

    class Factory(
        private val repository: YouTubeRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VideoListViewModel(repository) as T
        }
    }
}
