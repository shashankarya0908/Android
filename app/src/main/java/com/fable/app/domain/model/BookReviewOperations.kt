package com.fable.app.domain.model

fun createOrUpdateBookReview(
    previous: BookReview?,
    userId: String,
    username: String,
    book: Book,
    rating: Int,
    reviewText: String?,
    now: Long,
): BookReview {
    require(userId.isNotBlank()) { "A signed-in user is required to write a review." }
    require(username.isNotBlank()) { "A username is required to write a review." }
    require(rating in 1..5) { "Choose a rating from 1 to 5 stars." }
    require(previous == null || (previous.reviewerId == userId && previous.bookId == book.id)) {
        "You can only edit your own review for this book."
    }
    return BookReview(
        reviewerId = userId,
        reviewerUsername = username.trim(),
        bookId = book.id,
        bookTitle = book.title,
        bookThumbnailUrl = book.thumbnailUrl,
        rating = rating,
        reviewText = reviewText?.trim()?.takeIf(String::isNotEmpty),
        createdAt = previous?.createdAt ?: now,
        updatedAt = now,
        pendingSync = true,
    )
}

fun markBookReviewDeleted(review: BookReview, userId: String, now: Long): BookReview {
    require(review.reviewerId == userId) { "You can only delete your own review." }
    return review.copy(updatedAt = now, pendingSync = true, deleted = true)
}
