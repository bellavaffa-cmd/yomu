package com.yomu.reader

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.yomu.reader.data.AppPreferences
import com.yomu.reader.data.DownloadManager
import com.yomu.reader.data.ExtensionRepoStore
import com.yomu.reader.data.MangaRepository
import com.yomu.reader.data.db.YomuDatabase
import com.yomu.reader.extension.ExtensionManager
import com.yomu.reader.network.NetworkModule
import com.yomu.reader.source.ExtensionDependencies
import com.yomu.reader.source.SourceManager
import com.yomu.reader.sync.DriveSyncManager
import com.yomu.reader.update.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * App-wide dependency container. No DI framework — plain lazy singletons hung off
 * the Application, matching the project's other apps.
 *
 * Also supplies Coil's ImageLoader so page/cover requests reuse our OkHttp client
 * (and its required User-Agent header) when hitting MangaDex image nodes.
 */
class YomuApplication : Application(), ImageLoaderFactory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: YomuDatabase by lazy { YomuDatabase.get(this) }
    val sourceManager: SourceManager by lazy { SourceManager() }
    val downloadManager: DownloadManager by lazy {
        DownloadManager(this, database, sourceManager, NetworkModule.okHttpClient)
    }
    val repository: MangaRepository by lazy { MangaRepository(database, sourceManager, downloadManager) }
    val extensionRepoStore: ExtensionRepoStore by lazy { ExtensionRepoStore(this) }
    val extensionManager: ExtensionManager by lazy { ExtensionManager(this, sourceManager) }
    val appPreferences: AppPreferences by lazy { AppPreferences(this) }
    val updateManager: UpdateManager by lazy { UpdateManager(this) }
    val driveSyncManager: DriveSyncManager by lazy {
        DriveSyncManager(this, repository, extensionRepoStore, extensionManager, appPreferences)
    }

    override fun onCreate() {
        super.onCreate()
        // Provide the shared HTTP stack to SDK-based (HttpSource) extensions before loading.
        ExtensionDependencies.client = NetworkModule.okHttpClient
        ExtensionDependencies.userAgent = "Yomu/0.4 (Android manga reader)"
        // Load any installed extension APKs into live sources at startup.
        applicationScope.launch { extensionManager.loadInstalledExtensions() }
        // Auto-sync with Drive (both no-ops unless enabled + signed in):
        driveSyncManager.startAutoBackup(applicationScope)   // backup on library changes
        driveSyncManager.startAutoRestore(applicationScope)  // pull a newer backup on launch
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(NetworkModule.okHttpClient)
            .crossfade(true)
            .build()
    }
}
