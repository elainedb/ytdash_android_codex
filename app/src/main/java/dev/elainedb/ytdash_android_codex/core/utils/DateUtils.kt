package dev.elainedb.ytdash_android_codex.core.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object DateUtils {
    private val displayFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun formatIsoDateTime(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            Instant.parse(value).atZone(ZoneOffset.UTC).toLocalDate().format(displayFormatter)
        }.getOrNull()
    }

    fun formatIsoDate(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            LocalDate.parse(value).format(displayFormatter)
        }.getOrElse { formatIsoDateTime(value) }
    }

    fun currentTimestamp(): Long = System.currentTimeMillis()
}
