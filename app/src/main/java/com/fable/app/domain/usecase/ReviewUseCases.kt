package com.fable.app.domain.usecase

import com.fable.app.domain.model.Book
import com.fable.app.domain.repository.ReviewRepository

class SaveBookReview(private val repository: ReviewRepository) {
    suspend operator fun invoke(userId: String, username: String, book: Book, rating: Int, text: String?) =
        repository.saveReview(userId, username, book, rating, text)
}

class DeleteBookReview(private val repository: ReviewRepository) {
    suspend operator fun invoke(userId: String, bookId: String) = repository.deleteReview(userId, bookId)
}
