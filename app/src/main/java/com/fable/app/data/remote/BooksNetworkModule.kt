package com.fable.app.data.remote

import com.fable.app.data.remote.googlebooks.GoogleBooksApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object BooksNetworkModule {
    private const val GOOGLE_BOOKS_BASE_URL = "https://www.googleapis.com/books/v1/"

    fun createGoogleBooksService(): GoogleBooksApiService {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(GOOGLE_BOOKS_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleBooksApiService::class.java)
    }
}
