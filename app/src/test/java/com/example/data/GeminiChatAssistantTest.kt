package com.example.data

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

class GeminiChatAssistantTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun testAwaitReturnsResponseSuccessfully() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"ok"}"""))

        val request = Request.Builder().url(server.url("/test")).build()
        val response = client.newCall(request).await()

        assertEquals(200, response.code)
        assertEquals("""{"status":"ok"}""", response.body?.string())
    }

    @Test
    fun benchmarkBlockingVsSuspendingNetworkCalls() = runBlocking {
        val requestCount = 10
        val delayMillis = 150L

        // Enqueue mock responses with delay
        for (i in 0 until requestCount * 2) {
            server.enqueue(
                MockResponse()
                    .setBody("""{"candidates":[{"content":{"parts":[{"text":"Response $i"}]}}]}""")
                    .setBodyDelay(delayMillis, TimeUnit.MILLISECONDS)
            )
        }

        // Dispatcher restricted to 2 worker threads to simulate limited pool / thread saturation
        val limitedDispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

        // 1. Measure blocking execute()
        val blockingTime = measureTimeMillis {
            withContext(limitedDispatcher) {
                val jobs = (0 until requestCount).map {
                    async {
                        val request = Request.Builder().url(server.url("/blocking")).build()
                        val response = client.newCall(request).execute()
                        response.body?.string()
                    }
                }
                jobs.awaitAll()
            }
        }

        // 2. Measure suspendable await()
        val suspendingTime = measureTimeMillis {
            withContext(limitedDispatcher) {
                val jobs = (0 until requestCount).map {
                    async {
                        val request = Request.Builder().url(server.url("/suspending")).build()
                        val response = client.newCall(request).await()
                        response.body?.string()
                    }
                }
                jobs.awaitAll()
            }
        }

        println("==================================================")
        println("PERFORMANCE BENCHMARK RESULTS (10 requests on 2 threads, 150ms I/O delay):")
        println("Blocking execute():   $blockingTime ms")
        println("Suspending await():   $suspendingTime ms")
        println("Speedup Ratio:        ${String.format("%.2f", blockingTime.toDouble() / suspendingTime.toDouble())}x")
        println("==================================================")

        // Suspending calls should be significantly faster because threads are released while waiting for I/O
        assertTrue(
            "Suspending await() ($suspendingTime ms) should be faster than blocking execute() ($blockingTime ms)",
            suspendingTime < blockingTime
        )

        limitedDispatcher.close()
    }
}
