package com.fable.app.domain.usecase

import com.fable.app.domain.model.Book
import com.fable.app.domain.model.BookPage
import com.fable.app.domain.model.SearchHistoryEntry
import com.fable.app.domain.repository.BookRepository
import com.fable.app.domain.repository.BookSearchType
import kotlinx.coroutines.flow.Flow

class SearchBooks(private val repository: BookRepository) {
    suspend operator fun invoke(
        query: String,
        type: BookSearchType = BookSearchType.GENERAL,
        startIndex: Int = 0,
        pageSize: Int = 20,
    ): BookPage = repository.searchBooks(query, type, startIndex, pageSize)
}

class DiscoverBooks(private val repository: BookRepository) {
    suspend operator fun invoke(category: String, pageSize: Int = 20): BookPage =
        repository.discoverByCategory(category, pageSize = pageSize)
}

class GetBookDetails(private val repository: BookRepository) {
    suspend operator fun invoke(id: String): Book? = repository.getBook(id)
}

class ObserveSearchHistory(private val repository: BookRepository) {
    operator fun invoke(): Flow<List<SearchHistoryEntry>> = repository.observeSearchHistory()
}

class RecordSearch(private val repository: BookRepository) {
    suspend operator fun invoke(query: String) = repository.recordSearch(query)
}

class ClearSearchHistory(private val repository: BookRepository) {
    suspend operator fun invoke() = repository.clearSearchHistory()
}
