package com.yomu.reader.extension

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.yomu.reader.source.Source
import com.yomu.reader.source.SourceFactory
import dalvik.system.PathClassLoader
import java.io.File

/** An installed extension package that was successfully loaded into live sources. */
data class LoadedExtension(
    val pkg: String,
    val name: String,
    val versionName: String,
    val versionCode: Long,
    val sources: List<Source>,
)

/**
 * Discovers installed extension APKs and loads their sources.
 *
 * Discovery: any installed package that declares the
 * `com.yomu.reader.extension.class` manifest metadata. Loading: a [PathClassLoader]
 * over the APK (parented to the host classloader so the extension's references to
 * Yomu's ABI classes resolve to the host implementations), then each declared class
 * is instantiated as a [SourceFactory] or a [Source].
 */
class ExtensionLoader(private val context: Context) {

    fun loadExtensions(): List<LoadedExtension> {
        val pm = context.packageManager
        val installed = try {
            pm.getInstalledPackages(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enumerate packages", e)
            return emptyList()
        }

        return installed.mapNotNull { pkgInfo ->
            val metaData = pkgInfo.applicationInfo?.metaData ?: return@mapNotNull null
            if (!metaData.containsKey(METADATA_CLASS)) return@mapNotNull null
            runCatching { loadPackage(pkgInfo) }
                .onFailure { Log.e(TAG, "Failed to load extension ${pkgInfo.packageName}", it) }
                .getOrNull()
        }
    }

    private fun loadPackage(pkgInfo: PackageInfo): LoadedExtension? {
        val appInfo = pkgInfo.applicationInfo ?: return null
        val classList = appInfo.metaData.getString(METADATA_CLASS)?.takeIf { it.isNotBlank() }
            ?: return null

        // Combine the base APK with any split APKs for the class loader path.
        val dexPath = buildString {
            append(appInfo.sourceDir)
            appInfo.splitSourceDirs?.forEach { append(File.pathSeparatorChar).append(it) }
        }
        val classLoader = PathClassLoader(dexPath, appInfo.nativeLibraryDir, javaClass.classLoader)

        val sources = mutableListOf<Source>()
        for (raw in classList.split(";")) {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) continue
            val fqcn = if (trimmed.startsWith(".")) pkgInfo.packageName + trimmed else trimmed
            val instance = Class.forName(fqcn, false, classLoader)
                .getDeclaredConstructor()
                .newInstance()
            when (instance) {
                is SourceFactory -> sources += instance.createSources()
                is Source -> sources += instance
                else -> Log.w(TAG, "$fqcn is neither a Source nor a SourceFactory")
            }
        }
        if (sources.isEmpty()) return null

        val pm = context.packageManager
        return LoadedExtension(
            pkg = pkgInfo.packageName,
            name = appInfo.loadLabel(pm).toString(),
            versionName = pkgInfo.versionName ?: "",
            versionCode = pkgInfo.versionCodeCompat(),
            sources = sources,
        )
    }

    private fun PackageInfo.versionCodeCompat(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode
        else @Suppress("DEPRECATION") versionCode.toLong()

    companion object {
        private const val TAG = "ExtensionLoader"
        const val METADATA_CLASS = "com.yomu.reader.extension.class"
    }
}
