package com.yomu.reader.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomu.reader.data.DownloadProgress
import com.yomu.reader.data.MangaRepository
import com.yomu.reader.data.db.ChapterEntity
import com.yomu.reader.data.db.MangaEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MangaDetailViewModel(
    private val repository: MangaRepository,
    private val mangaId: Long,
) : ViewModel() {

    val manga: StateFlow<MangaEntity?> = repository.observeManga(mangaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val chapters: StateFlow<List<ChapterEntity>> = repository.observeChapters(mangaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadStates: StateFlow<Map<Long, DownloadProgress>> = repository.downloads.states
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        refresh()
        repository.downloads.syncManga(mangaId)
    }

    fun downloadChapter(chapterId: Long) = repository.downloads.downloadChapter(mangaId, chapterId)

    fun deleteDownload(chapterId: Long) = repository.downloads.deleteChapter(mangaId, chapterId)

    fun refresh() {
        viewModelScope.launch {
            _refreshing.value = true
            _error.value = null
            try {
                repository.refreshDetails(mangaId)
                repository.refreshChapters(mangaId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load"
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch { repository.toggleFavorite(mangaId) }
    }
}
