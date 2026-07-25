package com.yomu.reader.ui.updates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.yomu.reader.data.db.ChapterEntity
import com.yomu.reader.ui.rememberRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.DateFormat
import java.util.Date

class UpdatesViewModel(repository: MangaRepository) : ViewModel() {
    val updates: StateFlow<List<ChapterEntity>> = repository.observeRecentUpdates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@Composable
fun UpdatesScreen(
    contentPadding: PaddingValues,
    onChapterClick: (Long, Long) -> Unit,
) {
    val repository = rememberRepository()
    val vm: UpdatesViewModel = viewModel(
        factory = viewModelFactory { initializer { UpdatesViewModel(repository) } },
    )
    val updates by vm.updates.collectAsState()

    if (updates.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            Text(
                "No recent updates.\nAdd manga to your library to see new chapters here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
        items(updates, key = { it.id }) { chapter ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable { onChapterClick(chapter.mangaId, chapter.id) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    chapter.name,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (chapter.dateUpload > 0) {
                    Text(
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(chapter.dateUpload)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
