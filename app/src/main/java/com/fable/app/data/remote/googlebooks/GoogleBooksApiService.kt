package com.fable.app.data.remote.googlebooks

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleBooksApiService {
    @GET("volumes")
    suspend fun searchVolumes(
        @Query("q") query: String,
        @Query("startIndex") startIndex: Int,
        @Query("maxResults") maxResults: Int,
        @Query("orderBy") orderBy: String? = null,
        @Query("key") apiKey: String? = null,
    ): VolumesResponseDto

    @GET("volumes/{volumeId}")
    suspend fun getVolume(
        @Path("volumeId") volumeId: String,
        @Query("key") apiKey: String? = null,
    ): GoogleBookDto
}
