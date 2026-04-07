package dev.elainedb.ytdash_android_codex.domain.usecase

import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.core.usecases.UseCase
import dev.elainedb.ytdash_android_codex.domain.repository.AuthRepository
import javax.inject.Inject

class SignOut @Inject constructor(
    private val authRepository: AuthRepository
) : UseCase<Unit, Unit>() {
    override suspend fun invoke(params: Unit): Result<Unit> = authRepository.signOut()
}
