package dev.elainedb.ytdash_android_codex.model

import dev.elainedb.ytdash_android_codex.network.YouTubeLocation
import dev.elainedb.ytdash_android_codex.network.YouTubeVideoDetails
import dev.elainedb.ytdash_android_codex.network.YouTubeVideoItem

data class Video(
    val id: String,
    val title: String,
    val channelName: String,
    val channelId: String,
    val publishedAt: String,
    val thumbnailUrl: String,
    val description: String,
    val tags: List<String> = emptyList(),
    val locationCity: String? = null,
    val locationCountry: String? = null,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val recordingDate: String? = null,
)

fun YouTubeVideoItem.toVideo(): Video {
    return Video(
        id = id.videoId,
        title = snippet.title,
        channelName = snippet.channelTitle,
        channelId = snippet.channelId,
        publishedAt = snippet.publishedAt,
        thumbnailUrl = snippet.thumbnails.high?.url
            ?: snippet.thumbnails.medium?.url
            ?: snippet.thumbnails.default?.url
            .orEmpty(),
        description = snippet.description,
    )
}

fun Video.mergeWithDetails(details: YouTubeVideoDetails): Video {
    val location: YouTubeLocation? = details.recordingDetails?.location
    return copy(
        description = details.snippet.description.ifBlank { description },
        tags = details.snippet.tags.orEmpty(),
        locationLatitude = location?.latitude,
        locationLongitude = location?.longitude,
        recordingDate = details.recordingDetails?.recordingDate,
    )
}
