package com.yomu.reader.ui.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yomu.reader.ui.rememberRepository

@Composable
fun MoreScreen(contentPadding: PaddingValues) {
    val repository = rememberRepository()
    val sourceCount = repository.sources.catalogueSources().size

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
        ListItem(
            headlineContent = { Text("Sources") },
            supportingContent = { Text("$sourceCount installed") },
            leadingContent = { Icon(Icons.Filled.Source, contentDescription = null) },
        )
        ListItem(
            headlineContent = { Text("About") },
            supportingContent = { Text("Yomu v0.1.0 — a Mihon-style manga reader") },
            leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Downloads, trackers, categories and external extension support are planned for future versions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}
