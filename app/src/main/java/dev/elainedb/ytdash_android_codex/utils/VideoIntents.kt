package dev.elainedb.ytdash_android_codex.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

fun openVideo(context: Context, videoId: String) {
    val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
    val browserIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://www.youtube.com/watch?v=$videoId")
    )

    runCatching {
        context.startActivity(appIntent)
    }.recoverCatching {
        if (it is ActivityNotFoundException) {
            context.startActivity(browserIntent)
        } else {
            throw it
        }
    }
}
