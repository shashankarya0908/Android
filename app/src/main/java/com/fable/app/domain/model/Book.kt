package com.fable.app.domain.model

data class Book(
    val id: String,
    val source: String,
    val sourceId: String,
    val title: String,
    val subtitle: String? = null,
    val authors: List<String> = emptyList(),
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val publisher: String? = null,
    val publishedDate: String? = null,
    val pageCount: Int? = null,
    val categories: List<String> = emptyList(),
    val language: String? = null,
    val isbn10: String? = null,
    val isbn13: String? = null,
    val averageRating: Double? = null,
    val ratingsCount: Int? = null,
    val previewLink: String? = null,
)

object BookIds {
    fun create(source: String, sourceId: String): String {
        require(source.isNotBlank()) { "Book source cannot be blank." }
        require(sourceId.isNotBlank()) { "Book source ID cannot be blank." }
        return "${source.trim().lowercase()}:${sourceId.trim()}"
    }

    fun parse(id: String): Pair<String, String>? {
        val separator = id.indexOf(':')
        if (separator <= 0 || separator == id.lastIndex) return null
        return id.substring(0, separator) to id.substring(separator + 1)
    }
}

data class BookPage(
    val books: List<Book>,
    val totalItems: Int,
    val nextStartIndex: Int,
    val hasMore: Boolean,
)

data class SearchHistoryEntry(
    val query: String,
    val searchedAtMillis: Long,
)
