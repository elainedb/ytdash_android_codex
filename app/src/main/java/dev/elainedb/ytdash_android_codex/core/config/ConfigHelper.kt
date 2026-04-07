package dev.elainedb.ytdash_android_codex.core.config

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val properties: Properties by lazy {
        val result = Properties()
        val files = listOf(
            "config.properties",
            "config.properties.ci",
            "config.properties.template"
        )

        for (file in files) {
            runCatching {
                context.assets.open(file).use(result::load)
            }.onSuccess {
                if (result.isNotEmpty()) return@lazy result
            }
        }

        result.apply {
            putIfAbsent("authorized_emails", "")
            putIfAbsent("youtubeApiKey", "")
        }
    }

    fun getAuthorizedEmails(): Set<String> {
        return properties
            .getProperty("authorized_emails", "")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    fun getYoutubeApiKey(): String = properties.getProperty("youtubeApiKey", "")
}
