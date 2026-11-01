package com.fable.app.presentation.social

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.fable.app.di.FableDependencies
import com.fable.app.di.SocialViewModelFactory
import com.fable.app.domain.model.FriendRequest
import com.fable.app.domain.model.PublicReaderProfile
import com.fable.app.domain.model.ReaderActivity
import com.fable.app.domain.model.ReaderActivityType
import com.fable.app.domain.model.UserProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiscoverReadersScreen(
    viewModel: SocialViewModel,
    onReaderClick: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Discover Readers", style = MaterialTheme.typography.headlineMedium)
            Text("Search public reader profiles by username.")
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text("Username") },
                placeholder = { Text("Start typing a username") },
                singleLine = true,
            )
        }
        state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (state.query.trim().length < 2) item { Text("Enter at least two characters to search.") }
        else if (state.searchResults.isEmpty() && state.error == null) item { Text("No readers found for this username.") }
        items(state.searchResults, key = PublicReaderProfile::uid) { profile ->
            ReaderCard(profile, onClick = { onReaderClick(profile.uid) })
        }
    }
}

@Composable
fun ReaderProfileScreen(
    viewModel: SocialViewModel,
    userId: String,
    onEditProfile: () -> Unit,
    onSelectTheme: () -> Unit,
    onOpenRequests: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.selectedProfile
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.isLoading && profile == null) item { CircularProgressIndicator() }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (!state.isLoading && profile == null && state.error == null) item { Text("This reader profile is unavailable.") }
        profile?.let { reader ->
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ReaderAvatar(reader.profileImageUrl, reader.displayName, Modifier.size(76.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(reader.displayName, style = MaterialTheme.typography.headlineSmall)
                        Text("@${reader.username}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            reader.bio?.takeIf(String::isNotBlank)?.let { bio -> item { Text(bio, style = MaterialTheme.typography.bodyLarge) } }
            if (reader.favouriteGenres.isNotEmpty()) item {
                Text("Favourite genres: ${reader.favouriteGenres.joinToString()}")
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Reading statistics", style = MaterialTheme.typography.titleMedium)
                        Text("${reader.readingStats.completedBooks} completed · ${reader.readingStats.pagesRead} pages read")
                        Text("${reader.readingStats.currentlyReading} currently reading · ${reader.readingStats.wantToRead} to read")
                    }
                }
            }
            if (reader.uid == userId) item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onEditProfile) { Text("Edit profile") }
                    OutlinedButton(onClick = onSelectTheme) { Text("Appearance") }
                }
            }
            else {
                val friend = state.friends.firstOrNull { it.uid == reader.uid }
                val sentRequest = state.outgoingRequests.firstOrNull { it.recipientId == reader.uid && it.status.name == "PENDING" }
                val receivedRequest = state.incomingRequests.firstOrNull { it.senderId == reader.uid && it.status.name == "PENDING" }
                item {
                    when {
                        friend != null -> OutlinedButton(onClick = { viewModel.removeFriend(friend) }) { Text("Remove friend") }
                        sentRequest != null -> OutlinedButton(onClick = { viewModel.cancel(sentRequest) }) { Text("Cancel friend request") }
                        receivedRequest != null -> OutlinedButton(onClick = onOpenRequests) { Text("Respond to friend request") }
                        else -> Button(onClick = { viewModel.sendRequest(reader) }) { Text("Send friend request") }
                    }
                }
            }
        }
        state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
    }
}

@Composable
fun FriendRequestsScreen(viewModel: SocialViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("Friend Requests", style = MaterialTheme.typography.headlineMedium) }
        item { Text("Incoming", style = MaterialTheme.typography.titleLarge) }
        if (state.incomingRequests.isEmpty()) item { Text("No incoming requests.") }
        items(state.incomingRequests.filter { it.status.name == "PENDING" }, key = { "in-${it.senderId}" }) { request ->
            RequestCard(request, incoming = true, onAccept = { viewModel.accept(request) }, onReject = { viewModel.reject(request) })
        }
        item { Text("Outgoing", style = MaterialTheme.typography.titleLarge) }
        if (state.outgoingRequests.none { it.status.name == "PENDING" }) item { Text("No outgoing requests.") }
        items(state.outgoingRequests.filter { it.status.name == "PENDING" }, key = { "out-${it.recipientId}" }) { request ->
            RequestCard(request, incoming = false, onCancel = { viewModel.cancel(request) })
        }
        state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
    }
}

