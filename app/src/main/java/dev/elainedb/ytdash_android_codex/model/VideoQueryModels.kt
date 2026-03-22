package dev.elainedb.ytdash_android_codex.model

data class FilterOptions(
    val channelName: String? = null,
    val country: String? = null,
)

enum class SortOption {
    PUBLICATION_DATE_DESC,
    PUBLICATION_DATE_ASC,
    RECORDING_DATE_DESC,
    RECORDING_DATE_ASC,
}
