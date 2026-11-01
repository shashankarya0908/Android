package com.fable.app.data.remote.googlebooks

import com.fable.app.domain.model.Book
import com.fable.app.domain.model.BookIds
import com.fable.app.domain.model.BookPage

data class VolumesResponseDto(
    val totalItems: Int? = 0,
    val items: List<GoogleBookDto>? = emptyList(),
)

data class GoogleBookDto(
    val id: String? = null,
    val volumeInfo: GoogleVolumeInfoDto? = null,
)

data class GoogleVolumeInfoDto(
    val title: String? = null,
    val subtitle: String? = null,
    val authors: List<String>? = null,
    val description: String? = null,
    val publisher: String? = null,
    val publishedDate: String? = null,
    val pageCount: Int? = null,
    val categories: List<String>? = null,
    val averageRating: Double? = null,
    val ratingsCount: Int? = null,
    val imageLinks: GoogleImageLinksDto? = null,
    val language: String? = null,
    val industryIdentifiers: List<GoogleIndustryIdentifierDto>? = null,
    val previewLink: String? = null,
)

data class GoogleImageLinksDto(
    val thumbnail: String? = null,
    val smallThumbnail: String? = null,
)

data class GoogleIndustryIdentifierDto(
    val type: String? = null,
    val identifier: String? = null,
)

fun GoogleBookDto.toDomainOrNull(): Book? {
    val sourceId = id?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val info = volumeInfo ?: return null
    val title = info.title?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val identifiers = info.industryIdentifiers.orEmpty()
    val cover = info.imageLinks?.thumbnail ?: info.imageLinks?.smallThumbnail

    return Book(
        id = BookIds.create(GOOGLE_BOOKS_SOURCE, sourceId),
        source = GOOGLE_BOOKS_SOURCE,
        sourceId = sourceId,
        title = title,
        subtitle = info.subtitle.cleanOrNull(),
        authors = info.authors.orEmpty().map(String::trim).filter(String::isNotEmpty),
        description = info.description.cleanOrNull(),
        thumbnailUrl = cover.cleanCoverUrl(),
        publisher = info.publisher.cleanOrNull(),
        publishedDate = info.publishedDate.cleanOrNull(),
        pageCount = info.pageCount?.takeIf { it > 0 },
        categories = info.categories.orEmpty().map(String::trim).filter(String::isNotEmpty),
        language = info.language.cleanOrNull(),
        isbn10 = identifiers.firstOrNull { it.type == "ISBN_10" }?.identifier.cleanOrNull(),
        isbn13 = identifiers.firstOrNull { it.type == "ISBN_13" }?.identifier.cleanOrNull(),
        averageRating = info.averageRating?.takeIf { it in 0.0..5.0 },
        ratingsCount = info.ratingsCount?.takeIf { it >= 0 },
        previewLink = info.previewLink.cleanOrNull(),
    )
}

fun VolumesResponseDto.toDomainPage(startIndex: Int, requestedPageSize: Int): BookPage {
    val rawItems = items.orEmpty()
    val mappedItems = rawItems.mapNotNull(GoogleBookDto::toDomainOrNull).distinctBy(Book::id)
    val total = (totalItems ?: 0).coerceAtLeast(0)
    val next = startIndex + rawItems.size
    return BookPage(
        books = mappedItems,
        totalItems = total,
        nextStartIndex = next,
        hasMore = rawItems.isNotEmpty() && next < total && requestedPageSize > 0,
    )
}

private fun String?.cleanOrNull(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private fun String?.cleanCoverUrl(): String? = cleanOrNull()
    ?.replace(Regex("^http://", RegexOption.IGNORE_CASE), "https://")

const val GOOGLE_BOOKS_SOURCE = "google-books"
