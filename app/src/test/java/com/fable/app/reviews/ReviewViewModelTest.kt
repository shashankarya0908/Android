package com.fable.app.reviews

import com.fable.app.domain.model.Book
import com.fable.app.domain.model.BookReview
import com.fable.app.domain.repository.ReviewRepository
import com.fable.app.presentation.reviews.ReviewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun repositoryFailureIsShownInReviewUi() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = ReviewViewModel("uid-a", "reader_a", FailingReviewRepository())
            advanceUntilIdle()
            viewModel.save(Book("source:id", "source", "id", "Book"), 5, "Review")
            advanceUntilIdle()
            assertEquals("Review save failed", viewModel.uiState.value.error)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun reviewFeedFailureIsShownWithoutDiscardingCachedReviews() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val cached = BookReview(
                reviewerId = "uid-a",
                reviewerUsername = "reader_a",
                bookId = "google:book-1",
                bookTitle = "A cached book",
                bookThumbnailUrl = null,
                rating = 4,
                reviewText = "Cached review",
                createdAt = 1L,
                updatedAt = 1L,
            )
            val viewModel = ReviewViewModel(
                "uid-a", "reader_a", FailingReviewRepository(bookFailure = true, cachedReview = cached),
                bookId = cached.bookId,
            )
            advanceUntilIdle()
            assertEquals(listOf(cached), viewModel.uiState.value.reviews)
            assertEquals("Review feed unavailable", viewModel.uiState.value.error)
            assertEquals(false, viewModel.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private class FailingReviewRepository(
    private val bookFailure: Boolean = false,
    private val cachedReview: BookReview? = null,
) : ReviewRepository {
    override fun observeBookReviews(bookId: String): Flow<List<BookReview>> = if (bookFailure) flow {
        emit(listOfNotNull(cachedReview))
        error("Review feed unavailable")
    } else flowOf(emptyList())
    override fun observeMyReviews(userId: String): Flow<List<BookReview>> = flowOf(emptyList())
    override fun observePendingChanges(userId: String): Flow<Int> = flowOf(0)
    override suspend fun saveReview(userId: String, username: String, book: Book, rating: Int, reviewText: String?): BookReview =
        error("Review save failed")
    override suspend fun deleteReview(userId: String, bookId: String) = Unit
    override suspend fun synchronizePending(userId: String) = Unit
}
