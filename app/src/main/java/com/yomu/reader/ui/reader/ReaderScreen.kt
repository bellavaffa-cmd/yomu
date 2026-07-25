package com.yomu.reader.ui.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yomu.reader.data.ReadingMode
import com.yomu.reader.source.model.Page
import com.yomu.reader.ui.rememberAppPreferences
import com.yomu.reader.ui.rememberRepository
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    mangaId: Long,
    chapterId: Long,
    onBack: () -> Unit,
) {
    val repository = rememberRepository()
    val preferences = rememberAppPreferences()
    val vm: ReaderViewModel = viewModel(
        factory = viewModelFactory {
            initializer { ReaderViewModel(repository, preferences, mangaId, chapterId) }
        },
    )
    val state by vm.state.collectAsState()
    val mode by vm.readingMode.collectAsState()
    var uiVisible by remember { mutableStateOf(true) }
    var currentPage by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.initialPage, state.pages.size) { currentPage = state.initialPage }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)

            state.error != null -> Text(
                state.error!!,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )

            state.pages.isEmpty() -> Text(
                "No pages found",
                color = Color.White,
                modifier = Modifier.align(Alignment.Center),
            )

            else -> {
                val onPageChanged: (Int) -> Unit = { p ->
                    currentPage = p
                    vm.onPageChanged(p)
                }
                val toggleUi = { uiVisible = !uiVisible }
                if (mode == ReadingMode.WEBTOON) {
                    WebtoonReader(state.pages, state.initialPage, onPageChanged, toggleUi)
                } else {
                    PagedReader(state.pages, state.initialPage, onPageChanged, toggleUi)
                }
            }
        }

        if (state.pages.isNotEmpty()) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            ) {
                Text(
                    "${currentPage + 1} / ${state.pages.size}",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }

        if (uiVisible) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        state.chapterName,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    )
                    IconButton(onClick = { vm.toggleReadingMode() }) {
                        Icon(
                            if (mode == ReadingMode.WEBTOON) Icons.Filled.ViewDay else Icons.Filled.ViewCarousel,
                            contentDescription = if (mode == ReadingMode.WEBTOON) "Webtoon mode (tap for paged)"
                                else "Paged mode (tap for webtoon)",
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PagedReader(
    pages: List<Page>,
    initialPage: Int,
    onPageChanged: (Int) -> Unit,
    onTap: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { pages.size }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect(onPageChanged)
    }

    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
        val zoomState = rememberZoomState()
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(pages[index].imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Page ${index + 1}",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .zoomable(zoomState = zoomState, onTap = { onTap() }),
        )
    }
}

@Composable
private fun WebtoonReader(
    pages: List<Page>,
    initialPage: Int,
    onPageChanged: (Int) -> Unit,
    onTap: () -> Unit,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect(onPageChanged)
    }

    val interaction = remember { MutableInteractionSource() }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(pages, key = { it.index }) { page ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(page.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Page ${page.number}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
                    .clickable(interactionSource = interaction, indication = null) { onTap() },
            )
        }
    }
}
