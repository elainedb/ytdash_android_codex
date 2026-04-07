package dev.elainedb.ytdash_android_codex.domain.usecase

import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.core.usecases.UseCase
import dev.elainedb.ytdash_android_codex.domain.repository.AuthRepository
import javax.inject.Inject

class SignInWithGoogle @Inject constructor(
    private val authRepository: AuthRepository
) : UseCase<GoogleSignInAccount, Intent?>() {
    override suspend fun invoke(params: Intent?): Result<GoogleSignInAccount> =
        authRepository.handleSignInResult(params)
}
