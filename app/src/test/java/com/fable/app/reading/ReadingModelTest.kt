package com.fable.app.reading

import com.fable.app.data.remote.firestore.libraryDocumentId
import com.fable.app.domain.model.Book
import com.fable.app.domain.model.ReadingStatus
import com.fable.app.domain.model.calculateReadingStatistics
import com.fable.app.domain.model.moveUserBook
import com.fable.app.domain.model.withProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingModelTest {
    private val book = Book(
        id = "google-books:volume-1",
        source = "google-books",
        sourceId = "volume-1",
        title = "A Book",
        authors = listOf("A Reader"),
        pageCount = 240,
        categories = listOf("Fiction"),
    )

    @Test fun addAndMoveUsesOneStableUserBookRecord() {
        val first = moveUserBook(null, "uid-a", book, ReadingStatus.WANT_TO_READ, 10L)
        val moved = moveUserBook(first, "uid-a", book, ReadingStatus.CURRENTLY_READING, 20L)

        assertEquals(first.bookId, moved.bookId)
        assertEquals(first.addedAt, moved.addedAt)
        assertEquals(20L, moved.startedAt)
        assertEquals(ReadingStatus.CURRENTLY_READING, moved.status)
        assertTrue(moved.syncPending)
    }

    @Test fun repeatedMarkReadDoesNotAddDuplicateCompletionButRereadKeepsHistory() {
        val reading = moveUserBook(null, "uid-a", book, ReadingStatus.CURRENTLY_READING, 10L)
        val completed = moveUserBook(reading, "uid-a", book, ReadingStatus.READ, 20L)
        val markedReadAgain = moveUserBook(completed, "uid-a", book, ReadingStatus.READ, 30L)
        val rereading = moveUserBook(markedReadAgain, "uid-a", book, ReadingStatus.CURRENTLY_READING, 40L)
        val completedAgain = moveUserBook(rereading, "uid-a", book, ReadingStatus.READ, 50L)

        assertEquals(listOf(20L), markedReadAgain.completionHistory)
        assertNull(rereading.finishedAt)
        assertEquals(40L, rereading.startedAt)
        assertEquals(listOf(20L, 50L), completedAgain.completionHistory)
        assertEquals(1, calculateReadingStatistics(listOf(completedAgain)).totalBooksCompleted)
    }

    @Test fun progressPercentageIsReactiveAndMissingPageCountDoesNotDivideByZero() {
        val reading = moveUserBook(null, "uid-a", book, ReadingStatus.CURRENTLY_READING, 10L)
        val half = reading.withProgress(120, "halfway", 15L)
        val unknown = moveUserBook(null, "uid-a", book.copy(pageCount = null), ReadingStatus.CURRENTLY_READING, 10L)
            .withProgress(12, null, 15L)

        assertEquals(50, half.progress.percentage)
        assertEquals("halfway", half.personalNotes)
        assertNull(unknown.progress.percentage)
        assertEquals(12, unknown.currentPage)
    }

    @Test fun invalidProgressPagesAreRejected() {
        val reading = moveUserBook(null, "uid-a", book, ReadingStatus.CURRENTLY_READING, 10L)
        assertTrue(runCatching { reading.withProgress(-1, null, 11L) }.isFailure)
        assertTrue(runCatching { reading.withProgress(241, null, 11L) }.isFailure)
    }

    @Test fun completionStatisticsCountBooksOnceAndUseOnlyStoredRecords() {
        val one = moveUserBook(null, "uid-a", book, ReadingStatus.CURRENTLY_READING, 1_700_000_000_000L)
        val completed = moveUserBook(one, "uid-a", book, ReadingStatus.READ, 1_700_000_100_000L)
        val currentBook = book.copy(id = "google-books:volume-2", sourceId = "volume-2", pageCount = null, title = "Another")
        val current = moveUserBook(null, "uid-a", currentBook, ReadingStatus.CURRENTLY_READING, 1_700_000_200_000L)
            .withProgress(25, null, 1_700_000_300_000L)
        val want = moveUserBook(null, "uid-a", book.copy(id = "google-books:volume-3", sourceId = "volume-3"), ReadingStatus.WANT_TO_READ, 1_700_000_400_000L)
        val stats = calculateReadingStatistics(listOf(completed, current, want))

        assertEquals(1, stats.totalBooksCompleted)
        assertEquals(265, stats.totalPagesRead)
        assertEquals(1, stats.currentlyReadingCount)
        assertEquals(1, stats.wantToReadCount)
        assertEquals(240, stats.averagePagesPerCompletedBook)
        assertEquals(1, stats.activityByMonth.sumOf { it.booksCompleted })
        assertEquals(1, stats.readingHistory.size)
    }

    @Test fun deterministicCloudDocumentIdDoesNotUseBookTitle() {
        assertEquals(libraryDocumentId(book.id), libraryDocumentId(book.id))
        assertFalse(libraryDocumentId(book.id) == libraryDocumentId("google-books:other-volume"))
        assertEquals(64, libraryDocumentId(book.id).length)
    }
}
