package dev.elainedb.ytdash_android_codex.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dev.elainedb.ytdash_android_codex.database.VideoDao
import dev.elainedb.ytdash_android_codex.database.toVideo as entityToVideo
import dev.elainedb.ytdash_android_codex.database.toEntity
import dev.elainedb.ytdash_android_codex.model.Video
import dev.elainedb.ytdash_android_codex.model.YouTubeVideoDetails
import dev.elainedb.ytdash_android_codex.model.mergeWithDetails
import dev.elainedb.ytdash_android_codex.model.toVideo
import dev.elainedb.ytdash_android_codex.network.YouTubeApiService
import dev.elainedb.ytdash_android_codex.utils.ConfigHelper
import dev.elainedb.ytdash_android_codex.utils.LocationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class YouTubeRepository(
    private val context: Context,
    private val videoDao: VideoDao,
) {
    companion object {
        private const val BASE_URL = "https://www.googleapis.com/youtube/v3/"
        private const val CACHE_EXPIRY_HOURS = 24L
        private const val PAGE_LIMIT = 5
        private val CHANNEL_IDS = listOf(
            "UCynoa1DjwnvHAowA_jiMEAQ",
            "UCK0KOjX3beyB9nzonls0cuw",
            "UCACkIrvrGAQ7kuc0hMVwvmA",
            "UCtWRAKKvOEA0CXOue9BG8ZA",
        )
    }

    private val apiService: YouTubeApiService by lazy {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(packageMetadataInterceptor())
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(YouTubeApiService::class.java)
    }

    fun observeVideos(channelName: String?, country: String?, sortBy: SortOption): Flow<List<Video>> =
        videoDao.getVideosWithFiltersAndSort(channelName, country, sortBy.name).map { list ->
            list.map { it.entityToVideo() }
        }

    fun observeCountries(): Flow<List<String>> = videoDao.getDistinctCountries()

    fun observeChannels(): Flow<List<String>> = videoDao.getDistinctChannels()

    suspend fun getTotalVideoCount(): Int = videoDao.getTotalVideoCount()

    suspend fun getLatestVideos(): Result<Unit> = withContext(Dispatchers.IO) {
        val threshold = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(CACHE_EXPIRY_HOURS)
        val cached = videoDao.getVideosNewerThan(threshold)
        if (cached.isNotEmpty()) {
            Result.success(Unit)
        } else {
            refreshVideos()
        }
    }

    suspend fun refreshVideos(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val apiKey = ConfigHelper.getYoutubeApiKey(context)
            require(apiKey.isNotBlank()) { "YouTube API key is missing." }

            val fetchedVideos = coroutineScope {
                CHANNEL_IDS.map { channelId ->
                    async { fetchChannelVideos(channelId, apiKey) }
                }.awaitAll().flatten()
            }

            val now = System.currentTimeMillis()
            videoDao.deleteOldVideos(now - TimeUnit.HOURS.toMillis(CACHE_EXPIRY_HOURS))
            videoDao.insertVideos(fetchedVideos.map { it.toEntity(now) })
        }
    }

    suspend fun getVideosWithLocation(): List<Video> = withContext(Dispatchers.IO) {
        videoDao.getVideosWithLocation().map { it.entityToVideo() }
    }

    private suspend fun fetchChannelVideos(channelId: String, apiKey: String): List<Video> = coroutineScope {
        val baseVideos = mutableListOf<Video>()
        var nextPageToken: String? = null
        var pageCount = 0

        while (pageCount < PAGE_LIMIT) {
            val response = apiService.searchVideos(
                channelId = channelId,
                pageToken = nextPageToken,
                key = apiKey,
            )
            baseVideos += response.items.map { it.toVideo() }
            pageCount += 1
            nextPageToken = response.nextPageToken ?: break
        }

        val detailsById = baseVideos.map { it.id }.distinct().chunked(50).flatMap { batch ->
            apiService.getVideoDetails(
                id = batch.joinToString(","),
                key = apiKey,
            ).items
        }.associateBy(YouTubeVideoDetails::id)

        baseVideos.map { video ->
            val detail = detailsById[video.id]
            val location = detail?.recordingDetails?.location
            val cityAndCountry = if (location != null) {
                LocationUtils.getCityAndCountry(context, location.latitude, location.longitude)
            } else {
                null to null
            }
            video.mergeWithDetails(detail, cityAndCountry.first, cityAndCountry.second)
        }
    }

    private fun packageMetadataInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("X-Android-Package", context.packageName)
            .header("X-Android-Cert", getSigningCertificateSha1())
            .build()
        chain.proceed(request)
    }

    private fun getSigningCertificateSha1(): String {
        return runCatching {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            val signatureBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures?.firstOrNull()?.toByteArray()
            } ?: return ""

            MessageDigest.getInstance("SHA1")
                .digest(signatureBytes)
                .joinToString(":") { byte -> "%02X".format(byte) }
        }.getOrDefault("")
    }
}

enum class SortOption {
    PUBLISHED_NEWEST,
    PUBLISHED_OLDEST,
    RECORDING_NEWEST,
    RECORDING_OLDEST,
}

data class FilterOptions(
    val channelName: String? = null,
    val country: String? = null,
)
