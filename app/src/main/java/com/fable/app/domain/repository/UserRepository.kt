package com.fable.app.domain.repository

import com.fable.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeProfile(uid: String): Flow<UserProfile?>
    suspend fun createProfile(profile: UserProfile)
    suspend fun updateProfile(profile: UserProfile, previousUsername: String)
}
