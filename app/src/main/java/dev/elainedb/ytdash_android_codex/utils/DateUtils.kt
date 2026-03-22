package dev.elainedb.ytdash_android_codex.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object DateUtils {
    private val outputFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun formatIsoDate(value: String?): String {
        if (value.isNullOrBlank()) return ""
        return runCatching {
            Instant.parse(value).atZone(ZoneOffset.UTC).toLocalDate().format(outputFormatter)
        }.getOrElse {
            runCatching {
                LocalDate.parse(value.take(10)).format(outputFormatter)
            }.getOrDefault(value)
        }
    }
}
