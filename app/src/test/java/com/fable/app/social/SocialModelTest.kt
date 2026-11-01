package com.fable.app.social

import com.fable.app.domain.model.Book
import com.fable.app.domain.model.BookReview
import com.fable.app.domain.model.FriendRequest
import com.fable.app.domain.model.FriendRequestAction
import com.fable.app.domain.model.FriendRequestStatus
import com.fable.app.domain.model.PublicReaderProfile
import com.fable.app.domain.model.ReaderActivityType
import com.fable.app.domain.model.ReadingStatus
import com.fable.app.domain.model.SocialActionException
import com.fable.app.domain.model.UserBook
import com.fable.app.domain.model.UserProfile
import com.fable.app.domain.model.createFriendRequest
import com.fable.app.domain.model.filterFriendActivity
import com.fable.app.domain.model.friendshipPairKey
import com.fable.app.domain.model.generateLibraryActivities
import com.fable.app.domain.model.generateReviewActivity
import com.fable.app.domain.model.removeFriendFromList
import com.fable.app.domain.model.requireOwner
import com.fable.app.domain.model.toPublicReaderProfile
import com.fable.app.domain.model.transitionFriendRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialModelTest {
    @Test fun createsRequestUsingStableAuthenticatedIdentities() {
        val request = createFriendRequest(reader("alice-id", "alice"), reader("bob-id", "bob"), now = 10L)
        assertEquals("alice-id", request.senderId)
        assertEquals("bob-id", request.recipientId)
        assertEquals(FriendRequestStatus.PENDING, request.status)
        assertEquals(10L, request.createdAt)
    }

    @Test fun preventsDuplicatePendingAndAcceptedRequests() {
        val sender = reader("a", "alice")
        val recipient = reader("b", "bob")
        val pending = createFriendRequest(sender, recipient, now = 1L)
        assertThrows<SocialActionException> { createFriendRequest(sender, recipient, pending, now = 2L) }
        val accepted = pending.copy(status = FriendRequestStatus.ACCEPTED)
        assertThrows<SocialActionException> { createFriendRequest(sender, recipient, accepted, now = 3L) }
        val resent = createFriendRequest(sender, recipient, pending.copy(status = FriendRequestStatus.REJECTED), now = 4L)
        assertEquals(FriendRequestStatus.PENDING, resent.status)
        val resentAfterCancellation = createFriendRequest(sender, recipient, pending.copy(status = FriendRequestStatus.CANCELLED), now = 5L)
        assertEquals(FriendRequestStatus.PENDING, resentAfterCancellation.status)
    }

    @Test fun preventsSelfRequestsAndFriendshipDuplicates() {
        val sameReader = reader("same", "reader")
        assertThrows<SocialActionException> { createFriendRequest(sameReader, sameReader, now = 1L) }
        assertThrows<SocialActionException> {
            createFriendRequest(reader("a", "alice"), reader("b", "bob"), alreadyFriends = true, now = 1L)
        }
        assertEquals(friendshipPairKey("a", "b"), friendshipPairKey("b", "a"))
    }

    @Test fun acceptsRejectsAndCancelsOnlyByTheCorrectParticipant() {
        val pending = request()
        assertEquals(FriendRequestStatus.ACCEPTED,
            transitionFriendRequest(pending, "bob-id", FriendRequestAction.ACCEPT).status)
        assertEquals(FriendRequestStatus.REJECTED,
            transitionFriendRequest(pending, "bob-id", FriendRequestAction.REJECT).status)
        assertEquals(FriendRequestStatus.CANCELLED,
            transitionFriendRequest(pending, "alice-id", FriendRequestAction.CANCEL).status)
        assertThrows<SocialActionException> { transitionFriendRequest(pending, "alice-id", FriendRequestAction.ACCEPT) }
        assertThrows<SocialActionException> {
            transitionFriendRequest(pending.copy(status = FriendRequestStatus.REJECTED), "alice-id", FriendRequestAction.CANCEL)
        }
    }

    @Test fun removingFriendRemovesTheLocalRelationshipAndChecksOwnership() {
        val remaining = removeFriendFromList("alice-id", "bob-id", "alice-id", setOf("bob-id", "carol-id"))
        assertEquals(setOf("carol-id"), remaining)
        assertThrows<SocialActionException> {
            removeFriendFromList("alice-id", "bob-id", "mallory", setOf("bob-id"))
        }
    }

    @Test fun validatesProfileOwnerAndNormalizesUsername() {
        val owner = reader("alice-id", "  @Alice_1  ")
        assertEquals("alice_1", owner.requireOwner("alice-id").username)
        assertThrows<SocialActionException> { owner.requireOwner("someone-else") }
        assertThrows<IllegalArgumentException> { owner.copy(profileImageUrl = "http://example.com/pic").requireOwner("alice-id") }
    }

    @Test fun publicReaderProfileContainsNoPrivateEmailField() {
        val public = UserProfile("uid", "reader", "Reader", "private@example.com", bio = "Hi").toPublicReaderProfile()
        assertEquals("reader", public.username)
        assertFalse(PublicReaderProfile::class.java.declaredFields.any { it.name.equals("email", true) })
    }

    @Test fun libraryActivityGenerationIncludesSupportedEventsWithoutPrivateNotes() {
        val item = book(status = ReadingStatus.CURRENTLY_READING, currentPage = 35, totalPages = 100)
        val events = generateLibraryActivities(reader("alice-id", "alice"), item)
        assertTrue(events.any { it.eventType == ReaderActivityType.ADDED_TO_LIBRARY })
        assertTrue(events.any { it.eventType == ReaderActivityType.STARTED_READING })
        assertTrue(events.any { it.eventType == ReaderActivityType.PROGRESS_UPDATED && it.progressPercent == 30 })
        assertFalse(events.any { it.toString().contains("private note") })
    }

    @Test fun minorProgressChangesReuseMilestoneEventInsteadOfAddingDuplicates() {
        val profile = reader("alice-id", "alice")
        val first = generateLibraryActivities(profile, book(currentPage = 31)).filter { it.eventType == ReaderActivityType.PROGRESS_UPDATED }
        val next = generateLibraryActivities(profile, book(currentPage = 39)).filter { it.eventType == ReaderActivityType.PROGRESS_UPDATED }
        assertEquals(first.single().id, next.single().id)
    }

    @Test fun finishedAndReviewActivitiesUseStableEventTypesAndTimestamps() {
        val finished = generateLibraryActivities(reader("alice-id", "alice"), book(status = ReadingStatus.READ, completions = listOf(50L)))
        assertTrue(finished.any { it.eventType == ReaderActivityType.FINISHED_BOOK && it.timestamp == 50L })
        val feed = filterFriendActivity(finished, setOf("alice-id"))
        assertTrue(feed.isNotEmpty())
        val profile = reader("alice-id", "alice")
        val published = generateReviewActivity(profile, review(1L, 1L))
        val updated = generateReviewActivity(profile, review(1L, 2L))
        assertEquals(ReaderActivityType.REVIEW_PUBLISHED, published.eventType)
        assertEquals(ReaderActivityType.REVIEW_UPDATED, updated.eventType)
    }

    @Test fun feedIncludesOnlyAcceptedFriendAuthorsAndSortsNewestFirst() {
        val activities = listOf(
            activity("a-old", "friend-a", 1L),
            activity("stranger", "not-a-friend", 100L),
            activity("b-new", "friend-b", 3L),
            activity("a-old", "friend-a", 2L),
        )
        val visible = filterFriendActivity(activities, setOf("friend-a", "friend-b"))
        assertEquals(listOf("b-new", "a-old"), visible.map { it.id })
    }

    private fun reader(uid: String, username: String) = PublicReaderProfile(uid, username, username.replaceFirstChar(Char::uppercase))

    private fun request() = FriendRequest(
        senderId = "alice-id", senderUsername = "alice", senderDisplayName = "Alice", senderImageUrl = null,
        recipientId = "bob-id", recipientUsername = "bob", recipientDisplayName = "Bob", recipientImageUrl = null,
        status = FriendRequestStatus.PENDING, createdAt = 1L, updatedAt = 1L,
    )

    private fun book(
        status: ReadingStatus = ReadingStatus.CURRENTLY_READING,
        currentPage: Int = 0,
        totalPages: Int = 100,
        completions: List<Long> = emptyList(),
    ) = UserBook(
        userId = "alice-id", book = Book("google-books:book1", "google-books", "book1", "A Book"),
        status = status, currentPage = currentPage, totalPages = totalPages,
        startedAt = 20L, finishedAt = completions.lastOrNull(), addedAt = 10L, updatedAt = 40L,
        personalNotes = "private note", completionHistory = completions,
    )

    private fun activity(id: String, uid: String, timestamp: Long) =
        com.fable.app.domain.model.ReaderActivity(id, uid, uid, uid, ReaderActivityType.ADDED_TO_LIBRARY, "book", "Book", timestamp = timestamp)

    private fun review(createdAt: Long, updatedAt: Long) = BookReview(
        reviewerId = "alice-id", reviewerUsername = "alice", bookId = "google-books:book1", bookTitle = "A Book",
        bookThumbnailUrl = null, rating = 4, reviewText = "Enjoyed it", createdAt = createdAt, updatedAt = updatedAt,
    )

    private inline fun <reified T : Throwable> assertThrows(block: () -> Unit) {
        val error = runCatching(block).exceptionOrNull()
        assertTrue("Expected ${T::class.java.simpleName} but was $error", error is T)
    }
}
