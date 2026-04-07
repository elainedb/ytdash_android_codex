package dev.elainedb.ytdash_android_codex.domain.model

enum class SortOption(val dbValue: String, val label: String) {
    PUBLICATION_DATE_DESC("PUBLISHED_DESC", "Publication Date (Newest First)"),
    PUBLICATION_DATE_ASC("PUBLISHED_ASC", "Publication Date (Oldest First)"),
    RECORDING_DATE_DESC("RECORDING_DESC", "Recording Date (Newest First)"),
    RECORDING_DATE_ASC("RECORDING_ASC", "Recording Date (Oldest First)")
}
