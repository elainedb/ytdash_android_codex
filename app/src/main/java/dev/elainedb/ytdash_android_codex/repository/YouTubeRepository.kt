package dev.elainedb.ytdash_android_codex.repository

import android.content.Context
import dev.elainedb.ytdash_android_codex.config.AppConfig
import dev.elainedb.ytdash_android_codex.database.VideoDao
import dev.elainedb.ytdash_android_codex.database.toEntity
import dev.elainedb.ytdash_android_codex.database.toVideo
import dev.elainedb.ytdash_android_codex.model.FilterOptions
import dev.elainedb.ytdash_android_codex.model.SortOption
import dev.elainedb.ytdash_android_codex.model.Video
import dev.elainedb.ytdash_android_codex.model.mergeWithDetails
import dev.elainedb.ytdash_android_codex.model.toVideo
import dev.elainedb.ytdash_android_codex.network.YouTubeApiService
import dev.elainedb.ytdash_android_codex.network.YouTubeVideoDetails
import dev.elainedb.ytdash_android_codex.utils.LocationUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class YouTubeRepository(
    private val context: Context,
    private val videoDao: VideoDao,
    private val apiService: YouTubeApiService,
    private val config: AppConfig,
) {
    companion object {
        private const val CACHE_EXPIRY_HOURS = 24L
        private const val CACHE_EXPIRY_MS = CACHE_EXPIRY_HOURS * 60L * 60L * 1000L
        private const val MAX_PAGES_PER_CHANNEL = 5

        val CHANNEL_IDS = listOf(
            "UCynoa1DjwnvHAowA_jiMEAQ",
            "UCK0KOjX3beyB9nzonls0cuw",
            "UCACkIrvrGAQ7kuc0hMVwvmA",
            "UCtWRAKKvOEA0CXOue9BG8ZA",
        )
    }

    fun observeVideos(filterOptions: FilterOptions, sortOption: SortOption): Flow<List<Video>> {
        return videoDao.getVideosWithFiltersAndSort(
            channelName = filterOptions.channelName,
            country = filterOptions.country,
            sortBy = sortOption.name,
        ).map { entities -> entities.map { it.toVideo() } }
    }

    fun getDistinctCountries(): Flow<List<String>> = videoDao.getDistinctCountries()

    fun getDistinctChannels(): Flow<List<String>> = videoDao.getDistinctChannels()

    fun getTotalVideoCount(): Flow<Int> = videoDao.getTotalVideoCount()

    suspend fun getVideosWithLocation(): List<Video> = videoDao.getVideosWithLocation().map { it.toVideo() }

    suspend fun getLatestVideos(): Result<Unit> {
        val threshold = System.currentTimeMillis() - CACHE_EXPIRY_MS
        val cachedVideos = videoDao.getVideosNewerThan(threshold)
        if (cachedVideos.isNotEmpty()) {
            return Result.success(Unit)
        }

        return refreshVideos()
    }

    suspend fun refreshVideos(): Result<Unit> {
        return runCatching {
            val fetchedVideos = fetchAndEnrichVideos()
            val cacheTimestamp = System.currentTimeMillis()
            videoDao.deleteOldVideos(cacheTimestamp - CACHE_EXPIRY_MS)
            videoDao.insertVideos(fetchedVideos.map { it.toEntity(cacheTimestamp) })
        }.recoverCatching {
            val threshold = System.currentTimeMillis() - CACHE_EXPIRY_MS
            if (videoDao.getVideosNewerThan(threshold).isEmpty()) {
                throw it
            }
        }
    }

    private suspend fun fetchAndEnrichVideos(): List<Video> = coroutineScope {
        val baseVideos = CHANNEL_IDS.map { channelId ->
            async { fetchChannelVideos(channelId) }
        }.awaitAll().flatten()

        val detailsById = baseVideos.map { it.id }
            .distinct()
            .chunked(50)
            .map { chunk ->
                async {
                    apiService.getVideoDetails(
                        videoIds = chunk.joinToString(","),
                        key = config.youtubeApiKey,
                    ).items.associateBy { it.id }
                }
            }
            .awaitAll()
            .fold(emptyMap<String, YouTubeVideoDetails>()) { acc, detailMap -> acc + detailMap }

        baseVideos.map { video ->
            async {
                val merged = detailsById[video.id]?.let { video.mergeWithDetails(it) } ?: video
                val latitude = merged.locationLatitude
                val longitude = merged.locationLongitude
                if (latitude != null && longitude != null) {
                    val (city, country) = LocationUtils.reverseGeocode(context, latitude, longitude)
                    merged.copy(locationCity = city, locationCountry = country)
                } else {
                    merged
                }
            }
        }.awaitAll().sortedByDescending { it.publishedAt }
    }

    private suspend fun fetchChannelVideos(channelId: String): List<Video> {
        val videos = mutableListOf<Video>()
        var pageToken: String? = null
        repeat(MAX_PAGES_PER_CHANNEL) {
            val response = apiService.searchVideos(
                channelId = channelId,
                pageToken = pageToken,
                key = config.youtubeApiKey,
            )
            videos += response.items.map { it.toVideo() }
            pageToken = response.nextPageToken ?: return videos
        }
        return videos
    }
}
