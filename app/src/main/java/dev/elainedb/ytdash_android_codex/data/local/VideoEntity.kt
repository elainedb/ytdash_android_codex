package dev.elainedb.ytdash_android_codex.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.elainedb.ytdash_android_codex.domain.model.Video

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val channelName: String,
    val channelId: String,
    val publishedAt: String,
    val thumbnailUrl: String,
    val description: String,
    val tags: String,
    val locationCity: String?,
    val locationCountry: String?,
    val locationLatitude: Double?,
    val locationLongitude: Double?,
    val recordingDate: String?,
    val cacheTimestamp: Long
)

fun VideoEntity.toVideo(): Video = Video(
    id = id,
    title = title,
    channelName = channelName,
    channelId = channelId,
    publishedAt = publishedAt,
    thumbnailUrl = thumbnailUrl,
    description = description,
    tags = tags.split(",").map { it.trim() }.filter { it.isNotBlank() },
    locationCity = locationCity,
    locationCountry = locationCountry,
    locationLatitude = locationLatitude,
    locationLongitude = locationLongitude,
    recordingDate = recordingDate
)

fun Video.toEntity(cacheTimestamp: Long): VideoEntity = VideoEntity(
    id = id,
    title = title,
    channelName = channelName,
    channelId = channelId,
    publishedAt = publishedAt,
    thumbnailUrl = thumbnailUrl,
    description = description,
    tags = tags.joinToString(","),
    locationCity = locationCity,
    locationCountry = locationCountry,
    locationLatitude = locationLatitude,
    locationLongitude = locationLongitude,
    recordingDate = recordingDate,
    cacheTimestamp = cacheTimestamp
)
