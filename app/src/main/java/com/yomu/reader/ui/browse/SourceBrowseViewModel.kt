package com.yomu.reader.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomu.reader.data.MangaRepository
import com.yomu.reader.source.CatalogueSource
import com.yomu.reader.source.model.SManga
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class BrowseMode { POPULAR, LATEST, SEARCH }

data class SourceBrowseState(
    val sourceName: String = "",
    val mode: BrowseMode = BrowseMode.POPULAR,
    val query: String = "",
    val manga: List<SManga> = emptyList(),
    val page: Int = 1,
    val hasNextPage: Boolean = true,
    val loading: Boolean = false,
    val error: String? = null,
)

class SourceBrowseViewModel(
    private val repository: MangaRepository,
    private val sourceId: Long,
) : ViewModel() {

    private val source: CatalogueSource? = repository.sources.getCatalogue(sourceId)

    private val _state = MutableStateFlow(
        SourceBrowseState(sourceName = source?.name ?: "Unknown source"),
    )
    val state: StateFlow<SourceBrowseState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        reset(BrowseMode.POPULAR, "")
    }

    fun setMode(mode: BrowseMode) {
        if (mode == _state.value.mode && mode != BrowseMode.SEARCH) return
        reset(mode, if (mode == BrowseMode.SEARCH) _state.value.query else "")
    }

    fun search(query: String) {
        _state.value = _state.value.copy(query = query)
        reset(BrowseMode.SEARCH, query)
    }

    private fun reset(mode: BrowseMode, query: String) {
        loadJob?.cancel()
        _state.value = _state.value.copy(
            mode = mode, query = query, manga = emptyList(),
            page = 1, hasNextPage = true, error = null,
        )
        loadPage(1)
    }

    fun loadNextPage() {
        val s = _state.value
        if (s.loading || !s.hasNextPage) return
        loadPage(s.page)
    }

    private fun loadPage(page: Int) {
        val src = source ?: run {
            _state.value = _state.value.copy(error = "Source unavailable")
            return
        }
        loadJob = viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val result = when (_state.value.mode) {
                    BrowseMode.POPULAR -> src.getPopularManga(page)
                    BrowseMode.LATEST -> src.getLatestUpdates(page)
                    BrowseMode.SEARCH -> src.getSearchManga(page, _state.value.query, src.getFilterList())
                }
                _state.value = _state.value.copy(
                    manga = _state.value.manga + result.mangas,
                    page = page + 1,
                    hasNextPage = result.hasNextPage,
                    loading = false,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Failed to load")
            }
        }
    }

    /** Persist a browsed manga locally and return its id so we can open its detail page. */
    suspend fun openManga(sManga: SManga): Long = repository.getOrCreate(sourceId, sManga)
}
