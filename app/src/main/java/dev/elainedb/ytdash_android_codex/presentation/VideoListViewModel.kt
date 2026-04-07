package dev.elainedb.ytdash_android_codex.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.domain.model.FilterOptions
import dev.elainedb.ytdash_android_codex.domain.model.SortOption
import dev.elainedb.ytdash_android_codex.domain.repository.GetVideosParams
import dev.elainedb.ytdash_android_codex.domain.repository.YouTubeRepository
import dev.elainedb.ytdash_android_codex.domain.usecase.GetVideos
import dev.elainedb.ytdash_android_codex.util.ChannelCatalog
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class VideoListViewModel @Inject constructor(
    private val getVideos: GetVideos,
    private val repository: YouTubeRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<VideoListUiState>(VideoListUiState.Loading)
    val uiState: StateFlow<VideoListUiState> = _uiState

    private val _filterOptions = MutableStateFlow(FilterOptions())
    val filterOptions: StateFlow<FilterOptions> = _filterOptions

    private val _sortOption = MutableStateFlow(SortOption.PUBLICATION_DATE_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption

    val availableCountries: StateFlow<List<String>> = repository.getDistinctCountries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableChannels: StateFlow<List<String>> = repository.getDistinctChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeVideoChanges()
        loadVideos(forceRefresh = false)
    }

    fun loadVideos(forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.value = VideoListUiState.Loading
            when (
                val result = getVideos(
                    GetVideosParams(
                        channelIds = ChannelCatalog.channelIds,
                        forceRefresh = forceRefresh
                    )
                )
            ) {
                is Result.Success -> {
                    if (result.data.isEmpty()) {
                        _uiState.value = VideoListUiState.Empty
                    }
                }
                is Result.Error -> _uiState.value = VideoListUiState.Error(result.failure.message)
            }
        }
    }

    fun applyFilter(channelName: String?, country: String?) {
        _filterOptions.value = FilterOptions(
            channelName = channelName?.takeIf { it.isNotBlank() },
            country = country?.takeIf { it.isNotBlank() }
        )
    }

    fun applySorting(option: SortOption) {
        _sortOption.value = option
    }

    fun clearFilters() {
        _filterOptions.value = FilterOptions()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeVideoChanges() {
        viewModelScope.launch {
            combine(_filterOptions, _sortOption) { filters, sort -> filters to sort }
                .flatMapLatest { (filters, sort) -> repository.observeVideos(filters, sort) }
                .collect { videos ->
                    val totalCount = when (val totalResult = repository.getTotalVideoCount()) {
                        is Result.Success -> totalResult.data
                        is Result.Error -> videos.size
                    }
                    _uiState.value = when {
                        videos.isEmpty() -> VideoListUiState.Empty
                        else -> VideoListUiState.Success(videos, totalCount)
                    }
                }
        }
    }
}
