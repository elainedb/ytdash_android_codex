package dev.elainedb.ytdash_android_codex.data.config

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEY_AUTHORIZED_EMAILS = "authorized_emails"
        private const val KEY_API_KEY = "youtubeApiKey"
        private val FALLBACK_AUTHORIZED_EMAILS = emptySet<String>()
        private const val FALLBACK_API_KEY = ""
        private const val LOCAL_CONFIG = "config.properties"
        private const val CI_CONFIG = "config.properties.ci"
        private const val TEMPLATE_CONFIG = "config.properties.template"
    }

    @Volatile
    private var cachedConfig: AppConfig? = null

    fun getConfig(): AppConfig {
        return cachedConfig ?: synchronized(this) {
            cachedConfig ?: loadConfig().also { cachedConfig = it }
        }
    }

    private fun loadConfig(): AppConfig {
        val mergedProperties = loadProperties(LOCAL_CONFIG)
            ?: loadProperties(CI_CONFIG)
            ?: loadProperties(TEMPLATE_CONFIG)
            ?: Properties()

        val authorizedEmails = mergedProperties.getProperty(KEY_AUTHORIZED_EMAILS)
            ?.split(",")
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?.ifEmpty { FALLBACK_AUTHORIZED_EMAILS }
            ?: FALLBACK_AUTHORIZED_EMAILS

        val apiKey = mergedProperties.getProperty(KEY_API_KEY)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: FALLBACK_API_KEY

        return AppConfig(
            authorizedEmails = authorizedEmails,
            youtubeApiKey = apiKey
        )
    }

    private fun loadProperties(fileName: String): Properties? {
        return runCatching {
            Properties().apply {
                context.assets.open(fileName).use { load(it) }
            }
        }.getOrNull()
    }
}
