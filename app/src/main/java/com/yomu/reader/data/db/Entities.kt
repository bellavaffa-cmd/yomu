package com.yomu.reader.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A manga saved in the local database. A row exists once a manga has been opened
 * (details cached) or added to the library ([favorite] = true).
 */
@Entity(
    tableName = "manga",
    indices = [Index(value = ["source", "url"], unique = true)],
)
data class MangaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: Long,
    val url: String,
    val title: String,
    val author: String? = null,
    val artist: String? = null,
    val description: String? = null,
    val genre: String? = null,
    val status: Int = 0,
    val thumbnailUrl: String? = null,
    val favorite: Boolean = false,
    val dateAdded: Long = 0,
    val lastUpdate: Long = 0,
    val initialized: Boolean = false,
)

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = MangaEntity::class,
            parentColumns = ["id"],
            childColumns = ["mangaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["mangaId", "url"], unique = true),
        Index(value = ["mangaId"]),
    ],
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mangaId: Long,
    val url: String,
    val name: String,
    val scanlator: String? = null,
    val chapterNumber: Float = -1f,
    val dateUpload: Long = 0,
    val dateFetch: Long = 0,
    val read: Boolean = false,
    val lastPageRead: Int = 0,
)

@Entity(
    tableName = "history",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["chapterId"], unique = true), Index(value = ["mangaId"])],
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mangaId: Long,
    val chapterId: Long,
    val lastReadAt: Long,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sort: Int = 0,
)

@Entity(
    tableName = "manga_categories",
    primaryKeys = ["mangaId", "categoryId"],
    indices = [Index(value = ["categoryId"])],
)
data class MangaCategoryEntity(
    val mangaId: Long,
    val categoryId: Long,
)
