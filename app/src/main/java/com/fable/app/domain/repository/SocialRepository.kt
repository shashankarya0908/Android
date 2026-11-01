package com.fable.app.domain.repository

import com.fable.app.domain.model.FriendRequest
import com.fable.app.domain.model.PublicReaderProfile
import com.fable.app.domain.model.ReaderActivity
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    fun searchReaders(usernamePrefix: String): Flow<List<PublicReaderProfile>>
    fun observeReaderProfile(uid: String): Flow<PublicReaderProfile?>
    fun observeIncomingRequests(userId: String): Flow<List<FriendRequest>>
    fun observeOutgoingRequests(userId: String): Flow<List<FriendRequest>>
    fun observeFriends(userId: String): Flow<List<PublicReaderProfile>>
    fun observeActivityFeed(userId: String): Flow<List<ReaderActivity>>
    suspend fun sendFriendRequest(sender: PublicReaderProfile, recipient: PublicReaderProfile)
    suspend fun acceptFriendRequest(userId: String, senderId: String)
    suspend fun rejectFriendRequest(userId: String, senderId: String)
    suspend fun cancelFriendRequest(userId: String, recipientId: String)
    suspend fun removeFriend(userId: String, friendId: String)
}
