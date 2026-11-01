package com.fable.app.data.local

import android.content.Context
import com.fable.app.domain.model.FableThemeName
import com.fable.app.domain.model.SavedThemePreference
import com.fable.app.domain.repository.ThemePreferenceRepository

class LocalThemePreferenceRepository(context: Context) : ThemePreferenceRepository {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun read(ownerId: String?): SavedThemePreference? {
        val key = themePreferenceKey(ownerId)
        val stored = preferences.getString(key, null) ?: return null
        return SavedThemePreference(
            theme = FableThemeName.fromStorage(stored),
            cloudSyncPending = preferences.getBoolean("$key.pending", false),
        )
    }

    override fun write(ownerId: String?, theme: FableThemeName, cloudSyncPending: Boolean) {
        val key = themePreferenceKey(ownerId)
        check(
            preferences.edit()
                .putString(key, theme.storageValue)
                .putBoolean("$key.pending", cloudSyncPending)
                .commit(),
        ) { "Theme preference could not be saved on this device." }
    }

    private companion object {
        const val PREFERENCES_NAME = "fable_theme_preferences"
    }
}

internal fun themePreferenceKey(ownerId: String?): String =
    "selected_theme.${ownerId?.takeIf(String::isNotBlank) ?: "anonymous"}"
