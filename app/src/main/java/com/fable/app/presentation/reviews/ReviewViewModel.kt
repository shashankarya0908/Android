package com.fable.app.presentation.reviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fable.app.domain.model.Book
import com.fable.app.domain.model.BookReview
import com.fable.app.domain.model.calculateBookReviewSummary
import com.fable.app.domain.repository.ReviewRepository
import com.fable.app.domain.usecase.DeleteBookReview
import com.fable.app.domain.usecase.SaveBookReview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewUiState(
    val reviews: List<BookReview> = emptyList(),
    val myReviews: List<BookReview> = emptyList(),
    val pendingChanges: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
) {
    val summary get() = calculateBookReviewSummary(reviews)
}

class ReviewViewModel(
    private val userId: String,
    private val username: String,
    private val repository: ReviewRepository,
    bookId: String? = null,
) : ViewModel() {
    private val observesBook = !bookId.isNullOrBlank()
    private val saveReview = SaveBookReview(repository)
    private val deleteReview = DeleteBookReview(repository)
    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeMyReviews(userId)
                .catch { error -> _uiState.update { it.copy(error = error.userMessage(), isLoading = it.isLoading && observesBook) } }
                .collect { entries -> _uiState.update { it.copy(myReviews = entries, isLoading = observesBook && it.isLoading) } }
        }
        if (!bookId.isNullOrBlank()) {
            viewModelScope.launch {
                repository.observeBookReviews(bookId)
                    .catch { error -> _uiState.update { it.copy(isLoading = false, error = error.userMessage()) } }
                    .collect { entries -> _uiState.update { it.copy(reviews = entries, isLoading = false, error = null) } }
            }
        }
        viewModelScope.launch {
            repository.observePendingChanges(userId).collect { count -> _uiState.update { it.copy(pendingChanges = count) } }
        }
        viewModelScope.launch { runCatching { repository.synchronizePending(userId) } }
    }

    fun save(book: Book, rating: Int, text: String?) = perform {
        saveReview(userId, username, book, rating, text)
        _uiState.update { it.copy(message = "Your rating and review were saved.") }
    }

    fun delete(bookId: String) = perform {
        deleteReview(userId, bookId)
        _uiState.update { it.copy(message = "Your review was deleted.") }
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

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

    private fun Throwable.userMessage() = message?.takeIf(String::isNotBlank) ?: "We could not load or save reviews. Please try again."
}
