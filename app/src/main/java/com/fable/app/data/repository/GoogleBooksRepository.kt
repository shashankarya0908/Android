package com.fable.app.data.repository

import com.fable.app.data.local.BookDao
import com.fable.app.data.local.BookCacheEntity
import com.fable.app.data.local.SearchHistoryEntity
import com.fable.app.data.local.toCacheEntity
import com.fable.app.data.local.toDomain
import com.fable.app.data.remote.googlebooks.GOOGLE_BOOKS_SOURCE
import com.fable.app.data.remote.googlebooks.GoogleBooksApiService
import com.fable.app.data.remote.googlebooks.GoogleBooksQueryBuilder
import com.fable.app.data.remote.googlebooks.toDomainOrNull
import com.fable.app.data.remote.googlebooks.toDomainPage
import com.fable.app.domain.model.Book
import com.fable.app.domain.model.BookIds
import com.fable.app.domain.model.BookPage
import com.fable.app.domain.model.SearchHistoryEntry
import com.fable.app.domain.repository.BookRepository
import com.fable.app.domain.repository.BookSearchType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException

class BookRepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)

class GoogleBooksRepository(
    private val api: GoogleBooksApiService,
    private val bookDao: BookDao,
    private val apiKey: String? = null,
) : BookRepository {
    override suspend fun searchBooks(
        query: String,
        type: BookSearchType,
        startIndex: Int,
        pageSize: Int,
    ): BookPage {
        val safeStart = startIndex.coerceAtLeast(0)
        val safePageSize = pageSize.coerceIn(1, GOOGLE_BOOKS_MAX_PAGE_SIZE)
        val apiQuery = GoogleBooksQueryBuilder.build(query, type)
        return try {
            api.searchVolumes(apiQuery, safeStart, safePageSize, apiKey = apiKey)
                .toDomainPage(safeStart, safePageSize)
                .also { page -> bookDao.cacheBooks(page.books.map(Book::toCacheEntity)) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val cached = runCatching { bookDao.searchCachedBooks(query.trim(), safePageSize) }
                .getOrDefault(emptyList())
                .map(BookCacheEntity::toDomain)
            if (cached.isNotEmpty()) {
                BookPage(
                    books = cached.distinctBy(Book::id),
                    totalItems = cached.size,
                    nextStartIndex = safeStart,
                    hasMore = false,
                )
            } else {
                throw error.toBookRepositoryException()
            }
        }
    }

    override suspend fun discoverByCategory(category: String, startIndex: Int, pageSize: Int): BookPage =
        searchBooks(category, BookSearchType.SUBJECT, startIndex, pageSize)

    override suspend fun getBook(id: String): Book? {
        val (source, sourceId) = BookIds.parse(id) ?: return null
        if (source != GOOGLE_BOOKS_SOURCE) return null
        return try {
            val remote = api.getVolume(sourceId, apiKey).toDomainOrNull()
            if (remote != null) {
                bookDao.cacheBooks(listOf(remote.toCacheEntity()))
                remote
            } else {
                runCatching { bookDao.getCachedBook(id)?.toDomain() }.getOrNull()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            runCatching { bookDao.getCachedBook(id)?.toDomain() }.getOrNull()
                ?: throw error.toBookRepositoryException()
        }
    }

    override fun observeSearchHistory(): Flow<List<SearchHistoryEntry>> =
        bookDao.observeSearchHistory(BookDao.SEARCH_HISTORY_LIMIT).map { rows -> rows.map { it.toDomain() } }

    override suspend fun recordSearch(query: String) {
        val cleanQuery = query.trim().takeIf(String::isNotEmpty) ?: return
        bookDao.saveSearch(
            SearchHistoryEntity(
                normalizedQuery = cleanQuery.lowercase(),
                query = cleanQuery,
                searchedAtMillis = System.currentTimeMillis(),
            ),
        )
        bookDao.pruneSearchHistory(BookDao.SEARCH_HISTORY_LIMIT)
    }

    override suspend fun clearSearchHistory() = bookDao.clearSearchHistory()

    private fun Throwable.toBookRepositoryException(): BookRepositoryException = when (this) {
        is BookRepositoryException -> this
        is IOException -> BookRepositoryException("Network connection failed. Check your connection and try again.", this)
        is HttpException -> when {
            code() == 403 || code() == 429 -> BookRepositoryException(
                "Google Books is limiting requests right now. Try again in a little while.", this,
            )
            code() == 404 -> BookRepositoryException("This book could not be found in Google Books.", this)
            code() >= 500 -> BookRepositoryException("Google Books is temporarily unavailable. Try again shortly.", this)
            else -> BookRepositoryException("Google Books request failed (HTTP ${code()}).", this)
        }
        else -> BookRepositoryException("We could not load books. Please try again.", this)
    }

    companion object {
        const val GOOGLE_BOOKS_MAX_PAGE_SIZE = 40
    }
}
