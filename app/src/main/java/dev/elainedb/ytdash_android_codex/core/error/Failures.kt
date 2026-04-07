package dev.elainedb.ytdash_android_codex.core.error

sealed class Failure(
    open val message: String
) {
    data class Server(override val message: String) : Failure(message)
    data class Cache(override val message: String) : Failure(message)
    data class Network(override val message: String) : Failure(message)
    data class Auth(override val message: String) : Failure(message)
    data class Validation(override val message: String) : Failure(message)
    data class Unexpected(override val message: String) : Failure(message)
}
