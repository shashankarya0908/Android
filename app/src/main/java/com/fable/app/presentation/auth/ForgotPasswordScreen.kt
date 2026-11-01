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
fun ForgotPasswordScreen(
    firebaseConfigured: Boolean,
    state: AuthUiState,
    onReset: (String) -> Unit,
    onLogin: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        AuthPage("Reset your password", "Enter your account email and we’ll send a reset link.", firebaseConfigured, state) {
            AuthTextField(email, { email = it }, "Email", keyboardType = KeyboardType.Email)
            AuthSubmitButton("Send reset link", state.isLoading) { onReset(email) }
            TextButton(onClick = onLogin) { Text("Back to sign in") }
        }
    }
}
