package com.fable.app.domain.model

data class PublicReadingStats(
    val completedBooks: Int = 0,
    val pagesRead: Int = 0,
    val currentlyReading: Int = 0,
    val wantToRead: Int = 0,
)

data class PublicReaderProfile(
    val uid: String,
    val username: String,
    val displayName: String,
    val bio: String? = null,
    val profileImageUrl: String? = null,
    val favouriteGenres: List<String> = emptyList(),
    val readingStats: PublicReadingStats = PublicReadingStats(),
    val updatedAt: Long = 0L,
)

fun UserProfile.toPublicReaderProfile(stats: PublicReadingStats = PublicReadingStats()): PublicReaderProfile = PublicReaderProfile(
    uid = uid,
    username = username,
    displayName = displayName,
    bio = bio,
    profileImageUrl = profileImageUrl,
    favouriteGenres = favouriteGenres,
    readingStats = stats,
    updatedAt = updatedAt,
)

enum class FriendRequestStatus { PENDING, ACCEPTED, REJECTED, CANCELLED }
enum class FriendRequestAction { ACCEPT, REJECT, CANCEL }

data class FriendRequest(
    val senderId: String,
    val senderUsername: String,
    val senderDisplayName: String,
    val senderImageUrl: String?,
    val recipientId: String,
    val recipientUsername: String,
    val recipientDisplayName: String,
    val recipientImageUrl: String?,
    val status: FriendRequestStatus,
    val createdAt: Long,
    val updatedAt: Long,
)

data class ReaderActivity(
    val id: String,
    val readerId: String,
    val username: String,
    val displayName: String,
    val eventType: ReaderActivityType,
    val bookId: String,
    val bookTitle: String,
    val bookThumbnailUrl: String? = null,
    val currentPage: Int? = null,
    val progressPercent: Int? = null,
    val rating: Int? = null,
    val reviewText: String? = null,
    val timestamp: Long,
)

enum class ReaderActivityType {
    ADDED_TO_LIBRARY,
    STARTED_READING,
    PROGRESS_UPDATED,
    FINISHED_BOOK,
    REVIEW_PUBLISHED,
    REVIEW_UPDATED,
}

class SocialActionException(message: String) : IllegalStateException(message)

fun normalizeReaderUsername(username: String): String = username.trim().removePrefix("@").lowercase()

fun PublicReaderProfile.requireOwner(ownerUid: String): PublicReaderProfile {
    if (uid != ownerUid) throw SocialActionException("You can only edit your own profile.")
    val normalizedUsername = normalizeReaderUsername(username)
    require(displayName.isNotBlank()) { "Display name is required." }
    require(normalizedUsername.matches(Regex("^[a-z0-9_]{3,20}$"))) { "Username must be 3–20 letters, numbers, or underscores." }
    require(bio.orEmpty().length <= 280) { "Biography must be 280 characters or fewer." }
    require(profileImageUrl.isNullOrBlank() ||
        (profileImageUrl.startsWith("https://", ignoreCase = true) && profileImageUrl.length <= 2048)
    ) { "Profile images must use an HTTPS URL." }
    require(favouriteGenres.size <= 20 && favouriteGenres.all { it.length <= 50 }) { "Choose up to 20 valid favourite genres." }
    return copy(username = normalizedUsername, displayName = displayName.trim())
}

fun createFriendRequest(
    sender: PublicReaderProfile,
    recipient: PublicReaderProfile,
    previous: FriendRequest? = null,
    alreadyFriends: Boolean = false,
    now: Long,
): FriendRequest {
    if (sender.uid.isBlank() || recipient.uid.isBlank()) throw SocialActionException("A signed-in reader is required.")
    if (sender.uid == recipient.uid) throw SocialActionException("You cannot send a friend request to yourself.")
    if (alreadyFriends) throw SocialActionException("You are already friends.")
    when (previous?.status) {
        FriendRequestStatus.PENDING -> throw SocialActionException("A friend request is already pending.")
        FriendRequestStatus.ACCEPTED -> throw SocialActionException("You are already friends.")
        else -> Unit
    }
    return FriendRequest(
        senderId = sender.uid,
        senderUsername = sender.username,
        senderDisplayName = sender.displayName,
        senderImageUrl = sender.profileImageUrl,
        recipientId = recipient.uid,
        recipientUsername = recipient.username,
        recipientDisplayName = recipient.displayName,
        recipientImageUrl = recipient.profileImageUrl,
        status = FriendRequestStatus.PENDING,
        createdAt = now,
        updatedAt = now,
    )
}

