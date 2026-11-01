package com.fable.app.domain.model

data class BookReview(
    val reviewerId: String,
    val reviewerUsername: String,
    val bookId: String,
    val bookTitle: String,
    val bookThumbnailUrl: String?,
    val rating: Int,
    val reviewText: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val pendingSync: Boolean = false,
    val deleted: Boolean = false,
)

data class BookReviewSummary(
    val reviews: List<BookReview>,
    val averageRating: Double?,
    val reviewCount: Int,
)

fun calculateBookReviewSummary(reviews: List<BookReview>): BookReviewSummary {
    val visible = reviews.asSequence()
        .filterNot(BookReview::deleted)
        .filter { it.rating in 1..5 }
        .sortedByDescending(BookReview::updatedAt)
        .distinctBy(BookReview::reviewerId)
        .toList()
    return BookReviewSummary(
        reviews = visible,
        averageRating = if (visible.isEmpty()) null else visible.map(BookReview::rating).average(),
        reviewCount = visible.size,
    )
}
