package dev.elainedb.ytdash_android_codex.domain.repository

import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dev.elainedb.ytdash_android_codex.core.error.Result

interface AuthRepository {
    fun signInIntent(): Intent
    suspend fun handleSignInResult(data: Intent?): Result<GoogleSignInAccount>
    suspend fun signOut(): Result<Unit>
}
