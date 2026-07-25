package com.yomu.reader.extension.sample

import com.yomu.reader.source.CatalogueSource
import com.yomu.reader.source.Source
import com.yomu.reader.source.SourceFactory
import com.yomu.reader.source.model.FilterList
import com.yomu.reader.source.model.MangasPage
import com.yomu.reader.source.model.Page
import com.yomu.reader.source.model.SChapter
import com.yomu.reader.source.model.SManga

/**
 * Entry point Yomu's loader instantiates (declared in the manifest metadata).
 */
class DemoSourceFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(DemoSource())
}

/**
 * A fully-functional demo source with placeholder content. Proves the extension
 * pipeline (browse → detail → chapters → reader) end to end without touching any
 * copyrighted material — pages are deterministic placeholder images.
 */
class DemoSource : CatalogueSource {

    override val id: Long = 6820135790102938471L
    override val name: String = "Yomu Demo"
    override val lang: String = "en"
    override val supportsLatest: Boolean = true

    private val titles = listOf(
        "Azure Horizon", "Crimson Petals", "Neon Drifters",
        "Paper Lanterns", "Silent Vale", "Tidal Echoes",
    )

    private fun buildManga(i: Int): SManga = SManga(
        url = "demo/$i",
        title = titles[i % titles.size],
        author = "Demo Studio",
        artist = "Demo Studio",
        description = "A sample series from the Yomu Demo extension, used to demonstrate the " +
            "extension loader. All pages are placeholder images — no real manga content.",
        genre = listOf("Demo", "Placeholder"),
        status = SManga.STATUS_ONGOING,
        thumbnailUrl = "https://picsum.photos/seed/yomu-demo-$i-cover/300/450",
        initialized = true,
    )

    private fun catalogue(): MangasPage =
        MangasPage(titles.indices.map { buildManga(it) }, hasNextPage = false)

    override suspend fun getPopularManga(page: Int): MangasPage = catalogue()

    override suspend fun getLatestUpdates(page: Int): MangasPage = catalogue()

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
        val filtered = titles.indices.map { buildManga(it) }
            .filter { it.title.contains(query, ignoreCase = true) }
        return MangasPage(filtered, hasNextPage = false)
    }

    override suspend fun getMangaDetails(manga: SManga): SManga {
        val i = manga.url.substringAfterLast('/').toIntOrNull() ?: 0
        return buildManga(i)
    }

    override suspend fun getChapterList(manga: SManga): List<SChapter> {
        val i = manga.url.substringAfterLast('/').toIntOrNull() ?: 0
        return (12 downTo 1).map { n ->
            SChapter(
                url = "demo/$i/$n",
                name = "Chapter $n",
                chapterNumber = n.toFloat(),
                scanlator = "Demo",
            )
        }
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val parts = chapter.url.split('/')
        val i = parts.getOrNull(1) ?: "0"
        val n = parts.getOrNull(2) ?: "1"
        return (1..8).map { p ->
            Page(index = p - 1, imageUrl = "https://picsum.photos/seed/yomu-demo-$i-$n-$p/800/1200")
        }
    }
}
