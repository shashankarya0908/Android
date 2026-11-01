package com.fable.app.domain.usecase

import com.fable.app.domain.model.AuthUser
import com.fable.app.domain.model.UserProfile
import com.fable.app.domain.repository.AuthenticationRepository
import com.fable.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class ObserveAuthState(private val repository: AuthenticationRepository) {
    operator fun invoke(): Flow<AuthUser?> = repository.observeCurrentUser()
}

class RegisterAccount(private val repository: AuthenticationRepository) {
    suspend operator fun invoke(email: String, password: String): AuthUser =
        repository.register(email.trim(), password)
}

class Login(private val repository: AuthenticationRepository) {
    suspend operator fun invoke(email: String, password: String): AuthUser =
        repository.login(email.trim(), password)
}

class Logout(private val repository: AuthenticationRepository) {
    suspend operator fun invoke() = repository.logout()
}

class SendPasswordReset(private val repository: AuthenticationRepository) {
    suspend operator fun invoke(email: String) = repository.sendPasswordReset(email.trim())
}

class RegisterWithProfile(
    private val authenticationRepository: AuthenticationRepository,
    private val userRepository: UserRepository,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        username: String,
        displayName: String,
    ): UserProfile {
        val authUser = authenticationRepository.register(email.trim(), password)
        val profile = UserProfile(
            uid = authUser.uid,
            username = username.trim().lowercase(),
            displayName = displayName.trim(),
            email = authUser.email,
        )
        try {
            userRepository.createProfile(profile)
        } catch (error: Exception) {
            withContext(NonCancellable) {
                runCatching { authenticationRepository.deleteCurrentUser() }
            }
            throw error
        }
        return profile
    }
}
