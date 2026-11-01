package com.fable.app.domain.repository

import com.fable.app.domain.model.FableThemeName
import com.fable.app.domain.model.SavedThemePreference

interface ThemePreferenceRepository {
    fun read(ownerId: String?): SavedThemePreference?
    fun write(ownerId: String?, theme: FableThemeName, cloudSyncPending: Boolean)
}
