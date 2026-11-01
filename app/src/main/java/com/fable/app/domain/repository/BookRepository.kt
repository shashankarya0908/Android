package com.fable.app.domain.repository

import com.fable.app.domain.model.Book
import com.fable.app.domain.model.BookPage
import com.fable.app.domain.model.SearchHistoryEntry
import kotlinx.coroutines.flow.Flow

enum class BookSearchType(val queryPrefix: String?) {
    GENERAL(null),
    TITLE("intitle"),
    AUTHOR("inauthor"),
    ISBN("isbn"),
    SUBJECT("subject"),
}

interface BookRepository {
    suspend fun searchBooks(
        query: String,
        type: BookSearchType = BookSearchType.GENERAL,
        startIndex: Int = 0,
        pageSize: Int = 20,
    ): BookPage

    suspend fun discoverByCategory(
        category: String,
        startIndex: Int = 0,
        pageSize: Int = 20,
    ): BookPage

    suspend fun getBook(id: String): Book?
    fun observeSearchHistory(): Flow<List<SearchHistoryEntry>>
    suspend fun recordSearch(query: String)
    suspend fun clearSearchHistory()
}
