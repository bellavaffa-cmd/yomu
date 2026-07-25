package com.yomu.reader.extension.xkcd

import com.yomu.reader.source.Source
import com.yomu.reader.source.SourceFactory
import com.yomu.reader.source.model.FilterList
import com.yomu.reader.source.model.MangasPage
import com.yomu.reader.source.model.Page
import com.yomu.reader.source.model.SChapter
import com.yomu.reader.source.model.SManga
import com.yomu.reader.source.online.HttpSource
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject

class XkcdFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(XkcdSource())
}

/**
 * xkcd — "a webcomic of romance, sarcasm, math, and language" by Randall Munroe.
 * Freely readable and licensed CC BY-NC 2.5. Uses xkcd's public JSON API
 * (https://xkcd.com/json.html): `/info.0.json` for the latest, `/{n}/info.0.json`
 * for a specific comic.
 *
 * Modeled as a single "series" whose chapters are the individual comics — a compact
 * demonstration of the Yomu HttpSource SDK against a real network API.
 */
class XkcdSource : HttpSource() {

    override val name: String = "xkcd"
    override val baseUrl: String = "https://xkcd.com"
    override val lang: String = "en"
    override val supportsLatest: Boolean = false

    private fun infoUrl(num: Int? = null): String =
        if (num == null) "$baseUrl/info.0.json" else "$baseUrl/$num/info.0.json"

    private fun seriesFromLatest(json: JSONObject): SManga = SManga(
        url = SERIES_URL,
        title = "xkcd",
        author = "Randall Munroe",
        artist = "Randall Munroe",
        description = "A webcomic of romance, sarcasm, math, and language by Randall Munroe. " +
            "Freely readable, licensed CC BY-NC 2.5. Latest comic: #${json.getInt("num")} — " +
            json.optString("safe_title"),
        genre = listOf("Webcomic", "Humor"),
        status = SManga.STATUS_ONGOING,
        thumbnailUrl = json.optString("img").ifBlank { null },
        initialized = true,
    )

    // --- Browse: a single series ---

    override fun popularMangaRequest(page: Int): Request = GET(infoUrl())

    override fun popularMangaParse(response: Response): MangasPage =
        MangasPage(listOf(seriesFromLatest(JSONObject(response.body!!.string()))), hasNextPage = false)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = GET(infoUrl())

    override fun searchMangaParse(response: Response): MangasPage = popularMangaParse(response)

    // --- Details ---

    override fun mangaDetailsRequest(manga: SManga): Request = GET(infoUrl())

    override fun mangaDetailsParse(response: Response): SManga =
        seriesFromLatest(JSONObject(response.body!!.string()))

    // --- Chapters: one per comic, newest first ---

    override fun chapterListRequest(manga: SManga): Request = GET(infoUrl())

    override fun chapterListParse(response: Response): List<SChapter> {
        val latest = JSONObject(response.body!!.string()).getInt("num")
        return (latest downTo 1).map { n ->
            SChapter(
                url = "/$n/info.0.json",
                name = "#$n",
                chapterNumber = n.toFloat(),
                scanlator = "xkcd",
            )
        }
    }

    // --- Pages: the comic image (pageListRequest default = GET baseUrl + chapter.url) ---

    override fun pageListParse(response: Response): List<Page> {
        val json = JSONObject(response.body!!.string())
        val img = json.optString("img")
        if (img.isBlank()) return emptyList()
        return listOf(Page(index = 0, imageUrl = img))
    }

    companion object {
        private const val SERIES_URL = "/series/xkcd"
    }
}
