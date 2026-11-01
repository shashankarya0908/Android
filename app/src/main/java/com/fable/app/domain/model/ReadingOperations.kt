package com.fable.app.domain.model

fun moveUserBook(
    previous: UserBook?,
    userId: String,
    book: Book,
    status: ReadingStatus,
    now: Long,
): UserBook {
    require(userId.isNotBlank()) { "A signed-in user is required to update the library." }
    require(previous == null || (previous.userId == userId && previous.bookId == book.id)) {
        "A library entry must belong to the same user and book."
    }
    val newlyCompleted = status == ReadingStatus.READ && previous?.status != ReadingStatus.READ
    val returningToReading = status == ReadingStatus.CURRENTLY_READING && previous?.status == ReadingStatus.READ
    val totalPages = previous?.totalPages ?: book.pageCount
    return UserBook(
        userId = userId,
        book = book,
        status = status,
        currentPage = when {
            status == ReadingStatus.READ && totalPages != null -> totalPages
            returningToReading -> 0
            else -> previous?.currentPage ?: 0
        },
        totalPages = totalPages,
        startedAt = when {
            status != ReadingStatus.CURRENTLY_READING -> previous?.startedAt
            returningToReading -> now
            else -> previous?.startedAt ?: now
        },
        finishedAt = if (status == ReadingStatus.READ) {
            if (newlyCompleted) now else previous?.finishedAt ?: now
        } else null,
        addedAt = previous?.addedAt ?: now,
        updatedAt = now,
        personalNotes = previous?.personalNotes,
        completionHistory = previous?.completionHistory.orEmpty() + listOfNotNull(now.takeIf { newlyCompleted }),
        syncPending = true,
    )
}

fun UserBook.withProgress(currentPage: Int, personalNotes: String?, now: Long): UserBook {
    require(currentPage >= 0) { "Current page cannot be negative." }
    require(status == ReadingStatus.CURRENTLY_READING) { "Progress can only be updated for a currently reading book." }
    totalPages?.let { require(currentPage <= it) { "Current page cannot exceed the book's total pages." } }
    return copy(
        currentPage = currentPage,
        updatedAt = now,
        personalNotes = personalNotes?.trim()?.takeIf(String::isNotEmpty),
        syncPending = true,
    )
}
