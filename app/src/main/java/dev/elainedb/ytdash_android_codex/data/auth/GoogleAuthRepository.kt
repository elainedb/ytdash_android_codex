package dev.elainedb.ytdash_android_codex.data.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.elainedb.ytdash_android_codex.R
import dev.elainedb.ytdash_android_codex.core.error.Failure
import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.domain.repository.AuthRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class GoogleAuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthRepository {

    private val googleSignInClient by lazy {
        val defaultWebClientId = runCatching { context.getString(R.string.default_web_client_id) }.getOrDefault("")
        val optionsBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()

        if (defaultWebClientId.isNotBlank() && !defaultWebClientId.contains("dummy", ignoreCase = true)) {
            optionsBuilder.requestIdToken(defaultWebClientId)
        }

        GoogleSignIn.getClient(context, optionsBuilder.build())
    }

    override fun signInIntent(): Intent = googleSignInClient.signInIntent

    override suspend fun handleSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            if (account == null || account.email.isNullOrBlank()) {
                Result.Error(Failure.Auth("Unable to retrieve Google account email."))
            } else {
                Result.Success(account)
            }
        } catch (exception: ApiException) {
            Result.Error(Failure.Auth("Google Sign-In failed: ${exception.statusCode}"))
        } catch (exception: Exception) {
            Result.Error(Failure.Unexpected(exception.message ?: "Unexpected sign-in error."))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            googleSignInClient.signOut()
                .addOnCompleteListener {
                    continuation.resume(Result.Success(Unit))
                }
                .addOnFailureListener { error ->
                    continuation.resume(Result.Error(Failure.Auth(error.message ?: "Failed to sign out.")))
                }
        }
    }
}
