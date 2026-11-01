package com.fable.app.domain.repository

import com.fable.app.domain.model.Book
import com.fable.app.domain.model.ReadingStatistics
import com.fable.app.domain.model.ReadingSyncState
import com.fable.app.domain.model.ReadingStatus
import com.fable.app.domain.model.UserBook
import kotlinx.coroutines.flow.Flow

interface ReadingRepository {
    fun observeLibrary(userId: String): Flow<List<UserBook>>
    fun observeStatistics(userId: String): Flow<ReadingStatistics>
    fun observeSyncState(userId: String): Flow<ReadingSyncState>
    suspend fun addOrMoveBook(userId: String, book: Book, status: ReadingStatus): UserBook
    suspend fun removeBook(userId: String, bookId: String)
    suspend fun updateProgress(userId: String, bookId: String, currentPage: Int, personalNotes: String?): UserBook
    suspend fun synchronizePending(userId: String)
}
