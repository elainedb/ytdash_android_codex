package dev.elainedb.ytdash_android_codex.domain.model

data class FilterOptions(
    val channelName: String? = null,
    val country: String? = null
) {
    val hasActiveFilters: Boolean
        get() = !channelName.isNullOrBlank() || !country.isNullOrBlank()
}
