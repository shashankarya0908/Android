package com.fable.app.domain.model

enum class ReadingStatus { WANT_TO_READ, CURRENTLY_READING, READ }

data class ReadingProgress(
    val currentPage: Int,
    val totalPages: Int?,
    val percentage: Int?,
    val startedAt: Long?,
    val lastUpdatedAt: Long,
    val finishedAt: Long?,
    val personalNotes: String?,
)

data class UserBook(
    val userId: String,
    val book: Book,
    val status: ReadingStatus,
    val currentPage: Int = 0,
    val totalPages: Int? = book.pageCount,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val addedAt: Long,
    val updatedAt: Long,
    val personalNotes: String? = null,
    val completionHistory: List<Long> = emptyList(),
    val syncPending: Boolean = false,
) {
    val bookId: String get() = book.id
    val progress: ReadingProgress get() = ReadingProgress(
        currentPage = currentPage,
        totalPages = totalPages,
        percentage = totalPages?.takeIf { it > 0 }?.let { ((currentPage.toDouble() / it) * 100).toInt().coerceIn(0, 100) },
        startedAt = startedAt,
        lastUpdatedAt = updatedAt,
        finishedAt = finishedAt,
        personalNotes = personalNotes,
    )
}

data class MonthlyReadingActivity(
    val year: Int,
    val month: Int,
    val booksCompleted: Int,
    val pagesRead: Int,
)

data class ReadingStatistics(
    val totalBooksCompleted: Int,
    val totalPagesRead: Int,
    val currentlyReadingCount: Int,
    val wantToReadCount: Int,
    val activityByMonth: List<MonthlyReadingActivity>,
    val readingHistory: List<UserBook>,
    val averagePagesPerCompletedBook: Int?,
)

data class ReadingSyncState(val pendingChanges: Int = 0, val errorMessage: String? = null)

fun calculateReadingStatistics(books: List<UserBook>): ReadingStatistics {
    val completed = books.filter { it.completionHistory.isNotEmpty() }
    val monthly = books.flatMap { userBook ->
        userBook.completionHistory.mapNotNull { timestamp ->
            val date = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
            val pages = userBook.totalPages ?: userBook.currentPage
            Triple(date.get(java.util.Calendar.YEAR), date.get(java.util.Calendar.MONTH) + 1, pages.coerceAtLeast(0))
        }
    }.groupBy { it.first to it.second }.map { (yearMonth, events) ->
        MonthlyReadingActivity(yearMonth.first, yearMonth.second, events.size, events.sumOf { it.third })
    }.sortedWith(compareBy<MonthlyReadingActivity> { it.year }.thenBy { it.month })
    val pages = books.sumOf { item ->
        val pagesPerCompletion = (item.totalPages ?: item.currentPage).coerceAtLeast(0)
        val completedPages = pagesPerCompletion * item.completionHistory.size
        completedPages + if (item.status == ReadingStatus.CURRENTLY_READING) item.currentPage.coerceAtLeast(0) else 0
    }
    val completedPages = completed.sumOf { (it.totalPages ?: it.currentPage).coerceAtLeast(0) }
    return ReadingStatistics(
        totalBooksCompleted = completed.size,
        totalPagesRead = pages,
        currentlyReadingCount = books.count { it.status == ReadingStatus.CURRENTLY_READING },
        wantToReadCount = books.count { it.status == ReadingStatus.WANT_TO_READ },
        activityByMonth = monthly,
        readingHistory = books.filter { it.completionHistory.isNotEmpty() }
            .sortedByDescending { it.finishedAt ?: it.completionHistory.maxOrNull() ?: 0L },
        averagePagesPerCompletedBook = completed.takeIf { it.isNotEmpty() }?.let { completedPages / it.size },
    )
}
