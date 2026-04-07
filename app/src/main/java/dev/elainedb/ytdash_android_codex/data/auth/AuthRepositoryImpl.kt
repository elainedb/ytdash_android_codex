package dev.elainedb.ytdash_android_codex.data.auth

import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dev.elainedb.ytdash_android_codex.core.config.ConfigHelper
import dev.elainedb.ytdash_android_codex.core.error.Failure
import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.domain.model.User
import dev.elainedb.ytdash_android_codex.domain.repository.AuthRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val googleAuthManager: GoogleAuthManager,
    private val configHelper: ConfigHelper
) : AuthRepository {

    override fun getSignInIntent(): Intent = googleAuthManager.client.signInIntent

    override suspend fun completeSignIn(account: GoogleSignInAccount?): Result<User> {
        val email = account?.email?.trim().orEmpty()
        if (email.isBlank()) {
            return Result.Error(Failure.Auth("Google account email was unavailable."))
        }

        val authorizedEmails = configHelper.getAuthorizedEmails()
        if (email !in authorizedEmails) {
            signOut()
            return Result.Error(Failure.Auth("Access denied. Your email is not authorized."))
        }

        Log.i("AuthRepository", "Access granted to $email")
        return Result.Success(
            User(
                email = email,
                displayName = account?.displayName
            )
        )
    }

    override suspend fun signOut(): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            googleAuthManager.client.signOut().addOnCompleteListener {
                continuation.resume(Result.Success(Unit))
            }.addOnFailureListener { error ->
                continuation.resume(Result.Error(Failure.Auth(error.message ?: "Sign out failed.")))
            }
        }
    }

    override fun getSignedInUser(): User? {
        val account = googleAuthManager.getLastSignedInAccount() ?: return null
        val email = account.email ?: return null
        return if (email in configHelper.getAuthorizedEmails()) {
            User(email = email, displayName = account.displayName)
        } else {
            null
        }
    }
}
