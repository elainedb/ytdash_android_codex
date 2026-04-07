package dev.elainedb.ytdash_android_codex.data.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dev.elainedb.ytdash_android_codex.R
import dev.elainedb.ytdash_android_codex.config.ConfigHelper
import dev.elainedb.ytdash_android_codex.core.error.Failure
import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.domain.model.User
import dev.elainedb.ytdash_android_codex.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configHelper: ConfigHelper
) : AuthRepository {
    override suspend fun validateSignedInUser(email: String?, displayName: String?): Result<User> {
        val normalizedEmail = email?.trim()?.lowercase()
            ?: return Result.Error(Failure.Auth(context.getString(R.string.login_error_generic)))
        val authorized = configHelper.authorizedEmails.map { it.lowercase() }.toSet()
        return if (normalizedEmail in authorized) {
            android.util.Log.i("LoginActivity", "Access granted to $normalizedEmail")
            Result.Success(User(normalizedEmail, displayName))
        } else {
            signOut()
            Result.Error(Failure.Auth(context.getString(R.string.login_error_denied)))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return runCatching {
            GoogleSignIn.getClient(
                context,
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
            ).signOut().await()
            Unit
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(Failure.Auth(it.message ?: "Sign out failed")) }
        )
    }
}
