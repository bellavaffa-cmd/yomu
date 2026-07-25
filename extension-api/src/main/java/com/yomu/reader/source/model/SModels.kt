package com.yomu.reader.source.model

/**
 * Source-side manga info returned by a [com.yomu.reader.source.Source].
 * Mirrors Tachiyomi/Mihon's SManga so source logic ports easily.
 *
 * Part of Yomu's extension ABI: extensions compile against these classes
 * (compileOnly) and the host app provides the implementations at runtime.
 */
data class SManga(
    val url: String,
    val title: String,
    val artist: String? = null,
    val author: String? = null,
    val description: String? = null,
    val genre: List<String> = emptyList(),
    val status: Int = STATUS_UNKNOWN,
    val thumbnailUrl: String? = null,
    val initialized: Boolean = false,
) {
    companion object {
        const val STATUS_UNKNOWN = 0
        const val STATUS_ONGOING = 1
        const val STATUS_COMPLETED = 2
        const val STATUS_LICENSED = 3
        const val STATUS_PUBLISHING_FINISHED = 4
        const val STATUS_CANCELLED = 5
        const val STATUS_ON_HIATUS = 6
    }
}

/** Source-side chapter info. */
data class SChapter(
    val url: String,
    val name: String,
    val dateUpload: Long = 0L,
    val chapterNumber: Float = -1f,
    val scanlator: String? = null,
)

/** A single page within a chapter. [imageUrl] may be resolved lazily. */
data class Page(
    val index: Int,
    val url: String = "",
    val imageUrl: String? = null,
) {
    val number: Int get() = index + 1
}

/** A page of manga results from a catalogue browse/search. */
data class MangasPage(
    val mangas: List<SManga>,
    val hasNextPage: Boolean,
)
