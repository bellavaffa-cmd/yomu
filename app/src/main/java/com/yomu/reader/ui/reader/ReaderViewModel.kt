package com.yomu.reader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomu.reader.data.MangaRepository
import com.yomu.reader.source.model.Page
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReaderState(
    val loading: Boolean = true,
    val pages: List<Page> = emptyList(),
    val chapterName: String = "",
    val error: String? = null,
)

class ReaderViewModel(
    private val repository: MangaRepository,
    private val mangaId: Long,
    private val chapterId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderState())
    val state: StateFlow<ReaderState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = ReaderState(loading = true)
            try {
                val chapter = repository.getChapter(chapterId)
                val pages = repository.getPages(mangaId, chapterId)
                _state.value = ReaderState(
                    loading = false,
                    pages = pages,
                    chapterName = chapter?.name ?: "",
                )
                // Opening a chapter records it in history immediately.
                repository.recordHistory(mangaId, chapterId)
            } catch (e: Exception) {
                _state.value = ReaderState(loading = false, error = e.message ?: "Failed to load pages")
            }
        }
    }

    /** Persist reading progress; mark read once the last page is reached. */
    fun onPageChanged(pageIndex: Int) {
        val total = _state.value.pages.size
        if (total == 0) return
        val reachedEnd = pageIndex >= total - 1
        viewModelScope.launch {
            repository.setChapterRead(chapterId, read = reachedEnd, lastPageRead = pageIndex)
        }
    }
}
