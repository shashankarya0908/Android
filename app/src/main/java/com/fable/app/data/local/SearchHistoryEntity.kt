package com.fable.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fable.app.domain.model.SearchHistoryEntry

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val normalizedQuery: String,
    val query: String,
    val searchedAtMillis: Long,
)

fun SearchHistoryEntity.toDomain() = SearchHistoryEntry(query, searchedAtMillis)
