package com.fable.app.domain.usecase

import com.fable.app.domain.model.UserProfile
import com.fable.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

class ObserveUserProfile(private val repository: UserRepository) {
    operator fun invoke(uid: String): Flow<UserProfile?> = repository.observeProfile(uid)
}

class CreateUserProfile(private val repository: UserRepository) {
    suspend operator fun invoke(profile: UserProfile) = repository.createProfile(profile)
}

class UpdateUserProfile(private val repository: UserRepository) {
    suspend operator fun invoke(profile: UserProfile, previousUsername: String) =
        repository.updateProfile(profile, previousUsername)
}
