package com.fable.app.data.repository

import com.fable.app.data.local.BookDao
import com.fable.app.data.local.BookReviewEntity
import com.fable.app.data.local.toDomain
import com.fable.app.data.local.toEntity
import com.fable.app.data.remote.firestore.reviewCollectionId
import com.fable.app.data.remote.firestore.toBookReviewDtoOrNull
import com.fable.app.data.remote.firestore.toDto
import com.fable.app.data.remote.firestore.toEntity
import com.fable.app.data.remote.firestore.toFirestoreMap
import com.fable.app.domain.model.Book
import com.fable.app.domain.model.BookReview
import com.fable.app.domain.model.PublicReaderProfile
import com.fable.app.domain.model.ReaderActivity
import com.fable.app.domain.model.generateReviewActivity
import com.fable.app.domain.model.createOrUpdateBookReview
import com.fable.app.domain.model.markBookReviewDeleted
import com.fable.app.domain.repository.ReviewRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreReviewRepository(
    private val firestore: FirebaseFirestore,
    private val dao: BookDao,
) : ReviewRepository {
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = mutableSetOf<String>()

    override fun observeBookReviews(bookId: String): Flow<List<BookReview>> = channelFlow {
        val localJob = launch {
            dao.observeBookReviews(bookId).map { it.map(BookReviewEntity::toDomain) }.collect { send(it) }
        }
        val collection = firestore.collection("bookReviews").document(reviewCollectionId(bookId)).collection("reviews")
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
            } else if (snapshot != null) {
                syncScope.launch {
                    runCatching {
                        reconcile(bookId, snapshot.documents.mapNotNull { it.toBookReviewDtoOrNull() }, !snapshot.metadata.isFromCache)
                    }
                    pushPending(bookId = bookId)
                }
            }
        }
        pushPending(bookId = bookId)
        awaitClose { listener.remove(); localJob.cancel() }
    }

    override fun observeMyReviews(userId: String): Flow<List<BookReview>> = channelFlow {
        val localJob = launch {
            dao.observeMyReviews(userId).map { it.map(BookReviewEntity::toDomain) }.collect { send(it) }
        }
        val collection = firestore.collection("users").document(userId).collection("reviews")
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
            } else if (snapshot != null) {
                syncScope.launch {
                    val remote = snapshot.documents.mapNotNull { it.toBookReviewDtoOrNull() }.filter { it.reviewerId == userId }
                    runCatching { reconcile(userId, remote, !snapshot.metadata.isFromCache, userScope = true) }
                    pushPending(userId = userId)
                }
            }
        }
        pushPending(userId = userId)
        awaitClose { listener.remove(); localJob.cancel() }
    }

    override fun observePendingChanges(userId: String): Flow<Int> = dao.observePendingReviewChanges(userId)

    override suspend fun saveReview(
        userId: String,
        username: String,
        book: Book,
        rating: Int,
        reviewText: String?,
    ): BookReview {
        require(userId.isNotBlank()) { "A signed-in user is required to write a review." }
        val previousEntity = dao.getBookReview(userId, book.id)?.takeUnless { it.deleted }
        val previous = previousEntity?.toDomain()
        val review = createOrUpdateBookReview(previous, userId, username, book, rating, reviewText, nextTimestamp(previous?.updatedAt))
        dao.upsertBookReview(review.toEntity())
        pushPending(userId = userId, bookId = book.id)
        return review
    }

    override suspend fun deleteReview(userId: String, bookId: String) {
        require(userId.isNotBlank()) { "A signed-in user is required to delete a review." }
        val currentEntity = dao.getBookReview(userId, bookId)?.takeUnless { it.deleted } ?: return
        val deleted = markBookReviewDeleted(currentEntity.toDomain(), userId, nextTimestamp(currentEntity.updatedAt))
        dao.upsertBookReview(deleted.toEntity())
        pushPending(userId = userId, bookId = bookId)
    }

    override suspend fun synchronizePending(userId: String) {
        require(userId.isNotBlank())
        pushPending(userId = userId)
    }

    private fun pushPending(userId: String? = null, bookId: String? = null) {
        syncScope.launch {
            val pending = when {
                userId != null -> runCatching { dao.getAllReviewsForUser(userId).filter(BookReviewEntity::pendingSync) }.getOrNull()
                bookId != null -> runCatching { dao.getAllReviewsForBook(bookId).filter(BookReviewEntity::pendingSync) }.getOrNull()
                else -> null
            } ?: return@launch
            pending.forEach { entity ->
                val key = "${entity.userId}/${entity.bookId}"
                synchronized(inFlight) { if (!inFlight.add(key)) return@forEach }
                try {
                    val bookKey = reviewCollectionId(entity.bookId)
                    val publicDocument = firestore.collection("bookReviews").document(bookKey)
                        .collection("reviews").document(entity.userId)
                    val personalDocument = firestore.collection("users").document(entity.userId)
                        .collection("reviews").document(bookKey)
                    val batch = firestore.batch()
                    if (entity.deleted) {
                        batch.delete(publicDocument)
                        batch.delete(personalDocument)
                    } else {
                        val review = entity.toDomain()
                        val fields = review.toDto().toFirestoreMap()
                        batch.set(publicDocument, fields)
                        batch.set(personalDocument, fields)
                    }
                    batch.commit().addOnCompleteListener { task ->
                        syncScope.launch {
                            try {
                                if (task.isSuccessful) {
                                    if (entity.deleted) {
                                        dao.removeDeletedBookReviewIfCurrent(entity.userId, entity.bookId, entity.updatedAt)
                                        removeReviewActivity(entity.userId, entity.bookId)
                                    } else {
                                        dao.markBookReviewSynced(entity.userId, entity.bookId, entity.updatedAt)
                                        publishReviewActivity(entity.toDomain())
                                    }
                                }
                            } catch (_: Exception) {
                                // Keep the durable Room entry pending when local acknowledgement fails.
                            } finally {
                                synchronized(inFlight) { inFlight.remove(key) }
                                if (task.isSuccessful) pushPending(userId = entity.userId, bookId = entity.bookId)
                            }
                        }
                    }
                } catch (_: Exception) {
                    synchronized(inFlight) { inFlight.remove(key) }
                }
            }
        }
    }

    private suspend fun reconcile(
        scopeId: String,
        remote: List<com.fable.app.data.remote.firestore.BookReviewDto>,
        isServerSnapshot: Boolean,
        userScope: Boolean = false,
    ) {
        val local = if (userScope) dao.getAllReviewsForUser(scopeId) else dao.getAllReviewsForBook(scopeId)
        val pendingIds = local.filter(BookReviewEntity::pendingSync).mapTo(mutableSetOf()) { it.userId to it.bookId }
        val validRemote = remote.filter {
            it.bookId.isNotBlank() && if (userScope) it.reviewerId == scopeId else it.bookId == scopeId
        }
        val cache = validRemote.filterNot { (it.reviewerId to it.bookId) in pendingIds }.mapNotNull { it.toEntity() }
        if (cache.isNotEmpty()) dao.upsertBookReviews(cache)
        if (isServerSnapshot) {
            val remoteKeys = validRemote.mapTo(mutableSetOf()) { it.reviewerId to it.bookId }
            local.filter { !it.pendingSync && (it.userId to it.bookId) !in remoteKeys }
                .forEach { dao.removeBookReview(it.userId, it.bookId) }
        }
    }

    private suspend fun publishReviewActivity(review: BookReview) {
        runCatching {
            val profileSnapshot = firestore.collection("publicProfiles").document(review.reviewerId).get().await()
            val username = profileSnapshot.getString("username") ?: return
            val activity = generateReviewActivity(
                PublicReaderProfile(
                    uid = review.reviewerId,
                    username = username,
                    displayName = profileSnapshot.getString("displayName") ?: username,
                ),
                review,
            )
            firestore.collection("users").document(review.reviewerId).collection("activities")
                .document(activityDocumentId(activity.id)).set(activity.toFirestoreMap()).await()
        }
    }

    private suspend fun removeReviewActivity(userId: String, bookId: String) {
        runCatching {
            firestore.collection("users").document(userId).collection("activities")
                .document(activityDocumentId("review:$bookId")).delete().await()
        }
    }

    private fun ReaderActivity.toFirestoreMap(): Map<String, Any?> = mapOf(
        "readerId" to readerId,
        "username" to username,
        "displayName" to displayName,
        "eventType" to eventType.name,
        "bookId" to bookId,
        "bookTitle" to bookTitle,
        "bookThumbnailUrl" to bookThumbnailUrl,
        "currentPage" to currentPage,
        "progressPercent" to progressPercent,
        "rating" to rating,
        "reviewText" to reviewText,
        "timestamp" to timestamp,
    )

    private fun activityDocumentId(value: String): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun nextTimestamp(previous: Long?): Long {
        val now = System.currentTimeMillis()
        return if (previous == null) now else maxOf(now, previous + 1L)
    }
}
