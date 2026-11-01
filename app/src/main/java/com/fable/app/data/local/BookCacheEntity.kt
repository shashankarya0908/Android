package com.fable.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fable.app.domain.model.Book

@Entity(tableName = "cached_books", indices = [Index(value = ["title"])])
data class BookCacheEntity(
    @PrimaryKey val id: String,
    val source: String,
    val sourceId: String,
    val title: String,
    val subtitle: String?,
    val authorsEncoded: String,
    val description: String?,
    val thumbnailUrl: String?,
    val publisher: String?,
    val publishedDate: String?,
    val pageCount: Int?,
    val categoriesEncoded: String,
    val language: String?,
    val isbn10: String?,
    val isbn13: String?,
    val averageRating: Double?,
    val ratingsCount: Int?,
    val previewLink: String?,
    val searchableText: String,
    val cachedAtMillis: Long,
)

fun Book.toCacheEntity(now: Long = System.currentTimeMillis()): BookCacheEntity = BookCacheEntity(
    id = id,
    source = source,
    sourceId = sourceId,
    title = title,
    subtitle = subtitle,
    authorsEncoded = authors.encodeStrings(),
    description = description,
    thumbnailUrl = thumbnailUrl,
    publisher = publisher,
    publishedDate = publishedDate,
    pageCount = pageCount,
    categoriesEncoded = categories.encodeStrings(),
    language = language,
    isbn10 = isbn10,
    isbn13 = isbn13,
    averageRating = averageRating,
    ratingsCount = ratingsCount,
    previewLink = previewLink,
    searchableText = (authors + categories + listOfNotNull(isbn10, isbn13, subtitle))
        .joinToString(" ").lowercase(),
    cachedAtMillis = now,
)

fun BookCacheEntity.toDomain(): Book = Book(
    id = id,
    source = source,
    sourceId = sourceId,
    title = title,
    subtitle = subtitle,
    authors = authorsEncoded.toStringList(),
    description = description,
    thumbnailUrl = thumbnailUrl,
    publisher = publisher,
    publishedDate = publishedDate,
    pageCount = pageCount,
    categories = categoriesEncoded.toStringList(),
    language = language,
    isbn10 = isbn10,
    isbn13 = isbn13,
    averageRating = averageRating,
    ratingsCount = ratingsCount,
    previewLink = previewLink,
)

private fun List<String>.encodeStrings(): String = joinToString("|") {
    it.replace("%", "%25").replace("|", "%7C")
}

private fun String.toStringList(): List<String> = if (isEmpty()) emptyList() else split('|').map {
    it.replace("%7C", "|").replace("%25", "%")
}
