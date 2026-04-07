package dev.elainedb.ytdash_android_codex.domain.usecase

import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.core.usecases.UseCase
import dev.elainedb.ytdash_android_codex.domain.model.Video
import dev.elainedb.ytdash_android_codex.domain.repository.VideoRepository
import javax.inject.Inject

class GetVideosByCountryUseCase @Inject constructor(
    private val videoRepository: VideoRepository
) : UseCase<List<Video>, String>() {
    override suspend fun invoke(params: String): Result<List<Video>> {
        return videoRepository.getVideosByCountry(params)
    }
}
