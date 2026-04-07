package dev.elainedb.ytdash_android_codex.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object DateFormatter {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun toDisplayDate(isoDateTime: String?): String {
        if (isoDateTime.isNullOrBlank()) return "Unknown"
        return runCatching {
            Instant.parse(isoDateTime).atZone(ZoneOffset.UTC).toLocalDate().format(formatter)
        }.getOrElse {
            runCatching { LocalDate.parse(isoDateTime.take(10)).format(formatter) }.getOrDefault(isoDateTime)
        }
    }
}
