package dev.elainedb.ytdash_android_codex.domain.repository

import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.domain.model.FilterOptions
import dev.elainedb.ytdash_android_codex.domain.model.SortOption
import dev.elainedb.ytdash_android_codex.domain.model.Video
import kotlinx.coroutines.flow.Flow

data class GetVideosParams(
    val channelIds: List<String>,
    val forceRefresh: Boolean
)

interface YouTubeRepository {
    suspend fun getLatestVideos(channelIds: List<String>): Result<List<Video>>
    suspend fun refreshVideos(channelIds: List<String>): Result<List<Video>>
    fun observeVideos(filterOptions: FilterOptions, sortOption: SortOption): Flow<List<Video>>
    fun getDistinctCountries(): Flow<List<String>>
    fun getDistinctChannels(): Flow<List<String>>
    suspend fun getVideosByChannel(channelName: String): Result<List<Video>>
    suspend fun getVideosByCountry(country: String): Result<List<Video>>
    suspend fun getVideosWithLocation(): Result<List<Video>>
    suspend fun getTotalVideoCount(): Result<Int>
}
