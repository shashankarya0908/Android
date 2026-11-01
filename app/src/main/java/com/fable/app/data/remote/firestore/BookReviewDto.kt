package com.fable.app.data.remote.firestore

import com.fable.app.data.local.BookReviewEntity
import com.fable.app.domain.model.BookReview
import com.google.firebase.firestore.DocumentSnapshot
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

data class BookReviewDto(
    val reviewerId: String,
    val reviewerUsername: String,
    val bookId: String,
    val bookTitle: String,
    val bookThumbnailUrl: String?,
    val rating: Int,
    val reviewText: String,
    val createdAt: Long,
    val updatedAt: Long,
)

fun BookReview.toDto() = BookReviewDto(
    reviewerId, reviewerUsername, bookId, bookTitle, bookThumbnailUrl,
    rating, reviewText.orEmpty(), createdAt, updatedAt,
)

fun BookReviewDto.toFirestoreMap(): Map<String, Any?> = mapOf(
    "reviewerId" to reviewerId,
    "reviewerUsername" to reviewerUsername,
    "bookId" to bookId,
    "bookTitle" to bookTitle,
    "bookThumbnailUrl" to bookThumbnailUrl,
    "rating" to rating,
    "reviewText" to reviewText,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
)

fun DocumentSnapshot.toBookReviewDtoOrNull(): BookReviewDto? = runCatching {
    val data = data ?: return null
    BookReviewDto(
        reviewerId = data["reviewerId"] as? String ?: return null,
        reviewerUsername = data["reviewerUsername"] as? String ?: return null,
        bookId = data["bookId"] as? String ?: return null,
        bookTitle = data["bookTitle"] as? String ?: return null,
        bookThumbnailUrl = data["bookThumbnailUrl"] as? String,
        rating = (data["rating"] as? Number)?.toInt() ?: return null,
        reviewText = data["reviewText"] as? String ?: "",
        createdAt = (data["createdAt"] as? Number)?.toLong() ?: return null,
        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: return null,
    )
}.getOrNull()

fun BookReviewDto.toDomain(pendingSync: Boolean = false): BookReview? {
    if (reviewerId.isBlank() || reviewerUsername.isBlank() || bookId.isBlank() || rating !in 1..5) return null
    return BookReview(
        reviewerId = reviewerId,
        reviewerUsername = reviewerUsername,
        bookId = bookId,
        bookTitle = bookTitle,
        bookThumbnailUrl = bookThumbnailUrl,
        rating = rating,
        reviewText = reviewText.trim().takeIf(String::isNotEmpty),
        createdAt = createdAt,
        updatedAt = updatedAt,
        pendingSync = pendingSync,
    )
}

fun BookReviewDto.toEntity(pendingSync: Boolean = false): BookReviewEntity? = toDomain(pendingSync)?.let {
    BookReviewEntity(
        userId = it.reviewerId,
        bookId = it.bookId,
        reviewerUsername = it.reviewerUsername,
        bookTitle = it.bookTitle,
        bookThumbnailUrl = it.bookThumbnailUrl,
        rating = it.rating,
        reviewText = it.reviewText,
        createdAt = it.createdAt,
        updatedAt = it.updatedAt,
        pendingSync = pendingSync,
        deleted = false,
    )
}

fun reviewCollectionId(bookId: String): String = MessageDigest.getInstance("SHA-256")
    .digest(bookId.toByteArray(StandardCharsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
