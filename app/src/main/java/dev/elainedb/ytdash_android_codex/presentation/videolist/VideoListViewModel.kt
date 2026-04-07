package dev.elainedb.ytdash_android_codex.presentation.videolist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.data.repository.DefaultChannels
import dev.elainedb.ytdash_android_codex.domain.usecase.GetVideos
import dev.elainedb.ytdash_android_codex.domain.usecase.GetVideosParams
import dev.elainedb.ytdash_android_codex.domain.usecase.SignOut
import dev.elainedb.ytdash_android_codex.domain.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FilterOptions(
    val channelName: String? = null,
    val country: String? = null
)

enum class SortOption {
    PUBLISHED_NEWEST,
    PUBLISHED_OLDEST,
    RECORDING_NEWEST,
    RECORDING_OLDEST
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class VideoListViewModel @Inject constructor(
    private val getVideos: GetVideos,
    private val signOut: SignOut,
    private val videoRepository: VideoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<VideoListUiState>(VideoListUiState.Loading)
    val uiState: StateFlow<VideoListUiState> = _uiState

    private val _filterOptions = MutableStateFlow(FilterOptions())
    val filterOptions: StateFlow<FilterOptions> = _filterOptions

    private val _sortOption = MutableStateFlow(SortOption.PUBLISHED_NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption

    val availableCountries: StateFlow<List<String>> = videoRepository.getDistinctCountries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableChannels: StateFlow<List<String>> = videoRepository.getDistinctChannels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        observeVideoChanges()
        refreshVideos(forceRefresh = false)
    }

    fun refreshVideos(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            _uiState.value = VideoListUiState.Loading
            when (val result = getVideos(GetVideosParams(DefaultChannels.ids, forceRefresh))) {
                is Result.Success -> Unit
                is Result.Error -> _uiState.value = VideoListUiState.Error(result.failure.message)
            }
        }
    }

    fun applyFilter(filterOptions: FilterOptions) {
        _filterOptions.value = filterOptions
    }

    fun applySorting(sortOption: SortOption) {
        _sortOption.value = sortOption
    }

    fun clearFilters() {
        _filterOptions.value = FilterOptions()
    }

    fun signOut(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(signOut(Unit) is Result.Success)
        }
    }

    private fun observeVideoChanges() {
        viewModelScope.launch {
            combine(_filterOptions, _sortOption) { filter, sort -> filter to sort }
                .flatMapLatest { (filter, sort) ->
                    videoRepository.observeVideos(filter, sort)
                }
                .collect { videos ->
                    val totalCount = videoRepository.getTotalVideoCount()
                    _uiState.value = when {
                        videos.isEmpty() && totalCount == 0 -> VideoListUiState.Empty
                        videos.isEmpty() -> VideoListUiState.Empty
                        else -> VideoListUiState.Success(videos, totalCount)
                    }
                }
        }
    }
}
