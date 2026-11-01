package com.fable.app.domain.usecase

import com.fable.app.domain.model.Book
import com.fable.app.domain.model.ReadingStatus
import com.fable.app.domain.repository.ReadingRepository

class AddOrMoveBook(private val repository: ReadingRepository) {
    suspend operator fun invoke(userId: String, book: Book, status: ReadingStatus) = repository.addOrMoveBook(userId, book, status)
}

class RemoveLibraryBook(private val repository: ReadingRepository) {
    suspend operator fun invoke(userId: String, bookId: String) = repository.removeBook(userId, bookId)
}

class UpdateReadingProgress(private val repository: ReadingRepository) {
    suspend operator fun invoke(userId: String, bookId: String, currentPage: Int, notes: String?) =
        repository.updateProgress(userId, bookId, currentPage, notes)
}
