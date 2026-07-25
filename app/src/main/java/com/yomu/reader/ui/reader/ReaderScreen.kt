package com.yomu.reader.ui.reader

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
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
    val vm: ReaderViewModel = viewModel(
        factory = viewModelFactory { initializer { ReaderViewModel(repository, mangaId, chapterId) } },
    )
    val state by vm.state.collectAsState()
    var uiVisible by remember { mutableStateOf(true) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            state.loading -> CircularProgressIndicator(
                Modifier.align(Alignment.Center),
                color = Color.White,
            )

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
                val pagerState = rememberPagerState(pageCount = { state.pages.size })

                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.currentPage }.collect { page ->
                        vm.onPageChanged(page)
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { index ->
                    val page = state.pages[index]
                    val zoomState = rememberZoomState()
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(page.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Page ${page.number}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .zoomable(
                                zoomState = zoomState,
                                onTap = { uiVisible = !uiVisible },
                            ),
                    )
                }

                // Page counter
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                ) {
                    Text(
                        "${pagerState.currentPage + 1} / ${state.pages.size}",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }

        if (uiVisible) {
            Surface(color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.TopStart)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Text(
                    state.chapterName,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                )
            }
        }
    }
}
