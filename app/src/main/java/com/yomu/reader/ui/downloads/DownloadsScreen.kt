package com.yomu.reader.ui.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yomu.reader.data.MangaRepository
import com.yomu.reader.ui.rememberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DownloadedChapter(
    val mangaId: Long,
    val chapterId: Long,
    val mangaTitle: String,
    val chapterName: String,
)

class DownloadsViewModel(private val repository: MangaRepository) : ViewModel() {

    private val _items = MutableStateFlow<List<DownloadedChapter>>(emptyList())
    val items: StateFlow<List<DownloadedChapter>> = _items.asStateFlow()

    init {
        // Reload whenever downloads change (e.g. a new chapter finishes).
        viewModelScope.launch {
            repository.downloads.states.collect { reload() }
        }
    }

    private suspend fun reload() {
        val rows = repository.downloads.allDownloaded().mapNotNull { (mangaId, chapterId) ->
            val manga = repository.getManga(mangaId) ?: return@mapNotNull null
            val chapter = repository.getChapter(chapterId) ?: return@mapNotNull null
            DownloadedChapter(mangaId, chapterId, manga.title, chapter.name)
        }.sortedWith(compareBy({ it.mangaTitle.lowercase() }, { it.chapterName }))
        _items.value = rows
    }

    fun delete(item: DownloadedChapter) {
        repository.downloads.deleteChapter(item.mangaId, item.chapterId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onBack: () -> Unit,
    onOpenChapter: (Long, Long) -> Unit,
) {
    val repository = rememberRepository()
    val vm: DownloadsViewModel = viewModel(
        factory = viewModelFactory { initializer { DownloadsViewModel(repository) } },
    )
    val items by vm.items.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    "No downloaded chapters.\nDownload chapters from a manga's page to read offline.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            items(items, key = { it.chapterId }) { item ->
                androidx.compose.foundation.layout.Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenChapter(item.mangaId, item.chapterId) }
                        .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.mangaTitle,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            item.chapterName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { vm.delete(item) }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete download",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
