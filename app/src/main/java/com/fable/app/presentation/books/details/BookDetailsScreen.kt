package com.fable.app.presentation.books.details

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fable.app.di.BookDetailsViewModelFactory
import com.fable.app.di.FableDependencies
import com.fable.app.presentation.books.BookDetailsViewModel
import com.fable.app.presentation.books.components.BookCover
import com.fable.app.presentation.books.components.EmptyState
import com.fable.app.presentation.books.components.ErrorState
import com.fable.app.presentation.books.components.LoadingIndicator
import com.fable.app.di.ReadingViewModelFactory
import com.fable.app.domain.model.ReadingStatus
import com.fable.app.presentation.reading.ReadingViewModel
import com.fable.app.presentation.reading.label
import com.fable.app.presentation.reviews.BookReviewsSection

@Composable
fun BookDetailsScreen(dependencies: FableDependencies, userId: String, username: String, bookId: String) {
    val factory = BookDetailsViewModelFactory(dependencies, bookId)
    val viewModel: BookDetailsViewModel = viewModel(key = "book-$bookId", factory = factory)
    val readingViewModel: ReadingViewModel = viewModel(
        key = "reading-details-$userId",
        factory = ReadingViewModelFactory(dependencies, userId),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val readingState by readingViewModel.uiState.collectAsStateWithLifecycle()
    when {
        state.isLoading -> LoadingIndicator("Loading book details…", Modifier.fillMaxSize())
        state.book == null -> ErrorState(state.error ?: "This book could not be found.", viewModel::load)
        else -> {
            val book = state.book!!
            val context = LocalContext.current
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BookCover(book.thumbnailUrl, book.title, Modifier.fillMaxWidth(0.56f).aspectRatio(0.68f))
                Text(book.title, style = MaterialTheme.typography.headlineMedium)
                book.subtitle?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                Text(book.authors.joinToString().ifBlank { "Author unavailable" }, style = MaterialTheme.typography.titleSmall)
                if (book.averageRating != null) {
                    Text(
                        "${"%.1f".format(book.averageRating)} / 5" +
                            (book.ratingsCount?.let { " · $it ratings" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                book.description?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 16, overflow = TextOverflow.Ellipsis)
                }
                MetadataLine("Publisher", book.publisher)
                MetadataLine("Published", book.publishedDate)
                MetadataLine("Pages", book.pageCount?.toString())
                MetadataLine("Language", book.language)
                MetadataLine("ISBN-10", book.isbn10)
                MetadataLine("ISBN-13", book.isbn13)
                if (book.categories.isNotEmpty()) {
                    Text("Categories", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        book.categories.take(3).forEach { FilterChip(selected = false, onClick = {}, label = { Text(it) }) }
                    }
                }
                book.previewLink?.let { url ->
                    Button(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }) { Text("Preview on Google Books") }
                }
                val libraryEntry = readingState.books.firstOrNull { it.bookId == book.id }
                Text("Your shelf", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReadingStatus.entries.forEach { status ->
                        FilterChip(
                            selected = libraryEntry?.status == status,
                            onClick = { readingViewModel.addOrMove(book, status) },
                            label = { Text(status.label()) },
                        )
                    }
                }
                if (libraryEntry != null) {
                    Text("On ${libraryEntry.status.label()} · Added ${java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date(libraryEntry.addedAt))}")
                    if (libraryEntry.status == ReadingStatus.CURRENTLY_READING) {
                        var pageText by rememberSaveable(book.id, libraryEntry.currentPage) { mutableStateOf(libraryEntry.currentPage.toString()) }
                        var notes by rememberSaveable(book.id, libraryEntry.personalNotes) { mutableStateOf(libraryEntry.personalNotes.orEmpty()) }
                        OutlinedTextField(
                            value = pageText,
                            onValueChange = { pageText = it.filter(Char::isDigit) },
                            label = { Text(libraryEntry.totalPages?.let { "Current page (of $it)" } ?: "Current page") },
                            singleLine = true,
                        )
                        libraryEntry.progress.percentage?.let { Text("Reading progress: $it%") }
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Personal notes (optional)") }, minLines = 2)
                        Button(onClick = {
                            pageText.toIntOrNull()?.let { readingViewModel.saveProgress(book.id, it, notes) }
                        }) { Text("Save progress") }
                    }
                    OutlinedButton(onClick = { readingViewModel.removeBook(book.id) }) { Text("Remove from library") }
                }
                readingState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (readingState.syncState.pendingChanges > 0) Text("Saved locally · waiting to sync")
                readingState.syncState.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                readingState.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                BookReviewsSection(dependencies, userId, username, book)
            }
        }
    }
}

@Composable
private fun MetadataLine(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$label:", style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
