package dev.elainedb.ytdash_android_codex.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object VideoIntentHelper {
    fun openVideo(context: Context, videoId: String) {
        val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
        val browserIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/watch?v=$videoId")
        )
        try {
            context.startActivity(appIntent)
        } catch (_: ActivityNotFoundException) {
            context.startActivity(browserIntent)
        }
    }
}
