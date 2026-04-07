package dev.elainedb.ytdash_android_codex.domain.repository

import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.domain.model.User

interface AuthRepository {
    fun getSignInIntent(): Intent
    suspend fun completeSignIn(account: GoogleSignInAccount?): Result<User>
    suspend fun signOut(): Result<Unit>
    fun getSignedInUser(): User?
}
