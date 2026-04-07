package dev.elainedb.ytdash_android_codex.di

import android.content.Context
import dev.elainedb.ytdash_android_codex.data.auth.GoogleAuthRepository
import dev.elainedb.ytdash_android_codex.data.local.VideoDao
import dev.elainedb.ytdash_android_codex.data.local.VideoDatabase
import dev.elainedb.ytdash_android_codex.data.remote.YouTubeApiService
import dev.elainedb.ytdash_android_codex.data.repository.YouTubeRepositoryImpl
import dev.elainedb.ytdash_android_codex.domain.repository.AuthRepository
import dev.elainedb.ytdash_android_codex.domain.repository.YouTubeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val request = chain.request().newBuilder()
                    .header("X-Android-Package", context.packageName)
                    .header("X-Android-Cert", packageInfo.versionName ?: "debug")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideYouTubeApiService(json: Json, okHttpClient: OkHttpClient): YouTubeApiService {
        return Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/youtube/v3/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(YouTubeApiService::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VideoDatabase =
        VideoDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideVideoDao(database: VideoDatabase): VideoDao = database.videoDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindAuthRepository(repository: GoogleAuthRepository): AuthRepository

    @Binds
    abstract fun bindYouTubeRepository(repository: YouTubeRepositoryImpl): YouTubeRepository
}
