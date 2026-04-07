package dev.elainedb.ytdash_android_codex.di

import android.content.Context
import androidx.room.Room
import dev.elainedb.ytdash_android_codex.data.database.VideoDao
import dev.elainedb.ytdash_android_codex.data.database.VideoDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideVideoDatabase(@ApplicationContext context: Context): VideoDatabase {
        return Room.databaseBuilder(
            context,
            VideoDatabase::class.java,
            "video_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideVideoDao(videoDatabase: VideoDatabase): VideoDao = videoDatabase.videoDao()
}
