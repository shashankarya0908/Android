package com.fable.app.data.local

import androidx.room.Entity
import androidx.room.Index
import com.fable.app.domain.model.Book
import com.fable.app.domain.model.ReadingStatus
import com.fable.app.domain.model.UserBook
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(
    tableName = "user_books",
    primaryKeys = ["userId", "bookId"],
    indices = [Index("userId"), Index("status")],
)
data class LibraryCacheEntity(
    val userId: String,
    val bookId: String,
    val bookJson: String,
    val status: String,
    val currentPage: Int,
    val totalPages: Int?,
    val startedAt: Long?,
    val finishedAt: Long?,
    val addedAt: Long,
    val updatedAt: Long,
    val personalNotes: String?,
    val completionHistoryJson: String,
    val syncPending: Boolean,
    val deleted: Boolean,
)

private val gson = Gson()
private val longListType = object : TypeToken<List<Long>>() {}.type

fun UserBook.toCacheEntity(deleted: Boolean = false): LibraryCacheEntity = LibraryCacheEntity(
    userId = userId,
    bookId = book.id,
    bookJson = gson.toJson(book),
    status = status.name,
    currentPage = currentPage,
    totalPages = totalPages,
    startedAt = startedAt,
    finishedAt = finishedAt,
    addedAt = addedAt,
    updatedAt = updatedAt,
    personalNotes = personalNotes,
    completionHistoryJson = gson.toJson(completionHistory),
    syncPending = syncPending,
    deleted = deleted,
)

fun LibraryCacheEntity.toDomain(): UserBook = UserBook(
    userId = userId,
    book = gson.fromJson(bookJson, Book::class.java),
    status = runCatching { ReadingStatus.valueOf(status) }.getOrDefault(ReadingStatus.WANT_TO_READ),
    currentPage = currentPage,
    totalPages = totalPages,
    startedAt = startedAt,
    finishedAt = finishedAt,
    addedAt = addedAt,
    updatedAt = updatedAt,
    personalNotes = personalNotes,
    completionHistory = runCatching { gson.fromJson<List<Long>>(completionHistoryJson, longListType) }.getOrDefault(emptyList()),
    syncPending = syncPending,
)
