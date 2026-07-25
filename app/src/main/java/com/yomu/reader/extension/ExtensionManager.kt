package com.yomu.reader.extension

import android.content.Context
import android.os.Build
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.yomu.reader.data.ApkInstaller
import com.yomu.reader.network.NetworkModule
import com.yomu.reader.source.SourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File

data class FetchResult(
    val extensions: List<AvailableExtension>,
    val errors: List<String>,
)

/**
 * Discovers downloadable extensions from the user's configured repos and drives the
 * download → system-install flow. Loading an installed extension as a working source
 * (the extension runtime) is a separate, not-yet-built piece.
 */
class ExtensionManager(
    private val context: Context,
    private val sourceManager: SourceManager,
    private val client: OkHttpClient = NetworkModule.okHttpClient,
    moshi: Moshi = NetworkModule.moshi,
) {
    private val listAdapter = moshi.adapter<List<ExtensionDto>>(
        Types.newParameterizedType(List::class.java, ExtensionDto::class.java),
    )

    private val loader = ExtensionLoader(context)

    private val _installed = MutableStateFlow<List<LoadedExtension>>(emptyList())
    /** Extensions currently installed on the device and successfully loaded into sources. */
    val installed: StateFlow<List<LoadedExtension>> = _installed.asStateFlow()

    /**
     * Discover installed extension APKs, load their sources into the [SourceManager],
     * and publish the list of loaded extensions. Safe to call repeatedly (e.g. after an
     * install or on returning to the Extensions screen).
     */
    suspend fun loadInstalledExtensions() = withContext(Dispatchers.IO) {
        val loaded = loader.loadExtensions()
        sourceManager.setExtensionSources(loaded.flatMap { it.sources })
        _installed.value = loaded
    }

    /** Fetch and aggregate the extension listings across every configured repo. */
    suspend fun fetchExtensions(repoUrls: List<String>): FetchResult = withContext(Dispatchers.IO) {
        val all = mutableListOf<AvailableExtension>()
        val errors = mutableListOf<String>()
        for (repo in repoUrls) {
            try {
                all += fetchRepo(repo)
            } catch (e: Exception) {
                errors += "${repo.substringAfter("://")}: ${e.message ?: "failed"}"
            }
        }
        // De-duplicate by package, keeping the highest version seen across repos.
        val deduped = all
            .groupBy { it.pkg }
            .map { (_, dupes) -> dupes.maxByOrNull { it.versionCode }!! }
            .sortedBy { it.displayName.lowercase() }
        FetchResult(deduped, errors)
    }

    private fun fetchRepo(repo: String): List<AvailableExtension> {
        val indexUrl = "$repo/index.min.json"
        val request = Request.Builder().url(indexUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body?.string() ?: error("empty response")
            val dtos = listAdapter.fromJson(body) ?: emptyList()
            return dtos.map { it.toAvailable(repo) }
        }
    }

    private fun ExtensionDto.toAvailable(repo: String): AvailableExtension {
        val installed = installedVersionCode(pkg)
        val state = when {
            installed == null -> InstallState.NOT_INSTALLED
            installed < code -> InstallState.UPDATE_AVAILABLE
            else -> InstallState.INSTALLED
        }
        return AvailableExtension(
            name = name,
            pkg = pkg,
            versionName = version,
            versionCode = code,
            lang = lang,
            nsfw = nsfw == 1,
            sourceCount = sources.size,
            apkUrl = "$repo/apk/$apk",
            iconUrl = "$repo/icon/$pkg.png",
            repoBaseUrl = repo,
            installState = state,
        )
    }

    private fun installedVersionCode(pkg: String): Long? = try {
        val info = context.packageManager.getPackageInfo(pkg, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode
        else @Suppress("DEPRECATION") info.versionCode.toLong()
    } catch (e: Exception) {
        null
    }

    /**
     * Download the extension APK and hand it to the system package installer.
     * Returns false (and sends the user to settings) if install-from-unknown-sources
     * isn't permitted yet.
     */
    suspend fun downloadAndInstall(ext: AvailableExtension): Boolean {
        if (!ApkInstaller.canInstall(context)) {
            ApkInstaller.promptUnknownSources(context)
            return false
        }
        val apk = withContext(Dispatchers.IO) { downloadApk(ext) }
        ApkInstaller.install(context, apk)
        return true
    }

    private fun downloadApk(ext: AvailableExtension): File {
        val dir = File(context.cacheDir, "extensions").apply { mkdirs() }
        val target = File(dir, "${ext.pkg}.apk")
        val request = Request.Builder().url(ext.apkUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val source = response.body?.source() ?: error("empty apk body")
            target.sink().buffer().use { it.writeAll(source) }
        }
        return target
    }
}
