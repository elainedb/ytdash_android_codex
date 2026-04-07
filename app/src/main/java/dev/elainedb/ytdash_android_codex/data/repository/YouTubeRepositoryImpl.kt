package dev.elainedb.ytdash_android_codex.data.repository

import dev.elainedb.ytdash_android_codex.core.config.ConfigHelper
import dev.elainedb.ytdash_android_codex.core.error.Failure
import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.core.utils.DateUtils
import dev.elainedb.ytdash_android_codex.data.database.VideoDao
import dev.elainedb.ytdash_android_codex.data.database.toEntity
import dev.elainedb.ytdash_android_codex.data.database.toVideo
import dev.elainedb.ytdash_android_codex.data.location.LocationUtils
import dev.elainedb.ytdash_android_codex.data.model.mergeWithDetails
import dev.elainedb.ytdash_android_codex.data.model.toVideo
import dev.elainedb.ytdash_android_codex.data.network.YouTubeApiService
import dev.elainedb.ytdash_android_codex.domain.model.Video
import dev.elainedb.ytdash_android_codex.domain.repository.VideoRepository
import dev.elainedb.ytdash_android_codex.presentation.videolist.FilterOptions
import dev.elainedb.ytdash_android_codex.presentation.videolist.SortOption
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeRepositoryImpl @Inject constructor(
    private val apiService: YouTubeApiService,
    private val videoDao: VideoDao,
    private val configHelper: ConfigHelper,
    private val locationUtils: LocationUtils
) : VideoRepository {

    companion object {
        private const val CACHE_EXPIRY_HOURS = 24
    }

    override suspend fun getLatestVideos(forceRefresh: Boolean): Result<List<Video>> {
        return if (forceRefresh) refreshVideos() else loadFromCacheOrRemote()
    }

    override suspend fun refreshVideos(): Result<List<Video>> {
        return fetchAndCacheVideos()
    }

    override fun observeVideos(
        filterOptions: FilterOptions,
        sortOption: SortOption
    ): Flow<List<Video>> {
        return videoDao.getVideosWithFiltersAndSort(
            channelName = filterOptions.channelName,
            country = filterOptions.country,
            sortBy = sortOption.name
        ).map { items -> items.map { it.toVideo() } }
    }

    override fun getDistinctCountries(): Flow<List<String>> = videoDao.getDistinctCountries()

    override fun getDistinctChannels(): Flow<List<String>> = videoDao.getDistinctChannels()

    override fun getVideosWithLocation(): Flow<List<Video>> {
        return videoDao.getVideosWithLocation().map { items -> items.map { it.toVideo() } }
    }

    override suspend fun getTotalVideoCount(): Int = videoDao.getTotalVideoCount()

    override suspend fun getVideosByChannel(channelName: String): Result<List<Video>> {
        return Result.Success(videoDao.getVideosByChannel(channelName).map { it.toVideo() })
    }

    override suspend fun getVideosByCountry(country: String): Result<List<Video>> {
        return Result.Success(videoDao.getVideosByCountry(country).map { it.toVideo() })
    }

    private suspend fun loadFromCacheOrRemote(): Result<List<Video>> {
        val threshold = DateUtils.currentTimestamp() - CACHE_EXPIRY_HOURS * 60 * 60 * 1000L
        val freshCache = videoDao.getVideosNewerThan(threshold)
        return if (freshCache.isNotEmpty()) {
            Result.Success(videoDao.getAllVideos().map { it.toVideo() })
        } else {
            fetchAndCacheVideos()
        }
    }

    private suspend fun fetchAndCacheVideos(): Result<List<Video>> {
        val apiKey = configHelper.getYoutubeApiKey()
        if (apiKey.isBlank()) {
            return Result.Error(Failure.Validation("YouTube API key is missing."))
        }

        return runCatching {
            val videos = coroutineScope {
                DefaultChannels.ids.map { channelId ->
                    async { fetchChannelVideos(channelId, apiKey) }
                }.awaitAll().flatten()
            }

            val videoDetails = coroutineScope {
                videos.map { it.id }
                    .filter { it.isNotBlank() }
                    .chunked(50)
                    .map { ids ->
                        async {
                            apiService.getVideoDetails(
                                id = ids.joinToString(","),
                                apiKey = apiKey
                            ).items
                        }
                    }
                    .awaitAll()
                    .flatten()
                    .associateBy { it.id }
            }

            val enrichedVideos = coroutineScope {
                videos.map { video ->
                    async {
                        val details = videoDetails[video.id]
                        val location = details?.recordingDetails?.location
                        val locationDescription =
                            details?.snippet?.locationDescription ?: video.description
                        val (city, country) = locationUtils.resolveLocation(
                            latitude = location?.latitude,
                            longitude = location?.longitude,
                            locationDescription = locationDescription
                        )
                        video.mergeWithDetails(details, city, country)
                    }
                }.awaitAll()
            }.sortedByDescending { it.publishedAt }

            val now = DateUtils.currentTimestamp()
            videoDao.deleteOldVideos(now - CACHE_EXPIRY_HOURS * 60 * 60 * 1000L)
            videoDao.insertVideos(enrichedVideos.map { it.toEntity(now) })
            enrichedVideos
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { error ->
                val cached = videoDao.getAllVideos().map { it.toVideo() }
                if (cached.isNotEmpty()) {
                    Result.Success(cached)
                } else {
                    Result.Error(Failure.Network(error.message ?: "Unable to load videos."))
                }
            }
        )
    }

    private suspend fun fetchChannelVideos(channelId: String, apiKey: String): List<Video> {
        val items = mutableListOf<Video>()
        var nextPageToken: String? = null

        do {
            val response = apiService.searchVideos(
                channelId = channelId,
                pageToken = nextPageToken,
                apiKey = apiKey
            )
            items += response.items.mapNotNull { item ->
                item.id.videoId?.takeIf { it.isNotBlank() }?.let { item.toVideo() }
            }
            nextPageToken = response.nextPageToken
        } while (nextPageToken != null)

        return items
    }
}
