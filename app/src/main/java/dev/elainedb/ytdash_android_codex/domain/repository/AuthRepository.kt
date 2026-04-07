package dev.elainedb.ytdash_android_codex.domain.repository

import dev.elainedb.ytdash_android_codex.core.error.Result
import dev.elainedb.ytdash_android_codex.domain.model.User

interface AuthRepository {
    suspend fun validateSignedInUser(email: String?, displayName: String?): Result<User>
    suspend fun signOut(): Result<Unit>
}
