package com.example.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Suspendable await extension for OkHttp Call that converts OkHttp's asynchronous enqueue
 * callback into a non-blocking coroutine suspension.
 *
 * Replaces blocking `Call.execute()` to prevent thread starvation on Coroutine Dispatchers,
 * and cancels the underlying HTTP request over the wire if the calling coroutine is cancelled.
 */
suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }

        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isCancelled) return
            continuation.resumeWithException(e)
        }
    })

    continuation.invokeOnCancellation {
        try {
            cancel()
        } catch (_: Throwable) {}
    }
}
