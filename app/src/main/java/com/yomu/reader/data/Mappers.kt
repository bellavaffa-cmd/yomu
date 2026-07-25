package com.yomu.reader.data

import com.yomu.reader.data.db.ChapterEntity
import com.yomu.reader.data.db.MangaEntity
import com.yomu.reader.source.model.SChapter
import com.yomu.reader.source.model.SManga

fun MangaEntity.toSManga(): SManga = SManga(
    url = url,
    title = title,
    author = author,
    artist = artist,
    description = description,
    genre = genre?.split(GENRE_SEP)?.filter { it.isNotBlank() } ?: emptyList(),
    status = status,
    thumbnailUrl = thumbnailUrl,
    initialized = initialized,
)

/** Merge fresh source details onto an existing library/db row, preserving local fields. */
fun MangaEntity.updatedWith(s: SManga): MangaEntity = copy(
    title = s.title.ifBlank { title },
    author = s.author ?: author,
    artist = s.artist ?: artist,
    description = s.description ?: description,
    genre = if (s.genre.isNotEmpty()) s.genre.joinToString(GENRE_SEP) else genre,
    status = if (s.status != SManga.STATUS_UNKNOWN) s.status else status,
    thumbnailUrl = s.thumbnailUrl ?: thumbnailUrl,
    initialized = true,
)

fun SManga.toNewEntity(source: Long): MangaEntity = MangaEntity(
    source = source,
    url = url,
    title = title,
    author = author,
    artist = artist,
    description = description,
    genre = genre.takeIf { it.isNotEmpty() }?.joinToString(GENRE_SEP),
    status = status,
    thumbnailUrl = thumbnailUrl,
    initialized = initialized,
)

fun ChapterEntity.toSChapter(): SChapter = SChapter(
    url = url,
    name = name,
    dateUpload = dateUpload,
    chapterNumber = chapterNumber,
    scanlator = scanlator,
)

fun SChapter.toNewEntity(mangaId: Long, fetchedAt: Long): ChapterEntity = ChapterEntity(
    mangaId = mangaId,
    url = url,
    name = name,
    scanlator = scanlator,
    chapterNumber = chapterNumber,
    dateUpload = dateUpload,
    dateFetch = fetchedAt,
)

private const val GENRE_SEP = ", "
