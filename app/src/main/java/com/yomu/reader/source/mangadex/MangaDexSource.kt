package com.yomu.reader.source.mangadex

import com.yomu.reader.source.CatalogueSource
import com.yomu.reader.source.model.FilterList
import com.yomu.reader.source.model.MangasPage
import com.yomu.reader.source.model.Page
import com.yomu.reader.source.model.SChapter
import com.yomu.reader.source.model.SManga
import java.time.Instant

/**
 * Built-in source backed by the public MangaDex API.
 * Content language is fixed to English for this first version.
 */
class MangaDexSource(private val api: MangaDexApi) : CatalogueSource {

    override val id: Long = ID
    override val name: String = "MangaDex"
    override val lang: String = "en"
    override val supportsLatest: Boolean = true

    private val pageSize = 24

    override suspend fun getPopularManga(page: Int): MangasPage {
        val res = api.getMangaList(
            limit = pageSize,
            offset = (page - 1) * pageSize,
            params = mapOf("order[followedCount]" to "desc"),
        )
        return res.toMangasPage()
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        val res = api.getMangaList(
            limit = pageSize,
            offset = (page - 1) * pageSize,
            params = mapOf("order[latestUploadedChapter]" to "desc"),
        )
        return res.toMangasPage()
    }

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
        if (query.isBlank()) return getPopularManga(page)
        val res = api.searchManga(
            title = query,
            limit = pageSize,
            offset = (page - 1) * pageSize,
        )
        return res.toMangasPage()
    }

    override suspend fun getMangaDetails(manga: SManga): SManga {
        val res = api.getManga(manga.url.toMangaId())
        return res.data?.toSManga() ?: manga.copy(initialized = true)
    }

    override suspend fun getChapterList(manga: SManga): List<SChapter> {
        val res = api.getChapterFeed(manga.url.toMangaId())
        return res.data
            .filter { it.attributes?.externalUrl == null && (it.attributes?.pages ?: 0) > 0 }
            .map { it.toSChapter() }
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val res = api.getAtHomeServer(chapter.url)
        val base = res.baseUrl ?: return emptyList()
        val hash = res.chapter?.hash ?: return emptyList()
        val files = res.chapter.data
        return files.mapIndexed { i, file ->
            Page(index = i, imageUrl = "$base/data/$hash/$file")
        }
    }

    override fun getFilterList(): FilterList = emptyList()

    // ---- Mapping helpers ----

    private fun MDListResponse.toMangasPage(): MangasPage {
        val list = data.map { it.toSManga() }
        val hasNext = offset + limit < total
        return MangasPage(list, hasNext)
    }

    private fun MDManga.toSManga(): SManga {
        val attr = attributes
        val cover = relationships.firstOrNull { it.type == "cover_art" }?.attributes?.fileName
        val authorName = relationships.firstOrNull { it.type == "author" }?.attributes?.name
        val artistName = relationships.firstOrNull { it.type == "artist" }?.attributes?.name
        return SManga(
            url = id,
            title = attr?.title.localized() ?: "Untitled",
            author = authorName,
            artist = artistName,
            description = attr?.description.localized(),
            genre = attr?.tags.orEmpty().mapNotNull { it.attributes?.name.localized() },
            status = attr?.status.toStatus(),
            thumbnailUrl = cover?.let { "${MangaDexApi.COVER_URL}/$id/$it.256.jpg" },
            initialized = attr?.description != null,
        )
    }

    private fun MDChapter.toSChapter(): SChapter {
        val attr = attributes
        val num = attr?.chapter?.toFloatOrNull() ?: -1f
        val scanlator = relationships.firstOrNull { it.type == "scanlation_group" }?.attributes?.name
        val label = buildString {
            if (attr?.chapter != null) append("Chapter ${attr.chapter}") else append("Oneshot")
            if (!attr?.title.isNullOrBlank()) append(" - ${attr!!.title}")
        }
        return SChapter(
            url = id,
            name = label,
            dateUpload = attr?.publishAt.parseDate(),
            chapterNumber = num,
            scanlator = scanlator,
        )
    }

    private fun Map<String, String>?.localized(): String? {
        if (this.isNullOrEmpty()) return null
        return this["en"] ?: this.values.firstOrNull()
    }

    private fun String?.toStatus(): Int = when (this) {
        "ongoing" -> SManga.STATUS_ONGOING
        "completed" -> SManga.STATUS_COMPLETED
        "hiatus" -> SManga.STATUS_ON_HIATUS
        "cancelled" -> SManga.STATUS_CANCELLED
        else -> SManga.STATUS_UNKNOWN
    }

    private fun String?.parseDate(): Long = try {
        if (this == null) 0L else Instant.parse(this).toEpochMilli()
    } catch (e: Exception) {
        0L
    }

    // Library manga store the raw UUID in url; tolerate a leading path just in case.
    private fun String.toMangaId(): String = substringAfterLast('/')

    companion object {
        const val ID = 2499283573021220255L
    }
}
