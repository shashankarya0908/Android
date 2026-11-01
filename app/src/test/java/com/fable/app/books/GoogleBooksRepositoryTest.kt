package com.fable.app.books

import com.fable.app.data.local.BookCacheEntity
import com.fable.app.data.local.BookDao
import com.fable.app.data.local.LibraryCacheEntity
import com.fable.app.data.local.SearchHistoryEntity
import com.fable.app.data.local.toCacheEntity
import com.fable.app.data.remote.googlebooks.GoogleBookDto
import com.fable.app.data.remote.googlebooks.GoogleBooksApiService
import com.fable.app.data.remote.googlebooks.GoogleVolumeInfoDto
import com.fable.app.data.remote.googlebooks.VolumesResponseDto
import com.fable.app.data.repository.BookRepositoryException
import com.fable.app.data.repository.GoogleBooksRepository
import com.fable.app.domain.model.Book
import com.fable.app.domain.model.BookIds
import com.fable.app.domain.repository.BookSearchType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import retrofit2.HttpException
import retrofit2.Response

class GoogleBooksRepositoryTest {
    @Test
    fun repositoryClampsPagesAndCachesRemoteBooks() = runBlocking {
        val api = FakeGoogleBooksApi(VolumesResponseDto(1, listOf(GoogleBookDto("v1", GoogleVolumeInfoDto(title = "Dune")))))
        val dao = FakeBookDao()
        val repository = GoogleBooksRepository(api, dao)

        val page = repository.searchBooks("Dune", BookSearchType.TITLE, startIndex = -5, pageSize = 100)

        assertEquals("intitle:Dune", api.lastQuery)
        assertEquals(0, api.lastStartIndex)
        assertEquals(40, api.lastMaxResults)
        assertEquals("google-books:v1", page.books.single().id)
        assertTrue(dao.cachedBooks.containsKey("google-books:v1"))
    }

    @Test
    fun networkFailureFallsBackToCachedBooks() = runBlocking {
        val dao = FakeBookDao().apply { cacheBooks(listOf(sampleBook.toCacheEntity())) }
        val repository = GoogleBooksRepository(FakeGoogleBooksApi(searchFailure = IOException("offline")), dao)

        val result = repository.searchBooks("Dune")

        assertEquals("Dune", result.books.single().title)
        assertFalse(result.hasMore)
    }

    @Test
    fun networkFailureWithoutCacheHasUsefulError() = runBlocking {
        val repository = GoogleBooksRepository(FakeGoogleBooksApi(searchFailure = IOException("offline")), FakeBookDao())

        val error = runCatching { repository.searchBooks("Dune") }.exceptionOrNull()

        assertTrue(error is BookRepositoryException)
        assertTrue(error?.message?.contains("Network connection failed") == true)
    }

    @Test
    fun quotaHttpErrorHasSpecificMessage() = runBlocking {
        val response = Response.error<VolumesResponseDto>(
            429,
            "{}".toResponseBody("application/json".toMediaType()),
        )
        val repository = GoogleBooksRepository(FakeGoogleBooksApi(searchFailure = HttpException(response)), FakeBookDao())

        val error = runCatching { repository.searchBooks("Dune") }.exceptionOrNull()

        assertTrue(error is BookRepositoryException)
        assertTrue(error?.message?.contains("limiting requests") == true)
    }

    @Test
    fun searchHistoryIsTrimmedDeduplicatedAndPersistentInLocalStore() = runBlocking {
        val dao = FakeBookDao()
        val repository = GoogleBooksRepository(FakeGoogleBooksApi(), dao)
        repository.recordSearch("  Dune  ")
        repository.recordSearch("DUNE")
        repository.recordSearch("   ")

        val history = repository.observeSearchHistory().first()

        assertEquals(1, history.size)
        assertEquals("DUNE", history.single().query)
    }
}

private val sampleBook = Book(
    id = BookIds.create("google-books", "dune"),
    source = "google-books",
    sourceId = "dune",
    title = "Dune",
    authors = listOf("Frank Herbert"),
    isbn13 = "9780441172719",
)

