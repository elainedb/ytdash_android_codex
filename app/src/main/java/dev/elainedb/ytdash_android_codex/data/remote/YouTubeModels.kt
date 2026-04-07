package dev.elainedb.ytdash_android_codex.data.remote

import dev.elainedb.ytdash_android_codex.domain.model.Video
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YouTubeSearchResponse(
    @SerialName("items") val items: List<YouTubeVideoItem> = emptyList(),
    @SerialName("nextPageToken") val nextPageToken: String? = null
)

@Serializable
data class YouTubeVideoItem(
    @SerialName("id") val id: YouTubeVideoId,
    @SerialName("snippet") val snippet: YouTubeVideoSnippet
)

@Serializable
data class YouTubeVideoId(
    @SerialName("videoId") val videoId: String? = null
)

@Serializable
data class YouTubeVideoSnippet(
    @SerialName("title") val title: String,
    @SerialName("channelTitle") val channelTitle: String,
    @SerialName("channelId") val channelId: String,
    @SerialName("publishedAt") val publishedAt: String,
    @SerialName("description") val description: String = "",
    @SerialName("thumbnails") val thumbnails: YouTubeThumbnails,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("locationDescription") val locationDescription: String? = null
)

@Serializable
data class YouTubeThumbnails(
    @SerialName("default") val default: YouTubeThumbnail? = null,
    @SerialName("medium") val medium: YouTubeThumbnail? = null,
    @SerialName("high") val high: YouTubeThumbnail? = null
)

@Serializable
data class YouTubeThumbnail(
    @SerialName("url") val url: String
)

@Serializable
data class YouTubeVideosResponse(
    @SerialName("items") val items: List<YouTubeVideoDetails> = emptyList()
)

@Serializable
data class YouTubeVideoDetails(
    @SerialName("id") val id: String,
    @SerialName("snippet") val snippet: YouTubeVideoDetailsSnippet? = null,
    @SerialName("recordingDetails") val recordingDetails: YouTubeRecordingDetails? = null
)

@Serializable
data class YouTubeVideoDetailsSnippet(
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("description") val description: String = "",
    @SerialName("channelTitle") val channelTitle: String? = null,
    @SerialName("locationDescription") val locationDescription: String? = null
)

@Serializable
data class YouTubeRecordingDetails(
    @SerialName("recordingDate") val recordingDate: String? = null,
    @SerialName("location") val location: YouTubeLocation? = null
)

@Serializable
data class YouTubeLocation(
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null
)

fun YouTubeVideoItem.toVideo(): Video {
    val thumbnailUrl = snippet.thumbnails.high?.url
        ?: snippet.thumbnails.medium?.url
        ?: snippet.thumbnails.default?.url
        ?: ""
    return Video(
        id = id.videoId.orEmpty(),
        title = snippet.title,
        channelName = snippet.channelTitle,
        channelId = snippet.channelId,
        publishedAt = snippet.publishedAt,
        thumbnailUrl = thumbnailUrl,
        description = snippet.description,
        tags = snippet.tags
    )
}

fun Video.mergeWithDetails(details: YouTubeVideoDetails, locationName: Pair<String?, String?>): Video {
    val location = details.recordingDetails?.location
    return copy(
        description = details.snippet?.description ?: description,
        tags = details.snippet?.tags?.ifEmpty { tags } ?: tags,
        locationCity = locationName.first,
        locationCountry = locationName.second,
        locationLatitude = location?.latitude,
        locationLongitude = location?.longitude,
        recordingDate = details.recordingDetails?.recordingDate
    )
}
