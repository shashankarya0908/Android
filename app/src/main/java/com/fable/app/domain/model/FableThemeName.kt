package com.fable.app.domain.model

enum class FableThemeName(val storageValue: String, val label: String, val description: String) {
    GOTHIC("Gothic", "Gothic", "Charcoal, antique ivory, and literary serif headings."),
    GOTHIC_ROMANCE("Gothic Romance", "Gothic Romance", "Wine red, muted rose, and warm ivory."),
    DARK_ACADEMIA("Dark Academia", "Dark Academia", "Parchment, espresso, forest green, and muted gold."),
    DREAMSCAPE("Dreamscape", "Dreamscape", "Misty lavender, midnight blue, and soft silver."),
    MINIMALIST("Minimalist", "Minimalist", "Warm cream, forest green, and quiet neutral tones."),
    ;

    companion object {
        val DEFAULT = MINIMALIST

        fun fromStorage(value: String?): FableThemeName =
            entries.firstOrNull { it.storageValue.equals(value?.trim(), ignoreCase = true) } ?: DEFAULT
    }
}

data class SavedThemePreference(
    val theme: FableThemeName,
    val cloudSyncPending: Boolean,
)

fun restoreThemePreference(
    local: SavedThemePreference?,
    cloudTheme: String?,
    cloudAvailable: Boolean,
): FableThemeName = when {
    local?.cloudSyncPending == true -> local.theme
    cloudAvailable -> FableThemeName.fromStorage(cloudTheme)
    else -> local?.theme ?: FableThemeName.DEFAULT
}
