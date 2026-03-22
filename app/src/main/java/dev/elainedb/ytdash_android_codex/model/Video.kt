package dev.elainedb.ytdash_android_codex.model

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

fun Video.mergeWithDetails(
    detail: YouTubeVideoDetails?,
    city: String?,
    country: String?,
): Video {
    val recordingDetails = detail?.recordingDetails
    return copy(
        description = detail?.snippet?.description ?: description,
        tags = detail?.snippet?.tags.orEmpty(),
        locationCity = city,
        locationCountry = country,
        locationLatitude = recordingDetails?.location?.latitude,
        locationLongitude = recordingDetails?.location?.longitude,
        recordingDate = recordingDetails?.recordingDate,
    )
}
