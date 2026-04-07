package dev.elainedb.ytdash_android_codex.data.repository

import dev.elainedb.ytdash_android_codex.config.ConfigHelper
import dev.elainedb.ytdash_android_codex.core.error.Failure
import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.data.local.VideoDao
import dev.elainedb.ytdash_android_codex.data.local.toEntity
import dev.elainedb.ytdash_android_codex.data.local.toVideo
import dev.elainedb.ytdash_android_codex.data.model.mergeWithDetails
import dev.elainedb.ytdash_android_codex.data.model.toVideo
import dev.elainedb.ytdash_android_codex.data.network.YouTubeApiService
import dev.elainedb.ytdash_android_codex.domain.model.FilterOptions
import dev.elainedb.ytdash_android_codex.domain.model.SortOption
import dev.elainedb.ytdash_android_codex.domain.model.Video
import dev.elainedb.ytdash_android_codex.domain.repository.VideoRepository
import dev.elainedb.ytdash_android_codex.location.LocationUtils
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
        private const val CACHE_EXPIRY_HOURS = 24L
    }

    override suspend fun getLatestVideos(channelIds: List<String>, forceRefresh: Boolean): Result<List<Video>> {
        return runCatching {
            val threshold = System.currentTimeMillis() - CACHE_EXPIRY_HOURS * 60 * 60 * 1000
            val cached = if (!forceRefresh) videoDao.getVideosNewerThan(threshold) else emptyList()
            if (cached.isNotEmpty()) {
                return Result.Success(cached.map { it.toVideo() })
            }

            val videos = refreshVideos(channelIds)
            val now = System.currentTimeMillis()
            videoDao.deleteOldVideos(threshold)
            videoDao.insertVideos(videos.map { it.toEntity(now) })
            videos
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { error ->
                val fallback = videoDao.getVideosNewerThan(0).map { it.toVideo() }
                if (fallback.isNotEmpty()) {
                    Result.Success(fallback)
                } else {
                    Result.Error(Failure.Network(error.message ?: "Unable to load videos"))
                }
            }
        )
    }

    override fun observeVideos(filterOptions: FilterOptions, sortOption: SortOption): Flow<List<Video>> {
        return videoDao.getVideosWithFiltersAndSort(
            channelName = filterOptions.channelName,
            country = filterOptions.country,
            sortBy = sortOption.dbValue
        ).map { entities -> entities.map { it.toVideo() } }
    }

    override suspend fun getVideosByChannel(channelName: String): Result<List<Video>> =
        runCatching { videoDao.getVideosByChannel(channelName).map { it.toVideo() } }
            .fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Error(Failure.Cache(it.message ?: "Unable to query channel videos")) }
            )

    override suspend fun getVideosByCountry(country: String): Result<List<Video>> =
        runCatching { videoDao.getVideosByCountry(country).map { it.toVideo() } }
            .fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Error(Failure.Cache(it.message ?: "Unable to query country videos")) }
            )

    override suspend fun getAvailableCountries(): Result<List<String>> =
        runCatching { videoDao.getDistinctCountries() }
            .fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Error(Failure.Cache(it.message ?: "Unable to load countries")) }
            )

    override suspend fun getAvailableChannels(): Result<List<String>> =
        runCatching { videoDao.getDistinctChannels() }
            .fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Error(Failure.Cache(it.message ?: "Unable to load channels")) }
            )

    override suspend fun getTotalVideoCount(): Result<Int> =
        runCatching { videoDao.getTotalVideoCount() }
            .fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Error(Failure.Cache(it.message ?: "Unable to load total count")) }
            )

    override suspend fun getVideosWithLocation(): Result<List<Video>> =
        runCatching { videoDao.getVideosWithLocation().map { it.toVideo() } }
            .fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Error(Failure.Cache(it.message ?: "Unable to load mapped videos")) }
            )

    private suspend fun refreshVideos(channelIds: List<String>): List<Video> = coroutineScope {
        val apiKey = configHelper.youtubeApiKey
        channelIds.map { channelId ->
            async { fetchAllVideosForChannel(channelId, apiKey) }
        }.awaitAll().flatten()
            .let { videos -> enrichVideos(videos, apiKey) }
            .sortedByDescending { it.publishedAt }
    }

    private suspend fun fetchAllVideosForChannel(channelId: String, apiKey: String): List<Video> {
        val allVideos = mutableListOf<Video>()
        var nextPageToken: String? = null

        do {
            val response = apiService.searchVideos(
                channelId = channelId,
                pageToken = nextPageToken,
                key = apiKey
            )
            allVideos += response.items.map { it.toVideo() }
            nextPageToken = response.nextPageToken
        } while (nextPageToken != null)

        return allVideos
    }

    private suspend fun enrichVideos(videos: List<Video>, apiKey: String): List<Video> = coroutineScope {
        val detailsById = videos.map { it.id }.chunked(50).map { ids ->
            async {
                apiService.getVideoDetails(ids = ids.joinToString(","), key = apiKey).items
            }
        }.awaitAll().flatten().associateBy { it.id }

        videos.map { video ->
            val merged = detailsById[video.id]?.let { video.mergeWithDetails(it) } ?: video
            val resolved = locationUtils.resolveLocation(
                latitude = merged.locationLatitude,
                longitude = merged.locationLongitude,
                locationDescription = detailsById[video.id]?.snippet?.locationDescription
            )
            merged.copy(
                locationCity = resolved.first,
                locationCountry = resolved.second
            )
        }
    }
}
