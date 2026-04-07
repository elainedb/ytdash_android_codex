package dev.elainedb.ytdash_android_codex.domain.usecase

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.core.usecases.UseCase
import dev.elainedb.ytdash_android_codex.domain.model.User
import dev.elainedb.ytdash_android_codex.domain.repository.AuthRepository
import javax.inject.Inject

class SignInWithGoogle @Inject constructor(
    private val authRepository: AuthRepository
) : UseCase<User, GoogleSignInAccount?>() {
    override suspend fun invoke(params: GoogleSignInAccount?): Result<User> {
        return authRepository.completeSignIn(params)
    }
}
