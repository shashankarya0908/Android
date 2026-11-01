package com.fable.app.presentation.reading

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fable.app.di.FableDependencies
import com.fable.app.di.ReadingViewModelFactory
import com.fable.app.domain.model.ReadingStatus
import com.fable.app.presentation.books.components.BookCover

@Composable
fun ReadingHomeScreen(
    dependencies: FableDependencies,
    userId: String,
    displayName: String,
    onDiscover: () -> Unit,
    onSearch: () -> Unit,
    onLibrary: () -> Unit,
    onStatistics: () -> Unit,
    onMyReviews: () -> Unit,
    onBookClick: (String) -> Unit,
) {
    val viewModel: ReadingViewModel = viewModel(key = "home-reading-$userId", factory = ReadingViewModelFactory(dependencies, userId))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentReads = state.books.filter { it.status == ReadingStatus.CURRENTLY_READING }.take(5)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Welcome back, $displayName", style = MaterialTheme.typography.headlineMedium)
            Text("Find a new story or continue reading.", style = MaterialTheme.typography.bodyLarge)
        }
        item {
            Card(Modifier.fillMaxWidth().clickable(onClick = onStatistics)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Your reading", style = MaterialTheme.typography.titleMedium)
                    Text("${state.statistics.currentlyReadingCount} reading · ${state.statistics.wantToReadCount} to read · ${state.statistics.totalBooksCompleted} completed")
                    Text("${state.statistics.totalPagesRead} pages recorded · View statistics", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onDiscover, modifier = Modifier.weight(1f)) { Text("Discover") }
                OutlinedButton(onClick = onSearch, modifier = Modifier.weight(1f)) { Text("Search books") }
            }
        }
        item { OutlinedButton(onClick = onMyReviews) { Text("My Reviews") } }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Currently reading", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = onLibrary) { Text("My Library") }
            }
        }
        if (state.isLoading) item { androidx.compose.material3.CircularProgressIndicator() }
        else if (currentReads.isEmpty()) item {
            Text("Your Currently Reading shelf is empty. Add a book from Discover to start tracking progress.")
        }
        items(currentReads, key = { it.bookId }) { entry ->
            Card(Modifier.fillMaxWidth().clickable { onBookClick(entry.bookId) }) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BookCover(entry.book.thumbnailUrl, entry.book.title, Modifier.width(64.dp).aspectRatio(0.68f))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(entry.book.title, style = MaterialTheme.typography.titleMedium)
                        Text(entry.book.authors.joinToString().ifBlank { "Author unavailable" }, style = MaterialTheme.typography.bodySmall)
                        Text(if (entry.totalPages == null) "Page ${entry.currentPage}" else "Page ${entry.currentPage} of ${entry.totalPages}")
                        entry.progress.percentage?.let {
                            androidx.compose.material3.LinearProgressIndicator(progress = { it / 100f }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
        state.syncState.errorMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
    }
}
