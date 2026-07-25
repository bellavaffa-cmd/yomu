package com.yomu.reader.source

import okhttp3.OkHttpClient

/**
 * Host-provided dependencies available to extensions at runtime.
 *
 * This is Yomu's lightweight equivalent of Tachiyomi's Injekt-provided NetworkHelper:
 * the host app sets [client] (and [userAgent]) once at startup, before loading
 * extensions, and every [com.yomu.reader.source.online.HttpSource] reuses it — so
 * extensions share the app's OkHttp stack (caching, headers, timeouts) for free.
 */
object ExtensionDependencies {
    @Volatile
    var client: OkHttpClient = OkHttpClient()

    @Volatile
    var userAgent: String = "Yomu"
}
