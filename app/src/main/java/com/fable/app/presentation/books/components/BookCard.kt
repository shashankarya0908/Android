package com.fable.app.presentation.books.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fable.app.domain.model.Book

@Composable
fun BookCard(book: Book, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.width(154.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            BookCover(book.thumbnailUrl, book.title, Modifier.fillMaxWidth().aspectRatio(0.68f))
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    book.authors.firstOrNull() ?: "Author unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                book.averageRating?.let { rating ->
                    Text(
                        "★ ${"%.1f".format(rating)}${book.ratingsCount?.let { " · $it ratings" }.orEmpty()}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
