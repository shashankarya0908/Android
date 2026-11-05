package com.fable.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fable.app.domain.model.AuthUser
import com.fable.app.domain.model.UserProfile
import com.fable.app.domain.repository.AuthenticationRepository
import com.fable.app.domain.repository.UserRepository
import com.fable.app.domain.usecase.Login
import com.fable.app.domain.usecase.Logout
import com.fable.app.domain.usecase.ObserveAuthState
import com.fable.app.domain.usecase.ObserveUserProfile
import com.fable.app.domain.usecase.RegisterWithProfile
import com.fable.app.domain.usecase.SendPasswordReset
import com.fable.app.domain.validation.AuthValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
)

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class ProfileSetup(val authUser: AuthUser, val profile: UserProfile?) : SessionState
    data class Ready(val profile: UserProfile, val userId: String) : SessionState
    data class Failed(val message: String) : SessionState
}

class AuthenticationViewModel(
    authenticationRepository: AuthenticationRepository,
    userRepository: UserRepository,
) : ViewModel() {
    private val loginUseCase = Login(authenticationRepository)
    private val registerUseCase = RegisterWithProfile(authenticationRepository, userRepository)
    private val logoutUseCase = Logout(authenticationRepository)
    private val resetUseCase = SendPasswordReset(authenticationRepository)
    private val registrationInProgress = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionState: StateFlow<SessionState> = combine(
        ObserveAuthState(authenticationRepository)(),
        registrationInProgress,
    ) { authUser, registrationPending -> authUser to registrationPending }
        .flatMapLatest { (authUser, registrationPending) ->
            if (authUser == null) {
                AuthLogger.log("Auth session observed", "SignedOut")
                flowOf(SessionState.SignedOut)
            } else if (registrationPending) {
                flowOf(SessionState.Loading)
            } else {
                AuthLogger.log("Auth session observed", "User authenticated: ${authUser.uid}")
                ObserveUserProfile(userRepository)(authUser.uid)
                    .map { profile ->
                        when {
                            profile == null -> SessionState.ProfileSetup(authUser, null)
                            !profile.profileSetupComplete -> SessionState.ProfileSetup(authUser, profile)
                            else -> SessionState.Ready(profile, authUser.uid)
                        }
                    }
                    .onStart { emit(SessionState.Loading) }
            }
        }
        .catch {
            AuthLogger.logError("sessionState flow", it)
            emit(SessionState.Failed(it.toUserMessage()))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.Loading)

    fun login(email: String, password: String) {
        AuthValidation.validateLogin(email, password)?.let { return showError(it) }
        runAction("login") { loginUseCase(email, password) }
    }

    fun register(username: String, displayName: String, email: String, password: String, confirmPassword: String) {
        AuthValidation.validateRegistration(username, displayName, email, password, confirmPassword)
            ?.let { return showError(it) }
        runAction("register") {
            registrationInProgress.value = true
            try {
                registerUseCase(email, password, AuthValidation.normalizeUsername(username), displayName)
                _uiState.value = AuthUiState(message = "Your account is ready. Finish setting up your profile.")
            } finally {
                registrationInProgress.value = false
            }
        }
    }

    fun requestPasswordReset(email: String) {
        AuthValidation.validateEmail(email)?.let { return showError(it) }
        runAction("passwordReset") {
            resetUseCase(email)
            _uiState.value = AuthUiState(message = "If an account uses that email, a reset link has been sent.")
        }
    }

    fun logout() = runAction("logout") { logoutUseCase() }

    fun clearMessage() {
        _uiState.value = AuthUiState()
    }

    private fun runAction(actionName: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            AuthLogger.log("Starting $actionName")
            try {
                action()
                AuthLogger.log("Completed $actionName")
                if (_uiState.value.isLoading) _uiState.value = AuthUiState()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                AuthLogger.logError(actionName, error)
                _uiState.value = AuthUiState(error = error.toUserMessage())
            }
        }
    }

    private fun showError(message: String) {
        _uiState.value = AuthUiState(error = message)
    }
}
