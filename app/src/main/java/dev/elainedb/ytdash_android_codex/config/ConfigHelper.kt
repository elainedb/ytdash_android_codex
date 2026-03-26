package dev.elainedb.ytdash_android_codex.config

import android.content.Context
import java.util.Properties

class ConfigHelper(private val context: Context) {

    private val properties: Properties by lazy { loadProperties() }

    fun getAuthorizedEmails(): Set<String> {
        val configured = properties.getProperty(KEY_AUTHORIZED_EMAILS)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()
        return if (configured.isNotEmpty()) configured else FALLBACK_AUTHORIZED_EMAILS
    }

    fun getYouTubeApiKey(): String {
        return properties.getProperty(KEY_YOUTUBE_API_KEY)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: FALLBACK_YOUTUBE_API_KEY
    }

    private fun loadProperties(): Properties {
        val props = Properties()
        ASSET_FILE_CANDIDATES.forEach { assetName ->
            runCatching {
                context.assets.open(assetName).use(props::load)
                return props
            }
        }
        props[KEY_AUTHORIZED_EMAILS] = FALLBACK_AUTHORIZED_EMAILS.joinToString(",")
        props[KEY_YOUTUBE_API_KEY] = FALLBACK_YOUTUBE_API_KEY
        return props
    }

    companion object {
        private const val KEY_AUTHORIZED_EMAILS = "authorized_emails"
        private const val KEY_YOUTUBE_API_KEY = "youtubeApiKey"

        private val ASSET_FILE_CANDIDATES = listOf(
            "config.properties",
            "config.properties.ci",
            "config.properties.template"
        )

        private val FALLBACK_AUTHORIZED_EMAILS = setOf(
            "elaine.batista1105@gmail.com",
            "paulamcunha31@gmail.com",
            "edbpmc@gmail.com"
        )

        private const val FALLBACK_YOUTUBE_API_KEY = "AIzaSyCwrsNF4bGNlP6q1O_CAP4oaZFHZEXekmc"
    }
}
