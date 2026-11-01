package com.fable.app.data.remote.firestore

import com.fable.app.data.local.LibraryCacheEntity
import com.fable.app.domain.model.Book
import com.fable.app.domain.model.ReadingStatus
import com.fable.app.domain.model.UserBook
import com.google.firebase.firestore.DocumentSnapshot
import com.google.gson.Gson
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class UserBookDto(
    val userId: String,
    val bookId: String,
    val bookSnapshot: String,
    val status: String,
    val currentPage: Int,
    val totalPages: Int?,
    val startedAt: Long?,
    val finishedAt: Long?,
    val addedAt: Long,
    val updatedAt: Long,
    val personalNotes: String?,
    val completionHistory: List<Long>,
)

private val gson = Gson()

fun UserBook.toDto(): UserBookDto = UserBookDto(
    userId, book.id, gson.toJson(book), status.name, currentPage, totalPages,
    startedAt, finishedAt, addedAt, updatedAt, personalNotes, completionHistory,
)

fun UserBookDto.toFirestoreMap(): Map<String, Any?> = mapOf(
    "userId" to userId,
    "bookId" to bookId,
    "bookSnapshot" to bookSnapshot,
    "status" to status,
    "currentPage" to currentPage,
    "totalPages" to totalPages,
    "startedAt" to startedAt,
    "finishedAt" to finishedAt,
    "addedAt" to addedAt,
    "updatedAt" to updatedAt,
    "personalNotes" to personalNotes,
    "completionHistory" to completionHistory,
)

fun DocumentSnapshot.toUserBookDtoOrNull(): UserBookDto? = runCatching {
    val data = data ?: return null
    UserBookDto(
        userId = data["userId"] as? String ?: return null,
        bookId = data["bookId"] as? String ?: return null,
        bookSnapshot = data["bookSnapshot"] as? String ?: return null,
        status = data["status"] as? String ?: return null,
        currentPage = (data["currentPage"] as? Number)?.toInt() ?: 0,
        totalPages = (data["totalPages"] as? Number)?.toInt(),
        startedAt = (data["startedAt"] as? Number)?.toLong(),
        finishedAt = (data["finishedAt"] as? Number)?.toLong(),
        addedAt = (data["addedAt"] as? Number)?.toLong() ?: 0L,
        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L,
        personalNotes = data["personalNotes"] as? String,
        completionHistory = (data["completionHistory"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() }.orEmpty(),
    )
}.getOrNull()

fun UserBookDto.toDomain(syncPending: Boolean = false): UserBook? = runCatching {
    val book = gson.fromJson(bookSnapshot, Book::class.java) ?: return null
    if (book.id != bookId || userId.isBlank()) return null
    UserBook(
        userId = userId,
        book = book,
        status = ReadingStatus.valueOf(status),
        currentPage = currentPage.coerceAtLeast(0),
        totalPages = totalPages?.takeIf { it > 0 },
        startedAt = startedAt,
        finishedAt = finishedAt,
        addedAt = addedAt,
        updatedAt = updatedAt,
        personalNotes = personalNotes,
        completionHistory = completionHistory,
        syncPending = syncPending,
    )
}.getOrNull()

fun UserBookDto.toCacheEntity(syncPending: Boolean = false): LibraryCacheEntity? = toDomain(syncPending)?.let {
    LibraryCacheEntity(
        userId = it.userId,
        bookId = it.bookId,
        bookJson = gson.toJson(it.book),
        status = it.status.name,
        currentPage = it.currentPage,
        totalPages = it.totalPages,
        startedAt = it.startedAt,
        finishedAt = it.finishedAt,
        addedAt = it.addedAt,
        updatedAt = it.updatedAt,
        personalNotes = it.personalNotes,
        completionHistoryJson = gson.toJson(it.completionHistory),
        syncPending = syncPending,
        deleted = false,
    )
}

fun libraryDocumentId(bookId: String): String = MessageDigest.getInstance("SHA-256")
    .digest(bookId.toByteArray(StandardCharsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
