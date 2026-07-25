package com.yomu.reader.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomu.reader.data.MangaRepository
import com.yomu.reader.source.model.SManga
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SourceSearchResult(
    val sourceId: Long,
    val sourceName: String,
    val loading: Boolean = true,
    val mangas: List<SManga> = emptyList(),
    val error: String? = null,
)

class GlobalSearchViewModel(private val repository: MangaRepository) : ViewModel() {

    private val _results = MutableStateFlow<List<SourceSearchResult>>(emptyList())
    val results: StateFlow<List<SourceSearchResult>> = _results.asStateFlow()

    private val _hasSearched = MutableStateFlow(false)
    val hasSearched: StateFlow<Boolean> = _hasSearched.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        searchJob?.cancel()
        val sources = repository.sources.catalogueSources()
        _hasSearched.value = true
        _results.value = sources.map { SourceSearchResult(it.id, it.name, loading = true) }

        searchJob = viewModelScope.launch {
            // Fan out to every source concurrently; each updates its own row as it completes.
            sources.forEach { source ->
                launch {
                    val result = try {
                        val page = source.getSearchManga(1, trimmed, source.getFilterList())
                        SourceSearchResult(source.id, source.name, loading = false, mangas = page.mangas)
                    } catch (e: Exception) {
                        SourceSearchResult(source.id, source.name, loading = false, error = e.message ?: "Failed")
                    }
                    _results.value = _results.value.map { if (it.sourceId == source.id) result else it }
                }
            }
        }
    }

    /** Persist a result locally and return its id so the detail page can open it. */
    suspend fun openManga(sourceId: Long, sManga: SManga): Long = repository.getOrCreate(sourceId, sManga)
}
