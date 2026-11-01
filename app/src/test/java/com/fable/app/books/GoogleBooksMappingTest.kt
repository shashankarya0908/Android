package com.fable.app.books

import com.fable.app.data.remote.googlebooks.GoogleBookDto
import com.fable.app.data.remote.googlebooks.GoogleBooksQueryBuilder
import com.fable.app.data.remote.googlebooks.GoogleImageLinksDto
import com.fable.app.data.remote.googlebooks.GoogleIndustryIdentifierDto
import com.fable.app.data.remote.googlebooks.GoogleVolumeInfoDto
import com.fable.app.data.remote.googlebooks.VolumesResponseDto
import com.fable.app.data.remote.googlebooks.toDomainOrNull
import com.fable.app.data.remote.googlebooks.toDomainPage
import com.fable.app.domain.model.BookIds
import com.fable.app.domain.repository.BookSearchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleBooksMappingTest {
    @Test
    fun bookIdUsesSourceAndApiIdRatherThanTitle() {
        assertEquals("google-books:volume_42", BookIds.create(" Google-Books ", "volume_42"))
        assertEquals("volume_42", BookIds.parse("google-books:volume_42")?.second)
        assertNull(BookIds.parse("title-only"))
    }

    @Test
    fun mapperHandlesMissingFieldsAndNormalizesCoverUrl() {
        val book = GoogleBookDto(
            id = "vol-1",
            volumeInfo = GoogleVolumeInfoDto(
                title = "  A Book  ",
                imageLinks = GoogleImageLinksDto(thumbnail = "http://books.example/cover"),
                authors = null,
                pageCount = 0,
                averageRating = 6.2,
                ratingsCount = -1,
                industryIdentifiers = listOf(
                    GoogleIndustryIdentifierDto("ISBN_10", " 123456789X "),
                ),
            ),
        ).toDomainOrNull()!!

        assertEquals("A Book", book.title)
        assertEquals("https://books.example/cover", book.thumbnailUrl)
        assertTrue(book.authors.isEmpty())
        assertNull(book.description)
        assertNull(book.pageCount)
        assertNull(book.averageRating)
        assertNull(book.ratingsCount)
        assertEquals("123456789X", book.isbn10)
    }

    @Test
    fun incompleteVolumeWithoutTitleOrIdIsIgnored() {
        assertNull(GoogleBookDto(id = null, volumeInfo = GoogleVolumeInfoDto(title = "Title")).toDomainOrNull())
        assertNull(GoogleBookDto(id = "id", volumeInfo = GoogleVolumeInfoDto(title = " ")).toDomainOrNull())
        assertNull(GoogleBookDto(id = "id", volumeInfo = null).toDomainOrNull())
    }

    @Test
    fun queryBuilderSupportsEachGoogleBooksSearchField() {
        assertEquals("novel", GoogleBooksQueryBuilder.build(" novel ", BookSearchType.GENERAL))
        assertEquals("intitle:novel", GoogleBooksQueryBuilder.build("novel", BookSearchType.TITLE))
        assertEquals("inauthor:Le Guin", GoogleBooksQueryBuilder.build("Le Guin", BookSearchType.AUTHOR))
        assertEquals("isbn:9781234567890", GoogleBooksQueryBuilder.build("9781234567890", BookSearchType.ISBN))
        assertEquals("subject:fantasy", GoogleBooksQueryBuilder.build("fantasy", BookSearchType.SUBJECT))
    }

    @Test
    fun pageAdvancesByRawItemsAndDropsInvalidAndDuplicateRecords() {
        val item = GoogleBookDto("one", GoogleVolumeInfoDto(title = "Same"))
        val page = VolumesResponseDto(totalItems = 20, items = listOf(item, item, GoogleBookDto("broken")))
            .toDomainPage(startIndex = 10, requestedPageSize = 20)

        assertEquals(1, page.books.size)
        assertEquals(20, page.totalItems)
        assertEquals(13, page.nextStartIndex)
        assertTrue(page.hasMore)
    }

    @Test
    fun emptyApiPageStopsPagination() {
        val page = VolumesResponseDto(totalItems = 50, items = emptyList()).toDomainPage(20, 20)
        assertFalse(page.hasMore)
        assertEquals(20, page.nextStartIndex)
    }
}
