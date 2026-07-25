package com.yomu.reader.source.mangadex

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface MangaDexApi {

    @GET("manga")
    suspend fun getMangaList(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @QueryMap params: Map<String, String>,
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author", "artist"),
        @Query("contentRating[]") contentRating: List<String> = DEFAULT_CONTENT_RATING,
        @Query("availableTranslatedLanguage[]") lang: List<String> = listOf("en"),
        @Query("hasAvailableChapters") hasChapters: String = "true",
    ): MDListResponse

    @GET("manga")
    suspend fun searchManga(
        @Query("title") title: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author", "artist"),
        @Query("contentRating[]") contentRating: List<String> = DEFAULT_CONTENT_RATING,
        @Query("availableTranslatedLanguage[]") lang: List<String> = listOf("en"),
        @Query("order[relevance]") order: String = "desc",
    ): MDListResponse

    @GET("manga/{id}")
    suspend fun getManga(
        @Path("id") id: String,
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author", "artist"),
    ): MDEntityResponse

    @GET("manga/{id}/feed")
    suspend fun getChapterFeed(
        @Path("id") id: String,
        @Query("limit") limit: Int = 500,
        @Query("offset") offset: Int = 0,
        @Query("translatedLanguage[]") lang: List<String> = listOf("en"),
        @Query("order[volume]") orderVolume: String = "desc",
        @Query("order[chapter]") orderChapter: String = "desc",
        @Query("includes[]") includes: List<String> = listOf("scanlation_group"),
        @Query("contentRating[]") contentRating: List<String> = DEFAULT_CONTENT_RATING,
    ): MDChapterListResponse

    @GET("at-home/server/{chapterId}")
    suspend fun getAtHomeServer(
        @Path("chapterId") chapterId: String,
    ): MDAtHomeResponse

    companion object {
        const val BASE_URL = "https://api.mangadex.org/"
        const val COVER_URL = "https://uploads.mangadex.org/covers"
        val DEFAULT_CONTENT_RATING = listOf("safe", "suggestive")
    }
}
