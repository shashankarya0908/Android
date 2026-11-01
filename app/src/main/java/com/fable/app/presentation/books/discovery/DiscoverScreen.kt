package com.fable.app.presentation.books.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fable.app.di.FableDependencies
import com.fable.app.di.FableViewModelFactory
import com.fable.app.domain.model.Book
import com.fable.app.presentation.books.DiscoverViewModel
import com.fable.app.presentation.books.components.BookCard
import com.fable.app.presentation.books.components.BookCarousel
import com.fable.app.presentation.books.components.BookCover
import com.fable.app.presentation.books.components.EmptyState
import com.fable.app.presentation.books.components.ErrorState
import com.fable.app.presentation.books.components.GenreChip
import com.fable.app.presentation.books.components.LoadingIndicator

val DISCOVERY_GENRES = listOf("Fantasy", "Mystery", "Romance", "Science Fiction", "History", "Biography")

@Composable
fun DiscoverScreen(
    dependencies: FableDependencies,
    onBookClick: (Book) -> Unit,
    onGenreClick: (String) -> Unit,
    onSearchClick: () -> Unit,
) {
    val viewModel: DiscoverViewModel = viewModel(factory = FableViewModelFactory(dependencies))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Discover", style = MaterialTheme.typography.headlineMedium)
                Text("Find your next read in Google Books.", style = MaterialTheme.typography.bodyLarge)
                androidx.compose.material3.OutlinedButton(onClick = onSearchClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Search books, authors, or ISBN")
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Explore genres")
                androidx.compose.foundation.lazy.LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) { items(DISCOVERY_GENRES) { genre -> GenreChip(genre) { onGenreClick(genre) } } }
            }
        }
        when {
            state.isLoading -> item { LoadingIndicator("Finding books…") }
            state.error != null -> item { ErrorState(state.error.orEmpty(), viewModel::load) }
            state.featuredBook == null -> item { EmptyState("No discoveries yet", "Try again in a moment.") }
            else -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle("Featured from Fiction")
                        FeaturedBookCard(state.featuredBook!!, onClick = { onBookClick(state.featuredBook!!) })
                    }
                }
                if (state.popularDiscoveries.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle("Popular discoveries")
                            Text(
                                "Ordered by Google Books rating count where available.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            BookCarousel(state.popularDiscoveries, onBookClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun FeaturedBookCard(book: Book, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(12.dp)) {
            BookCover(book.thumbnailUrl, book.title, Modifier.fillMaxWidth(0.32f).aspectRatio(0.68f))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium)
                Text(book.authors.joinToString().ifBlank { "Author unavailable" }, style = MaterialTheme.typography.bodyMedium)
                book.description?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 5, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        }
    }
}
