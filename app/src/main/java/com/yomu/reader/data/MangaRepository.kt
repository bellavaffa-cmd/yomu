package com.yomu.reader.data

import com.yomu.reader.data.db.ChapterEntity
import com.yomu.reader.data.db.HistoryEntity
import com.yomu.reader.data.db.HistoryWithRelations
import com.yomu.reader.data.db.MangaEntity
import com.yomu.reader.data.db.YomuDatabase
import com.yomu.reader.source.SourceManager
import com.yomu.reader.source.model.Page
import com.yomu.reader.source.model.SManga
import kotlinx.coroutines.flow.Flow

/**
 * Central repository. Bridges remote [SourceManager] calls with the local Room cache,
 * so the UI only ever talks to the database (single source of truth).
 */
class MangaRepository(
    private val db: YomuDatabase,
    private val sourceManager: SourceManager,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val mangaDao get() = db.mangaDao()
    private val chapterDao get() = db.chapterDao()
    private val historyDao get() = db.historyDao()

    val sources get() = sourceManager

    // --- Library ---

    fun observeLibrary(): Flow<List<MangaEntity>> = mangaDao.observeLibrary()

    fun observeManga(id: Long): Flow<MangaEntity?> = mangaDao.observeManga(id)

    fun observeChapters(mangaId: Long): Flow<List<ChapterEntity>> = chapterDao.observeForManga(mangaId)

    fun observeRecentUpdates(): Flow<List<ChapterEntity>> = chapterDao.observeRecentUpdates()

    fun observeHistory(): Flow<List<HistoryWithRelations>> = historyDao.observeHistory()

    suspend fun getManga(id: Long): MangaEntity? = mangaDao.getById(id)

    suspend fun getChapter(id: Long): ChapterEntity? = chapterDao.getById(id)

    /** Ensure a db row exists for a browsed manga; returns its local id. */
    suspend fun getOrCreate(source: Long, sManga: SManga): Long {
        val existing = mangaDao.getByUrl(source, sManga.url)
        if (existing != null) return existing.id
        val rowId = mangaDao.insert(sManga.toNewEntity(source))
        // If insert was ignored (race), fall back to a lookup.
        return if (rowId > 0) rowId else mangaDao.getByUrl(source, sManga.url)!!.id
    }

    suspend fun toggleFavorite(mangaId: Long): Boolean {
        val manga = mangaDao.getById(mangaId) ?: return false
        val newState = !manga.favorite
        mangaDao.setFavorite(mangaId, newState, if (newState) now() else manga.dateAdded)
        return newState
    }

    /** Fetch fresh details from the source and persist them. */
    suspend fun refreshDetails(mangaId: Long) {
        val entity = mangaDao.getById(mangaId) ?: return
        val source = sourceManager.get(entity.source) ?: return
        val fresh = source.getMangaDetails(entity.toSManga())
        mangaDao.update(entity.updatedWith(fresh))
    }

    /** Fetch the chapter list from the source and insert any new chapters. */
    suspend fun refreshChapters(mangaId: Long) {
        val entity = mangaDao.getById(mangaId) ?: return
        val source = sourceManager.get(entity.source) ?: return
        val remote = source.getChapterList(entity.toSManga())
        if (remote.isEmpty()) return
        val existingUrls = chapterDao.getForManga(mangaId).map { it.url }.toHashSet()
        val fetchedAt = now()
        val newOnes = remote.filter { it.url !in existingUrls }
            .map { it.toNewEntity(mangaId, fetchedAt) }
        if (newOnes.isNotEmpty()) chapterDao.insertAll(newOnes)
        mangaDao.update(entity.copy(lastUpdate = fetchedAt))
    }

    suspend fun getPages(mangaId: Long, chapterId: Long): List<Page> {
        val manga = mangaDao.getById(mangaId) ?: return emptyList()
        val chapter = chapterDao.getById(chapterId) ?: return emptyList()
        val source = sourceManager.get(manga.source) ?: return emptyList()
        return source.getPageList(chapter.toSChapter())
    }

    // --- Read state / history ---

    suspend fun setChapterRead(chapterId: Long, read: Boolean, lastPageRead: Int) {
        val chapter = chapterDao.getById(chapterId) ?: return
        chapterDao.setReadState(chapterId, read, lastPageRead.coerceAtLeast(chapter.lastPageRead))
    }

    suspend fun recordHistory(mangaId: Long, chapterId: Long) {
        historyDao.upsert(HistoryEntity(mangaId = mangaId, chapterId = chapterId, lastReadAt = now()))
    }
}
