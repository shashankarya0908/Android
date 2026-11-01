package com.fable.app.domain.model

data class UserProfile(
    val uid: String,
    val username: String,
    val displayName: String,
    val email: String,
    val bio: String? = null,
    val profileImageUrl: String? = null,
    val favouriteGenres: List<String> = emptyList(),
    val favouriteBooks: List<String> = emptyList(),
    val selectedTheme: String = "Minimalist",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val profileSetupComplete: Boolean
        get() = bio != null
}
