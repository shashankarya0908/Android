package com.fable.app.data.repository

import com.fable.app.domain.model.AuthUser
import com.fable.app.domain.repository.AuthenticationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

class FirebaseAuthenticationRepository(
    private val firebaseAuth: FirebaseAuth,
) : AuthenticationRepository {
    override fun observeCurrentUser(): Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser?.let { AuthUser(it.uid, it.email.orEmpty()) }
            trySend(user)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun register(email: String, password: String): AuthUser {
        val user = firebaseAuth.createUserWithEmailAndPassword(email, password).await().user
            ?: error("Firebase created the account without returning a user.")
        return AuthUser(user.uid, user.email ?: email)
    }

    override suspend fun login(email: String, password: String): AuthUser {
        val user = firebaseAuth.signInWithEmailAndPassword(email, password).await().user
            ?: error("Firebase signed in without returning a user.")
        return AuthUser(user.uid, user.email ?: email)
    }

    override suspend fun sendPasswordReset(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    override suspend fun logout() = firebaseAuth.signOut()

    override suspend fun deleteCurrentUser() {
        firebaseAuth.currentUser?.delete()?.await()
    }
}
