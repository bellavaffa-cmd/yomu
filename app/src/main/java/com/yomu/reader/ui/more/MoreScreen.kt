package com.yomu.reader.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.yomu.reader.BuildConfig
import com.yomu.reader.ui.rememberRepository
import com.yomu.reader.ui.rememberUpdateManager

@Composable
fun MoreScreen(
    contentPadding: PaddingValues,
    onOpenDownloads: () -> Unit,
    onOpenCategories: () -> Unit,
) {
    val repository = rememberRepository()
    val updateManager = rememberUpdateManager()
    val sourceCount = repository.sources.catalogueSources().size
    val vm: MoreViewModel = viewModel(
        factory = viewModelFactory { initializer { MoreViewModel(updateManager) } },
    )
    val updateState by vm.update.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            "Yomu",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(20.dp),
        )

        val update = updateState.update
        if (update != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Update available — v${update.versionName}",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "You have v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(12.dp))
                    if (updateState.downloading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(
                                "Downloading…",
                                modifier = Modifier.padding(start = 12.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    } else {
                        Button(onClick = { vm.install() }) { Text("Download & install") }
                    }
                    updateState.error?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        } else {
            ListItem(
                headlineContent = { Text("Check for updates") },
                supportingContent = {
                    Text(
                        when {
                            updateState.checking -> "Checking…"
                            updateState.error != null -> updateState.error!!
                            updateState.checked -> "Up to date (v${BuildConfig.VERSION_NAME})"
                            else -> "Tap to check"
                        },
                    )
                },
                leadingContent = { Icon(Icons.Filled.SystemUpdate, contentDescription = null) },
                trailingContent = {
                    if (updateState.checking) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                },
                modifier = Modifier.clickable { vm.checkForUpdate() },
            )
        }

        ListItem(
            headlineContent = { Text("Downloads") },
            supportingContent = { Text("Read chapters offline") },
            leadingContent = { Icon(Icons.Filled.Download, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onOpenDownloads),
        )
        ListItem(
            headlineContent = { Text("Categories") },
            supportingContent = { Text("Organise your library into tabs") },
            leadingContent = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onOpenCategories),
        )
        ListItem(
            headlineContent = { Text("Sources") },
            supportingContent = { Text("$sourceCount installed") },
            leadingContent = { Icon(Icons.Filled.Source, contentDescription = null) },
        )
        ListItem(
            headlineContent = { Text("About") },
            supportingContent = { Text("Yomu v${BuildConfig.VERSION_NAME} — a Mihon-style manga reader") },
            leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Trackers and per-source preferences are planned for future versions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}
