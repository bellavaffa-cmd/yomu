package com.yomu.reader.source.online

import com.yomu.reader.source.model.MangasPage
import com.yomu.reader.source.model.SChapter
import com.yomu.reader.source.model.SManga
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * [HttpSource] specialised for scraping HTML with JSoup. Subclasses express each
 * listing as a CSS selector plus an element→model mapper — the classic Tachiyomi
 * `*Selector()` / `*FromElement()` pattern.
 */
abstract class ParsedHttpSource : HttpSource() {

    // --- Popular ---

    protected abstract fun popularMangaSelector(): String
    protected abstract fun popularMangaFromElement(element: Element): SManga
    protected abstract fun popularMangaNextPageSelector(): String?

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(popularMangaSelector()).map { popularMangaFromElement(it) }
        val hasNext = popularMangaNextPageSelector()?.let { document.selectFirst(it) != null } ?: false
        return MangasPage(mangas, hasNext)
    }

    // --- Search ---

    protected abstract fun searchMangaSelector(): String
    protected abstract fun searchMangaFromElement(element: Element): SManga
    protected abstract fun searchMangaNextPageSelector(): String?

    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select(searchMangaSelector()).map { searchMangaFromElement(it) }
        val hasNext = searchMangaNextPageSelector()?.let { document.selectFirst(it) != null } ?: false
        return MangasPage(mangas, hasNext)
    }

    // --- Details ---

    protected abstract fun mangaDetailsParse(document: Document): SManga
    override fun mangaDetailsParse(response: Response): SManga = mangaDetailsParse(response.asJsoup())

    // --- Chapters ---

    protected abstract fun chapterListSelector(): String
    protected abstract fun chapterFromElement(element: Element): SChapter
    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return document.select(chapterListSelector()).map { chapterFromElement(it) }
    }

    /** Parse the response body into a JSoup [Document], preserving the base URI. */
    protected fun Response.asJsoup(): Document {
        val body = body ?: error("Empty response body")
        return Jsoup.parse(body.string(), request.url.toString())
    }
}
