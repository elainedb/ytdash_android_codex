package dev.elainedb.ytdash_android_codex.domain.usecase

import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.core.usecases.UseCase
import dev.elainedb.ytdash_android_codex.domain.model.Video
import dev.elainedb.ytdash_android_codex.domain.repository.YouTubeRepository
import javax.inject.Inject

class GetVideosByCountry @Inject constructor(
    private val repository: YouTubeRepository
) : UseCase<List<Video>, String>() {
    override suspend fun invoke(params: String): Result<List<Video>> = repository.getVideosByCountry(params)
}
