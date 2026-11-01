package com.fable.app.social

import com.fable.app.domain.model.FriendRequest
import com.fable.app.domain.model.PublicReaderProfile
import com.fable.app.domain.model.ReaderActivity
import com.fable.app.domain.repository.SocialRepository
import com.fable.app.presentation.social.SocialViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

class SocialViewModelTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun profileReadFailureIsShownWithoutInventingSocialData() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = SocialViewModel("reader-1", PublicReaderProfile("reader-1", "reader", "Reader"), FailingSocialRepository())
            advanceUntilIdle()
            viewModel.openProfile("reader-2")
            advanceUntilIdle()
            assertEquals("Profile read failed", viewModel.uiState.value.error)
            assertEquals(null, viewModel.uiState.value.selectedProfile)
            assertEquals(emptyList<PublicReaderProfile>(), viewModel.uiState.value.friends)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun friendRequestFailureIsShownWithoutReportingSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = SocialViewModel("reader-1", PublicReaderProfile("reader-1", "reader", "Reader"), FailingSocialRepository())
            viewModel.sendRequest(PublicReaderProfile("reader-2", "other", "Other"))
            advanceUntilIdle()
            assertEquals("Request send failed", viewModel.uiState.value.error)
            assertEquals(null, viewModel.uiState.value.message)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private class FailingSocialRepository : SocialRepository {
    override fun searchReaders(usernamePrefix: String): Flow<List<PublicReaderProfile>> = flowOf(emptyList())
    override fun observeReaderProfile(uid: String): Flow<PublicReaderProfile?> = flow { error("Profile read failed") }
    override fun observeIncomingRequests(userId: String): Flow<List<FriendRequest>> = flowOf(emptyList())
    override fun observeOutgoingRequests(userId: String): Flow<List<FriendRequest>> = flowOf(emptyList())
    override fun observeFriends(userId: String): Flow<List<PublicReaderProfile>> = flowOf(emptyList())
    override fun observeActivityFeed(userId: String): Flow<List<ReaderActivity>> = flowOf(emptyList())
    override suspend fun sendFriendRequest(sender: PublicReaderProfile, recipient: PublicReaderProfile) = error("Request send failed")
    override suspend fun acceptFriendRequest(userId: String, senderId: String) = Unit
    override suspend fun rejectFriendRequest(userId: String, senderId: String) = Unit
    override suspend fun cancelFriendRequest(userId: String, recipientId: String) = Unit
    override suspend fun removeFriend(userId: String, friendId: String) = Unit
}
