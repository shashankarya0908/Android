package com.fable.app.domain.repository

import com.fable.app.domain.model.Book
import com.fable.app.domain.model.BookReview
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun observeBookReviews(bookId: String): Flow<List<BookReview>>
    fun observeMyReviews(userId: String): Flow<List<BookReview>>
    fun observePendingChanges(userId: String): Flow<Int>
    suspend fun saveReview(userId: String, username: String, book: Book, rating: Int, reviewText: String?): BookReview
    suspend fun deleteReview(userId: String, bookId: String)
    suspend fun synchronizePending(userId: String)
}
