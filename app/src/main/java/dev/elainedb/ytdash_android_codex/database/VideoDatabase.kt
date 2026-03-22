package dev.elainedb.ytdash_android_codex.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [VideoEntity::class], version = 1, exportSchema = false)
abstract class VideoDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao

    companion object {
        @Volatile
        private var INSTANCE: VideoDatabase? = null

        fun getInstance(context: Context): VideoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VideoDatabase::class.java,
                    "videos.db"
                ).build().also { INSTANCE = it }
            }
    }
}
