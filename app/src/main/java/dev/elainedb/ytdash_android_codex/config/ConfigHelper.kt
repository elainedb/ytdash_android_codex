package dev.elainedb.ytdash_android_codex.config

import android.content.Context
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigHelper @Inject constructor(
    private val context: Context
) {
    private val properties: Properties by lazy { loadProperties() }

    val authorizedEmails: Set<String>
        get() = properties.getProperty("authorized_emails")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()

    val youtubeApiKey: String
        get() = properties.getProperty("youtubeApiKey").orEmpty()

    private fun loadProperties(): Properties {
        val merged = Properties()
        listOf(
            "config.properties.template",
            "config.properties.ci",
            "config.properties"
        ).forEach { fileName ->
            runCatching {
                context.assets.open(fileName).use { merged.load(it) }
            }
        }

        if (!merged.containsKey("authorized_emails")) {
            merged["authorized_emails"] = ""
        }
        if (!merged.containsKey("youtubeApiKey")) {
            merged["youtubeApiKey"] = ""
        }

        return merged
    }
}
