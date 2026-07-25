package com.yomu.reader.source.mangadex

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/*
 * Minimal DTOs for the public MangaDex API (https://api.mangadex.org).
 * Only the fields Yomu actually reads are modeled.
 */

@JsonClass(generateAdapter = true)
data class MDListResponse(
    val result: String?,
    val data: List<MDManga> = emptyList(),
    val limit: Int = 0,
    val offset: Int = 0,
    val total: Int = 0,
)

@JsonClass(generateAdapter = true)
data class MDEntityResponse(
    val result: String?,
    val data: MDManga?,
)

@JsonClass(generateAdapter = true)
data class MDManga(
    val id: String,
    val type: String?,
    val attributes: MDMangaAttributes?,
    val relationships: List<MDRelationship> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class MDMangaAttributes(
    val title: Map<String, String> = emptyMap(),
    val description: Map<String, String> = emptyMap(),
    val status: String? = null,
    val tags: List<MDTag> = emptyList(),
    val contentRating: String? = null,
)

@JsonClass(generateAdapter = true)
data class MDTag(
    val id: String?,
    val attributes: MDTagAttributes?,
)

@JsonClass(generateAdapter = true)
data class MDTagAttributes(
    val name: Map<String, String> = emptyMap(),
)

@JsonClass(generateAdapter = true)
data class MDRelationship(
    val id: String,
    val type: String,
    val attributes: MDRelationshipAttributes? = null,
)

@JsonClass(generateAdapter = true)
data class MDRelationshipAttributes(
    // cover_art
    val fileName: String? = null,
    // author / artist
    val name: String? = null,
)

// --- Chapter feed ---

@JsonClass(generateAdapter = true)
data class MDChapterListResponse(
    val result: String?,
    val data: List<MDChapter> = emptyList(),
    val limit: Int = 0,
    val offset: Int = 0,
    val total: Int = 0,
)

@JsonClass(generateAdapter = true)
data class MDChapter(
    val id: String,
    val attributes: MDChapterAttributes?,
    val relationships: List<MDRelationship> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class MDChapterAttributes(
    val volume: String? = null,
    val chapter: String? = null,
    val title: String? = null,
    val translatedLanguage: String? = null,
    val externalUrl: String? = null,
    val pages: Int = 0,
    val publishAt: String? = null,
    val readableAt: String? = null,
)

// --- At-home (page) server ---

@JsonClass(generateAdapter = true)
data class MDAtHomeResponse(
    val result: String?,
    val baseUrl: String?,
    val chapter: MDAtHomeChapter?,
)

@JsonClass(generateAdapter = true)
data class MDAtHomeChapter(
    val hash: String?,
    val data: List<String> = emptyList(),
    @Json(name = "dataSaver") val dataSaver: List<String> = emptyList(),
)
