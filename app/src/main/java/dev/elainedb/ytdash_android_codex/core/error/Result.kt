package dev.elainedb.ytdash_android_codex.core.error

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val failure: Failure) : Result<Nothing>()
}
