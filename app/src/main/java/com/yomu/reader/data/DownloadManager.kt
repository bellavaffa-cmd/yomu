package com.yomu.reader.data

import android.content.Context
import android.net.Uri
import com.yomu.reader.data.db.YomuDatabase
import com.yomu.reader.source.SourceManager
import com.yomu.reader.source.model.Page
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File

enum class DownloadStatus { QUEUED, DOWNLOADING, DOWNLOADED, ERROR }

data class DownloadProgress(
    val status: DownloadStatus,
    val done: Int = 0,
    val total: Int = 0,
)

/**
 * Downloads chapter pages to internal storage for offline reading and tracks progress.
 *
 * The filesystem is the source of truth: a chapter is "downloaded" once its directory
 * contains a `.complete` marker. This avoids any DB schema/migration and survives app
 * restarts. Downloads run on an internal scope so they continue across navigation.
 */
class DownloadManager(
    private val context: Context,
    private val db: YomuDatabase,
    private val sourceManager: SourceManager,
    private val client: OkHttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _states = MutableStateFlow<Map<Long, DownloadProgress>>(emptyMap())
    /** chapterId -> current download progress. */
    val states: StateFlow<Map<Long, DownloadProgress>> = _states.asStateFlow()

    private val baseDir: File get() = File(context.filesDir, "downloads")

    private fun chapterDir(mangaId: Long, chapterId: Long) =
        File(baseDir, "$mangaId/$chapterId")

    private fun marker(dir: File) = File(dir, ".complete")

    fun isDownloaded(mangaId: Long, chapterId: Long): Boolean =
        marker(chapterDir(mangaId, chapterId)).exists()

    /** Local pages for a downloaded chapter, or null if not fully downloaded. */
    fun localPages(mangaId: Long, chapterId: Long): List<Page>? {
        val dir = chapterDir(mangaId, chapterId)
        if (!marker(dir).exists()) return null
        val files = dir.listFiles { f -> f.name.endsWith(EXT) }?.sortedBy { it.name } ?: return null
        return files.mapIndexed { i, file ->
            Page(index = i, imageUrl = Uri.fromFile(file).toString())
        }
    }

    fun downloadChapter(mangaId: Long, chapterId: Long) {
        if (isDownloaded(mangaId, chapterId)) {
            updateState(chapterId, DownloadProgress(DownloadStatus.DOWNLOADED))
            return
        }
        val existing = _states.value[chapterId]?.status
        if (existing == DownloadStatus.QUEUED || existing == DownloadStatus.DOWNLOADING) return

        updateState(chapterId, DownloadProgress(DownloadStatus.QUEUED))
        scope.launch {
            try {
                val pages = remotePages(mangaId, chapterId)
                if (pages.isEmpty()) {
                    updateState(chapterId, DownloadProgress(DownloadStatus.ERROR))
                    return@launch
                }
                val dir = chapterDir(mangaId, chapterId).apply { mkdirs() }
                updateState(chapterId, DownloadProgress(DownloadStatus.DOWNLOADING, 0, pages.size))
                pages.forEachIndexed { index, page ->
                    val url = page.imageUrl ?: return@forEachIndexed
                    downloadImage(url, File(dir, "%03d%s".format(index, EXT)))
                    updateState(chapterId, DownloadProgress(DownloadStatus.DOWNLOADING, index + 1, pages.size))
                }
                marker(dir).createNewFile()
                updateState(chapterId, DownloadProgress(DownloadStatus.DOWNLOADED, pages.size, pages.size))
            } catch (e: Exception) {
                updateState(chapterId, DownloadProgress(DownloadStatus.ERROR))
            }
        }
    }

    fun deleteChapter(mangaId: Long, chapterId: Long) {
        scope.launch {
            chapterDir(mangaId, chapterId).deleteRecursively()
            _states.value = _states.value - chapterId
        }
    }

    /** Populate DOWNLOADED states for a manga's already-downloaded chapters. */
    fun syncManga(mangaId: Long) {
        scope.launch {
            val mangaDir = File(baseDir, "$mangaId")
            val dirs = mangaDir.listFiles()?.filter { it.isDirectory && marker(it).exists() } ?: emptyList()
            if (dirs.isEmpty()) return@launch
            val additions = dirs.mapNotNull { it.name.toLongOrNull() }
                .associateWith { DownloadProgress(DownloadStatus.DOWNLOADED) }
            _states.value = _states.value + additions
        }
    }

    /** All downloaded (mangaId, chapterId) pairs currently on disk. */
    suspend fun allDownloaded(): List<Pair<Long, Long>> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Pair<Long, Long>>()
        baseDir.listFiles()?.forEach mangaLoop@{ mangaDir ->
            val mangaId = mangaDir.name.toLongOrNull() ?: return@mangaLoop
            mangaDir.listFiles()?.forEach chapterLoop@{ chDir ->
                val chapterId = chDir.name.toLongOrNull() ?: return@chapterLoop
                if (marker(chDir).exists()) result += mangaId to chapterId
            }
        }
        result
    }

    private suspend fun remotePages(mangaId: Long, chapterId: Long): List<Page> {
        val manga = db.mangaDao().getById(mangaId) ?: return emptyList()
        val chapter = db.chapterDao().getById(chapterId) ?: return emptyList()
        val source = sourceManager.get(manga.source) ?: return emptyList()
        return source.getPageList(chapter.toSChapter())
    }

    private fun downloadImage(url: String, target: File) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body?.source() ?: error("empty body")
            target.sink().buffer().use { it.writeAll(body) }
        }
    }

    private fun updateState(chapterId: Long, progress: DownloadProgress) {
        _states.value = _states.value + (chapterId to progress)
    }

    companion object {
        private const val EXT = ".img"
    }
}
