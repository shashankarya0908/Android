package com.fable.app.presentation.reading

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fable.app.di.FableDependencies
import com.fable.app.di.ReadingViewModelFactory
import com.fable.app.domain.model.ReadingStatus
import com.fable.app.domain.model.UserBook
import com.fable.app.presentation.books.components.BookCover

@Composable
fun MyLibraryScreen(
    dependencies: FableDependencies,
    userId: String,
    onBookClick: (String) -> Unit,
    onProgressClick: () -> Unit,
) {
    val viewModel: ReadingViewModel = viewModel(key = "reading-$userId", factory = ReadingViewModelFactory(dependencies, userId))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("My Library", style = MaterialTheme.typography.headlineMedium)
                    Text("Your shelves and reading progress", style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedButton(onClick = onProgressClick) { Text("Statistics") }
            }
        }
        item { LibraryStats(state) }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReadingStatus.entries.forEach { status ->
                    FilterChip(
                        selected = state.selectedShelf == status,
                        onClick = { viewModel.selectShelf(status) },
                        label = { Text("${status.label()} · ${state.books.count { it.status == status }}") },
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search your library") },
                singleLine = true,
            )
        }
        if (state.categories.isNotEmpty()) {
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.category == null, onClick = { viewModel.setCategory(null) }, label = { Text("All categories") })
                    state.categories.forEach { category ->
                        FilterChip(selected = state.category == category, onClick = { viewModel.setCategory(category) }, label = { Text(category) })
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sort:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                LibrarySort.entries.forEach { sort ->
                    FilterChip(selected = state.sort == sort, onClick = { viewModel.setSort(sort) }, label = { Text(sort.label()) })
                }
            }
        }
        state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.primary) } }
        state.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
        if (state.syncState.pendingChanges > 0) item {
            Text("${state.syncState.pendingChanges} saved change(s) waiting to sync", color = MaterialTheme.colorScheme.tertiary)
        }
        state.syncState.errorMessage?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error) } }
        if (state.isLoading) item { androidx.compose.material3.CircularProgressIndicator() }
        else if (state.visibleBooks.isEmpty()) item { LibraryEmptyState(state.selectedShelf) }
        items(state.visibleBooks, key = { it.bookId }) { entry ->
            LibraryBookCard(
                entry = entry,
                onOpen = { onBookClick(entry.bookId) },
                onMove = { status -> viewModel.addOrMove(entry.book, status) },
                onRemove = { viewModel.removeBook(entry.bookId) },
            )
        }
    }
}

@Composable
private fun LibraryStats(state: ReadingUiState) {
    val stats = state.statistics
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Reading at a glance", style = MaterialTheme.typography.titleMedium)
            Text("${stats.totalBooksCompleted} completed · ${stats.totalPagesRead} pages logged")
            Text("${stats.currentlyReadingCount} currently reading · ${stats.wantToReadCount} on your list")
        }
    }
}

@Composable
private fun LibraryBookCard(
    entry: UserBook,
    onOpen: () -> Unit,
    onMove: (ReadingStatus) -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember(entry.bookId) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BookCover(entry.book.thumbnailUrl, entry.book.title, Modifier.width(82.dp).aspectRatio(0.68f).clickable(onClick = onOpen))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.book.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.clickable(onClick = onOpen))
                Text(entry.book.authors.joinToString().ifBlank { "Author unavailable" }, style = MaterialTheme.typography.bodySmall)
                Text(entry.status.label(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                if (entry.status == ReadingStatus.CURRENTLY_READING) {
                    val pct = entry.progress.percentage
                    if (pct != null) LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth())
                    Text(if (entry.totalPages == null) "Page ${entry.currentPage}" else "Page ${entry.currentPage} of ${entry.totalPages}${pct?.let { " · $it%" }.orEmpty()}", style = MaterialTheme.typography.bodySmall)
                }
                if (entry.syncPending) Text("Waiting to sync", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { menuOpen = true }) { Text("Move") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        ReadingStatus.entries.filterNot { it == entry.status }.forEach { status ->
                            DropdownMenuItem(text = { Text("Move to ${status.label()}") }, onClick = { menuOpen = false; onMove(status) })
                        }
                    }
                    OutlinedButton(onClick = onRemove) { Text("Remove") }
                }
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(status: ReadingStatus) {
    Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("This shelf is empty", style = MaterialTheme.typography.titleLarge)
        Text("Add a book from Discover or Search and it will appear in ${status.label()}.", style = MaterialTheme.typography.bodyMedium)
    }
}

private fun LibrarySort.label() = when (this) {
    LibrarySort.ADDED_DATE -> "Date added"
    LibrarySort.TITLE -> "Title"
    LibrarySort.AUTHOR -> "Author"
}
