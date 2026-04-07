package dev.elainedb.ytdash_android_codex.domain.usecase

import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.core.usecases.UseCase
import dev.elainedb.ytdash_android_codex.domain.model.Video
import dev.elainedb.ytdash_android_codex.domain.repository.GetVideosParams
import dev.elainedb.ytdash_android_codex.domain.repository.YouTubeRepository
import javax.inject.Inject

class GetVideos @Inject constructor(
    private val repository: YouTubeRepository
) : UseCase<List<Video>, GetVideosParams>() {
    override suspend fun invoke(params: GetVideosParams): Result<List<Video>> {
        return if (params.forceRefresh) {
            repository.refreshVideos(params.channelIds)
        } else {
            repository.getLatestVideos(params.channelIds)
        }
    }
}
