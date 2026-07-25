package com.yomu.reader.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.extensionDataStore by preferencesDataStore(name = "extension_repos")

/**
 * Persists the set of extension-repo base URLs the user has added.
 *
 * Yomu ships with no default repo (like modern Mihon) — the user supplies repo URLs
 * and the app lists whatever extensions those repos publish.
 */
class ExtensionRepoStore(private val context: Context) {

    private val key = stringSetPreferencesKey("repo_urls")

    val repos: Flow<List<String>> = context.extensionDataStore.data
        .map { prefs -> prefs[key].orEmpty().sorted() }

    /** Current repo list snapshot (for backup). */
    suspend fun current(): List<String> = repos.first()

    suspend fun add(rawUrl: String) {
        val normalized = normalize(rawUrl) ?: return
        context.extensionDataStore.edit { prefs ->
            prefs[key] = prefs[key].orEmpty() + normalized
        }
    }

    suspend fun remove(url: String) {
        context.extensionDataStore.edit { prefs ->
            prefs[key] = prefs[key].orEmpty() - url
        }
    }

    /** Reduce user input to a repo base directory (the folder holding index.min.json). */
    private fun normalize(raw: String): String? {
        var url = raw.trim()
        if (url.isEmpty()) return null
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        url = url.trimEnd('/')
        if (url.endsWith("/index.min.json")) {
            url = url.removeSuffix("/index.min.json")
        } else if (url.endsWith("/index.json")) {
            url = url.removeSuffix("/index.json")
        }
        return url
    }
}
