package com.yomu.reader.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yomu.reader.data.DownloadProgress
import com.yomu.reader.data.DownloadStatus
import com.yomu.reader.data.db.ChapterEntity
import com.yomu.reader.data.db.MangaEntity
import com.yomu.reader.source.model.SManga
import com.yomu.reader.ui.rememberRepository
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaDetailScreen(
    mangaId: Long,
    onBack: () -> Unit,
    onChapterClick: (Long, Long) -> Unit,
) {
    val repository = rememberRepository()
    val vm: MangaDetailViewModel = viewModel(
        factory = viewModelFactory { initializer { MangaDetailViewModel(repository, mangaId) } },
    )
    val manga by vm.manga.collectAsState()
    val chapters by vm.chapters.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val downloadStates by vm.downloadStates.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(manga?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            item {
                manga?.let { m ->
                    DetailHeader(m, onToggleFavorite = vm::toggleFavorite)
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${chapters.size} chapters",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.weight(1f))
                    AnimatedVisibility(refreshing) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                }
            }
            items(chapters, key = { it.id }) { chapter ->
                ChapterRow(
                    chapter = chapter,
                    progress = downloadStates[chapter.id],
                    onClick = { onChapterClick(mangaId, chapter.id) },
                    onDownload = { vm.downloadChapter(chapter.id) },
                    onDeleteDownload = { vm.deleteDownload(chapter.id) },
                )
            }
        }
    }
}

@Composable
private fun DetailHeader(manga: MangaEntity, onToggleFavorite: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(16.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(manga.thumbnailUrl).crossfade(true).build(),
                contentDescription = manga.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 110.dp, height = 160.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Column(Modifier.padding(start = 14.dp)) {
                Text(
                    manga.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                manga.author?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    statusLabel(manga.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(
            onClick = onToggleFavorite,
            colors = if (manga.favorite) {
                ButtonDefaults.buttonColors()
            } else {
                ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            },
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Icon(
                if (manga.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(if (manga.favorite) "In library" else "Add to library")
        }

        manga.description?.takeIf { it.isNotBlank() }?.let { desc ->
            Text(
                desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: ChapterEntity,
    progress: DownloadProgress?,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onDeleteDownload: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                chapter.name,
                color = if (chapter.read) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row {
                if (chapter.dateUpload > 0) {
                    Text(
                        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(chapter.dateUpload)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                chapter.scanlator?.let {
                    Text(
                        "  •  $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        DownloadControl(progress, onDownload, onDeleteDownload)
    }
}

@Composable
private fun DownloadControl(
    progress: DownloadProgress?,
    onDownload: () -> Unit,
    onDeleteDownload: () -> Unit,
) {
    when (progress?.status) {
        null -> IconButton(onClick = onDownload) {
            Icon(
                Icons.Filled.Download,
                contentDescription = "Download",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        DownloadStatus.QUEUED -> Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
        DownloadStatus.DOWNLOADING -> Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (progress.total > 0) {
                CircularProgressIndicator(
                    progress = { progress.done.toFloat() / progress.total },
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
        DownloadStatus.DOWNLOADED -> IconButton(onClick = onDeleteDownload) {
            Icon(
                Icons.Filled.DownloadDone,
                contentDescription = "Downloaded — tap to delete",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        DownloadStatus.ERROR -> IconButton(onClick = onDownload) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = "Download failed — tap to retry",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun statusLabel(status: Int): String = when (status) {
    SManga.STATUS_ONGOING -> "Ongoing"
    SManga.STATUS_COMPLETED -> "Completed"
    SManga.STATUS_ON_HIATUS -> "Hiatus"
    SManga.STATUS_CANCELLED -> "Cancelled"
    SManga.STATUS_PUBLISHING_FINISHED -> "Publishing finished"
    SManga.STATUS_LICENSED -> "Licensed"
    else -> "Unknown"
}
