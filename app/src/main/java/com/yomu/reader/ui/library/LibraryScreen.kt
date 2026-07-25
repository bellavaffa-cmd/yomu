package com.yomu.reader.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.compose.runtime.collectAsState
import com.yomu.reader.data.db.MangaEntity
import com.yomu.reader.ui.common.MangaCoverCard
import com.yomu.reader.ui.rememberRepository

@Composable
fun LibraryScreen(
    contentPadding: PaddingValues,
    onMangaClick: (Long) -> Unit,
) {
    val repository = rememberRepository()
    val vm: LibraryViewModel = viewModel(
        factory = viewModelFactory { initializer { LibraryViewModel(repository) } },
    )
    val library by vm.library.collectAsState()
    val categories by vm.categories.collectAsState()
    val selected by vm.selectedCategoryId.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        if (categories.isNotEmpty()) {
            // Tabs are [All] + one per category; index 0 == All (null selection).
            val selectedIndex = if (selected == null) 0 else categories.indexOfFirst { it.id == selected } + 1
            ScrollableTabRow(
                selectedTabIndex = selectedIndex.coerceAtLeast(0),
                edgePadding = 12.dp,
            ) {
                Tab(
                    selected = selectedIndex == 0,
                    onClick = { vm.selectCategory(null) },
                    text = { Text("All") },
                )
                categories.forEach { category ->
                    Tab(
                        selected = selected == category.id,
                        onClick = { vm.selectCategory(category.id) },
                        text = { Text(category.name) },
                    )
                }
            }
        }

        LibraryGrid(
            library = library,
            bottomPadding = contentPadding.calculateBottomPadding(),
            emptyMessage = if (categories.isNotEmpty() && selected != null) {
                "No manga in this category yet."
            } else {
                "Your library is empty.\nAdd manga from the Browse tab."
            },
            onMangaClick = onMangaClick,
        )
    }
}

@Composable
private fun LibraryGrid(
    library: List<MangaEntity>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    emptyMessage: String,
    onMangaClick: (Long) -> Unit,
) {
    if (library.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                emptyMessage,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(112.dp),
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = bottomPadding + 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(library, key = { it.id }) { manga ->
            MangaCoverCard(
                title = manga.title,
                thumbnailUrl = manga.thumbnailUrl,
                onClick = { onMangaClick(manga.id) },
            )
        }
    }
}
