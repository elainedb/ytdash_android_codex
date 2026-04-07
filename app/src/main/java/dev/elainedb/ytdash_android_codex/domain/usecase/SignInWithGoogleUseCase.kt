package dev.elainedb.ytdash_android_codex.domain.usecase

import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.core.usecases.UseCase
import dev.elainedb.ytdash_android_codex.domain.model.User
import dev.elainedb.ytdash_android_codex.domain.repository.AuthRepository
import javax.inject.Inject

data class SignInParams(
    val email: String?,
    val displayName: String?
)

class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) : UseCase<User, SignInParams>() {
    override suspend fun invoke(params: SignInParams): Result<User> {
        return authRepository.validateSignedInUser(params.email, params.displayName)
    }
}
