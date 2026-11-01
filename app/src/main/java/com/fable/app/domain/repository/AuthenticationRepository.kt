package com.fable.app.domain.repository

import com.fable.app.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthenticationRepository {
    fun observeCurrentUser(): Flow<AuthUser?>
    suspend fun register(email: String, password: String): AuthUser
    suspend fun login(email: String, password: String): AuthUser
    suspend fun sendPasswordReset(email: String)
    suspend fun logout()
    suspend fun deleteCurrentUser()
}
