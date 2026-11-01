package com.fable.app.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fable.app.domain.model.FableThemeName
import com.fable.app.domain.model.UserProfile
import com.fable.app.domain.model.restoreThemePreference
import com.fable.app.domain.repository.ThemePreferenceRepository
import com.fable.app.domain.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ThemeSettingsState(
    val selectedTheme: FableThemeName = FableThemeName.DEFAULT,
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class ThemeSettingsViewModel(
    private val userRepository: UserRepository,
    private val preferences: ThemePreferenceRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ThemeSettingsState())
    val state: StateFlow<ThemeSettingsState> = _state.asStateFlow()
    private var cloudSync: Job? = null
    private var cloudSyncKey: String? = null

    fun restore(ownerId: String?, profile: UserProfile?, cloudAvailable: Boolean) {
        val local = runCatching { preferences.read(ownerId) }.getOrNull()
        val selected = restoreThemePreference(local, profile?.selectedTheme, cloudAvailable && profile != null)
        _state.update {
            it.copy(
                selectedTheme = selected,
                message = if (local?.cloudSyncPending == true && profile != null && cloudAvailable)
                    "Restoring your saved theme to your profile…" else it.message,
                error = null,
            )
        }
        if (local?.cloudSyncPending == true && profile != null && cloudAvailable) {
            syncToCloud(ownerId, profile, local.theme)
        }
    }

    fun select(ownerId: String?, profile: UserProfile?, theme: FableThemeName, cloudAvailable: Boolean) {
        val shouldSync = cloudAvailable && profile != null
        try {
            preferences.write(ownerId, theme, cloudSyncPending = shouldSync)
        } catch (error: Exception) {
            _state.update { it.copy(error = error.message ?: "Theme preference could not be saved.", message = null) }
            return
        }
        _state.update {
            it.copy(
                selectedTheme = theme,
                isSaving = shouldSync,
                message = if (shouldSync) "Theme saved on this device. Updating your profile…"
                else "Theme saved on this device.",
                error = null,
            )
        }
        if (shouldSync) syncToCloud(ownerId, checkNotNull(profile), theme)
    }

    fun clearMessage() = _state.update { it.copy(message = null, error = null) }

    private fun syncToCloud(ownerId: String?, profile: UserProfile, theme: FableThemeName) {
        val key = "${profile.uid}:${theme.storageValue}"
        if (cloudSyncKey == key && cloudSync?.isActive == true) return
        cloudSync?.cancel()
        cloudSyncKey = key
        cloudSync = viewModelScope.launch {
            try {
                userRepository.updateProfile(
                    profile.copy(selectedTheme = theme.storageValue, updatedAt = System.currentTimeMillis()),
                    previousUsername = profile.username,
                )
                preferences.write(ownerId, theme, cloudSyncPending = false)
                _state.update { it.copy(selectedTheme = theme, isSaving = false, message = "Theme saved to your profile.", error = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        selectedTheme = theme,
                        isSaving = false,
                        message = "Theme saved on this device; profile sync will retry when available.",
                        error = error.message?.takeIf(String::isNotBlank),
                    )
                }
            } finally {
                if (cloudSyncKey == key) cloudSyncKey = null
            }
        }
    }
}
