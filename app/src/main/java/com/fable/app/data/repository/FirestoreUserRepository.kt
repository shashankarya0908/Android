package com.fable.app.data.repository

import com.fable.app.domain.model.UserProfile
import com.fable.app.domain.repository.UserRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UsernameAlreadyTakenException : Exception("That username is already in use.")

class FirestoreUserRepository(
    private val firestore: FirebaseFirestore,
) : UserRepository {
    override fun observeProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = firestore.collection(USERS).document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                } else {
                    trySend(snapshot?.takeIf { it.exists() }?.toUserProfile())
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun createProfile(profile: UserProfile) {
        val normalized = profile.username.trim().lowercase()
        val usernameRef = firestore.collection(USERNAMES).document(normalized)
        val userRef = firestore.collection(USERS).document(profile.uid)
        firestore.runTransaction { transaction ->
            val usernameSnapshot = transaction.get(usernameRef)
            val userSnapshot = transaction.get(userRef)
            if (usernameSnapshot.exists()) throw UsernameAlreadyTakenException()
            if (userSnapshot.exists()) throw IllegalStateException("A profile already exists for this account.")
            transaction.set(usernameRef, mapOf("uid" to profile.uid, "username" to normalized))
            transaction.set(userRef, profile.toCreateMap(normalized))
            null
        }.await()
    }

    override suspend fun updateProfile(profile: UserProfile, previousUsername: String) {
        val normalized = profile.username.trim().lowercase()
        val userRef = firestore.collection(USERS).document(profile.uid)
        val oldUsernameRef = firestore.collection(USERNAMES).document(previousUsername.trim().lowercase())
        val newUsernameRef = firestore.collection(USERNAMES).document(normalized)
        firestore.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)
            val oldUsernameSnapshot = transaction.get(oldUsernameRef)
            val newUsernameSnapshot = if (oldUsernameRef == newUsernameRef) oldUsernameSnapshot
                else transaction.get(newUsernameRef)

            if (!userSnapshot.exists()) throw IllegalStateException("Your profile could not be found.")
            if (newUsernameSnapshot.exists() && newUsernameSnapshot.getString("uid") != profile.uid) {
                throw UsernameAlreadyTakenException()
            }

            if (oldUsernameRef != newUsernameRef) {
                transaction.delete(oldUsernameRef)
                transaction.set(newUsernameRef, mapOf("uid" to profile.uid, "username" to normalized))
            }
            transaction.update(userRef, profile.toUpdateMap(normalized))
            null
        }.await()
    }

    private fun UserProfile.toCreateMap(normalizedUsername: String): Map<String, Any?> = mapOf(
        "uid" to uid,
        "username" to normalizedUsername,
        "displayName" to displayName.trim(),
        "email" to email,
        "bio" to bio,
        "profileImageUrl" to profileImageUrl,
        "favouriteGenres" to favouriteGenres,
        "favouriteBooks" to favouriteBooks,
        "selectedTheme" to selectedTheme,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    private fun UserProfile.toUpdateMap(normalizedUsername: String): Map<String, Any?> = mapOf(
        "username" to normalizedUsername,
        "displayName" to displayName.trim(),
        "bio" to bio,
        "profileImageUrl" to profileImageUrl,
        "favouriteGenres" to favouriteGenres,
        "favouriteBooks" to favouriteBooks,
        "selectedTheme" to selectedTheme,
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toUserProfile(): UserProfile {
        fun timestampMillis(key: String) = (getTimestamp(key) ?: Timestamp.now()).toDate().time
        return UserProfile(
            uid = getString("uid").orEmpty(),
            username = getString("username").orEmpty(),
            displayName = getString("displayName").orEmpty(),
            email = getString("email").orEmpty(),
            bio = getString("bio"),
            profileImageUrl = getString("profileImageUrl"),
            favouriteGenres = stringList("favouriteGenres"),
            favouriteBooks = stringList("favouriteBooks"),
            selectedTheme = getString("selectedTheme") ?: "Minimalist",
            createdAt = timestampMillis("createdAt"),
            updatedAt = timestampMillis("updatedAt"),
        )
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.stringList(key: String): List<String> =
        (get(key) as? List<*>)?.mapNotNull { it as? String }.orEmpty()

    private companion object {
        const val USERS = "users"
        const val USERNAMES = "usernames"
    }
}
