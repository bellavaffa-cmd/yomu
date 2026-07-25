package com.yomu.reader.data

import com.squareup.moshi.JsonClass

/**
 * The full backup document stored as JSON in the user's Google Drive app-data folder.
 * Categories and category membership are keyed by *name* (ids differ per device).
 */
@JsonClass(generateAdapter = true)
data class SyncPayload(
    val version: Int = 1,
    val updatedAt: Long = 0,
    val categories: List<String> = emptyList(),
    val library: List<MangaSnapshot> = emptyList(),
    val extensionRepos: List<String> = emptyList(),
    val installedExtensions: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class MangaSnapshot(
    val source: Long,
    val url: String,
    val title: String,
    val author: String? = null,
    val artist: String? = null,
    val description: String? = null,
    val genre: List<String> = emptyList(),
    val status: Int = 0,
    val thumbnailUrl: String? = null,
    val categories: List<String> = emptyList(),
)
