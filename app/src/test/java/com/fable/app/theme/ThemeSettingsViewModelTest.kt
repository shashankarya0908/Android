package com.fable.app.theme

import com.fable.app.domain.model.FableThemeName
import com.fable.app.domain.model.SavedThemePreference
import com.fable.app.domain.model.UserProfile
import com.fable.app.domain.repository.ThemePreferenceRepository
import com.fable.app.domain.repository.UserRepository
import com.fable.app.presentation.theme.ThemeSettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ThemeSettingsViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun localFallbackDoesNotClaimCloudSynchronization() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val prefs = FakeThemePreferences()
            val viewModel = ThemeSettingsViewModel(FakeUserRepository(), prefs)
            viewModel.select(ownerId = null, profile = null, theme = FableThemeName.GOTHIC, cloudAvailable = false)
            advanceUntilIdle()
            assertEquals(FableThemeName.GOTHIC, viewModel.state.value.selectedTheme)
            assertEquals("Theme saved on this device.", viewModel.state.value.message)
            assertEquals(false, prefs.read(null)?.cloudSyncPending)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun cloudSelectionUpdatesExistingSelectedThemeFieldAndMarksLocalCacheSynced() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val prefs = FakeThemePreferences()
            val users = FakeUserRepository()
            val profile = UserProfile("uid-a", "reader_a", "Reader", "private@example.com", bio = "Hello")
            val viewModel = ThemeSettingsViewModel(users, prefs)
            viewModel.select(profile.uid, profile, FableThemeName.DARK_ACADEMIA, cloudAvailable = true)
            advanceUntilIdle()
            assertEquals("Dark Academia", users.updated?.selectedTheme)
            assertEquals(false, prefs.read(profile.uid)?.cloudSyncPending)
            assertEquals("Theme saved to your profile.", viewModel.state.value.message)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun pendingLocalChoiceRestoresAndRetriesProfileSynchronization() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val prefs = FakeThemePreferences().apply {
                write("uid-a", FableThemeName.DREAMSCAPE, cloudSyncPending = true)
            }
            val users = FakeUserRepository()
            val profile = UserProfile("uid-a", "reader_a", "Reader", "private@example.com", bio = "Hello")
            val viewModel = ThemeSettingsViewModel(users, prefs)
            viewModel.restore("uid-a", profile, cloudAvailable = true)
            advanceUntilIdle()
            assertEquals(FableThemeName.DREAMSCAPE, viewModel.state.value.selectedTheme)
            assertEquals("Dreamscape", users.updated?.selectedTheme)
            assertFalse(prefs.read("uid-a")?.cloudSyncPending ?: true)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun cloudFailureKeepsLocalThemeAndPendingSyncWithoutClaimingSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val prefs = FakeThemePreferences()
            val users = FakeUserRepository().apply { shouldFail = true }
            val profile = UserProfile("uid-a", "reader_a", "Reader", "private@example.com")
            val viewModel = ThemeSettingsViewModel(users, prefs)
            viewModel.select(profile.uid, profile, FableThemeName.GOTHIC, cloudAvailable = true)
            advanceUntilIdle()
            assertEquals(FableThemeName.GOTHIC, viewModel.state.value.selectedTheme)
            assertEquals("Theme saved on this device; profile sync will retry when available.", viewModel.state.value.message)
            assertEquals(true, prefs.read(profile.uid)?.cloudSyncPending)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private class FakeThemePreferences : ThemePreferenceRepository {
    private val values = mutableMapOf<String, SavedThemePreference>()
    override fun read(ownerId: String?): SavedThemePreference? = values[ownerId ?: "anonymous"]
    override fun write(ownerId: String?, theme: FableThemeName, cloudSyncPending: Boolean) {
        values[ownerId ?: "anonymous"] = SavedThemePreference(theme, cloudSyncPending)
    }
}

private class FakeUserRepository : UserRepository {
    var updated: UserProfile? = null
    var shouldFail = false
    override fun observeProfile(uid: String): Flow<UserProfile?> = emptyFlow()
    override suspend fun createProfile(profile: UserProfile) = Unit
    override suspend fun updateProfile(profile: UserProfile, previousUsername: String) {
        if (shouldFail) error("Cloud unavailable")
        updated = profile
    }
}
