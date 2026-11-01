package com.fable.app.presentation.reviews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fable.app.di.FableDependencies
import com.fable.app.di.ReviewViewModelFactory
import com.fable.app.domain.model.Book
import com.fable.app.domain.model.BookReview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BookReviewsSection(
    dependencies: FableDependencies,
    userId: String,
    username: String,
    book: Book,
) {
    val factory = ReviewViewModelFactory(dependencies, userId, username, book.id)
    val viewModel: ReviewViewModel = viewModel(key = "reviews-${book.id}-$userId", factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ownReview = state.reviews.firstOrNull { it.reviewerId == userId }
        ?: state.myReviews.firstOrNull { it.bookId == book.id }
    var ratingDraft by rememberSaveable(book.id, ownReview?.updatedAt) { mutableIntStateOf(ownReview?.rating ?: 0) }
    var reviewText by rememberSaveable(book.id, ownReview?.updatedAt) { mutableStateOf(ownReview?.reviewText.orEmpty()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Reviews", style = MaterialTheme.typography.headlineSmall)
        val average = state.summary.averageRating
        Text(
            if (average == null) "No ratings yet · Be the first to rate this book"
            else "${"%.1f".format(Locale.getDefault(), average)} out of 5 · ${state.summary.reviewCount} ${if (state.summary.reviewCount == 1) "review" else "reviews"}",
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(if (ownReview == null) "Your rating" else "Your rating and review", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..5).forEach { value ->
                Text(
                    text = if (value <= ratingDraft) "★" else "☆",
                    color = if (value <= ratingDraft) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .clickable { ratingDraft = value }
                        .semantics { contentDescription = "$value star${if (value == 1) "" else "s"}" },
                )
            }
        }
        OutlinedTextField(
            value = reviewText,
            onValueChange = { reviewText = it.take(MAX_REVIEW_LENGTH) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Write a review (optional)") },
            supportingText = { Text("${reviewText.length}/$MAX_REVIEW_LENGTH") },
            minLines = 3,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = ratingDraft in 1..5,
                onClick = { viewModel.save(book, ratingDraft, reviewText) },
            ) { Text(if (ownReview == null) "Submit rating" else "Update review") }
            if (ownReview != null) {
                OutlinedButton(onClick = { viewModel.delete(book.id) }) { Text("Delete") }
            }
        }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.pendingChanges > 0) {
            Text("${state.pendingChanges} review change(s) waiting to sync", color = MaterialTheme.colorScheme.tertiary)
        }
        if (state.isLoading && state.reviews.isEmpty()) CircularProgressIndicator()
        else if (state.summary.reviews.isEmpty()) {
            Text("No reviews yet. Your rating will be the first one.", style = MaterialTheme.typography.bodyMedium)
        } else {
            state.summary.reviews.forEach { review -> ReviewCard(review, isOwn = review.reviewerId == userId) }
        }
    }
}

@Composable
private fun ReviewCard(review: BookReview, isOwn: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (isOwn) "${review.reviewerUsername} · You" else review.reviewerUsername, style = MaterialTheme.typography.titleSmall)
                Text("★".repeat(review.rating), color = MaterialTheme.colorScheme.primary)
            }
            review.reviewText?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text(
                if (review.updatedAt > review.createdAt) {
                    "Reviewed ${DATE_FORMAT.format(Date(review.createdAt))} · Updated ${DATE_FORMAT.format(Date(review.updatedAt))}"
                } else {
                    "Reviewed ${DATE_FORMAT.format(Date(review.createdAt))}"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val DATE_FORMAT = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
private const val MAX_REVIEW_LENGTH = 4000
