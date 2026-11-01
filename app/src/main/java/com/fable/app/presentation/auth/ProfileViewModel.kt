package com.fable.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fable.app.data.repository.UsernameAlreadyTakenException
import com.fable.app.domain.model.AuthUser
import com.fable.app.domain.model.UserProfile
import com.fable.app.domain.repository.UserRepository
import com.fable.app.domain.usecase.CreateUserProfile
import com.fable.app.domain.usecase.UpdateUserProfile
import com.fable.app.domain.validation.AuthValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val createUserProfile = CreateUserProfile(userRepository)
    private val updateUserProfile = UpdateUserProfile(userRepository)
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun saveSetup(
        authUser: AuthUser,
        existing: UserProfile?,
        username: String,
        displayName: String,
        bio: String,
        favouriteGenres: List<String>,
        profileImageUrl: String? = existing?.profileImageUrl,
    ) {
        AuthValidation.validateProfile(username, displayName, bio, profileImageUrl)?.let {
            _uiState.value = AuthUiState(error = it)
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val normalized = AuthValidation.normalizeUsername(username)
            val profile = (existing ?: UserProfile(
                uid = authUser.uid,
                username = normalized,
                displayName = displayName.trim(),
                email = authUser.email,
                createdAt = System.currentTimeMillis(),
            )).copy(
                username = normalized,
                displayName = displayName.trim(),
                bio = bio.trim(),
                profileImageUrl = profileImageUrl?.trim()?.takeIf(String::isNotBlank),
                favouriteGenres = favouriteGenres.distinct(),
                updatedAt = System.currentTimeMillis(),
            )
            try {
                if (existing == null) createUserProfile(profile)
                else updateUserProfile(profile, previousUsername = existing.username)
                _uiState.value = AuthUiState(message = "Profile saved.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = AuthUiState(error = error.toUserMessage())
            }
        }
    }
}

internal fun Throwable.toUserMessage(): String = FirebaseErrorMessages.message(this)