private class FakeGoogleBooksApi(
    private val response: VolumesResponseDto = VolumesResponseDto(totalItems = 0, items = emptyList()),
    private val searchFailure: Exception? = null,
) : GoogleBooksApiService {
    var lastQuery: String? = null
    var lastStartIndex: Int? = null
    var lastMaxResults: Int? = null

    override suspend fun searchVolumes(
        query: String,
        startIndex: Int,
        maxResults: Int,
        orderBy: String?,
        apiKey: String?,
    ): VolumesResponseDto {
        lastQuery = query
        lastStartIndex = startIndex
        lastMaxResults = maxResults
        searchFailure?.let { throw it }
        return response
    }

    override suspend fun getVolume(volumeId: String, apiKey: String?): GoogleBookDto =
        GoogleBookDto(volumeId, GoogleVolumeInfoDto(title = "Dune"))
}

private class FakeBookDao : BookDao {
    val cachedBooks = mutableMapOf<String, BookCacheEntity>()
    private val library = mutableMapOf<Pair<String, String>, LibraryCacheEntity>()
    private val searches = MutableStateFlow<List<SearchHistoryEntity>>(emptyList())

    override suspend fun upsertLibraryEntry(entry: LibraryCacheEntity) { library[entry.userId to entry.bookId] = entry }
    override suspend fun upsertLibraryEntries(entries: List<LibraryCacheEntity>) = entries.forEach { upsertLibraryEntry(it) }
    override fun observeLibrary(userId: String): Flow<List<LibraryCacheEntity>> = MutableStateFlow(library.values.filter { it.userId == userId })
    override fun observePendingLibraryChanges(userId: String): Flow<Int> = MutableStateFlow(library.values.count { it.userId == userId && it.syncPending })
    override suspend fun getAllLibraryEntries(userId: String): List<LibraryCacheEntity> = library.values.filter { it.userId == userId }
    override suspend fun getLibraryEntry(userId: String, bookId: String): LibraryCacheEntity? = library[userId to bookId]
    override suspend fun markLibrarySynced(userId: String, bookId: String, expectedUpdatedAt: Long) { library[userId to bookId]?.takeIf { it.updatedAt == expectedUpdatedAt }?.let { library[userId to bookId] = it.copy(syncPending = false) } }
    override suspend fun removeDeletedLibraryEntryIfCurrent(userId: String, bookId: String, expectedUpdatedAt: Long) { library[userId to bookId]?.takeIf { it.updatedAt == expectedUpdatedAt && it.deleted }?.let { library.remove(userId to bookId) } }
    override suspend fun removeLibraryEntry(userId: String, bookId: String) { library.remove(userId to bookId) }
    override suspend fun removeRemoteMissingEntries(userId: String, remoteBookIds: List<String>) = Unit
    override suspend fun removeAllSyncedLibraryEntries(userId: String) = Unit

    override suspend fun cacheBooks(books: List<BookCacheEntity>) { books.forEach { cachedBooks[it.id] = it } }
    override suspend fun getCachedBook(id: String): BookCacheEntity? = cachedBooks[id]
    override suspend fun searchCachedBooks(query: String, limit: Int): List<BookCacheEntity> =
        cachedBooks.values.filter {
            it.title.contains(query, ignoreCase = true) || it.searchableText.contains(query, ignoreCase = true)
        }.take(limit)

    override fun observeSearchHistory(limit: Int): Flow<List<SearchHistoryEntity>> =
        searches.map { it.sortedByDescending(SearchHistoryEntity::searchedAtMillis).take(limit) }

    override suspend fun saveSearch(entity: SearchHistoryEntity) {
        searches.value = searches.value.filterNot { it.normalizedQuery == entity.normalizedQuery } + entity
    }

    override suspend fun pruneSearchHistory(limit: Int) {
        searches.value = searches.value.sortedByDescending(SearchHistoryEntity::searchedAtMillis).take(limit)
    }

    override suspend fun clearSearchHistory() { searches.value = emptyList() }
}
