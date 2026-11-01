package com.fable.app.data.remote.googlebooks

import com.fable.app.domain.repository.BookSearchType

object GoogleBooksQueryBuilder {
    fun build(query: String, type: BookSearchType): String {
        val normalized = query.trim()
        require(normalized.isNotEmpty()) { "Search query cannot be blank." }
        val prefix = type.queryPrefix ?: return normalized
        return "$prefix:$normalized"
    }
}
