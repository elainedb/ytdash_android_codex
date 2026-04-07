package dev.elainedb.ytdash_android_codex.domain.repository

import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.domain.model.Video
import dev.elainedb.ytdash_android_codex.presentation.videolist.FilterOptions
import dev.elainedb.ytdash_android_codex.presentation.videolist.SortOption
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    suspend fun getLatestVideos(forceRefresh: Boolean = false): Result<List<Video>>
    suspend fun refreshVideos(): Result<List<Video>>
    fun observeVideos(filterOptions: FilterOptions, sortOption: SortOption): Flow<List<Video>>
    fun getDistinctCountries(): Flow<List<String>>
    fun getDistinctChannels(): Flow<List<String>>
    fun getVideosWithLocation(): Flow<List<Video>>
    suspend fun getTotalVideoCount(): Int
    suspend fun getVideosByChannel(channelName: String): Result<List<Video>>
    suspend fun getVideosByCountry(country: String): Result<List<Video>>
}
