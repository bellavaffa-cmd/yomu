package com.yomu.reader.ui.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yomu.reader.ui.common.MangaCoverCard
import com.yomu.reader.ui.rememberRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceBrowseScreen(
    sourceId: Long,
    onBack: () -> Unit,
    onMangaClick: (Long) -> Unit,
) {
    val repository = rememberRepository()
    val vm: SourceBrowseViewModel = viewModel(
        factory = viewModelFactory { initializer { SourceBrowseViewModel(repository, sourceId) } },
    )
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    // Infinite scroll: load more when the last item is close to becoming visible.
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.manga.size - 6 && state.hasNextPage && !state.loading
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) vm.loadNextPage()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.sourceName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.mode == BrowseMode.POPULAR,
                    onClick = { vm.setMode(BrowseMode.POPULAR) },
                    label = { Text("Popular") },
                )
                FilterChip(
                    selected = state.mode == BrowseMode.LATEST,
                    onClick = { vm.setMode(BrowseMode.LATEST) },
                    label = { Text("Latest") },
                )
                FilterChip(
                    selected = state.mode == BrowseMode.SEARCH,
                    onClick = { vm.setMode(BrowseMode.SEARCH) },
                    label = { Text("Search") },
                )
            }

            if (state.mode == BrowseMode.SEARCH) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { vm.search(it) },
                    placeholder = { Text("Search ${state.sourceName}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }

            Box(Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(112.dp),
                    contentPadding = PaddingValues(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(state.manga, key = { _, m -> m.url }) { _, manga ->
                        MangaCoverCard(
                            title = manga.title,
                            thumbnailUrl = manga.thumbnailUrl,
                            onClick = {
                                scope.launch {
                                    val id = vm.openManga(manga)
                                    onMangaClick(id)
                                }
                            },
                        )
                    }
                }

                if (state.loading && state.manga.isEmpty()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                state.error?.let { err ->
                    if (state.manga.isEmpty()) {
                        Text(
                            err,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        )
                    }
                }
            }
        }
    }
}
