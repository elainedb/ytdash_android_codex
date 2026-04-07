package dev.elainedb.ytdash_android_codex.domain.repository

import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.domain.model.FilterOptions
import dev.elainedb.ytdash_android_codex.domain.model.SortOption
import dev.elainedb.ytdash_android_codex.domain.model.Video
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    suspend fun getLatestVideos(channelIds: List<String>, forceRefresh: Boolean): Result<List<Video>>
    fun observeVideos(filterOptions: FilterOptions, sortOption: SortOption): Flow<List<Video>>
    suspend fun getVideosByChannel(channelName: String): Result<List<Video>>
    suspend fun getVideosByCountry(country: String): Result<List<Video>>
    suspend fun getAvailableCountries(): Result<List<String>>
    suspend fun getAvailableChannels(): Result<List<String>>
    suspend fun getTotalVideoCount(): Result<Int>
    suspend fun getVideosWithLocation(): Result<List<Video>>
}
