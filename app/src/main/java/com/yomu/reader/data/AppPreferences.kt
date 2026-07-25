package com.yomu.reader.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** How the reader lays out pages. */
enum class ReadingMode {
    /** Horizontal, one page per swipe. */
    PAGED,

    /** Continuous vertical scroll (webtoon/long-strip). */
    WEBTOON,
}

private val Context.appPrefsDataStore by preferencesDataStore(name = "app_prefs")

/** App-wide user preferences. */
class AppPreferences(private val context: Context) {

    private val readingModeKey = stringPreferencesKey("reading_mode")

    val readingMode: Flow<ReadingMode> = context.appPrefsDataStore.data.map { prefs ->
        runCatching { ReadingMode.valueOf(prefs[readingModeKey] ?: ReadingMode.PAGED.name) }
            .getOrDefault(ReadingMode.PAGED)
    }

    suspend fun setReadingMode(mode: ReadingMode) {
        context.appPrefsDataStore.edit { it[readingModeKey] = mode.name }
    }

    private val lastDriveSyncKey = longPreferencesKey("last_drive_sync")

    val lastDriveSync: Flow<Long> = context.appPrefsDataStore.data.map { it[lastDriveSyncKey] ?: 0L }

    suspend fun setLastDriveSync(timestamp: Long) {
        context.appPrefsDataStore.edit { it[lastDriveSyncKey] = timestamp }
    }
}
