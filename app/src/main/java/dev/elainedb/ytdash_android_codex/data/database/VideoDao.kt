package dev.elainedb.ytdash_android_codex.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query(
        """
        SELECT * FROM videos
        WHERE (:channelName IS NULL OR channelName = :channelName)
          AND (:country IS NULL OR locationCountry = :country)
        ORDER BY
          CASE WHEN :sortBy = 'PUBLISHED_NEWEST' THEN publishedAt END DESC,
          CASE WHEN :sortBy = 'PUBLISHED_OLDEST' THEN publishedAt END ASC,
          CASE WHEN :sortBy = 'RECORDING_NEWEST' THEN recordingDate END DESC,
          CASE WHEN :sortBy = 'RECORDING_OLDEST' THEN recordingDate END ASC,
          publishedAt DESC
        """
    )
    fun getVideosWithFiltersAndSort(
        channelName: String?,
        country: String?,
        sortBy: String
    ): Flow<List<VideoEntity>>

    @Query("SELECT DISTINCT locationCountry FROM videos WHERE locationCountry IS NOT NULL AND locationCountry != '' ORDER BY locationCountry ASC")
    fun getDistinctCountries(): Flow<List<String>>

    @Query("SELECT DISTINCT channelName FROM videos ORDER BY channelName ASC")
    fun getDistinctChannels(): Flow<List<String>>

    @Query("SELECT * FROM videos WHERE locationLatitude IS NOT NULL AND locationLongitude IS NOT NULL")
    fun getVideosWithLocation(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE cacheTimestamp >= :threshold")
    suspend fun getVideosNewerThan(threshold: Long): List<VideoEntity>

    @Query("SELECT COUNT(*) FROM videos")
    suspend fun getTotalVideoCount(): Int

    @Query("SELECT * FROM videos ORDER BY publishedAt DESC")
    suspend fun getAllVideos(): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE channelName = :channelName ORDER BY publishedAt DESC")
    suspend fun getVideosByChannel(channelName: String): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE locationCountry = :country ORDER BY publishedAt DESC")
    suspend fun getVideosByCountry(country: String): List<VideoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :videoId")
    suspend fun deleteVideo(videoId: String)

    @Query("DELETE FROM videos")
    suspend fun deleteAllVideos()

    @Query("DELETE FROM videos WHERE cacheTimestamp < :threshold")
    suspend fun deleteOldVideos(threshold: Long)
}
