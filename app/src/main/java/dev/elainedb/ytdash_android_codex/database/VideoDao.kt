package dev.elainedb.ytdash_android_codex.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.elainedb.ytdash_android_codex.viewmodel.SortOption
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query(
        """
        SELECT * FROM videos
        WHERE (:channelName IS NULL OR channelName = :channelName)
        AND (:country IS NULL OR locationCountry = :country)
        ORDER BY
            CASE WHEN :sortBy = 'PUBLISHED_DESC' THEN publishedAt END DESC,
            CASE WHEN :sortBy = 'PUBLISHED_ASC' THEN publishedAt END ASC,
            CASE WHEN :sortBy = 'RECORDING_DESC' THEN COALESCE(recordingDate, publishedAt) END DESC,
            CASE WHEN :sortBy = 'RECORDING_ASC' THEN COALESCE(recordingDate, publishedAt) END ASC,
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
    suspend fun getVideosWithLocation(): List<VideoEntity>

    @Query("SELECT * FROM videos WHERE cacheTimestamp >= :threshold")
    suspend fun getVideosNewerThan(threshold: Long): List<VideoEntity>

    @Query("SELECT COUNT(*) FROM videos")
    fun getTotalVideoCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoEntity)

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideo(id: String)

    @Query("DELETE FROM videos")
    suspend fun deleteAllVideos()

    @Query("DELETE FROM videos WHERE cacheTimestamp < :threshold")
    suspend fun deleteOldVideos(threshold: Long)
}
