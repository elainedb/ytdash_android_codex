package dev.elainedb.ytdash_android_codex.domain.usecase

import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.core.usecases.UseCase
import dev.elainedb.ytdash_android_codex.domain.model.Video
import dev.elainedb.ytdash_android_codex.domain.repository.VideoRepository
import javax.inject.Inject

data class GetVideosParams(
    val channelIds: List<String> = emptyList(),
    val forceRefresh: Boolean = false
)

class GetVideos @Inject constructor(
    private val videoRepository: VideoRepository
) : UseCase<List<Video>, GetVideosParams>() {
    override suspend fun invoke(params: GetVideosParams): Result<List<Video>> {
        return if (params.forceRefresh) {
            videoRepository.refreshVideos()
        } else {
            videoRepository.getLatestVideos()
        }
    }
}
