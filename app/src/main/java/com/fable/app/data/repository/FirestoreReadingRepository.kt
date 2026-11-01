package com.fable.app.data.repository

import com.fable.app.data.local.BookDao
import com.fable.app.data.local.LibraryCacheEntity
import com.fable.app.data.local.toCacheEntity
import com.fable.app.data.local.toDomain
import com.fable.app.data.remote.firestore.libraryDocumentId
import com.fable.app.data.remote.firestore.toCacheEntity
import com.fable.app.data.remote.firestore.toDto
import com.fable.app.data.remote.firestore.toFirestoreMap
import com.fable.app.data.remote.firestore.toUserBookDtoOrNull
import com.fable.app.domain.model.Book
import com.fable.app.domain.model.ReadingStatus
import com.fable.app.domain.model.ReadingSyncState
import com.fable.app.domain.model.PublicReaderProfile
import com.fable.app.domain.model.UserBook
import com.fable.app.domain.model.calculateReadingStatistics
import com.fable.app.domain.model.generateLibraryActivities
import com.fable.app.domain.model.moveUserBook
import com.fable.app.domain.model.withProgress
import com.fable.app.domain.repository.ReadingRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

class FirestoreReadingRepository(
    private val firestore: FirebaseFirestore,
    private val dao: BookDao,
) : ReadingRepository {
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inFlight = mutableSetOf<String>()
    private val syncStates = ConcurrentHashMap<String, MutableStateFlow<ReadingSyncState>>()

    override fun observeSyncState(userId: String): Flow<ReadingSyncState> = combine(
        dao.observePendingLibraryChanges(userId),
        syncState(userId),
    ) { pending, state -> state.copy(pendingChanges = pending) }

    override fun observeLibrary(userId: String): Flow<List<UserBook>> = channelFlow {
        val localJob = launch {
            dao.observeLibrary(userId).map { entries ->
                entries.filterNot(LibraryCacheEntity::deleted).map(LibraryCacheEntity::toDomain)
            }.collect { send(it) }
        }
        val listener = firestore.collection("users").document(userId).collection("library")
            .addSnapshotListener { snapshot, error ->
                if (error != null) syncState(userId).value = ReadingSyncState(errorMessage = "Library sync is unavailable. Saved changes are waiting to sync.")
                if (error == null && snapshot != null) {
                    if (!snapshot.metadata.isFromCache) syncState(userId).value = ReadingSyncState()
                    syncScope.launch {
                        reconcileCloudSnapshot(
                            userId,
                            snapshot.documents.mapNotNull { it.toUserBookDtoOrNull() },
                            isServerSnapshot = !snapshot.metadata.isFromCache,
                        )
                        if (!snapshot.metadata.isFromCache) refreshPublicReadingStats(userId)
                        pushPending(userId)
                    }
                }
            }
        pushPending(userId)
        awaitClose { listener.remove(); localJob.cancel() }
    }

    override fun observeStatistics(userId: String) = observeLibrary(userId).map(::calculateReadingStatistics)

    override suspend fun addOrMoveBook(userId: String, book: Book, status: ReadingStatus): UserBook {
        val previousEntity = dao.getLibraryEntry(userId, book.id)?.takeUnless { it.deleted }
        val previous = previousEntity?.toDomain()
        val now = nextTimestamp(previous?.updatedAt)
        val userBook = moveUserBook(previous, userId, book, status, now)
        dao.upsertLibraryEntry(userBook.toCacheEntity())
        pushPending(userId)
        return userBook
    }

    override suspend fun removeBook(userId: String, bookId: String) {
        require(userId.isNotBlank()) { "A signed-in user is required to update the library." }
        val current = dao.getLibraryEntry(userId, bookId) ?: return
        dao.upsertLibraryEntry(current.copy(syncPending = true, deleted = true, updatedAt = nextTimestamp(current.updatedAt)))
        pushPending(userId)
    }

    override suspend fun updateProgress(userId: String, bookId: String, currentPage: Int, personalNotes: String?): UserBook {
        val current = dao.getLibraryEntry(userId, bookId)?.takeUnless { it.deleted }?.toDomain()
            ?: error("This book is not in your library.")
        val updated = current.withProgress(currentPage, personalNotes, nextTimestamp(current.updatedAt))
        dao.upsertLibraryEntry(updated.toCacheEntity())
        pushPending(userId)
        return updated
    }

    override suspend fun synchronizePending(userId: String) {
        require(userId.isNotBlank())
        pushPending(userId)
    }

    private fun pushPending(userId: String) {
        syncScope.launch {
            val pending = runCatching { dao.getAllLibraryEntries(userId).filter(LibraryCacheEntity::syncPending) }.getOrNull()
                ?: return@launch
            pending.forEach { entry ->
                val key = "$userId/${entry.bookId}"
                synchronized(inFlight) { if (!inFlight.add(key)) return@forEach }
                val document = firestore.collection("users").document(userId).collection("library")
                    .document(libraryDocumentId(entry.bookId))
                val completion = if (entry.deleted) document.delete()
                else {
                    val userBook = entry.toDomain()
                    document.set(userBook.toDto().toFirestoreMap())
                }
                completion.addOnCompleteListener { task ->
                    syncScope.launch {
                        try {
                            if (task.isSuccessful) {
                                if (entry.deleted) dao.removeDeletedLibraryEntryIfCurrent(userId, entry.bookId, entry.updatedAt)
                                else dao.markLibrarySynced(userId, entry.bookId, entry.updatedAt)
                                refreshPublicReadingStats(userId)
                                if (!entry.deleted) publishLibraryActivities(entry.toDomain())
                                val stillPending = dao.getAllLibraryEntries(userId).any(LibraryCacheEntity::syncPending)
                                if (!stillPending) syncState(userId).value = ReadingSyncState()
                            } else {
                                syncState(userId).value = ReadingSyncState(errorMessage = "Library sync failed. Saved changes are waiting to sync.")
                            }
                        } finally {
                            synchronized(inFlight) { inFlight.remove(key) }
                            if (task.isSuccessful) pushPending(userId)
                        }
                    }
                }
            }
        }
    }

    private suspend fun reconcileCloudSnapshot(
        userId: String,
        remote: List<com.fable.app.data.remote.firestore.UserBookDto>,
        isServerSnapshot: Boolean,
    ) {
        val local = dao.getAllLibraryEntries(userId)
        val pendingIds = local.filter(LibraryCacheEntity::syncPending).mapTo(mutableSetOf(), LibraryCacheEntity::bookId)
        val userRemote = remote.filter { it.userId == userId }
        val cacheEntries = userRemote.filterNot { it.bookId in pendingIds }.mapNotNull { it.toCacheEntity() }
        if (cacheEntries.isNotEmpty()) dao.upsertLibraryEntries(cacheEntries)
        val remoteIds = userRemote.map { it.bookId }
        val remoteIdSet = remoteIds.toSet()
        if (isServerSnapshot) {
            local.filter { !it.syncPending && it.bookId !in remoteIdSet }
                .forEach { dao.removeLibraryEntry(userId, it.bookId) }
        }
    }

    private suspend fun refreshPublicReadingStats(userId: String) {
        runCatching {
            val records = dao.getAllLibraryEntries(userId)
                .filter { !it.deleted && !it.syncPending }
                .map(LibraryCacheEntity::toDomain)
            val stats = calculateReadingStatistics(records)
            firestore.collection("publicProfiles").document(userId).update(
                mapOf(
                    "completedBooks" to stats.totalBooksCompleted,
                    "pagesRead" to stats.totalPagesRead,
                    "currentlyReading" to stats.currentlyReadingCount,
                    "wantToRead" to stats.wantToReadCount,
                ),
            ).await()
        }
    }

    private suspend fun publishLibraryActivities(book: UserBook) {
        runCatching {
            val profileSnapshot = firestore.collection("publicProfiles").document(book.userId).get().await()
            val username = profileSnapshot.getString("username") ?: return
            val profile = PublicReaderProfile(
                uid = book.userId,
                username = username,
                displayName = profileSnapshot.getString("displayName") ?: username,
            )
            val activityCollection = firestore.collection("users").document(book.userId).collection("activities")
            generateLibraryActivities(profile, book).forEach { activity ->
                val document = activityCollection.document(activityDocumentId(activity.id))
                if (!document.get().await().exists()) document.set(activity.toFirestoreMap()).await()
            }
        }
    }

    private fun com.fable.app.domain.model.ReaderActivity.toFirestoreMap(): Map<String, Any?> = mapOf(
        "readerId" to readerId,
        "username" to username,
        "displayName" to displayName,
        "eventType" to eventType.name,
        "bookId" to bookId,
        "bookTitle" to bookTitle,
        "bookThumbnailUrl" to bookThumbnailUrl,
        "currentPage" to currentPage,
        "progressPercent" to progressPercent,
        "rating" to null,
        "reviewText" to null,
        "timestamp" to timestamp,
    )

    private fun activityDocumentId(value: String): String = java.security.MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun nextTimestamp(previous: Long?): Long {
        val wallClock = System.currentTimeMillis()
        return if (previous == null) wallClock else maxOf(wallClock, previous + 1L)
    }

    private fun syncState(userId: String) = syncStates.computeIfAbsent(userId) { MutableStateFlow(ReadingSyncState()) }
}
