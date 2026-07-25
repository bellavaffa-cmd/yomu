package com.yomu.reader.source

import com.yomu.reader.network.NetworkModule
import com.yomu.reader.source.mangadex.MangaDexSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Registry of available sources — built-in sources plus any provided by loaded
 * extension APKs (see the extension loader). Exposes a [StateFlow] so the UI updates
 * when extensions are loaded or removed at runtime.
 */
class SourceManager {

    private val builtInSources: List<Source> = listOf(
        MangaDexSource(NetworkModule.mangaDexApi),
    )

    private val _sources = MutableStateFlow(builtInSources)
    val sources: StateFlow<List<Source>> = _sources.asStateFlow()

    @Volatile
    private var byId: Map<Long, Source> = builtInSources.associateBy { it.id }

    /** Replace the set of extension-provided sources (built-ins are always kept). */
    fun setExtensionSources(extensionSources: List<Source>) {
        val all = builtInSources + extensionSources
        byId = all.associateBy { it.id }
        _sources.value = all
    }

    fun get(id: Long): Source? = byId[id]

    fun getCatalogue(id: Long): CatalogueSource? = byId[id] as? CatalogueSource

    fun catalogueSources(): List<CatalogueSource> = _sources.value.filterIsInstance<CatalogueSource>()
}
