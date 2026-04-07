package dev.elainedb.ytdash_android_codex.di

import dev.elainedb.ytdash_android_codex.data.auth.AuthRepositoryImpl
import dev.elainedb.ytdash_android_codex.data.repository.YouTubeRepositoryImpl
import dev.elainedb.ytdash_android_codex.domain.repository.AuthRepository
import dev.elainedb.ytdash_android_codex.domain.repository.VideoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindVideoRepository(impl: YouTubeRepositoryImpl): VideoRepository
}
