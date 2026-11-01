package com.fable.app.presentation.reviews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fable.app.di.FableDependencies
import com.fable.app.di.ReviewViewModelFactory
import com.fable.app.domain.model.BookReview
import com.fable.app.presentation.books.components.BookCover
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MyReviewsScreen(
    dependencies: FableDependencies,
    userId: String,
    username: String,
    onReviewClick: (String) -> Unit,
) {
    val viewModel: ReviewViewModel = viewModel(
        key = "my-reviews-$userId",
        factory = ReviewViewModelFactory(dependencies, userId, username),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("My Reviews", style = MaterialTheme.typography.headlineMedium)
            Text("Ratings and reviews you’ve written", style = MaterialTheme.typography.bodyMedium)
        }
        state.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
        if (state.isLoading && state.myReviews.isEmpty()) item { CircularProgressIndicator() }
        else if (state.myReviews.isEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 32.dp)) {
                    Text("You haven’t reviewed any books yet", style = MaterialTheme.typography.titleLarge)
                    Text("Rate a book from its details page and it will appear here.")
                }
            }
        } else {
            items(state.myReviews, key = { it.bookId }) { review -> ReviewHistoryCard(review) { onReviewClick(review.bookId) } }
        }
    }
}

@Composable
private fun ReviewHistoryCard(review: BookReview, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BookCover(review.bookThumbnailUrl, review.bookTitle, Modifier.width(70.dp).aspectRatio(0.68f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(review.bookTitle, style = MaterialTheme.typography.titleMedium)
                Text("★".repeat(review.rating), color = MaterialTheme.colorScheme.primary)
                review.reviewText?.let { Text(it, maxLines = 3, style = MaterialTheme.typography.bodySmall) }
                Text(DATE_FORMAT.format(Date(review.updatedAt)), style = MaterialTheme.typography.labelSmall)
                if (review.pendingSync) Text("Waiting to sync", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private val DATE_FORMAT = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