fun transitionFriendRequest(
    request: FriendRequest,
    actorUid: String,
    action: FriendRequestAction,
): FriendRequest {
    if (request.senderId == request.recipientId) throw SocialActionException("Invalid friend request.")
    val allowed = when (action) {
        FriendRequestAction.ACCEPT, FriendRequestAction.REJECT -> actorUid == request.recipientId
        FriendRequestAction.CANCEL -> actorUid == request.senderId
    }
    if (!allowed) throw SocialActionException("You cannot change this friend request.")
    if (request.status == FriendRequestStatus.ACCEPTED && action == FriendRequestAction.ACCEPT) return request
    if (request.status != FriendRequestStatus.PENDING) {
        throw SocialActionException("This friend request is no longer pending.")
    }
    val status = when (action) {
        FriendRequestAction.ACCEPT -> FriendRequestStatus.ACCEPTED
        FriendRequestAction.REJECT -> FriendRequestStatus.REJECTED
        FriendRequestAction.CANCEL -> FriendRequestStatus.CANCELLED
    }
    return request.copy(status = status, updatedAt = System.currentTimeMillis())
}

fun friendshipPairKey(firstUid: String, secondUid: String): String {
    require(firstUid.isNotBlank() && secondUid.isNotBlank() && firstUid != secondUid)
    val pair = listOf(firstUid, secondUid).sorted()
    return pair.joinToString("__")
}

fun removeFriendFromList(ownerUid: String, friendUid: String, actorUid: String, friendIds: Set<String>): Set<String> {
    if (ownerUid == friendUid || actorUid !in setOf(ownerUid, friendUid)) {
        throw SocialActionException("You cannot remove this friendship.")
    }
    return friendIds - friendUid
}

fun generateLibraryActivities(profile: PublicReaderProfile, book: UserBook): List<ReaderActivity> = buildList {
    fun add(type: ReaderActivityType, suffix: String, at: Long, progress: Int? = null, page: Int? = null) {
        add(
            ReaderActivity(
                id = "$type:$suffix",
                readerId = profile.uid,
                username = profile.username,
                displayName = profile.displayName,
                eventType = type,
                bookId = book.book.id,
                bookTitle = book.book.title,
                bookThumbnailUrl = book.book.thumbnailUrl,
                currentPage = page,
                progressPercent = progress,
                timestamp = at,
            ),
        )
    }
    add(ReaderActivityType.ADDED_TO_LIBRARY, "${book.book.id}:added", book.addedAt)
    book.startedAt?.let { add(ReaderActivityType.STARTED_READING, "${book.book.id}:started:$it", it) }
    book.completionHistory.distinct().forEach { at ->
        add(ReaderActivityType.FINISHED_BOOK, "${book.book.id}:finished:$at", at, 100, book.totalPages)
    }
    if (book.status == ReadingStatus.CURRENTLY_READING) {
        val pages = book.currentPage.coerceAtLeast(0)
        val totalPages = book.totalPages?.takeIf { it > 0 }
        if (totalPages != null) {
            val percent = (pages.toDouble() / totalPages * 100).toInt().coerceIn(0, 100)
            val milestone = (percent / 10) * 10
            if (milestone in 10..90) {
                add(ReaderActivityType.PROGRESS_UPDATED, "${book.book.id}:progress:$milestone", book.updatedAt, milestone, pages)
            }
        } else {
            val milestonePages = (pages / 10) * 10
            if (milestonePages >= 10) {
                add(ReaderActivityType.PROGRESS_UPDATED, "${book.book.id}:progress-pages:$milestonePages", book.updatedAt, page = milestonePages)
            }
        }
    }
}

fun generateReviewActivity(profile: PublicReaderProfile, review: BookReview): ReaderActivity = ReaderActivity(
    id = "review:${review.bookId}",
    readerId = profile.uid,
    username = profile.username,
    displayName = profile.displayName,
    eventType = if (review.createdAt == review.updatedAt) ReaderActivityType.REVIEW_PUBLISHED
    else ReaderActivityType.REVIEW_UPDATED,
    bookId = review.bookId,
    bookTitle = review.bookTitle,
    bookThumbnailUrl = review.bookThumbnailUrl,
    rating = review.rating,
    reviewText = review.reviewText,
    timestamp = review.updatedAt,
)

fun filterFriendActivity(activities: List<ReaderActivity>, acceptedFriendIds: Set<String>): List<ReaderActivity> =
    activities.asSequence()
        .filter { it.readerId in acceptedFriendIds }
        .distinctBy(ReaderActivity::id)
        .sortedByDescending(ReaderActivity::timestamp)
        .toList()
