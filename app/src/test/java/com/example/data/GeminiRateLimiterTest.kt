package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiRateLimiterTest {

    @Test
    fun testProModelHasStrictQuotaConfig() {
        val config = GeminiRateLimiter.getModelConfig("gemini-3.1-pro-preview")
        assertEquals(2, config.rpm)
        assertEquals(50, config.rpd)
        assertTrue(config.minIntervalMs >= 20_000L)
    }

    @Test
    fun testFlashModelHasStandardQuotaConfig() {
        val config = GeminiRateLimiter.getModelConfig("gemini-3.8-flash")
        assertEquals(15, config.rpm)
        assertEquals(1500, config.rpd)
    }

    @Test
    fun testFlashLiteHasHighThroughputQuotaConfig() {
        val config = GeminiRateLimiter.getModelConfig("gemini-3.1-flash-lite-preview")
        assertEquals(15, config.rpm)
        assertEquals(1000, config.rpd)
    }

    @Test
    fun testCachePutAndGetSavesTokens() {
        val model = "gemini-3.8-flash"
        val prompt = "What is Bernoulli's principle in fluid mechanics?"
        val mockAnswer = "Bernoulli's principle states that an increase in the speed of a fluid occurs simultaneously with a decrease in static pressure."

        // Initially nothing in cache
        assertNull(GeminiRateLimiter.getCachedResponse(model, prompt))

        // Put in cache
        GeminiRateLimiter.putCachedResponse(model, prompt, null, mockAnswer)

        // Read from cache
        val retrieved = GeminiRateLimiter.getCachedResponse(model, prompt)
        assertNotNull(retrieved)
        assertEquals(mockAnswer, retrieved)
    }
}
