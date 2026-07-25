package com.yomu.reader.data

import com.yomu.reader.data.db.CategoryEntity
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
    private val downloadManager: DownloadManager,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val mangaDao get() = db.mangaDao()
    private val chapterDao get() = db.chapterDao()
    private val historyDao get() = db.historyDao()
    private val categoryDao get() = db.categoryDao()

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
        // Offline-first: serve downloaded pages when present.
        downloadManager.localPages(mangaId, chapterId)?.let { return it }
        val manga = mangaDao.getById(mangaId) ?: return emptyList()
        val chapter = chapterDao.getById(chapterId) ?: return emptyList()
        val source = sourceManager.get(manga.source) ?: return emptyList()
        return source.getPageList(chapter.toSChapter())
    }

    val downloads get() = downloadManager

    // --- Categories ---

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    fun observeLibraryInCategory(categoryId: Long): Flow<List<MangaEntity>> =
        categoryDao.observeLibraryInCategory(categoryId)

    fun observeCategoryIdsForManga(mangaId: Long): Flow<List<Long>> =
        categoryDao.observeCategoryIdsForManga(mangaId)

    suspend fun createCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        categoryDao.insert(CategoryEntity(name = trimmed, sort = categoryDao.nextSort()))
    }

    suspend fun renameCategory(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) categoryDao.rename(id, trimmed)
    }

    suspend fun deleteCategory(id: Long) = categoryDao.delete(id)

    suspend fun setMangaCategories(mangaId: Long, categoryIds: List<Long>) =
        categoryDao.setCategoriesForManga(mangaId, categoryIds)

    // --- Sync (backup / restore) ---

    /** Snapshot the library + categories for backup. */
    suspend fun exportLibrary(): Pair<List<String>, List<MangaSnapshot>> {
        val categories = categoryDao.getAll()
        val nameById = categories.associate { it.id to it.name }
        val favorites = mangaDao.getFavorites()
        val snapshots = favorites.map { m ->
            val categoryNames = categoryDao.getCategoryIdsForManga(m.id).mapNotNull { nameById[it] }
            MangaSnapshot(
                source = m.source,
                url = m.url,
                title = m.title,
                author = m.author,
                artist = m.artist,
                description = m.description,
                genre = m.genre?.split(", ")?.filter { it.isNotBlank() } ?: emptyList(),
                status = m.status,
                thumbnailUrl = m.thumbnailUrl,
                categories = categoryNames,
            )
        }
        return categories.map { it.name } to snapshots
    }

    /** Apply a restored snapshot: recreate categories, favorite the manga, reassign categories. */
    suspend fun importLibrary(categoryNames: List<String>, mangas: List<MangaSnapshot>) {
        val nameToId = categoryDao.getAll().associate { it.name to it.id }.toMutableMap()
        for (name in categoryNames) {
            if (name !in nameToId) {
                val id = categoryDao.insert(CategoryEntity(name = name, sort = categoryDao.nextSort()))
                if (id > 0) nameToId[name] = id
                else categoryDao.getAll().firstOrNull { it.name == name }?.let { nameToId[name] = it.id }
            }
        }
        for (snap in mangas) {
            val sManga = SManga(
                url = snap.url,
                title = snap.title,
                author = snap.author,
                artist = snap.artist,
                description = snap.description,
                genre = snap.genre,
                status = snap.status,
                thumbnailUrl = snap.thumbnailUrl,
                initialized = snap.description != null,
            )
            val mangaId = getOrCreate(snap.source, sManga)
            mangaDao.setFavorite(mangaId, true, now())
            val ids = snap.categories.mapNotNull { nameToId[it] }
            categoryDao.setCategoriesForManga(mangaId, ids)
        }
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
