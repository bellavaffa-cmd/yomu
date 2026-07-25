package com.yomu.reader.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yomu.reader.data.MangaRepository
import com.yomu.reader.data.db.MangaEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(repository: MangaRepository) : ViewModel() {
    val library: StateFlow<List<MangaEntity>> = repository.observeLibrary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
