package dev.elainedb.ytdash_android_codex.domain.model

enum class SortOption(val queryValue: String, val label: String) {
    PUBLICATION_DATE_DESC("published_desc", "Publication Date (Newest First)"),
    PUBLICATION_DATE_ASC("published_asc", "Publication Date (Oldest First)"),
    RECORDING_DATE_DESC("recording_desc", "Recording Date (Newest First)"),
    RECORDING_DATE_ASC("recording_asc", "Recording Date (Oldest First)");
}
