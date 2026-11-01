package com.fable.app.reviews

import com.fable.app.domain.model.Book
import com.fable.app.domain.model.BookReview
import com.fable.app.domain.model.calculateBookReviewSummary
import com.fable.app.domain.model.createOrUpdateBookReview
import com.fable.app.domain.model.markBookReviewDeleted
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookReviewTest {
    private val book = Book("google-books:volume-1", "google-books", "volume-1", "A Book", thumbnailUrl = "https://covers.test/book.jpg")

    @Test fun createsReviewWithStableBookAndReviewerIdentity() {
        val review = createOrUpdateBookReview(null, "uid-a", "reader_a", book, 5, "  A lovely read  ", 100L)

        assertEquals("uid-a", review.reviewerId)
        assertEquals(book.id, review.bookId)
        assertEquals(5, review.rating)
        assertEquals("A lovely read", review.reviewText)
        assertEquals(100L, review.createdAt)
    }

    @Test fun editingReviewPreservesCreationDateAndChangesUpdateDate() {
        val original = createOrUpdateBookReview(null, "uid-a", "reader_a", book, 3, "Good", 100L)
        val edited = createOrUpdateBookReview(original, "uid-a", "reader_a", book, 4, "Better", 200L)

        assertEquals(100L, edited.createdAt)
        assertEquals(200L, edited.updatedAt)
        assertEquals(4, edited.rating)
        assertEquals("Better", edited.reviewText)
    }

    @Test fun deletingReviewMarksOnlyItsRecordAsDeleted() {
        val original = createOrUpdateBookReview(null, "uid-a", "reader_a", book, 4, null, 100L)
        val deleted = markBookReviewDeleted(original, "uid-a", 200L)

        assertTrue(deleted.deleted)
        assertTrue(deleted.pendingSync)
        assertEquals(100L, deleted.createdAt)
        assertEquals(200L, deleted.updatedAt)
    }

    @Test fun repeatedWritesForSameUserAndBookUseTheSameUniqueKey() {
        val original = createOrUpdateBookReview(null, "uid-a", "reader_a", book, 3, null, 100L)
        val updated = createOrUpdateBookReview(original, "uid-a", "reader_a", book, 5, "Changed my mind", 200L)
        val keyedRows = listOf(original, updated).associateBy { it.reviewerId to it.bookId }

        assertEquals(1, keyedRows.size)
        assertEquals(5, keyedRows.values.single().rating)
    }

    @Test fun ratingMustBeBetweenOneAndFiveStars() {
        assertTrue(runCatching { createOrUpdateBookReview(null, "uid-a", "reader_a", book, 0, null, 1L) }.isFailure)
        assertTrue(runCatching { createOrUpdateBookReview(null, "uid-a", "reader_a", book, 6, null, 1L) }.isFailure)
    }

    @Test fun onlyOwnerCanEditOrDeleteReview() {
        val original = createOrUpdateBookReview(null, "uid-a", "reader_a", book, 4, "Review", 100L)

        assertTrue(runCatching { createOrUpdateBookReview(original, "uid-b", "reader_b", book, 5, "Changed", 200L) }.isFailure)
        assertTrue(runCatching { markBookReviewDeleted(original, "uid-b", 200L) }.isFailure)
    }

    @Test fun averageRatingUsesStoredValidRatings() {
        val summary = calculateBookReviewSummary(listOf(review("uid-a", 4), review("uid-b", 5), review("uid-c", 3)))

        assertEquals(4.0, summary.averageRating!!, 0.001)
    }

    @Test fun reviewCountCountsOneRecordPerReviewer() {
        val duplicate = review("uid-a", 4, updatedAt = 200L)
        val summary = calculateBookReviewSummary(listOf(review("uid-a", 2), duplicate, review("uid-b", 5)))

        assertEquals(2, summary.reviewCount)
        assertEquals(4.5, summary.averageRating!!, 0.001)
    }

    @Test fun emptyReviewStateHasNoAverageAndZeroCount() {
        val summary = calculateBookReviewSummary(emptyList())

        assertTrue(summary.reviews.isEmpty())
        assertNull(summary.averageRating)
        assertEquals(0, summary.reviewCount)
        assertFalse(review("uid-a", 5).deleted)
    }

    private fun review(reviewerId: String, rating: Int, updatedAt: Long = 100L) = BookReview(
        reviewerId = reviewerId,
        reviewerUsername = reviewerId,
        bookId = book.id,
        bookTitle = book.title,
        bookThumbnailUrl = book.thumbnailUrl,
        rating = rating,
        reviewText = null,
        createdAt = 100L,
        updatedAt = updatedAt,
    )
}
