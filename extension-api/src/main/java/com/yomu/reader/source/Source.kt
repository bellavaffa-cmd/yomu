package com.yomu.reader.source

import com.yomu.reader.source.model.FilterList
import com.yomu.reader.source.model.MangasPage
import com.yomu.reader.source.model.Page
import com.yomu.reader.source.model.SChapter
import com.yomu.reader.source.model.SManga

/**
 * A content source. Built-in sources implement this directly; external extension
 * APKs are loaded and adapted onto the same contract by the extension loader.
 */
interface Source {
    /** Stable unique id (ties library manga to their source). */
    val id: Long

    /** Display name, e.g. "MangaDex". */
    val name: String

    /** ISO language code of the source's content, or "all". */
    val lang: String

    /** Fetch full details for a manga (description, status, cover, etc.). */
    suspend fun getMangaDetails(manga: SManga): SManga

    /** Fetch the chapter list for a manga, newest first. */
    suspend fun getChapterList(manga: SManga): List<SChapter>

    /** Fetch the ordered list of pages for a chapter. */
    suspend fun getPageList(chapter: SChapter): List<Page>
}

/**
 * A source that can be browsed and searched from the catalogue.
 */
interface CatalogueSource : Source {
    /** Popular/most-followed manga, paginated (1-based). */
    suspend fun getPopularManga(page: Int): MangasPage

    /** Latest updated manga, paginated (1-based). */
    suspend fun getLatestUpdates(page: Int): MangasPage

    /** Search manga by query + filters, paginated (1-based). */
    suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage

    /** Whether this source supports the "latest" listing. */
    val supportsLatest: Boolean

    /** Default filter widgets for the search UI. */
    fun getFilterList(): FilterList = emptyList()
}
