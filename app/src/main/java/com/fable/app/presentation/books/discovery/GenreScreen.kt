package com.fable.app.presentation.books.discovery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fable.app.di.CategoryBooksViewModelFactory
import com.fable.app.di.FableDependencies
import com.fable.app.domain.model.Book
import com.fable.app.presentation.books.CategoryBooksViewModel
import com.fable.app.presentation.books.components.BookGrid
import com.fable.app.presentation.books.components.EmptyState
import com.fable.app.presentation.books.components.ErrorState
import com.fable.app.presentation.books.components.LoadingIndicator

@Composable
fun GenreScreen(dependencies: FableDependencies, category: String, onBookClick: (Book) -> Unit) {
    val factory = CategoryBooksViewModelFactory(dependencies, category)
    val viewModel: CategoryBooksViewModel = viewModel(key = "genre-$category", factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(top = 16.dp)) {
        Text(category, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 20.dp))
        when {
            state.isLoading -> LoadingIndicator("Loading $category books…", Modifier.weight(1f))
            state.error != null -> ErrorState(state.error.orEmpty(), viewModel::load, Modifier.weight(1f))
            state.books.isEmpty() -> EmptyState("No books found", "Try another genre.", Modifier.weight(1f))
            else -> BookGrid(state.books, false, false, {}, onBookClick, Modifier.weight(1f))
        }
    }
}
