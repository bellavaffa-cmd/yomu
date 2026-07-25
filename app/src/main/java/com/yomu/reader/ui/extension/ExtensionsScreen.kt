package com.yomu.reader.ui.extension

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yomu.reader.extension.AvailableExtension
import com.yomu.reader.extension.InstallState
import com.yomu.reader.extension.LoadedExtension
import com.yomu.reader.ui.rememberExtensionManager
import com.yomu.reader.ui.rememberExtensionRepoStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    onBack: () -> Unit,
    onManageRepos: () -> Unit,
) {
    val manager = rememberExtensionManager()
    val repoStore = rememberExtensionRepoStore()
    val vm: ExtensionsViewModel = viewModel(
        factory = viewModelFactory { initializer { ExtensionsViewModel(manager, repoStore) } },
    )
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.reloadInstalled() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extensions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onManageRepos) {
                        Icon(Icons.Filled.Settings, contentDescription = "Manage repos")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.repoCount > 0) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = vm::setQuery,
                    placeholder = { Text("Search extensions") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.installed.isNotEmpty()) {
                    item { SectionHeader("Installed") }
                    items(state.installed, key = { it.pkg }) { ext ->
                        InstalledRow(ext)
                    }
                }

                item { SectionHeader("Available") }

                when {
                    state.repoCount == 0 -> item { EmptyRepos(onManageRepos) }
                    state.loading && state.all.isEmpty() -> item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    state.all.isEmpty() -> item {
                        Text(
                            if (state.errors.isNotEmpty()) state.errors.joinToString("\n")
                            else "No extensions found in your repos.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                        )
                    }
                    else -> {
                        items(state.errors) { err ->
                            Text(
                                "⚠ $err",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                        items(state.visible, key = { it.pkg }) { ext ->
                            ExtensionRow(
                                ext = ext,
                                installing = ext.pkg in state.installing,
                                onInstall = { vm.install(ext) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun InstalledRow(ext: LoadedExtension) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                ext.name.removePrefix("Yomu: "),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "v${ext.versionName} • ${ext.sources.joinToString { it.name }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ExtensionRow(
    ext: AvailableExtension,
    installing: Boolean,
    onInstall: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(ext.iconUrl).crossfade(true).build(),
            contentDescription = ext.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                ext.displayName,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    append(ext.lang.uppercase())
                    append(" • v${ext.versionName}")
                    if (ext.sourceCount > 1) append(" • ${ext.sourceCount} sources")
                    if (ext.nsfw) append(" • 18+")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        InstallButton(ext.installState, installing, onInstall)
    }
}

@Composable
private fun InstallButton(state: InstallState, installing: Boolean, onInstall: () -> Unit) {
    if (installing) {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        return
    }
    when (state) {
        InstallState.NOT_INSTALLED -> Button(onClick = onInstall) { Text("Install") }
        InstallState.UPDATE_AVAILABLE -> Button(onClick = onInstall) { Text("Update") }
        InstallState.INSTALLED -> OutlinedButton(onClick = onInstall, enabled = false) { Text("Installed") }
    }
}

@Composable
private fun EmptyRepos(onManageRepos: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Extension,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No extension repos added",
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Yomu lists extensions from repos you add. Paste a Tachiyomi/Mihon-compatible " +
                "repo URL (the folder containing index.min.json) to see everything it offers.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onManageRepos) { Text("Add extension repo") }
    }
}
