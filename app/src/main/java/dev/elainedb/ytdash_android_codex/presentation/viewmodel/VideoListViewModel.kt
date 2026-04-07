package dev.elainedb.ytdash_android_codex.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.domain.model.FilterOptions
import dev.elainedb.ytdash_android_codex.domain.model.SortOption
import dev.elainedb.ytdash_android_codex.domain.usecase.GetVideosParams
import dev.elainedb.ytdash_android_codex.domain.usecase.GetVideosUseCase
import dev.elainedb.ytdash_android_codex.domain.usecase.SignOutUseCase
import dev.elainedb.ytdash_android_codex.domain.repository.VideoRepository
import dev.elainedb.ytdash_android_codex.presentation.model.VideoListUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoListViewModel @Inject constructor(
    private val getVideosUseCase: GetVideosUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val videoRepository: VideoRepository
) : ViewModel() {
    private val channelIds = listOf(
        "UCynoa1DjwnvHAowA_jiMEAQ",
        "UCK0KOjX3beyB9nzonls0cuw",
        "UCACkIrvrGAQ7kuc0hMVwvmA",
        "UCtWRAKKvOEA0CXOue9BG8ZA"
    )

    private val _uiState = MutableStateFlow<VideoListUiState>(VideoListUiState.Loading)
    val uiState: StateFlow<VideoListUiState> = _uiState.asStateFlow()

    private val _filterOptions = MutableStateFlow(FilterOptions())
    val filterOptions: StateFlow<FilterOptions> = _filterOptions.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.PUBLICATION_DATE_DESC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _availableCountries = MutableStateFlow<List<String>>(emptyList())
    val availableCountries: StateFlow<List<String>> = _availableCountries.asStateFlow()

    private val _availableChannels = MutableStateFlow<List<String>>(emptyList())
    val availableChannels: StateFlow<List<String>> = _availableChannels.asStateFlow()

    init {
        refreshVideos(forceRefresh = false)
        observeVideoChanges()
        loadFilterOptions()
    }

    fun refreshVideos(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = VideoListUiState.Loading
            when (val result = getVideosUseCase(GetVideosParams(channelIds, forceRefresh))) {
                is Result.Success -> {
                    if (result.data.isEmpty()) {
                        _uiState.value = VideoListUiState.Empty
                    }
                    loadFilterOptions()
                }
                is Result.Error -> _uiState.value = VideoListUiState.Error(result.failure.message)
            }
        }
    }

    private fun observeVideoChanges() {
        viewModelScope.launch {
            combine(_filterOptions, _sortOption) { filter, sort -> filter to sort }
                .collectLatest { (filter, sort) ->
                    videoRepository.observeVideos(filter, sort).collectLatest { videos ->
                        when {
                            videos.isEmpty() -> _uiState.value = VideoListUiState.Empty
                            else -> {
                                val totalCount = when (val result = videoRepository.getTotalVideoCount()) {
                                    is Result.Success -> result.data
                                    is Result.Error -> videos.size
                                }
                                _uiState.value = VideoListUiState.Success(videos, totalCount)
                            }
                        }
                    }
                }
        }
    }

    private fun loadFilterOptions() {
        viewModelScope.launch {
            val countries = when (val result = videoRepository.getAvailableCountries()) {
                is Result.Success -> result.data
                is Result.Error -> emptyList()
            }
            val channels = when (val result = videoRepository.getAvailableChannels()) {
                is Result.Success -> result.data
                is Result.Error -> emptyList()
            }
            _availableCountries.value = countries
            _availableChannels.value = channels
        }
    }

    fun applyFilter(filterOptions: FilterOptions) {
        _filterOptions.value = filterOptions
    }

    fun clearFilters() {
        _filterOptions.value = FilterOptions()
    }

    fun applySorting(sortOption: SortOption) {
        _sortOption.value = sortOption
    }

    fun signOut(onComplete: () -> Unit) {
        viewModelScope.launch {
            signOutUseCase(Unit)
            onComplete()
        }
    }
}
