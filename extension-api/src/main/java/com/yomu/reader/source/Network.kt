package com.yomu.reader.source

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

/**
 * Await an OkHttp [Call] from a coroutine, cancelling the request if the coroutine
 * is cancelled. Throws on non-2xx responses. The returned [Response] is open — the
 * caller is responsible for closing it (HttpSource does so via `use { }`).
 */
suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            if (!response.isSuccessful) {
                response.close()
                cont.resumeWith(Result.failure(IOException("HTTP ${response.code}")))
                return
            }
            cont.resumeWith(Result.success(response))
        }

        override fun onFailure(call: Call, e: IOException) {
            if (cont.isCancelled) return
            cont.resumeWith(Result.failure(e))
        }
    })
    cont.invokeOnCancellation { runCatching { cancel() } }
}
