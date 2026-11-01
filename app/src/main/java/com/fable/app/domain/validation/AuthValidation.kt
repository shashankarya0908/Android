package com.fable.app.domain.validation

object AuthValidation {
    private val usernamePattern = Regex("^[a-z0-9_]{3,20}$")
    private val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun normalizeUsername(value: String): String = value.trim().lowercase()

    fun validateEmail(email: String): String? = when {
        email.isBlank() -> "Enter your email address."
        !emailPattern.matches(email.trim()) -> "Enter a valid email address."
        else -> null
    }

    fun validatePassword(password: String): String? = when {
        password.isBlank() -> "Enter your password."
        password.length < 8 -> "Use at least 8 characters for your password."
        else -> null
    }

    fun validateLogin(email: String, password: String): String? =
        validateEmail(email) ?: if (password.isBlank()) "Enter your password." else null

    fun validateRegistration(
        username: String,
        displayName: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): String? {
        val normalizedUsername = normalizeUsername(username)
        return when {
            !usernamePattern.matches(normalizedUsername) ->
                "Username must be 3–20 characters using letters, numbers, or underscores."
            displayName.trim().length !in 2..40 -> "Display name must be 2–40 characters."
            validateEmail(email) != null -> validateEmail(email)
            validatePassword(password) != null -> validatePassword(password)
            password != confirmPassword -> "Passwords do not match."
            else -> null
        }
    }

    fun validateProfile(username: String, displayName: String, bio: String): String? = when {
        !usernamePattern.matches(normalizeUsername(username)) ->
            "Username must be 3–20 characters using letters, numbers, or underscores."
        displayName.trim().length !in 2..40 -> "Display name must be 2–40 characters."
        bio.length > 280 -> "Biography must be 280 characters or fewer."
        else -> null
    }
}
