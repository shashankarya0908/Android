package com.fable.app.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.fable.app.domain.model.FableThemeName

@Composable
fun LoginScreen(
    firebaseConfigured: Boolean,
    state: AuthUiState,
    onLogin: (String, String) -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
    currentTheme: FableThemeName,
    onThemes: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AuthPage("Welcome to Fable", "Sign in to return to your reading life.", firebaseConfigured, state) {
            AuthTextField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
            PasswordField(password, { password = it }, "Password")
            AuthSubmitButton("Sign in", state.isLoading) { onLogin(email, password) }
            TextButton(onClick = onForgotPassword) { Text("Forgot password?") }
            TextButton(onClick = onRegister) { Text("Create an account") }
            TextButton(onClick = onThemes) { Text("Appearance · ${currentTheme.label}") }
        }
    }
}
