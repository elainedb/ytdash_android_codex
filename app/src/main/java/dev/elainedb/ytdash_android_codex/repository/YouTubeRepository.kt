package dev.elainedb.ytdash_android_codex.repository

import android.content.Context
import dev.elainedb.ytdash_android_codex.config.ConfigHelper
import dev.elainedb.ytdash_android_codex.database.VideoDao
import dev.elainedb.ytdash_android_codex.database.toEntity
import dev.elainedb.ytdash_android_codex.database.toVideo
import dev.elainedb.ytdash_android_codex.model.Video
import dev.elainedb.ytdash_android_codex.model.YouTubeVideoDetails
import dev.elainedb.ytdash_android_codex.model.mergeWithDetails
import dev.elainedb.ytdash_android_codex.model.toVideo
import dev.elainedb.ytdash_android_codex.network.YouTubeApiService
import dev.elainedb.ytdash_android_codex.utils.LocationUtils
import dev.elainedb.ytdash_android_codex.viewmodel.FilterOptions
import dev.elainedb.ytdash_android_codex.viewmodel.SortOption
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class YouTubeRepository(
    private val context: Context,
    private val apiService: YouTubeApiService,
    private val videoDao: VideoDao,
    private val configHelper: ConfigHelper
) {
    fun observeVideos(filterOptions: FilterOptions, sortOption: SortOption): Flow<List<Video>> {
        return videoDao.getVideosWithFiltersAndSort(
            channelName = filterOptions.channelName,
            country = filterOptions.country,
            sortBy = sortOption.name
        ).map { entities -> entities.map { it.toVideo() } }
    }

    fun getDistinctCountries() = videoDao.getDistinctCountries()

    fun getDistinctChannels() = videoDao.getDistinctChannels()

    fun getTotalVideoCount() = videoDao.getTotalVideoCount()

    suspend fun getVideosWithLocation(): List<Video> {
        return videoDao.getVideosWithLocation().map { it.toVideo() }
    }

    suspend fun getLatestVideos(): Result<Unit> {
        val threshold = System.currentTimeMillis() - CACHE_EXPIRY_HOURS * 60 * 60 * 1000L
        val cachedVideos = videoDao.getVideosNewerThan(threshold)
        if (cachedVideos.isNotEmpty()) {
            return Result.success(Unit)
        }
        return refreshVideos()
    }

    suspend fun refreshVideos(): Result<Unit> {
        return runCatching {
            val videos = fetchRemoteVideos()
            val cacheTimestamp = System.currentTimeMillis()
            videoDao.deleteOldVideos(cacheTimestamp - CACHE_EXPIRY_HOURS * 60 * 60 * 1000L)
            videoDao.insertVideos(videos.map { it.toEntity(cacheTimestamp) })
        }.recoverCatching { error ->
            val threshold = System.currentTimeMillis() - CACHE_EXPIRY_HOURS * 60 * 60 * 1000L
            val cachedVideos = videoDao.getVideosNewerThan(threshold)
            if (cachedVideos.isEmpty()) {
                throw error
            }
        }
    }

    private suspend fun fetchRemoteVideos(): List<Video> = coroutineScope {
        val apiKey = configHelper.getYouTubeApiKey()
        val seedVideos = CHANNEL_IDS.map { channelId ->
            async { fetchChannelVideos(channelId, apiKey) }
        }.awaitAll().flatten()
            .filter { it.id.isNotBlank() }
            .distinctBy { it.id }

        val detailsById = seedVideos
            .map { it.id }
            .chunked(50)
            .map { ids ->
                async {
                    apiService.getVideoDetails(ids = ids.joinToString(","), key = apiKey).items
                }
            }
            .awaitAll()
            .flatten()
            .associateBy(YouTubeVideoDetails::id)

        seedVideos.map { video ->
            val details = detailsById[video.id] ?: return@map video
            val location = details.recordingDetails?.location
            val (city, country) = if (location?.latitude != null && location.longitude != null) {
                LocationUtils.reverseGeocode(context, location.latitude, location.longitude)
            } else {
                null to null
            }
            video.mergeWithDetails(details, city, country)
        }.sortedByDescending { it.publishedAt }
    }

    private suspend fun fetchChannelVideos(channelId: String, apiKey: String): List<Video> {
        val videos = mutableListOf<Video>()
        var nextPageToken: String? = null

        repeat(MAX_PAGES_PER_CHANNEL) {
            val response = apiService.searchVideos(
                channelId = channelId,
                pageToken = nextPageToken,
                key = apiKey
            )
            videos += response.items.map { it.toVideo() }
            nextPageToken = response.nextPageToken
            if (nextPageToken == null) {
                return videos
            }
        }

        return videos
    }

    companion object {
        private const val CACHE_EXPIRY_HOURS = 24
        private const val MAX_PAGES_PER_CHANNEL = 5

        private val CHANNEL_IDS = listOf(
            "UCynoa1DjwnvHAowA_jiMEAQ",
            "UCK0KOjX3beyB9nzonls0cuw",
            "UCACkIrvrGAQ7kuc0hMVwvmA",
            "UCtWRAKKvOEA0CXOue9BG8ZA"
        )
    }
}
