package com.fable.app.presentation.books.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fable.app.domain.model.Book

@Composable
fun BookCarousel(books: List<Book>, onBookClick: (Book) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(books, key = Book::id) { book -> BookCard(book, { onBookClick(book) }) }
    }
}

@Composable
fun BookGrid(
    books: List<Book>,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 148.dp),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(books, key = Book::id) { book ->
            BookCard(book, { onBookClick(book) }, modifier = Modifier.fillMaxWidth())
        }
        if (hasMore || isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                if (isLoadingMore) CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                else Button(onClick = onLoadMore, modifier = Modifier.padding(16.dp)) { Text("Load more") }
            }
        }
    }
}
