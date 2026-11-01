package com.fable.app.presentation.books.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fable.app.di.FableDependencies
import com.fable.app.domain.model.Book
import com.fable.app.domain.model.UserProfile
import com.fable.app.presentation.books.details.BookDetailsScreen
import com.fable.app.presentation.books.discovery.DiscoverScreen
import com.fable.app.presentation.books.discovery.GenreScreen
import com.fable.app.presentation.books.search.BookSearchScreen
import com.fable.app.presentation.reading.MyLibraryScreen
import com.fable.app.presentation.reading.ReadingStatisticsScreen
import com.fable.app.presentation.reading.ReadingHomeScreen
import com.fable.app.presentation.reviews.MyReviewsScreen

private val primaryDestinations = listOf("home", "discover", "search", "library")

@Composable
fun AuthenticatedBooksApp(
    profile: UserProfile,
    userId: String,
    dependencies: FableDependencies,
    onLogout: () -> Unit,
) {
    val navController = rememberNavController()
    val navEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navEntry?.destination?.route
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Fable · @${profile.username}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                TextButton(onClick = onLogout) { Text("Sign out") }
            }
        },
        bottomBar = {
            if (currentRoute in primaryDestinations) {
                NavigationBar {
                    listOf("home" to "Home", "discover" to "Discover", "search" to "Search", "library" to "My Library").forEach { (route, label) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            label = { Text(label) },
                            icon = { Text(when (route) { "home" -> "⌂"; "discover" -> "✧"; "search" -> "⌕"; else -> "▤" }) },
                        )
                    }
                }
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(contentPadding),
        ) {
            composable("home") {
                ReadingHomeScreen(
                    dependencies = dependencies,
                    userId = userId,
                    displayName = profile.displayName,
                    onDiscover = { navController.navigate("discover") },
                    onSearch = { navController.navigate("search") },
                    onLibrary = { navController.navigate("library") },
                    onStatistics = { navController.navigate("reading-statistics") },
                    onMyReviews = { navController.navigate("my-reviews") },
                    onBookClick = { bookId -> navController.navigate("book/${Uri.encode(bookId)}") },
                )
            }
            composable("discover") {
                DiscoverScreen(
                    dependencies = dependencies,
                    onBookClick = { book -> navController.openBook(book) },
                    onGenreClick = { genre -> navController.navigate("genre/${Uri.encode(genre)}") },
                    onSearchClick = { navController.navigate("search") },
                )
            }
            composable("search") {
                BookSearchScreen(dependencies) { book -> navController.openBook(book) }
            }
            composable(
                route = "genre/{category}",
                arguments = listOf(navArgument("category") { type = NavType.StringType }),
            ) { entry ->
                GenreScreen(
                    dependencies,
                    entry.arguments?.getString("category").orEmpty(),
                    onBookClick = { book -> navController.openBook(book) },
                )
            }
            composable("library") {
                MyLibraryScreen(
                    dependencies = dependencies,
                    userId = userId,
                    onBookClick = { bookId -> navController.navigate("book/${Uri.encode(bookId)}") },
                    onProgressClick = { navController.navigate("reading-statistics") },
                )
            }
            composable("reading-statistics") {
                ReadingStatisticsScreen(dependencies, userId)
            }
            composable("my-reviews") {
                MyReviewsScreen(dependencies, userId, profile.username) { bookId ->
                    navController.navigate("book/${Uri.encode(bookId)}")
                }
            }
            composable(
                route = "book/{bookId}",
                arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
            ) { entry ->
                BookDetailsScreen(dependencies, userId, profile.username, entry.arguments?.getString("bookId").orEmpty())
            }
        }
    }
}

private fun androidx.navigation.NavHostController.openBook(book: Book) {
    navigate("book/${Uri.encode(book.id)}")
}
