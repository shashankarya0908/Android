package com.fable.app.presentation.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fable.app.domain.model.Book
import com.fable.app.domain.model.ReadingStatus
import com.fable.app.domain.model.ReadingSyncState
import com.fable.app.domain.model.UserBook
import com.fable.app.domain.model.calculateReadingStatistics
import com.fable.app.domain.repository.ReadingRepository
import com.fable.app.domain.usecase.AddOrMoveBook
import com.fable.app.domain.usecase.RemoveLibraryBook
import com.fable.app.domain.usecase.UpdateReadingProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibrarySort { ADDED_DATE, TITLE, AUTHOR }

data class ReadingUiState(
    val books: List<UserBook> = emptyList(),
    val selectedShelf: ReadingStatus = ReadingStatus.CURRENTLY_READING,
    val query: String = "",
    val category: String? = null,
    val sort: LibrarySort = LibrarySort.ADDED_DATE,
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
    val syncState: ReadingSyncState = ReadingSyncState(),
) {
    val statistics get() = calculateReadingStatistics(books)
    val categories get() = books.flatMap { it.book.categories }.distinct().sorted()
    val visibleBooks: List<UserBook>
        get() = books.asSequence()
            .filter { it.status == selectedShelf }
            .filter { category == null || category in it.book.categories }
            .filter { query.isBlank() || it.book.title.contains(query, true) || it.book.authors.any { author -> author.contains(query, true) } }
            .let { sequence ->
                when (sort) {
                    LibrarySort.ADDED_DATE -> sequence.sortedByDescending(UserBook::addedAt)
                    LibrarySort.TITLE -> sequence.sortedBy { it.book.title.lowercase() }
                    LibrarySort.AUTHOR -> sequence.sortedBy { it.book.authors.firstOrNull().orEmpty().lowercase() }
                }
            }.toList()
}

class ReadingViewModel(private val userId: String, private val repository: ReadingRepository) : ViewModel() {
    private val addOrMove = AddOrMoveBook(repository)
    private val remove = RemoveLibraryBook(repository)
    private val updateProgress = UpdateReadingProgress(repository)
    private val _uiState = MutableStateFlow(ReadingUiState())
    val uiState: StateFlow<ReadingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeLibrary(userId)
                .catch { error -> _uiState.update { it.copy(isLoading = false, error = error.userMessage()) } }
                .collect { books -> _uiState.update { it.copy(books = books, isLoading = false, error = null) } }
        }
        viewModelScope.launch {
            runCatching { repository.synchronizePending(userId) }
        }
        viewModelScope.launch {
            repository.observeSyncState(userId).collect { sync ->
                _uiState.update { it.copy(syncState = sync) }
            }
        }
    }

    fun selectShelf(status: ReadingStatus) = _uiState.update { it.copy(selectedShelf = status) }
    fun setQuery(query: String) = _uiState.update { it.copy(query = query) }
    fun setCategory(category: String?) = _uiState.update { it.copy(category = category) }
    fun setSort(sort: LibrarySort) = _uiState.update { it.copy(sort = sort) }
    fun clearMessage() = _uiState.update { it.copy(message = null) }

    fun addOrMove(book: Book, status: ReadingStatus) = perform {
        val saved = addOrMove(userId, book, status)
        _uiState.update {
            it.copy(message = if (saved.syncPending) "Saved on this device. Waiting to sync with your account." else "Moved to ${status.label()}.")
        }
    }

    fun removeBook(bookId: String) = perform {
        remove(userId, bookId)
        _uiState.update { it.copy(message = "Removed from your library.") }
    }

    fun saveProgress(bookId: String, currentPage: Int, notes: String?) = perform {
        val saved = updateProgress(userId, bookId, currentPage, notes)
        _uiState.update {
            it.copy(message = if (saved.syncPending) "Progress saved on this device and queued to sync." else "Reading progress updated.")
        }
    }

    private fun perform(action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null, message = null) }
            try {
                action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _uiState.update { it.copy(error = error.userMessage()) }
            }
        }
    }

    private fun Throwable.userMessage() = message?.takeIf(String::isNotBlank) ?: "We could not update your library. Please try again."
}

fun ReadingStatus.label(): String = when (this) {
    ReadingStatus.WANT_TO_READ -> "Want to Read"
    ReadingStatus.CURRENTLY_READING -> "Currently Reading"
    ReadingStatus.READ -> "Read"
}
