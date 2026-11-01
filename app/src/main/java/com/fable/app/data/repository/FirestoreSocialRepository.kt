package com.fable.app.data.repository

import com.fable.app.domain.model.FriendRequest
import com.fable.app.domain.model.FriendRequestAction
import com.fable.app.domain.model.FriendRequestStatus
import com.fable.app.domain.model.PublicReaderProfile
import com.fable.app.domain.model.PublicReadingStats
import com.fable.app.domain.model.ReaderActivity
import com.fable.app.domain.model.ReaderActivityType
import com.fable.app.domain.model.createFriendRequest
import com.fable.app.domain.model.friendshipPairKey
import com.fable.app.domain.model.filterFriendActivity
import com.fable.app.domain.model.transitionFriendRequest
import com.fable.app.domain.repository.SocialRepository
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreSocialRepository(private val firestore: FirebaseFirestore) : SocialRepository {
    override fun searchReaders(usernamePrefix: String): Flow<List<PublicReaderProfile>> = callbackFlow {
        val prefix = usernamePrefix.trim().removePrefix("@").lowercase()
        if (prefix.length < 2) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = publicProfiles()
            .orderBy("usernameNormalized")
            .startAt(prefix)
            .endAt(prefix + "\uf8ff")
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) close(error)
                else trySend(snapshot?.documents.orEmpty().mapNotNull { it.toPublicProfile() })
            }
        awaitClose { registration.remove() }
    }

    override fun observeReaderProfile(uid: String): Flow<PublicReaderProfile?> = callbackFlow {
        val registration = publicProfiles().document(uid).addSnapshotListener { snapshot, error ->
            if (error != null) close(error)
            else trySend(snapshot?.takeIf(DocumentSnapshot::exists)?.toPublicProfile())
        }
        awaitClose { registration.remove() }
    }

    override fun observeIncomingRequests(userId: String): Flow<List<FriendRequest>> = observeRequests(userId, INCOMING)

    override fun observeOutgoingRequests(userId: String): Flow<List<FriendRequest>> = observeRequests(userId, OUTGOING)

    override fun observeFriends(userId: String): Flow<List<PublicReaderProfile>> = callbackFlow {
        val profileListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()
        val profiles = mutableMapOf<String, PublicReaderProfile>()
        val friendsRef = users().document(userId).collection(FRIENDS)
        val registration = friendsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val friendIds = snapshot?.documents.orEmpty().mapNotNull { it.getString("friendId") }.toSet()
            (profileListeners.keys - friendIds).forEach { id ->
                profileListeners.remove(id)?.remove()
                profiles.remove(id)
            }
            (friendIds - profileListeners.keys).forEach { id ->
                profileListeners[id] = publicProfiles().document(id).addSnapshotListener { profile, profileError ->
                    if (profileError != null) close(profileError)
                    else {
                        profile?.takeIf(DocumentSnapshot::exists)?.toPublicProfile()?.let { profiles[id] = it }
                        trySend(profiles.values.sortedBy { it.username })
                    }
                }
            }
            trySend(profiles.values.sortedBy { it.username })
        }
        awaitClose {
            registration.remove()
            profileListeners.values.forEach { it.remove() }
        }
    }

    override fun observeActivityFeed(userId: String): Flow<List<ReaderActivity>> = callbackFlow {
        val activityListeners = mutableMapOf<String, com.google.firebase.firestore.ListenerRegistration>()
        val activities = mutableMapOf<String, List<ReaderActivity>>()
        var acceptedFriendIds = emptySet<String>()
        val friendsRef = users().document(userId).collection(FRIENDS)
        val registration = friendsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val friendIds = snapshot?.documents.orEmpty().mapNotNull { it.getString("friendId") }.toSet()
            acceptedFriendIds = friendIds
            (activityListeners.keys - friendIds).forEach { id ->
                activityListeners.remove(id)?.remove()
                activities.remove(id)
            }
            (friendIds - activityListeners.keys).forEach { friendId ->
                activityListeners[friendId] = users().document(friendId).collection(ACTIVITIES)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(FEED_PER_FRIEND.toLong())
                    .addSnapshotListener { feed, feedError ->
                        if (feedError != null) close(feedError)
                        else {
                            activities[friendId] = feed?.documents.orEmpty().mapNotNull { it.toReaderActivity() }
                            trySend(filterFriendActivity(activities.values.flatten(), acceptedFriendIds).take(FEED_LIMIT))
                        }
                    }
            }
            trySend(filterFriendActivity(activities.values.flatten(), friendIds).take(FEED_LIMIT))
        }
        awaitClose {
            registration.remove()
            activityListeners.values.forEach { it.remove() }
        }
    }

    override suspend fun sendFriendRequest(sender: PublicReaderProfile, recipient: PublicReaderProfile) {
        val incoming = incomingRequests(recipient.uid).document(sender.uid)
        val outgoing = outgoingRequests(sender.uid).document(recipient.uid)
        val reverseIncoming = incomingRequests(sender.uid).document(recipient.uid)
        val friendship = friendRef(sender.uid, recipient.uid)
        val reverseFriendship = friendRef(recipient.uid, sender.uid)
        val now = System.currentTimeMillis()
        firestore.runTransaction { transaction ->
            val outgoingSnapshot = transaction.get(outgoing)
            val friendshipSnapshot = transaction.get(friendship)
            val reverseFriendshipSnapshot = transaction.get(reverseFriendship)
            val reverseSnapshot = transaction.get(reverseIncoming)
            if (friendshipSnapshot.exists() || reverseFriendshipSnapshot.exists()) {
                throw IllegalStateException("You are already friends.")
            }
            if (reverseSnapshot.getString("status") == FriendRequestStatus.PENDING.name) {
                throw IllegalStateException("This reader already sent you a request. Accept it from Friend Requests.")
            }
            val previous = outgoingSnapshot.takeIf(DocumentSnapshot::exists)?.toFriendRequest()
            val request = createFriendRequest(
                sender = sender,
                recipient = recipient,
                previous = previous,
                now = now,
            )
            val fields = request.toFirestoreMap()
            transaction.set(incoming, fields)
            transaction.set(outgoing, fields)
            null
        }.await()
    }

    override suspend fun acceptFriendRequest(userId: String, senderId: String) {
        val incoming = incomingRequests(userId).document(senderId)
        val outgoing = outgoingRequests(senderId).document(userId)
        val ownFriend = friendRef(userId, senderId)
        val otherFriend = friendRef(senderId, userId)
        firestore.runTransaction { transaction ->
            val requestSnapshot = transaction.get(incoming)
            val outgoingSnapshot = transaction.get(outgoing)
            val ownFriendSnapshot = transaction.get(ownFriend)
            val otherFriendSnapshot = transaction.get(otherFriend)
            if (requestSnapshot.getString("status") == FriendRequestStatus.ACCEPTED.name &&
                ownFriendSnapshot.exists() && otherFriendSnapshot.exists()
            ) return@runTransaction null
            val request = requestSnapshot.toFriendRequestOrThrow()
            val updated = transitionFriendRequest(request, userId, FriendRequestAction.ACCEPT)
            if (!outgoingSnapshot.exists()) throw IllegalStateException("The matching request could not be found.")
            val now = System.currentTimeMillis()
            val pairKey = friendshipPairKey(userId, senderId)
            transaction.update(incoming, "status", updated.status.name, "updatedAt", now)
            transaction.update(outgoing, "status", updated.status.name, "updatedAt", now)
            transaction.set(ownFriend, mapOf("friendId" to senderId, "pairKey" to pairKey, "createdAt" to now))
            transaction.set(otherFriend, mapOf("friendId" to userId, "pairKey" to pairKey, "createdAt" to now))
            null
        }.await()
    }

    override suspend fun rejectFriendRequest(userId: String, senderId: String) = transitionRequest(
        userId, senderId, FriendRequestAction.REJECT,
    )

    override suspend fun cancelFriendRequest(userId: String, recipientId: String) = transitionRequest(
        userId, recipientId, FriendRequestAction.CANCEL,
    )

    override suspend fun removeFriend(userId: String, friendId: String) {
        require(userId != friendId) { "You cannot remove yourself as a friend." }
        val ownFriend = friendRef(userId, friendId)
        val otherFriend = friendRef(friendId, userId)
        val requests = listOf(
            incomingRequests(userId).document(friendId),
            outgoingRequests(userId).document(friendId),
            incomingRequests(friendId).document(userId),
            outgoingRequests(friendId).document(userId),
        )
        firestore.runTransaction { transaction ->
            val own = transaction.get(ownFriend)
            val other = transaction.get(otherFriend)
            val requestSnapshots = requests.map(transaction::get)
            if (!own.exists() && !other.exists()) return@runTransaction null
            transaction.delete(ownFriend)
            transaction.delete(otherFriend)
            requestSnapshots.zip(requests).forEach { (snapshot, reference) ->
                if (snapshot.getString("status") == FriendRequestStatus.ACCEPTED.name) {
                    transaction.update(reference, "status", FriendRequestStatus.CANCELLED.name, "updatedAt", System.currentTimeMillis())
                }
            }
            null
        }.await()
    }

    private suspend fun transitionRequest(userId: String, otherId: String, action: FriendRequestAction) {
        val isCancel = action == FriendRequestAction.CANCEL
        val incoming = if (isCancel) incomingRequests(otherId).document(userId) else incomingRequests(userId).document(otherId)
        val outgoing = if (isCancel) outgoingRequests(userId).document(otherId) else outgoingRequests(otherId).document(userId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(incoming)
            val outgoingSnapshot = transaction.get(outgoing)
            val request = snapshot.toFriendRequestOrThrow()
            val updated = transitionFriendRequest(request, userId, action)
            if (!outgoingSnapshot.exists()) throw IllegalStateException("The matching request could not be found.")
            val now = System.currentTimeMillis()
            transaction.update(incoming, "status", updated.status.name, "updatedAt", now)
            transaction.update(outgoing, "status", updated.status.name, "updatedAt", now)
            null
        }.await()
    }

    private fun observeRequests(userId: String, collection: String): Flow<List<FriendRequest>> = callbackFlow {
        val reference = when (collection) {
            INCOMING -> incomingRequests(userId)
            else -> outgoingRequests(userId)
        }
        val registration = reference.orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) close(error)
                else trySend(snapshot?.documents.orEmpty().mapNotNull { it.toFriendRequest() })
            }
        awaitClose { registration.remove() }
    }

    private fun FriendRequest.toFirestoreMap(): Map<String, Any?> = mapOf(
        "senderId" to senderId,
        "senderUsername" to senderUsername,
        "senderDisplayName" to senderDisplayName,
        "senderImageUrl" to senderImageUrl,
        "recipientId" to recipientId,
        "recipientUsername" to recipientUsername,
        "recipientDisplayName" to recipientDisplayName,
        "recipientImageUrl" to recipientImageUrl,
        "status" to status.name,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

    private fun DocumentSnapshot.toPublicProfile(): PublicReaderProfile? = runCatching {
        val id = getString("uid") ?: id
        PublicReaderProfile(
            uid = id,
            username = getString("username") ?: return null,
            displayName = getString("displayName") ?: return null,
            bio = getString("bio"),
            profileImageUrl = getString("profileImageUrl"),
            favouriteGenres = (get("favouriteGenres") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            readingStats = PublicReadingStats(
                completedBooks = (get("completedBooks") as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                pagesRead = (get("pagesRead") as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                currentlyReading = (get("currentlyReading") as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                wantToRead = (get("wantToRead") as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
            ),
            updatedAt = (get("updatedAt") as? Number)?.toLong() ?: 0L,
        )
    }.getOrNull()

    private fun DocumentSnapshot.toFriendRequest(): FriendRequest? = runCatching {
        FriendRequest(
            senderId = getString("senderId") ?: return null,
            senderUsername = getString("senderUsername").orEmpty(),
            senderDisplayName = getString("senderDisplayName").orEmpty(),
            senderImageUrl = getString("senderImageUrl"),
            recipientId = getString("recipientId") ?: return null,
            recipientUsername = getString("recipientUsername").orEmpty(),
            recipientDisplayName = getString("recipientDisplayName").orEmpty(),
            recipientImageUrl = getString("recipientImageUrl"),
            status = FriendRequestStatus.valueOf(getString("status") ?: return null),
            createdAt = (get("createdAt") as? Number)?.toLong() ?: 0L,
            updatedAt = (get("updatedAt") as? Number)?.toLong() ?: 0L,
        )
    }.getOrNull()

    private fun DocumentSnapshot.toFriendRequestOrThrow(): FriendRequest =
        takeIf(DocumentSnapshot::exists)?.toFriendRequest() ?: throw IllegalStateException("This friend request no longer exists.")

    private fun DocumentSnapshot.toReaderActivity(): ReaderActivity? = runCatching {
        ReaderActivity(
            id = id,
            readerId = getString("readerId") ?: return null,
            username = getString("username").orEmpty(),
            displayName = getString("displayName").orEmpty(),
            eventType = ReaderActivityType.valueOf(getString("eventType") ?: return null),
            bookId = getString("bookId") ?: return null,
            bookTitle = getString("bookTitle") ?: return null,
            bookThumbnailUrl = getString("bookThumbnailUrl"),
            currentPage = (get("currentPage") as? Number)?.toInt(),
            progressPercent = (get("progressPercent") as? Number)?.toInt(),
            rating = (get("rating") as? Number)?.toInt(),
            reviewText = getString("reviewText"),
            timestamp = (get("timestamp") as? Number)?.toLong() ?: 0L,
        )
    }.getOrNull()

    private fun users() = firestore.collection(USERS)
    private fun publicProfiles() = firestore.collection(PUBLIC_PROFILES)
    private fun incomingRequests(uid: String) = users().document(uid).collection(INCOMING)
    private fun outgoingRequests(uid: String) = users().document(uid).collection(OUTGOING)
    private fun friendRef(uid: String, friendUid: String) = users().document(uid).collection(FRIENDS).document(friendUid)

    private companion object {
        const val USERS = "users"
        const val PUBLIC_PROFILES = "publicProfiles"
        const val INCOMING = "incomingRequests"
        const val OUTGOING = "outgoingRequests"
        const val FRIENDS = "friends"
        const val ACTIVITIES = "activities"
        const val FEED_PER_FRIEND = 20
        const val FEED_LIMIT = 100
    }
}
