package com.fable.app.data.local

import androidx.room.Entity
import androidx.room.Index
import com.fable.app.domain.model.BookReview

@Entity(
    tableName = "book_reviews",
    primaryKeys = ["userId", "bookId"],
    indices = [Index("bookId"), Index("userId")],
)
data class BookReviewEntity(
    val userId: String,
    val bookId: String,
    val reviewerUsername: String,
    val bookTitle: String,
    val bookThumbnailUrl: String?,
    val rating: Int,
    val reviewText: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val pendingSync: Boolean,
    val deleted: Boolean,
)

fun BookReview.toEntity(deleted: Boolean = this.deleted): BookReviewEntity = BookReviewEntity(
    userId = reviewerId,
    bookId = bookId,
    reviewerUsername = reviewerUsername,
    bookTitle = bookTitle,
    bookThumbnailUrl = bookThumbnailUrl,
    rating = rating,
    reviewText = reviewText,
    createdAt = createdAt,
    updatedAt = updatedAt,
    pendingSync = pendingSync,
    deleted = deleted,
)

fun BookReviewEntity.toDomain(): BookReview = BookReview(
    reviewerId = userId,
    reviewerUsername = reviewerUsername,
    bookId = bookId,
    bookTitle = bookTitle,
    bookThumbnailUrl = bookThumbnailUrl,
    rating = rating,
    reviewText = reviewText,
    createdAt = createdAt,
    updatedAt = updatedAt,
    pendingSync = pendingSync,
    deleted = deleted,
)
