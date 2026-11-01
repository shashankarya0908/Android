package com.fable.app.presentation.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fable.app.domain.model.Book
import com.fable.app.domain.model.SearchHistoryEntry
import com.fable.app.domain.repository.BookRepository
import com.fable.app.domain.repository.BookSearchType
import com.fable.app.domain.usecase.ClearSearchHistory
import com.fable.app.domain.usecase.ObserveSearchHistory
import com.fable.app.domain.usecase.RecordSearch
import com.fable.app.domain.usecase.SearchBooks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookSearchUiState(
    val query: String = "",
    val type: BookSearchType = BookSearchType.GENERAL,
    val books: List<Book> = emptyList(),
    val searchHistory: List<SearchHistoryEntry> = emptyList(),
    val totalItems: Int = 0,
    val nextStartIndex: Int = 0,
    val hasMore: Boolean = false,
    val hasSearched: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
)

class BookSearchViewModel(repository: BookRepository) : ViewModel() {
    private val searchBooks = SearchBooks(repository)
    private val recordSearch = RecordSearch(repository)
    private val clearHistory = ClearSearchHistory(repository)
    private val _uiState = MutableStateFlow(BookSearchUiState())
    val uiState: StateFlow<BookSearchUiState> = _uiState.asStateFlow()
    private var scheduledSearch: Job? = null
    private var pageRequest: Job? = null

    init {
        viewModelScope.launch {
            ObserveSearchHistory(repository)()
                .catch { /* Search remains available if the local history database is unavailable. */ }
                .collect { history -> _uiState.update { it.copy(searchHistory = history) } }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query, error = null) }
        scheduleSearch()
    }

    fun onTypeChange(type: BookSearchType) {
        _uiState.update { it.copy(type = type, error = null) }
        if (_uiState.value.query.isNotBlank()) scheduleSearch()
    }

    fun submitSearch() {
        scheduledSearch?.cancel()
        val state = _uiState.value
        if (state.query.trim().isEmpty()) return
        pageRequest?.cancel()
        pageRequest = viewModelScope.launch { executeSearch(state.query, state.type) }
    }

    fun useHistory(query: String) {
        _uiState.update { it.copy(query = query) }
        submitSearch()
    }

    fun clearQuery() {
        scheduledSearch?.cancel()
        pageRequest?.cancel()
        _uiState.update {
            it.copy(query = "", books = emptyList(), hasSearched = false, isLoading = false, isLoadingMore = false, error = null)
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (!state.hasMore || state.isLoading || state.isLoadingMore || state.query.isBlank()) return
        pageRequest = viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true, error = null) }
            try {
                val page = searchBooks(state.query, state.type, state.nextStartIndex, PAGE_SIZE)
                _uiState.update { current ->
                    val merged = (current.books + page.books).distinctBy(Book::id)
                    current.copy(
                        books = merged,
                        totalItems = page.totalItems,
                        nextStartIndex = page.nextStartIndex,
                        hasMore = page.hasMore && page.nextStartIndex > state.nextStartIndex && merged.size > state.books.size,
                        isLoadingMore = false,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(isLoadingMore = false, error = error.toUserMessage()) }
            }
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { runCatching { clearHistory() } }
    }

    private fun scheduleSearch() {
        scheduledSearch?.cancel()
        pageRequest?.cancel()
        val state = _uiState.value
        if (state.query.trim().length < MIN_QUERY_LENGTH) {
            _uiState.update {
                it.copy(books = emptyList(), hasSearched = false, isLoading = false, isLoadingMore = false, error = null)
            }
            return
        }
        scheduledSearch = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            executeSearch(state.query, state.type)
        }
    }

    private suspend fun executeSearch(query: String, type: BookSearchType) {
        _uiState.update {
            it.copy(books = emptyList(), totalItems = 0, nextStartIndex = 0, hasMore = false,
                hasSearched = true, isLoading = true, isLoadingMore = false, error = null)
        }
        try {
            recordSearch(query)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Search results do not depend on the optional local history write.
        }
        try {
            val page = searchBooks(query, type, startIndex = 0, pageSize = PAGE_SIZE)
            _uiState.update {
                it.copy(
                    books = page.books.distinctBy(Book::id),
                    totalItems = page.totalItems,
                    nextStartIndex = page.nextStartIndex,
                    hasMore = page.hasMore,
                    isLoading = false,
                    error = null,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _uiState.update { it.copy(isLoading = false, error = error.toUserMessage()) }
        }
    }

    private fun Throwable.toUserMessage(): String = message?.takeIf(String::isNotBlank)
        ?: "We could not load books. Please try again."

    companion object {
        const val PAGE_SIZE = 20
        const val MIN_QUERY_LENGTH = 2
        const val SEARCH_DEBOUNCE_MILLIS = 400L
    }
}

data class DiscoverUiState(
    val featuredBook: Book? = null,
    val popularDiscoveries: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class DiscoverViewModel(repository: BookRepository) : ViewModel() {
    private val discoverBooks = com.fable.app.domain.usecase.DiscoverBooks(repository)
    private val _uiState = MutableStateFlow(DiscoverUiState(isLoading = true))
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = DiscoverUiState(isLoading = true)
            try {
                val books = discoverBooks(DEFAULT_DISCOVERY_CATEGORY, pageSize = DISCOVERY_PAGE_SIZE).books
                _uiState.value = DiscoverUiState(
                    featuredBook = books.firstOrNull(),
                    popularDiscoveries = books.filter { (it.ratingsCount ?: 0) > 0 }
                        .sortedWith(compareByDescending<Book> { it.ratingsCount }.thenByDescending { it.averageRating })
                        .distinctBy(Book::id),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = DiscoverUiState(error = error.message ?: "We could not load discoveries.")
            }
        }
    }

    companion object {
        const val DEFAULT_DISCOVERY_CATEGORY = "Fiction"
        const val DISCOVERY_PAGE_SIZE = 40
    }
}

data class CategoryBooksUiState(
    val category: String,
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

class CategoryBooksViewModel(repository: BookRepository, private val category: String) : ViewModel() {
    private val discoverBooks = com.fable.app.domain.usecase.DiscoverBooks(repository)
    private val _uiState = MutableStateFlow(CategoryBooksUiState(category))
    val uiState: StateFlow<CategoryBooksUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = CategoryBooksUiState(category, isLoading = true)
            try {
                val page = discoverBooks(category, BookSearchViewModel.PAGE_SIZE)
                _uiState.value = CategoryBooksUiState(category, page.books, isLoading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = CategoryBooksUiState(
                    category = category,
                    isLoading = false,
                    error = error.message ?: "We could not load this category.",
                )
            }
        }
    }
}

data class BookDetailsUiState(
    val isLoading: Boolean = true,
    val book: Book? = null,
    val error: String? = null,
)

class BookDetailsViewModel(repository: BookRepository, private val bookId: String) : ViewModel() {
    private val getBookDetails = com.fable.app.domain.usecase.GetBookDetails(repository)
    private val _uiState = MutableStateFlow(BookDetailsUiState())
    val uiState: StateFlow<BookDetailsUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = BookDetailsUiState(isLoading = true)
            try {
                val book = getBookDetails(bookId)
                _uiState.value = if (book == null) BookDetailsUiState(isLoading = false, error = "Book not found.")
                else BookDetailsUiState(isLoading = false, book = book)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.value = BookDetailsUiState(isLoading = false, error = error.message ?: "We could not load this book.")
            }
        }
    }
}
