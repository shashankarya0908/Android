package com.fable.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @androidx.room.Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLibraryEntry(entry: LibraryCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLibraryEntries(entries: List<LibraryCacheEntity>)

    @Query("SELECT * FROM user_books WHERE userId = :userId AND deleted = 0 ORDER BY addedAt DESC")
    fun observeLibrary(userId: String): Flow<List<LibraryCacheEntity>>

    @Query("SELECT COUNT(*) FROM user_books WHERE userId = :userId AND syncPending = 1")
    fun observePendingLibraryChanges(userId: String): Flow<Int>

    @Query("SELECT * FROM user_books WHERE userId = :userId ORDER BY updatedAt ASC")
    suspend fun getAllLibraryEntries(userId: String): List<LibraryCacheEntity>

    @Query("SELECT * FROM user_books WHERE userId = :userId AND bookId = :bookId LIMIT 1")
    suspend fun getLibraryEntry(userId: String, bookId: String): LibraryCacheEntity?

    @Query("UPDATE user_books SET syncPending = 0 WHERE userId = :userId AND bookId = :bookId AND updatedAt = :expectedUpdatedAt AND deleted = 0")
    suspend fun markLibrarySynced(userId: String, bookId: String, expectedUpdatedAt: Long)

    @Query("DELETE FROM user_books WHERE userId = :userId AND bookId = :bookId AND updatedAt = :expectedUpdatedAt AND deleted = 1")
    suspend fun removeDeletedLibraryEntryIfCurrent(userId: String, bookId: String, expectedUpdatedAt: Long)

    @Query("DELETE FROM user_books WHERE userId = :userId AND bookId = :bookId")
    suspend fun removeLibraryEntry(userId: String, bookId: String)

    @Query("DELETE FROM user_books WHERE userId = :userId AND syncPending = 0 AND bookId NOT IN (:remoteBookIds)")
    suspend fun removeRemoteMissingEntries(userId: String, remoteBookIds: List<String>)

    @Query("DELETE FROM user_books WHERE userId = :userId AND syncPending = 0")
    suspend fun removeAllSyncedLibraryEntries(userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun cacheBooks(books: List<BookCacheEntity>)

    @Query("SELECT * FROM cached_books WHERE id = :id LIMIT 1")
    suspend fun getCachedBook(id: String): BookCacheEntity?

    @Query(
        "SELECT * FROM cached_books " +
            "WHERE title LIKE '%' || :query || '%' COLLATE NOCASE " +
            "OR searchableText LIKE '%' || :query || '%' COLLATE NOCASE " +
            "ORDER BY cachedAtMillis DESC LIMIT :limit",
    )
    suspend fun searchCachedBooks(query: String, limit: Int): List<BookCacheEntity>

    @Query("SELECT * FROM search_history ORDER BY searchedAtMillis DESC LIMIT :limit")
    fun observeSearchHistory(limit: Int): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSearch(entity: SearchHistoryEntity)

    @Query(
        "DELETE FROM search_history WHERE normalizedQuery NOT IN " +
            "(SELECT normalizedQuery FROM search_history ORDER BY searchedAtMillis DESC LIMIT :limit)",
    )
    suspend fun pruneSearchHistory(limit: Int)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    companion object {
        const val SEARCH_HISTORY_LIMIT = 10
    }
}
