package com.fable.app.presentation.books.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fable.app.di.FableDependencies
import com.fable.app.di.FableViewModelFactory
import com.fable.app.domain.model.Book
import com.fable.app.domain.repository.BookSearchType
import com.fable.app.presentation.books.BookSearchViewModel
import com.fable.app.presentation.books.components.BookGrid
import com.fable.app.presentation.books.components.BookSearchBar
import com.fable.app.presentation.books.components.EmptyState
import com.fable.app.presentation.books.components.ErrorState
import com.fable.app.presentation.books.components.LoadingIndicator

@Composable
fun BookSearchScreen(
    dependencies: FableDependencies,
    onBookClick: (Book) -> Unit,
) {
    val viewModel: BookSearchViewModel = viewModel(factory = FableViewModelFactory(dependencies))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("Search", style = MaterialTheme.typography.headlineMedium)
        BookSearchBar(
            query = state.query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = viewModel::submitSearch,
            onClear = viewModel::clearQuery,
            modifier = Modifier.padding(top = 8.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            items(BookSearchType.entries) { type ->
                FilterChip(
                    selected = state.type == type,
                    onClick = { viewModel.onTypeChange(type) },
                    label = { Text(type.label()) },
                )
            }
        }
        if (state.searchHistory.isNotEmpty() && !state.hasSearched) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Recent searches", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = viewModel::clearSearchHistory) { Text("Clear") }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.searchHistory, key = { "${it.query}-${it.searchedAtMillis}" }) { entry ->
                    FilterChip(
                        selected = false,
                        onClick = { viewModel.useHistory(entry.query) },
                        label = { Text(entry.query, maxLines = 1) },
                    )
                }
            }
        }
        when {
            state.isLoading -> LoadingIndicator("Searching Google Books…", Modifier.weight(1f))
            state.error != null && state.books.isEmpty() -> ErrorState(state.error.orEmpty(), viewModel::submitSearch, Modifier.weight(1f))
            state.error != null -> {
                Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                BookGrid(state.books, state.isLoadingMore, state.hasMore, viewModel::loadNextPage, onBookClick, Modifier.weight(1f))
            }
            state.hasSearched && state.books.isEmpty() -> EmptyState(
                "No books found", "Try a different title, author, ISBN, or subject.", Modifier.weight(1f),
            )
            state.books.isNotEmpty() -> BookGrid(
                state.books,
                state.isLoadingMore,
                state.hasMore,
                viewModel::loadNextPage,
                onBookClick,
                Modifier.weight(1f),
            )
            else -> EmptyState("Find a book", "Search by title, author, ISBN, or subject.", Modifier.weight(1f))
        }
    }
}

private fun BookSearchType.label(): String = when (this) {
    BookSearchType.GENERAL -> "All"
    BookSearchType.TITLE -> "Title"
    BookSearchType.AUTHOR -> "Author"
    BookSearchType.ISBN -> "ISBN"
    BookSearchType.SUBJECT -> "Subject"
}
