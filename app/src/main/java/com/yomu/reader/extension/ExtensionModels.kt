package com.yomu.reader.extension

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * One entry of a Tachiyomi/Mihon-compatible repo `index.min.json`.
 * Only the fields Yomu reads are modeled.
 */
@JsonClass(generateAdapter = true)
data class ExtensionDto(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long = 0,
    val version: String = "",
    val nsfw: Int = 0,
    val sources: List<ExtensionSourceDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class ExtensionSourceDto(
    val name: String = "",
    val lang: String = "",
    val id: String = "",
    @Json(name = "baseUrl") val baseUrl: String = "",
)

/** Install state of an available extension relative to what's on the device. */
enum class InstallState { NOT_INSTALLED, INSTALLED, UPDATE_AVAILABLE }

/** A downloadable extension, resolved against a specific repo base URL. */
data class AvailableExtension(
    val name: String,
    val pkg: String,
    val versionName: String,
    val versionCode: Long,
    val lang: String,
    val nsfw: Boolean,
    val sourceCount: Int,
    val apkUrl: String,
    val iconUrl: String,
    val repoBaseUrl: String,
    val installState: InstallState = InstallState.NOT_INSTALLED,
) {
    /** Display name without the conventional "Tachiyomi: " prefix. */
    val displayName: String
        get() = name.removePrefix("Tachiyomi: ").removePrefix("Yomu: ")
}
