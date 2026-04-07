package dev.elainedb.ytdash_android_codex.data.repository

import dev.elainedb.ytdash_android_codex.core.error.Failure
import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.data.config.ConfigHelper
import dev.elainedb.ytdash_android_codex.data.local.VideoDao
import dev.elainedb.ytdash_android_codex.data.local.toEntity
import dev.elainedb.ytdash_android_codex.data.local.toVideo
import dev.elainedb.ytdash_android_codex.data.remote.YouTubeApiService
import dev.elainedb.ytdash_android_codex.data.remote.mergeWithDetails
import dev.elainedb.ytdash_android_codex.data.remote.toVideo
import dev.elainedb.ytdash_android_codex.domain.model.FilterOptions
import dev.elainedb.ytdash_android_codex.domain.model.SortOption
import dev.elainedb.ytdash_android_codex.domain.model.Video
import dev.elainedb.ytdash_android_codex.domain.repository.YouTubeRepository
import dev.elainedb.ytdash_android_codex.util.LocationUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class YouTubeRepositoryImpl @Inject constructor(
    private val configHelper: ConfigHelper,
    private val videoDao: VideoDao,
    private val youTubeApiService: YouTubeApiService,
    private val locationUtils: LocationUtils
) : YouTubeRepository {
    companion object {
        private const val CACHE_EXPIRY_HOURS = 24L
        private const val CACHE_EXPIRY_MILLIS = CACHE_EXPIRY_HOURS * 60 * 60 * 1000
    }

    override suspend fun getLatestVideos(channelIds: List<String>): Result<List<Video>> {
        return try {
            val threshold = System.currentTimeMillis() - CACHE_EXPIRY_MILLIS
            val cachedVideos = videoDao.getVideosNewerThan(threshold)
            if (cachedVideos.isNotEmpty()) {
                Result.Success(cachedVideos.map { it.toVideo() })
            } else {
                refreshVideos(channelIds)
            }
        } catch (exception: Exception) {
            Result.Error(Failure.Cache(exception.message ?: "Failed to read cached videos."))
        }
    }

    override suspend fun refreshVideos(channelIds: List<String>): Result<List<Video>> {
        return try {
            val apiKey = configHelper.getConfig().youtubeApiKey
            val baseVideos = coroutineScope {
                channelIds.map { channelId ->
                    async { fetchAllVideosForChannel(channelId, apiKey) }
                }.awaitAll().flatten()
            }

            val detailsById = fetchDetails(baseVideos.map { it.id }, apiKey)
            val enrichedVideos = baseVideos.map { video ->
                val details = detailsById[video.id]
                if (details == null) {
                    video
                } else {
                    val location = details.recordingDetails?.location
                    val locationPair = locationUtils.resolveLocation(
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        locationDescription = details.snippet?.locationDescription
                    )
                    video.mergeWithDetails(details, locationPair)
                }
            }.sortedByDescending { it.publishedAt }

            val timestamp = System.currentTimeMillis()
            videoDao.deleteOldVideos(timestamp - CACHE_EXPIRY_MILLIS)
            videoDao.insertVideos(enrichedVideos.map { it.toEntity(timestamp) })
            Result.Success(enrichedVideos)
        } catch (exception: Exception) {
            val cachedFallback = runCatching { videoDao.getVideosNewerThan(0).map { it.toVideo() } }.getOrDefault(emptyList())
            if (cachedFallback.isNotEmpty()) {
                Result.Success(cachedFallback)
            } else {
                Result.Error(Failure.Network(exception.message ?: "Failed to refresh videos."))
            }
        }
    }

    override fun observeVideos(filterOptions: FilterOptions, sortOption: SortOption): Flow<List<Video>> {
        return videoDao.getVideosWithFiltersAndSort(
            channelName = filterOptions.channelName,
            country = filterOptions.country,
            sortBy = sortOption.queryValue
        ).map { list -> list.map { it.toVideo() } }
    }

    override fun getDistinctCountries(): Flow<List<String>> = videoDao.getDistinctCountries()

    override fun getDistinctChannels(): Flow<List<String>> = videoDao.getDistinctChannels()

    override suspend fun getVideosByChannel(channelName: String): Result<List<Video>> = try {
        Result.Success(videoDao.getVideosByChannel(channelName).map { it.toVideo() })
    } catch (exception: Exception) {
        Result.Error(Failure.Cache(exception.message ?: "Failed to query channel videos."))
    }

    override suspend fun getVideosByCountry(country: String): Result<List<Video>> = try {
        Result.Success(videoDao.getVideosByCountry(country).map { it.toVideo() })
    } catch (exception: Exception) {
        Result.Error(Failure.Cache(exception.message ?: "Failed to query country videos."))
    }

    override suspend fun getVideosWithLocation(): Result<List<Video>> = try {
        Result.Success(videoDao.getVideosWithLocation().map { it.toVideo() })
    } catch (exception: Exception) {
        Result.Error(Failure.Cache(exception.message ?: "Failed to load map videos."))
    }

    override suspend fun getTotalVideoCount(): Result<Int> = try {
        Result.Success(videoDao.getTotalVideoCount())
    } catch (exception: Exception) {
        Result.Error(Failure.Cache(exception.message ?: "Failed to count videos."))
    }

    private suspend fun fetchAllVideosForChannel(channelId: String, apiKey: String): List<Video> {
        val videos = mutableListOf<Video>()
        var nextPageToken: String? = null
        do {
            val response = youTubeApiService.searchVideos(
                channelId = channelId,
                pageToken = nextPageToken,
                key = apiKey
            )
            videos += response.items.mapNotNull { item ->
                item.takeIf { !it.id.videoId.isNullOrBlank() }?.toVideo()
            }
            nextPageToken = response.nextPageToken
        } while (nextPageToken != null)
        return videos
    }

    private suspend fun fetchDetails(videoIds: List<String>, apiKey: String) =
        coroutineScope {
            videoIds.chunked(50).map { chunk ->
                async {
                    youTubeApiService.getVideoDetails(
                        ids = chunk.joinToString(","),
                        key = apiKey
                    ).items.associateBy { it.id }
                }
            }.awaitAll().fold(mutableMapOf<String, dev.elainedb.ytdash_android_codex.data.remote.YouTubeVideoDetails>()) { acc, map ->
                acc.apply { putAll(map) }
            }
        }
}
