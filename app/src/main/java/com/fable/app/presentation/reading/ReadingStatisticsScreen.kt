package com.fable.app.presentation.reading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.fable.app.di.ReadingViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReadingStatisticsScreen(dependencies: FableDependencies, userId: String) {
    val viewModel: ReadingViewModel = viewModel(key = "statistics-$userId", factory = ReadingViewModelFactory(dependencies, userId))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val stats = state.statistics
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Reading Progress", style = MaterialTheme.typography.headlineMedium)
            Text("Statistics from the books in your personal library.", style = MaterialTheme.typography.bodyMedium)
        }
        item {
            StatCard("Books completed", stats.totalBooksCompleted.toString())
        }
        item { StatCard("Pages read", stats.totalPagesRead.toString()) }
        item { StatCard("Currently reading", stats.currentlyReadingCount.toString()) }
        item { StatCard("Want to read", stats.wantToReadCount.toString()) }
        item { StatCard("Average pages per completed book", stats.averagePagesPerCompletedBook?.toString() ?: "Not enough completed books") }
        item { Text("Monthly activity", style = MaterialTheme.typography.titleLarge) }
        if (stats.activityByMonth.isEmpty()) {
            item { Text("Completed-book activity will appear here when you finish a book.") }
        } else {
            items(stats.activityByMonth, key = { "${it.year}-${it.month}" }) { activity ->
                Text("${monthName(activity.month)} ${activity.year} · ${activity.booksCompleted} completed · ${activity.pagesRead} pages")
            }
        }
        item { Text("Reading history", style = MaterialTheme.typography.titleLarge) }
        if (stats.readingHistory.isEmpty()) item { Text("No completed books yet.") }
        items(stats.readingHistory, key = { it.bookId }) { entry ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(entry.book.title, style = MaterialTheme.typography.titleMedium)
                    Text(entry.book.authors.joinToString().ifBlank { "Author unavailable" }, style = MaterialTheme.typography.bodySmall)
                    entry.finishedAt?.let { Text("Finished ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))}") }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun monthName(month: Int): String = java.text.DateFormatSymbols.getInstance().months[month - 1]
