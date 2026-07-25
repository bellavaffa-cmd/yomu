package com.yomu.reader.update

import android.content.Context
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.yomu.reader.BuildConfig
import com.yomu.reader.data.ApkInstaller
import com.yomu.reader.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val tag: String,
    val notes: String,
    val apkUrl: String,
)

/**
 * Checks GitHub Releases for a newer build and installs it in-app.
 *
 * Uses the releases *list* endpoint (not `releases/latest`) because Yomu's releases
 * are marked pre-release, which `releases/latest` skips.
 */
class UpdateManager(
    private val context: Context,
    private val client: OkHttpClient = NetworkModule.okHttpClient,
    moshi: Moshi = NetworkModule.moshi,
) {
    private val listAdapter = moshi.adapter<List<GhRelease>>(
        Types.newParameterizedType(List::class.java, GhRelease::class.java),
    )

    /** Returns an [UpdateInfo] if a newer version is available, or null if up to date. */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$REPO/releases?per_page=10")
            .header("Accept", "application/vnd.github+json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code}")
            val body = response.body?.string() ?: return@withContext null
            val releases = listAdapter.fromJson(body).orEmpty()
                .filter { !it.draft }
            for (release in releases) {
                val code = parseVersionCode(release.tagName) ?: continue
                if (code <= BuildConfig.VERSION_CODE) continue
                val apk = release.assets.firstOrNull { it.isAppApk() } ?: continue
                return@withContext UpdateInfo(
                    versionName = release.tagName.removePrefix("v"),
                    versionCode = code,
                    tag = release.tagName,
                    notes = release.body.orEmpty(),
                    apkUrl = apk.browserDownloadUrl,
                )
            }
            null
        }
    }

    /** Download the update APK and hand it to the system installer. */
    suspend fun downloadAndInstall(info: UpdateInfo): Boolean {
        if (!ApkInstaller.canInstall(context)) {
            ApkInstaller.promptUnknownSources(context)
            return false
        }
        val apk = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val target = File(dir, "yomu-${info.tag}.apk")
            client.newCall(Request.Builder().url(info.apkUrl).build()).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                val source = response.body?.source() ?: error("empty apk body")
                target.sink().buffer().use { it.writeAll(source) }
            }
            target
        }
        ApkInstaller.install(context, apk)
        return true
    }

    private fun GhAsset.isAppApk(): Boolean =
        name.matches(Regex("yomu-v[0-9.]+\\.apk")) ||
            (name.startsWith("yomu-v") && name.endsWith(".apk") && !name.contains("extension"))

    /** "v0.8.0" -> 800 using major*10000 + minor*100 + patch. */
    private fun parseVersionCode(tag: String): Int? {
        val parts = tag.removePrefix("v").split(".")
        if (parts.isEmpty()) return null
        val major = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return major * 10000 + minor * 100 + patch
    }

    companion object {
        private const val REPO = "bellavaffa-cmd/yomu"
    }
}

@JsonClass(generateAdapter = true)
data class GhRelease(
    @Json(name = "tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GhAsset> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GhAsset(
    val name: String,
    @Json(name = "browser_download_url") val browserDownloadUrl: String,
    val size: Long = 0,
)
