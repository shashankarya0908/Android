package com.fable.app.domain

import com.fable.app.domain.model.AuthUser
import com.fable.app.domain.model.UserProfile
import com.fable.app.domain.repository.AuthenticationRepository
import com.fable.app.domain.repository.UserRepository
import com.fable.app.domain.usecase.Login
import com.fable.app.domain.usecase.Logout
import com.fable.app.domain.usecase.ObserveAuthState
import com.fable.app.domain.usecase.RegisterWithProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticationUseCasesTest {
    @Test
    fun registrationCreatesProfileWithNormalizedUsername() = runBlocking {
        val auth = FakeAuthenticationRepository()
        val users = FakeUserRepository()

        val profile = RegisterWithProfile(auth, users)(
            email = " reader@example.com ",
            password = "correct-horse-42",
            username = " Book_Lover ",
            displayName = " Book Lover ",
        )

        assertEquals("book_lover", profile.username)
        assertEquals("Book Lover", profile.displayName)
        assertEquals("reader@example.com", profile.email)
        assertEquals(profile, users.createdProfile)
    }

    @Test
    fun failedProfileCreationDeletesNewAuthAccountAndPropagatesError() = runBlocking {
        val auth = FakeAuthenticationRepository()
        val users = FakeUserRepository(createFailure = IllegalStateException("username is taken"))

        val failure = runCatching {
            RegisterWithProfile(auth, users)("reader@example.com", "password123", "reader", "Reader")
        }.exceptionOrNull()

        assertEquals("username is taken", failure?.message)
        assertTrue(auth.accountDeleted)
        assertNull(users.createdProfile)
    }

    @Test
    fun authStateChangesOnLoginAndLogout() = runBlocking {
        val auth = FakeAuthenticationRepository()
        val authState = ObserveAuthState(auth)()
        assertNull(authState.first())

        Login(auth)("reader@example.com", "password123")
        assertEquals("reader-uid", authState.first()?.uid)

        Logout(auth)()
        assertNull(authState.first())
        assertTrue(auth.loggedOut)
    }

    @Test
    fun loginPropagatesRepositoryCredentialErrors() = runBlocking {
        val auth = FakeAuthenticationRepository(loginFailure = IllegalArgumentException("incorrect credentials"))
        val failure = runCatching { Login(auth)("reader@example.com", "wrong") }.exceptionOrNull()
        assertEquals("incorrect credentials", failure?.message)
        assertFalse(auth.loggedOut)
    }
}

private class FakeAuthenticationRepository(
    private val loginFailure: Exception? = null,
) : AuthenticationRepository {
    private val state = MutableStateFlow<AuthUser?>(null)
    var accountDeleted = false
    var loggedOut = false

    override fun observeCurrentUser(): Flow<AuthUser?> = state

    override suspend fun register(email: String, password: String): AuthUser =
        AuthUser("reader-uid", email.trim()).also { state.value = it }

    override suspend fun login(email: String, password: String): AuthUser {
        loginFailure?.let { throw it }
        return AuthUser("reader-uid", email).also { state.value = it }
    }

    override suspend fun sendPasswordReset(email: String) = Unit
    override suspend fun logout() { loggedOut = true; state.value = null }
    override suspend fun deleteCurrentUser() { accountDeleted = true; state.value = null }
}

private class FakeUserRepository(
    private val createFailure: Exception? = null,
) : UserRepository {
    var createdProfile: UserProfile? = null

    override fun observeProfile(uid: String): Flow<UserProfile?> = MutableStateFlow(createdProfile)

    override suspend fun createProfile(profile: UserProfile) {
        createFailure?.let { throw it }
        createdProfile = profile
    }

    override suspend fun updateProfile(profile: UserProfile, previousUsername: String) {
        createdProfile = profile
    }

}
