package com.yomu.reader.source.online

import com.yomu.reader.source.CatalogueSource
import com.yomu.reader.source.ExtensionDependencies
import com.yomu.reader.source.await
import com.yomu.reader.source.model.FilterList
import com.yomu.reader.source.model.MangasPage
import com.yomu.reader.source.model.Page
import com.yomu.reader.source.model.SChapter
import com.yomu.reader.source.model.SManga
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response

/**
 * Base class for HTTP-backed sources. Subclasses supply *requests* and *parsers*;
 * this class wires them into the coroutine [CatalogueSource] contract using the
 * host's shared OkHttp client. Modeled on Tachiyomi's HttpSource so the mental model
 * carries over for anyone who has written extensions before.
 *
 * Minimal source: override [baseUrl], the `*Request` builders and `*Parse` methods.
 */
abstract class HttpSource : CatalogueSource {

    /** Base URL, e.g. "https://example.com" (no trailing slash). */
    abstract val baseUrl: String

    override val lang: String get() = "en"

    override val supportsLatest: Boolean get() = false

    /** Bump when a source's [id] must change (e.g. domain migration). */
    open val versionId: Int get() = 1

    /** Stable id derived from name/lang/version — matches Tachiyomi's scheme shape. */
    override val id: Long by lazy { generateId(name, lang, versionId) }

    /** Shared client from the host; override to customise per source. */
    open val client: OkHttpClient get() = ExtensionDependencies.client

    open fun headersBuilder(): Headers.Builder =
        Headers.Builder().add("User-Agent", ExtensionDependencies.userAgent)

    val headers: Headers by lazy { headersBuilder().build() }

    // --- Request helpers ---

    protected fun GET(url: String, headers: Headers = this.headers): Request =
        Request.Builder().url(url).headers(headers).get().build()

    protected fun POST(url: String, headers: Headers = this.headers, body: RequestBody): Request =
        Request.Builder().url(url).headers(headers).post(body).build()

    // --- Popular ---

    protected abstract fun popularMangaRequest(page: Int): Request
    protected abstract fun popularMangaParse(response: Response): MangasPage
    override suspend fun getPopularManga(page: Int): MangasPage =
        client.newCall(popularMangaRequest(page)).await().use { popularMangaParse(it) }

    // --- Latest (opt-in via supportsLatest) ---

    protected open fun latestUpdatesRequest(page: Int): Request =
        throw UnsupportedOperationException("Latest updates not supported by this source")

    protected open fun latestUpdatesParse(response: Response): MangasPage =
        throw UnsupportedOperationException("Latest updates not supported by this source")

    override suspend fun getLatestUpdates(page: Int): MangasPage =
        client.newCall(latestUpdatesRequest(page)).await().use { latestUpdatesParse(it) }

    // --- Search ---

    protected abstract fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request
    protected abstract fun searchMangaParse(response: Response): MangasPage
    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage =
        client.newCall(searchMangaRequest(page, query, filters)).await().use { searchMangaParse(it) }

    // --- Details ---

    open fun mangaDetailsRequest(manga: SManga): Request = GET(baseUrl + manga.url)
    protected abstract fun mangaDetailsParse(response: Response): SManga
    override suspend fun getMangaDetails(manga: SManga): SManga =
        client.newCall(mangaDetailsRequest(manga)).await().use { mangaDetailsParse(it) }

    // --- Chapters ---

    open fun chapterListRequest(manga: SManga): Request = GET(baseUrl + manga.url)
    protected abstract fun chapterListParse(response: Response): List<SChapter>
    override suspend fun getChapterList(manga: SManga): List<SChapter> =
        client.newCall(chapterListRequest(manga)).await().use { chapterListParse(it) }

    // --- Pages ---

    open fun pageListRequest(chapter: SChapter): Request = GET(baseUrl + chapter.url)
    protected abstract fun pageListParse(response: Response): List<Page>
    override suspend fun getPageList(chapter: SChapter): List<Page> =
        client.newCall(pageListRequest(chapter)).await().use { pageListParse(it) }

    companion object {
        /** Deterministic 63-bit id from a source's identity. */
        fun generateId(name: String, lang: String, versionId: Int): Long {
            val key = "${name.lowercase()}/$lang/$versionId"
            var hash = 0L
            for (c in key) hash = 31 * hash + c.code
            return hash and Long.MAX_VALUE
        }
    }
}
