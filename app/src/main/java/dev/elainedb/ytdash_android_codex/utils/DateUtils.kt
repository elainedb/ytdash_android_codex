package dev.elainedb.ytdash_android_codex.utils

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object DateUtils {
    private val outputFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun toDisplayDate(isoDateTime: String?): String? {
        if (isoDateTime.isNullOrBlank()) return null
        return runCatching {
            OffsetDateTime.parse(isoDateTime).format(outputFormatter)
        }.getOrElse { isoDateTime.take(10) }
    }
}
