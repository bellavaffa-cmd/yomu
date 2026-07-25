@file:Suppress("DEPRECATION") // classic GoogleSignIn is deprecated but remains the pragmatic Drive-scope path

package com.yomu.reader.sync

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.squareup.moshi.Moshi
import com.yomu.reader.data.AppPreferences
import com.yomu.reader.data.ExtensionRepoStore
import com.yomu.reader.data.MangaRepository
import com.yomu.reader.data.SyncPayload
import com.yomu.reader.extension.ExtensionManager
import com.yomu.reader.network.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class DriveSyncState(
    val email: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
)

/**
 * Backs up / restores the library, categories and extension-repo list to a JSON file
 * in the user's private Google Drive app-data folder.
 *
 * Requires a Google Cloud OAuth (Android) client for package `com.yomu.reader` with the
 * signing SHA-1 registered, the Drive API enabled, and the account added as a test user.
 * Auth uses classic Google Sign-In + GoogleAuthUtil to get a bearer token; Drive access
 * is via the plain REST API (no heavyweight client libraries).
 *
 * (Classic GoogleSignIn is deprecated in favour of Credential Manager, but remains the
 * pragmatic path for requesting a Drive scope + an account usable with GoogleAuthUtil.)
 */
class DriveSyncManager(
    private val context: Context,
    private val repository: MangaRepository,
    private val extensionRepoStore: ExtensionRepoStore,
    private val extensionManager: ExtensionManager,
    private val appPreferences: AppPreferences,
    private val client: OkHttpClient = NetworkModule.okHttpClient,
    moshi: Moshi = NetworkModule.moshi,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    private val adapter = moshi.adapter(SyncPayload::class.java)

    private val _state = MutableStateFlow(DriveSyncState(email = lastAccount()?.email))
    val state: StateFlow<DriveSyncState> = _state.asStateFlow()

    private val signInOptions: GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_APPDATA_SCOPE))
            .build()

    val autoSync: Flow<Boolean> = appPreferences.autoSync

    suspend fun setAutoSync(enabled: Boolean) = appPreferences.setAutoSync(enabled)

    /**
     * Watch library/category and extension-repo changes and auto-back-up (debounced),
     * but only while auto-sync is enabled and an account is signed in.
     */
    @OptIn(FlowPreview::class)
    fun startAutoBackup(scope: CoroutineScope) {
        scope.launch {
            merge(
                repository.libraryChanges.drop(1).map { },
                extensionRepoStore.repos.drop(1).map { },
            )
                .debounce(AUTO_BACKUP_DEBOUNCE_MS)
                .collect {
                    if (appPreferences.autoSync.first() && lastAccount() != null && !_state.value.busy) {
                        backup()
                    }
                }
        }
    }

    fun signInIntent(): Intent = GoogleSignIn.getClient(context, signInOptions).signInIntent

    fun onSignInResult(data: Intent?) {
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(com.google.android.gms.common.api.ApiException::class.java)
            _state.value = _state.value.copy(email = account.email, message = "Signed in")
        } catch (e: Exception) {
            _state.value = _state.value.copy(message = "Sign-in failed: ${e.message}")
        }
    }

    fun signOut() {
        GoogleSignIn.getClient(context, signInOptions).signOut()
        _state.value = DriveSyncState(email = null, message = "Signed out")
    }

    private fun lastAccount(): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

    suspend fun backup() = runSync("Backing up") { token ->
        val payload = buildPayload()
        val json = adapter.toJson(payload)
        val existing = findFileId(token)
        if (existing == null) createFile(token, json) else updateFile(token, existing, json)
        appPreferences.setLastDriveSync(payload.updatedAt)
        "Backed up ${payload.library.size} manga"
    }

    suspend fun restore() = runSync("Restoring") { token ->
        val id = findFileId(token) ?: return@runSync "No backup found on Drive"
        val json = download(token, id)
        val payload = adapter.fromJson(json) ?: error("Corrupt backup")
        repository.importLibrary(payload.categories, payload.library)
        payload.extensionRepos.forEach { extensionRepoStore.add(it) }
        extensionManager.loadInstalledExtensions()
        appPreferences.setLastDriveSync(payload.updatedAt)
        "Restored ${payload.library.size} manga"
    }

    private suspend fun runSync(action: String, block: suspend (String) -> String) {
        val account = lastAccount()
        if (account == null) {
            _state.value = _state.value.copy(message = "Sign in first")
            return
        }
        _state.value = _state.value.copy(busy = true, message = "$action…")
        try {
            val token = withContext(Dispatchers.IO) {
                GoogleAuthUtil.getToken(context, account.account!!, "oauth2:$DRIVE_APPDATA_SCOPE")
            }
            val result = withContext(Dispatchers.IO) { block(token) }
            _state.value = _state.value.copy(busy = false, message = result)
        } catch (e: Exception) {
            _state.value = _state.value.copy(busy = false, message = "$action failed: ${e.message}")
        }
    }

    private suspend fun buildPayload(): SyncPayload {
        val (categories, mangas) = repository.exportLibrary()
        return SyncPayload(
            updatedAt = now(),
            categories = categories,
            library = mangas,
            extensionRepos = extensionRepoStore.current(),
            installedExtensions = extensionManager.installed.value.map { it.pkg },
        )
    }

    // --- Drive v3 REST (appDataFolder) ---

    private fun findFileId(token: String): String? {
        val url = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
            .addQueryParameter("spaces", "appDataFolder")
            .addQueryParameter("q", "name = '$FILE_NAME'")
            .addQueryParameter("fields", "files(id,modifiedTime)")
            .build()
        val request = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Drive list HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            // Minimal extraction to avoid another DTO: pull the first "id":"...".
            val match = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(body)
            return match?.groupValues?.getOrNull(1)
        }
    }

    private fun download(token: String, id: String): String {
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$id?alt=media")
            .header("Authorization", "Bearer $token")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Drive download HTTP ${response.code}")
            return response.body?.string() ?: error("Empty backup")
        }
    }

    private fun createFile(token: String, content: String) {
        val metadata = """{"name":"$FILE_NAME","parents":["appDataFolder"]}"""
        val body = MultipartBody.Builder().setType("multipart/related".toMediaType())
            .addPart(metadata.toRequestBody(JSON))
            .addPart(content.toRequestBody(JSON))
            .build()
        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Drive create HTTP ${response.code}")
        }
    }

    private fun updateFile(token: String, id: String, content: String) {
        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files/$id?uploadType=media")
            .header("Authorization", "Bearer $token")
            .patch(content.toRequestBody(JSON))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Drive update HTTP ${response.code}")
        }
    }

    companion object {
        private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val FILE_NAME = "yomu-sync.json"
        private const val AUTO_BACKUP_DEBOUNCE_MS = 5000L
        private val JSON = "application/json".toMediaType()
    }
}
