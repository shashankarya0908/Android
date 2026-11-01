package com.fable.app.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fable.app.domain.model.AuthUser
import com.fable.app.domain.model.UserProfile

private val starterGenres = listOf("Fantasy", "Mystery", "Romance", "Science Fiction", "Literary", "Nonfiction")

@Composable
fun ProfileSetupScreen(
    authUser: AuthUser,
    profile: UserProfile?,
    state: AuthUiState,
    onSave: (String, String, String, List<String>, String?) -> Unit,
    onLogout: () -> Unit,
    title: String = "Set up your profile",
    showLogout: Boolean = true,
) {
    var username by rememberSaveable(profile?.username) { mutableStateOf(profile?.username.orEmpty()) }
    var displayName by rememberSaveable(profile?.displayName) { mutableStateOf(profile?.displayName.orEmpty()) }
    var bio by rememberSaveable(profile?.bio) { mutableStateOf(profile?.bio.orEmpty()) }
    var profileImageUrl by rememberSaveable(profile?.profileImageUrl) { mutableStateOf(profile?.profileImageUrl.orEmpty()) }
    var selectedGenres by rememberSaveable(profile?.favouriteGenres) {
        mutableStateOf(profile?.favouriteGenres.orEmpty())
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AuthPage(title, "A few details help other readers get to know you.", true, state) {
            AuthTextField(username, { username = it }, "Username")
            AuthTextField(displayName, { displayName = it }, "Display name")
            AuthTextField(profileImageUrl, { profileImageUrl = it }, "Profile image URL (HTTPS, optional)")
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Short biography") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                supportingText = { Text("${bio.length}/280") },
            )
            Text("Choose favourite genres")
            starterGenres.forEach { genre ->
                FilterChip(
                    selected = genre in selectedGenres,
                    onClick = {
                        selectedGenres = if (genre in selectedGenres) selectedGenres - genre else selectedGenres + genre
                    },
                    label = { Text(genre) },
                )
            }
            AuthSubmitButton("Save profile", state.isLoading) {
                onSave(username, displayName, bio, selectedGenres, profileImageUrl)
            }
            if (showLogout) TextButton(onClick = onLogout) { Text("Sign out") }
            Text("Signed in as ${authUser.email}")
        }
    }
}
