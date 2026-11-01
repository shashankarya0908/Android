package com.fable.app.theme

import com.fable.app.data.local.themePreferenceKey
import com.fable.app.domain.model.FableThemeName
import com.fable.app.domain.model.SavedThemePreference
import com.fable.app.domain.model.restoreThemePreference
import com.fable.app.presentation.theme.fableThemeDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePreferenceTest {
    @Test fun minimalIsTheDefaultAndUnknownStoredThemesFallBackSafely() {
        assertEquals(FableThemeName.MINIMALIST, FableThemeName.DEFAULT)
        assertEquals(FableThemeName.MINIMALIST, FableThemeName.fromStorage(null))
        assertEquals(FableThemeName.MINIMALIST, FableThemeName.fromStorage("old-or-invalid-theme"))
        assertEquals(FableThemeName.DARK_ACADEMIA, FableThemeName.fromStorage("Dark Academia"))
    }

    @Test fun allFiveThemeDefinitionsHaveDistinctPalettesAndReadableBodyText() {
        assertEquals(5, FableThemeName.entries.size)
        val definitions = FableThemeName.entries.map(::fableThemeDefinition)
        assertEquals(5, definitions.map { it.palette.background to it.palette.primary }.distinct().size)
        assertTrue(definitions.all { it.palette.onSurface != it.palette.surface })
        assertTrue(definitions.all { it.typography.bodyMedium.fontFamily != null })
    }

    @Test fun signedOutThemeRestoresFromLocalPreferenceWhenCloudIsUnavailable() {
        val restored = restoreThemePreference(
            SavedThemePreference(FableThemeName.DREAMSCAPE, cloudSyncPending = false),
            cloudTheme = null,
            cloudAvailable = false,
        )
        assertEquals(FableThemeName.DREAMSCAPE, restored)
    }

    @Test fun cloudProfileWinsAfterSyncButPendingLocalChoiceCanRetry() {
        val local = SavedThemePreference(FableThemeName.GOTHIC_ROMANCE, cloudSyncPending = false)
        assertEquals(FableThemeName.DARK_ACADEMIA,
            restoreThemePreference(local, "Dark Academia", cloudAvailable = true))
        assertEquals(FableThemeName.GOTHIC_ROMANCE,
            restoreThemePreference(local.copy(cloudSyncPending = true), "Dark Academia", cloudAvailable = true))
    }

    @Test fun localPreferenceKeysAreSeparatedForEachAccountAndAnonymousUse() {
        assertNotEquals(themePreferenceKey("uid-a"), themePreferenceKey("uid-b"))
        assertNotEquals(themePreferenceKey(null), themePreferenceKey("uid-a"))
        assertEquals(themePreferenceKey(null), themePreferenceKey(" "))
    }
}
