package com.fable.app.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fable.app.domain.model.UserProfile
import com.fable.app.presentation.auth.AuthUiState

@Composable
fun AuthenticatedHomeScreen(
    profile: UserProfile,
    state: AuthUiState,
    onLogout: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Fable", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("Welcome, ${profile.displayName}", style = MaterialTheme.typography.headlineMedium)
            Text("@${profile.username}", style = MaterialTheme.typography.titleMedium)
            profile.bio?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            if (profile.favouriteGenres.isNotEmpty()) {
                Text("Favourite genres: ${profile.favouriteGenres.joinToString()}")
            }
            Text("Signed in as ${profile.email}", style = MaterialTheme.typography.bodyMedium)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = onLogout, enabled = !state.isLoading) {
                Text(if (state.isLoading) "Signing out…" else "Sign out")
            }
        }
    }
}
