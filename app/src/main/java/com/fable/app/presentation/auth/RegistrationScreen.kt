package com.fable.app.presentation.auth

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

@Composable
fun RegistrationScreen(
    firebaseConfigured: Boolean,
    state: AuthUiState,
    onRegister: (String, String, String, String, String) -> Unit,
    onLogin: () -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AuthPage("Create your account", "Start building a home for the books you love.", firebaseConfigured, state) {
            AuthTextField(username, { username = it }, "Username")
            AuthTextField(displayName, { displayName = it }, "Display name")
            AuthTextField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
            PasswordField(password, { password = it }, "Password")
            PasswordField(confirmPassword, { confirmPassword = it }, "Confirm password")
            Text("Use at least 8 characters. Usernames use letters, numbers, and underscores.")
            AuthSubmitButton("Create account", state.isLoading) {
                onRegister(username, displayName, email, password, confirmPassword)
            }
            TextButton(onClick = onLogin) { Text("Already have an account? Sign in") }
        }
    }
}
