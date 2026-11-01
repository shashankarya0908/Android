package com.fable.app.di

import android.content.Context
import com.fable.app.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fable.app.data.repository.FirebaseAuthenticationRepository
import com.fable.app.data.repository.FirestoreUserRepository
import com.fable.app.data.repository.GoogleBooksRepository
import com.fable.app.data.repository.FirestoreReadingRepository
import com.fable.app.data.local.FableDatabase
import com.fable.app.data.remote.BooksNetworkModule
import com.fable.app.domain.model.AuthUser
import com.fable.app.domain.model.UserProfile
import com.fable.app.domain.repository.AuthenticationRepository
import com.fable.app.domain.repository.UserRepository
import com.fable.app.domain.repository.BookRepository
import com.fable.app.domain.repository.ReadingRepository
import com.fable.app.presentation.auth.AuthenticationViewModel
import com.fable.app.presentation.auth.ProfileViewModel
import com.fable.app.presentation.books.BookSearchViewModel
import com.fable.app.presentation.books.DiscoverViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import androidx.room.Room

class FirebaseConfigurationRequiredException :
    IllegalStateException("Firebase is not configured. Add app/google-services.json and sync the project.")

class FableDependencies(context: Context) {
    val firebaseConfigured: Boolean = FirebaseApp.getApps(context.applicationContext).isNotEmpty()

    val authenticationRepository: AuthenticationRepository
    val userRepository: UserRepository
    val bookRepository: BookRepository
    val readingRepository: ReadingRepository

    init {
        val database = Room.databaseBuilder(context.applicationContext, FableDatabase::class.java, "fable.db")
            .addMigrations(FableDatabase.MIGRATION_1_2)
            .build()
        bookRepository = GoogleBooksRepository(
            BooksNetworkModule.createGoogleBooksService(),
            database.bookDao(),
            BuildConfig.GOOGLE_BOOKS_API_KEY.takeIf(String::isNotBlank),
        )
        if (firebaseConfigured) {
            authenticationRepository = FirebaseAuthenticationRepository(FirebaseAuth.getInstance())
            userRepository = FirestoreUserRepository(FirebaseFirestore.getInstance())
            readingRepository = FirestoreReadingRepository(FirebaseFirestore.getInstance(), database.bookDao())
        } else {
            authenticationRepository = UnconfiguredAuthenticationRepository()
            userRepository = UnconfiguredUserRepository()
            readingRepository = UnconfiguredReadingRepository()
        }
    }
}

class FableViewModelFactory(private val dependencies: FableDependencies) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AuthenticationViewModel::class.java) ->
            AuthenticationViewModel(dependencies.authenticationRepository, dependencies.userRepository) as T
        modelClass.isAssignableFrom(ProfileViewModel::class.java) ->
            ProfileViewModel(dependencies.userRepository) as T
        modelClass.isAssignableFrom(BookSearchViewModel::class.java) ->
            BookSearchViewModel(dependencies.bookRepository) as T
        modelClass.isAssignableFrom(DiscoverViewModel::class.java) ->
            DiscoverViewModel(dependencies.bookRepository) as T
        else -> error("Unknown ViewModel class: ${modelClass.name}")
    }
}

class CategoryBooksViewModelFactory(
    private val dependencies: FableDependencies,
    private val category: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        com.fable.app.presentation.books.CategoryBooksViewModel(dependencies.bookRepository, category) as T
}

class BookDetailsViewModelFactory(
    private val dependencies: FableDependencies,
    private val bookId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        com.fable.app.presentation.books.BookDetailsViewModel(dependencies.bookRepository, bookId) as T
}

class ReadingViewModelFactory(
    private val dependencies: FableDependencies,
    private val userId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        com.fable.app.presentation.reading.ReadingViewModel(userId, dependencies.readingRepository) as T
}

private class UnconfiguredAuthenticationRepository : AuthenticationRepository {
    override fun observeCurrentUser(): Flow<AuthUser?> = flowOf(null)
    override suspend fun register(email: String, password: String): AuthUser = throw FirebaseConfigurationRequiredException()
    override suspend fun login(email: String, password: String): AuthUser = throw FirebaseConfigurationRequiredException()
    override suspend fun sendPasswordReset(email: String) = throw FirebaseConfigurationRequiredException()
    override suspend fun logout() = Unit
    override suspend fun deleteCurrentUser() = Unit
}

private class UnconfiguredUserRepository : UserRepository {
    override fun observeProfile(uid: String): Flow<UserProfile?> = flowOf(null)
    override suspend fun createProfile(profile: UserProfile) = throw FirebaseConfigurationRequiredException()
    override suspend fun updateProfile(profile: UserProfile, previousUsername: String) =
        throw FirebaseConfigurationRequiredException()
}

private class UnconfiguredReadingRepository : ReadingRepository {
    override fun observeLibrary(userId: String): Flow<List<com.fable.app.domain.model.UserBook>> = flowOf(emptyList())
    override fun observeStatistics(userId: String): Flow<com.fable.app.domain.model.ReadingStatistics> =
        flowOf(com.fable.app.domain.model.calculateReadingStatistics(emptyList()))
    override fun observeSyncState(userId: String): Flow<com.fable.app.domain.model.ReadingSyncState> =
        flowOf(com.fable.app.domain.model.ReadingSyncState(errorMessage = "Firebase is not configured."))
    override suspend fun addOrMoveBook(userId: String, book: com.fable.app.domain.model.Book, status: com.fable.app.domain.model.ReadingStatus): com.fable.app.domain.model.UserBook =
        throw FirebaseConfigurationRequiredException()
    override suspend fun removeBook(userId: String, bookId: String) = throw FirebaseConfigurationRequiredException()
    override suspend fun updateProgress(userId: String, bookId: String, currentPage: Int, personalNotes: String?): com.fable.app.domain.model.UserBook =
        throw FirebaseConfigurationRequiredException()
    override suspend fun synchronizePending(userId: String) = throw FirebaseConfigurationRequiredException()
}