@Composable
fun FriendsScreen(
    viewModel: SocialViewModel,
    onReaderClick: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("Friends", style = MaterialTheme.typography.headlineMedium) }
        if (state.friends.isEmpty()) item { Text("Your friends will appear here after a request is accepted.") }
        items(state.friends, key = PublicReaderProfile::uid) { friend ->
            ReaderCard(friend, onClick = { onReaderClick(friend.uid) }, action = {
                OutlinedButton(onClick = { viewModel.removeFriend(friend) }) { Text("Remove") }
            })
        }
        state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
    }
}

@Composable
fun ActivityFeedScreen(
    viewModel: SocialViewModel,
    onReaderClick: (String) -> Unit,
    onBookClick: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Activity Feed", style = MaterialTheme.typography.headlineMedium)
            Text("Recent reading activity from your accepted friends.")
        }
        state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        if (state.activityFeed.isEmpty() && state.error == null) item { Text("Your friends’ activity will appear here.") }
        items(state.activityFeed, key = ReaderActivity::id) { activity ->
            ActivityCard(activity, onReader = { onReaderClick(activity.readerId) }, onBook = { onBookClick(activity.bookId) })
        }
    }
}

@Composable
private fun ReaderCard(profile: PublicReaderProfile, onClick: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ReaderAvatar(profile.profileImageUrl, profile.displayName, Modifier.size(54.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(profile.displayName, style = MaterialTheme.typography.titleMedium)
                Text("@${profile.username}", style = MaterialTheme.typography.bodySmall)
                Text("${profile.readingStats.completedBooks} books completed · ${profile.readingStats.currentlyReading} reading")
            }
            action?.invoke()
        }
    }
}

@Composable
private fun RequestCard(
    request: FriendRequest,
    incoming: Boolean,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    val username = if (incoming) request.senderUsername else request.recipientUsername
    val name = if (incoming) request.senderDisplayName else request.recipientDisplayName
    val image = if (incoming) request.senderImageUrl else request.recipientImageUrl
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ReaderAvatar(image, name, Modifier.size(48.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Text("@$username")
            }
            if (incoming) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(onClick = onAccept) { Text("Accept") }
                    OutlinedButton(onClick = onReject) { Text("Reject") }
                }
            } else OutlinedButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

@Composable
private fun ActivityCard(activity: ReaderActivity, onReader: () -> Unit, onBook: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(activity.displayName + " · @" + activity.username, style = MaterialTheme.typography.titleSmall, modifier = Modifier.clickable(onClick = onReader))
            Text(activity.eventType.displayLabel())
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.clickable(onClick = onBook)) {
                if (!activity.bookThumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = activity.bookThumbnailUrl,
                        contentDescription = "Cover of ${activity.bookTitle}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(48.dp).aspectRatio(0.68f),
                    )
                }
                Column {
                    Text(activity.bookTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    activity.currentPage?.let { Text("Page $it") }
                    activity.progressPercent?.let { Text("Progress: $it%") }
                    activity.rating?.let { Text("Rating: ${"★".repeat(it.coerceIn(0, 5))}") }
                }
            }
            activity.reviewText?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(DATE_FORMAT.format(Date(activity.timestamp)), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ReaderAvatar(imageUrl: String?, description: String, modifier: Modifier = Modifier) {
    if (imageUrl.isNullOrBlank()) {
        Card(modifier.clip(CircleShape)) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(description.firstOrNull()?.uppercase() ?: "R", style = MaterialTheme.typography.headlineSmall)
            }
        }
    } else {
        AsyncImage(
            model = imageUrl,
            contentDescription = "$description profile image",
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape),
        )
    }
}

private fun ReaderActivityType.displayLabel(): String = when (this) {
    ReaderActivityType.ADDED_TO_LIBRARY -> "Added a book to their library"
    ReaderActivityType.STARTED_READING -> "Started reading"
    ReaderActivityType.PROGRESS_UPDATED -> "Updated reading progress"
    ReaderActivityType.FINISHED_BOOK -> "Finished a book"
    ReaderActivityType.REVIEW_PUBLISHED -> "Published a review"
    ReaderActivityType.REVIEW_UPDATED -> "Updated a review"
}

private val DATE_FORMAT = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
