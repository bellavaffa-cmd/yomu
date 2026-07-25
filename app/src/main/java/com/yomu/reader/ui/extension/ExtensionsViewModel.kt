package com.yomu.reader.ui.extension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomu.reader.data.ExtensionRepoStore
import com.yomu.reader.extension.AvailableExtension
import com.yomu.reader.extension.ExtensionManager
import com.yomu.reader.extension.LoadedExtension
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class ExtensionsUiState(
    val loading: Boolean = false,
    val repoCount: Int = 0,
    val query: String = "",
    val all: List<AvailableExtension> = emptyList(),
    val installed: List<LoadedExtension> = emptyList(),
    val errors: List<String> = emptyList(),
    val installing: Set<String> = emptySet(),
) {
    val visible: List<AvailableExtension>
        get() = if (query.isBlank()) all else all.filter {
            it.displayName.contains(query, true) || it.lang.contains(query, true)
        }
}

class ExtensionsViewModel(
    private val manager: ExtensionManager,
    private val repoStore: ExtensionRepoStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ExtensionsUiState())
    val state: StateFlow<ExtensionsUiState> = _state.asStateFlow()

    private var currentRepos: List<String> = emptyList()

    init {
        viewModelScope.launch {
            repoStore.repos.collectLatest { repos ->
                currentRepos = repos
                _state.value = _state.value.copy(repoCount = repos.size)
                load(repos)
            }
        }
        viewModelScope.launch {
            manager.installed.collect { installed ->
                _state.value = _state.value.copy(installed = installed)
            }
        }
        reloadInstalled()
    }

    /** Re-scan installed extension APKs (e.g. after installing one). */
    fun reloadInstalled() = viewModelScope.launch { manager.loadInstalledExtensions() }

    fun refresh() = viewModelScope.launch {
        reloadInstalled()
        load(currentRepos)
    }

    fun setQuery(q: String) {
        _state.value = _state.value.copy(query = q)
    }

    private suspend fun load(repos: List<String>) {
        if (repos.isEmpty()) {
            _state.value = _state.value.copy(loading = false, all = emptyList(), errors = emptyList())
            return
        }
        _state.value = _state.value.copy(loading = true, errors = emptyList())
        val result = manager.fetchExtensions(repos)
        _state.value = _state.value.copy(
            loading = false,
            all = result.extensions,
            errors = result.errors,
        )
    }

    fun install(ext: AvailableExtension) {
        viewModelScope.launch {
            _state.value = _state.value.copy(installing = _state.value.installing + ext.pkg)
            try {
                manager.downloadAndInstall(ext)
            } catch (_: Exception) {
                // Surface nothing intrusive; the row simply returns to its prior state.
            } finally {
                _state.value = _state.value.copy(installing = _state.value.installing - ext.pkg)
            }
        }
    }
}
