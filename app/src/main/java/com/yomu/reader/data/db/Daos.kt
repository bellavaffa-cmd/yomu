package com.yomu.reader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaDao {
    @Query("SELECT * FROM manga WHERE favorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeLibrary(): Flow<List<MangaEntity>>

    @Query("SELECT * FROM manga WHERE id = :id")
    fun observeManga(id: Long): Flow<MangaEntity?>

    @Query("SELECT * FROM manga WHERE id = :id")
    suspend fun getById(id: Long): MangaEntity?

    @Query("SELECT * FROM manga WHERE source = :source AND url = :url LIMIT 1")
    suspend fun getByUrl(source: Long, url: String): MangaEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(manga: MangaEntity): Long

    @Update
    suspend fun update(manga: MangaEntity)

    @Query("UPDATE manga SET favorite = :favorite, dateAdded = :dateAdded WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean, dateAdded: Long)
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE mangaId = :mangaId ORDER BY chapterNumber DESC")
    fun observeForManga(mangaId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE mangaId = :mangaId ORDER BY chapterNumber DESC")
    suspend fun getForManga(mangaId: Long): List<ChapterEntity>

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getById(id: Long): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(chapters: List<ChapterEntity>): List<Long>

    @Update
    suspend fun update(chapter: ChapterEntity)

    @Query("UPDATE chapters SET read = :read, lastPageRead = :lastPageRead WHERE id = :id")
    suspend fun setReadState(id: Long, read: Boolean, lastPageRead: Int)

    @Query(
        """
        SELECT c.* FROM chapters c
        INNER JOIN manga m ON m.id = c.mangaId
        WHERE m.favorite = 1
        ORDER BY c.dateFetch DESC, c.dateUpload DESC
        LIMIT 200
        """
    )
    fun observeRecentUpdates(): Flow<List<ChapterEntity>>
}

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(history: HistoryEntity)

    @Query("DELETE FROM history WHERE mangaId = :mangaId")
    suspend fun deleteForManga(mangaId: Long)

    @Query(
        """
        SELECT h.id AS historyId, h.lastReadAt AS lastReadAt,
               m.id AS mangaId, m.title AS mangaTitle, m.thumbnailUrl AS thumbnailUrl,
               c.id AS chapterId, c.name AS chapterName, c.chapterNumber AS chapterNumber
        FROM history h
        INNER JOIN manga m ON m.id = h.mangaId
        INNER JOIN chapters c ON c.id = h.chapterId
        ORDER BY h.lastReadAt DESC
        LIMIT 200
        """
    )
    fun observeHistory(): Flow<List<HistoryWithRelations>>
}

data class HistoryWithRelations(
    val historyId: Long,
    val lastReadAt: Long,
    val mangaId: Long,
    val mangaTitle: String,
    val thumbnailUrl: String?,
    val chapterId: Long,
    val chapterName: String,
    val chapterNumber: Float,
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sort ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT COALESCE(MAX(sort), -1) + 1 FROM categories")
    suspend fun nextSort(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)

    // --- manga ↔ category links ---

    @Query("SELECT categoryId FROM manga_categories WHERE mangaId = :mangaId")
    fun observeCategoryIdsForManga(mangaId: Long): Flow<List<Long>>

    @Query("DELETE FROM manga_categories WHERE mangaId = :mangaId")
    suspend fun clearCategoriesForManga(mangaId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMangaCategories(rows: List<MangaCategoryEntity>)

    @Transaction
    suspend fun setCategoriesForManga(mangaId: Long, categoryIds: List<Long>) {
        clearCategoriesForManga(mangaId)
        insertMangaCategories(categoryIds.map { MangaCategoryEntity(mangaId, it) })
    }

    @Query(
        """
        SELECT m.* FROM manga m
        INNER JOIN manga_categories mc ON mc.mangaId = m.id
        WHERE m.favorite = 1 AND mc.categoryId = :categoryId
        ORDER BY m.title COLLATE NOCASE ASC
        """
    )
    fun observeLibraryInCategory(categoryId: Long): Flow<List<MangaEntity>>
}
