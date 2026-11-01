package com.fable.app.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fable.app.di.FableDependencies
import com.fable.app.di.FableViewModelFactory
import com.fable.app.di.ThemeSettingsViewModelFactory
import com.fable.app.presentation.auth.AuthenticationViewModel
import com.fable.app.presentation.auth.ForgotPasswordScreen
import com.fable.app.presentation.auth.LoginScreen
import com.fable.app.presentation.auth.ProfileSetupScreen
import com.fable.app.presentation.auth.ProfileViewModel
import com.fable.app.presentation.auth.RegistrationScreen
import com.fable.app.presentation.auth.SessionState
import com.fable.app.presentation.home.AuthenticatedHomeScreen
import com.fable.app.presentation.books.navigation.AuthenticatedBooksApp
import com.fable.app.domain.model.FableThemeName
import com.fable.app.presentation.theme.FableTheme
import com.fable.app.presentation.theme.FableThemeTokens
import com.fable.app.presentation.theme.ThemeSelectionScreen
import com.fable.app.presentation.theme.ThemeSettingsViewModel

private enum class AuthDestination { Login, Register, ForgotPassword, ThemeSelection }

@Composable
fun FableApp(dependencies: FableDependencies) {
    val factory = FableViewModelFactory(dependencies)
    val authenticationViewModel: AuthenticationViewModel = viewModel(factory = factory)
    val profileViewModel: ProfileViewModel = viewModel(factory = factory)
    val themeViewModel: ThemeSettingsViewModel = viewModel(factory = ThemeSettingsViewModelFactory(dependencies))
    val session by authenticationViewModel.sessionState.collectAsStateWithLifecycle()
    val authUi by authenticationViewModel.uiState.collectAsStateWithLifecycle()
    val profileUi by profileViewModel.uiState.collectAsStateWithLifecycle()
    val themeState by themeViewModel.state.collectAsStateWithLifecycle()
    var authDestination by rememberSaveable { mutableStateOf(AuthDestination.Login) }

    LaunchedEffect(session, dependencies.firebaseConfigured) {
        when (val current = session) {
            SessionState.Loading, is SessionState.Failed -> Unit
            SessionState.SignedOut -> themeViewModel.restore(ownerId = null, profile = null, cloudAvailable = false)
            is SessionState.ProfileSetup -> themeViewModel.restore(
                ownerId = current.authUser.uid,
                profile = current.profile,
                cloudAvailable = dependencies.firebaseConfigured && current.profile != null,
            )
            is SessionState.Ready -> themeViewModel.restore(
                ownerId = current.userId,
                profile = current.profile,
                cloudAvailable = dependencies.firebaseConfigured,
            )
        }
    }

    FableTheme(themeState.selectedTheme) {
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxSize().background(FableThemeTokens.current.pageBrush),
        ) {
            when (val state = session) {
                SessionState.Loading -> LoadingScreen()
                SessionState.SignedOut -> when (authDestination) {
                    AuthDestination.Login -> LoginScreen(
                        firebaseConfigured = dependencies.firebaseConfigured,
                        state = authUi,
                        onLogin = authenticationViewModel::login,
                        onForgotPassword = {
                            authenticationViewModel.clearMessage()
                            authDestination = AuthDestination.ForgotPassword
                        },
                        onRegister = {
                            authenticationViewModel.clearMessage()
                            authDestination = AuthDestination.Register
                        },
                        currentTheme = themeState.selectedTheme,
                        onThemes = { authDestination = AuthDestination.ThemeSelection },
                    )
                    AuthDestination.Register -> RegistrationScreen(
                        firebaseConfigured = dependencies.firebaseConfigured,
                        state = authUi,
                        onRegister = authenticationViewModel::register,
                        onLogin = {
                            authenticationViewModel.clearMessage()
                            authDestination = AuthDestination.Login
                        },
                    )
                    AuthDestination.ForgotPassword -> ForgotPasswordScreen(
                        firebaseConfigured = dependencies.firebaseConfigured,
                        state = authUi,
                        onReset = authenticationViewModel::requestPasswordReset,
                        onLogin = {
                            authenticationViewModel.clearMessage()
                            authDestination = AuthDestination.Login
                        },
                    )
                    AuthDestination.ThemeSelection -> ThemeSelectionScreen(
                        selectedTheme = themeState.selectedTheme,
                        statusMessage = themeState.message,
                        errorMessage = themeState.error,
                        onSelect = { theme -> themeViewModel.select(null, null, theme, cloudAvailable = false) },
                        onDone = { authDestination = AuthDestination.Login },
                    )
                }
                is SessionState.ProfileSetup -> ProfileSetupScreen(
                    authUser = state.authUser,
                    profile = state.profile,
                    state = profileUi,
                    onSave = { username, displayName, bio, genres, imageUrl ->
                        profileViewModel.saveSetup(state.authUser, state.profile, username, displayName, bio, genres, imageUrl)
                    },
                    onLogout = {
                        authDestination = AuthDestination.Login
                        authenticationViewModel.logout()
                    },
                )
                is SessionState.Ready -> AuthenticatedBooksApp(
                    profile = state.profile,
                    userId = state.userId,
                    dependencies = dependencies,
                    selectedTheme = themeState.selectedTheme,
                    themeMessage = themeState.message,
                    themeError = themeState.error,
                    onSelectTheme = { theme ->
                        themeViewModel.select(state.userId, state.profile, theme, dependencies.firebaseConfigured)
                    },
                    onLogout = {
                        authDestination = AuthDestination.Login
                        authenticationViewModel.logout()
                    },
                )
                is SessionState.Failed -> SessionErrorScreen(state.message, authenticationViewModel::logout)
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Text("Checking your account…", modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun SessionErrorScreen(message: String, onSignOut: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("We couldn’t load your profile.", style = MaterialTheme.typography.titleLarge)
            Text(message, modifier = Modifier.padding(top = 8.dp))
            androidx.compose.material3.Button(onClick = onSignOut, modifier = Modifier.padding(top = 12.dp)) {
                Text("Return to sign in")
            }
        }
    }
}
