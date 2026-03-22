package dev.elainedb.ytdash_android_codex.utils

import android.content.Context
import java.util.Properties

object ConfigHelper {
    private const val CONFIG_FILE = "config.properties"
    private const val CONFIG_CI_FILE = "config.properties.ci"
    private const val CONFIG_TEMPLATE_FILE = "config.properties.template"
    private const val DEFAULT_YOUTUBE_API_KEY = ""

    private data class CachedConfig(
        val authorizedEmails: Set<String>,
        val youtubeApiKey: String,
    )

    @Volatile
    private var cachedConfig: CachedConfig? = null

    fun getAuthorizedEmails(context: Context): Set<String> = load(context).authorizedEmails

    fun getYoutubeApiKey(context: Context): String = load(context).youtubeApiKey

    private fun load(context: Context): CachedConfig {
        cachedConfig?.let { return it }

        return synchronized(this) {
            cachedConfig?.let { return it }

            listOf(CONFIG_FILE, CONFIG_CI_FILE, CONFIG_TEMPLATE_FILE).forEach { name ->
                val loadedConfig = runCatching {
                    val properties = Properties()
                    context.assets.open(name).use { stream ->
                        properties.load(stream)
                    }
                    CachedConfig(
                        authorizedEmails = properties.getProperty("authorized_emails")
                            .orEmpty()
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .toSet(),
                        youtubeApiKey = properties.getProperty("youtubeApiKey", DEFAULT_YOUTUBE_API_KEY).trim(),
                    )
                }.getOrNull()

                if (loadedConfig != null) {
                    cachedConfig = loadedConfig
                    return@synchronized loadedConfig
                }
            }

            CachedConfig(
                authorizedEmails = emptySet(),
                youtubeApiKey = DEFAULT_YOUTUBE_API_KEY,
            ).also { cachedConfig = it }
        }
    }
}
