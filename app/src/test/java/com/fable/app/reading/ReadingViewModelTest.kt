package com.fable.app.reading

import com.fable.app.domain.model.Book
import com.fable.app.domain.model.ReadingStatistics
import com.fable.app.domain.model.ReadingStatus
import com.fable.app.domain.model.ReadingSyncState
import com.fable.app.domain.model.UserBook
import com.fable.app.domain.model.calculateReadingStatistics
import com.fable.app.domain.repository.ReadingRepository
import com.fable.app.presentation.reading.ReadingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun repositoryWriteFailureIsSurfacedToTheLibraryUi() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FailingReadingRepository()
            val viewModel = ReadingViewModel("user-1", repository)
            advanceUntilIdle()

            viewModel.addOrMove(
                Book("source:book-1", "source", "book-1", "A Book"),
                ReadingStatus.WANT_TO_READ,
            )
            advanceUntilIdle()

            assertEquals("Cloud write failed", viewModel.uiState.value.error)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private class FailingReadingRepository : ReadingRepository {
    override fun observeLibrary(userId: String): Flow<List<UserBook>> = flowOf(emptyList())
    override fun observeStatistics(userId: String): Flow<ReadingStatistics> = flowOf(calculateReadingStatistics(emptyList()))
    override fun observeSyncState(userId: String): Flow<ReadingSyncState> = flowOf(ReadingSyncState())
    override suspend fun addOrMoveBook(userId: String, book: Book, status: ReadingStatus): UserBook = error("Cloud write failed")
    override suspend fun removeBook(userId: String, bookId: String) = Unit
    override suspend fun updateProgress(userId: String, bookId: String, currentPage: Int, personalNotes: String?): UserBook =
        error("Cloud write failed")
    override suspend fun synchronizePending(userId: String) = Unit
}
