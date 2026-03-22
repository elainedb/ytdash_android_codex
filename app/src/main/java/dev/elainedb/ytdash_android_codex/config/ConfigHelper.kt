package dev.elainedb.ytdash_android_codex.config

import android.content.Context
import java.util.Properties

data class AppConfig(
    val authorizedEmails: Set<String>,
    val youtubeApiKey: String,
)

object ConfigHelper {
    private const val LOCAL_CONFIG = "config.properties"
    private const val CI_CONFIG = "config.properties.ci"
    private const val TEMPLATE_CONFIG = "config.properties.template"

    private val fallbackConfig = AppConfig(
        authorizedEmails = emptySet(),
        youtubeApiKey = "",
    )

    fun loadConfig(context: Context): AppConfig {
        val properties = Properties()
        val assetManager = context.assets
        val sources = listOf(LOCAL_CONFIG, CI_CONFIG, TEMPLATE_CONFIG)

        for (source in sources) {
            runCatching {
                assetManager.open(source).use { properties.load(it) }
                return createConfig(properties)
            }
        }

        return fallbackConfig
    }

    private fun createConfig(properties: Properties): AppConfig {
        val authorizedEmails = properties.getProperty("authorized_emails")
            .orEmpty()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        return AppConfig(
            authorizedEmails = authorizedEmails,
            youtubeApiKey = properties.getProperty("youtubeApiKey").orEmpty(),
        )
    }
}
