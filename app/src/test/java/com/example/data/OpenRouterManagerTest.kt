package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OpenRouterManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testFallbackModelsExistAndHaveFreeAndVisionOptions() {
        val models = OpenRouterManager.fallbackModels
        assertTrue("Fallback models should not be empty", models.isNotEmpty())

        val freeModels = models.filter { it.isFree }
        assertTrue("Should have at least one free model in fallback list", freeModels.isNotEmpty())

        val visionModels = models.filter { it.isVision }
        assertTrue("Should have vision models in fallback list for OCR", visionModels.isNotEmpty())
    }

    @Test
    fun testAiProviderPreferenceStorage() {
        // Default is NATIVE_GEMINI
        val defaultProvider = OpenRouterManager.getAiProvider(context)
        assertEquals(AiProvider.NATIVE_GEMINI, defaultProvider)

        // Switch to OPENROUTER
        OpenRouterManager.setAiProvider(context, AiProvider.OPENROUTER)
        assertEquals(AiProvider.OPENROUTER, OpenRouterManager.getAiProvider(context))

        // Switch back to NATIVE_GEMINI
        OpenRouterManager.setAiProvider(context, AiProvider.NATIVE_GEMINI)
        assertEquals(AiProvider.NATIVE_GEMINI, OpenRouterManager.getAiProvider(context))
    }

    @Test
    fun testOpenRouterModelSelectionStorage() {
        val testModel = "deepseek/deepseek-r1:free"
        OpenRouterManager.setSelectedModel(context, testModel)
        assertEquals(testModel, OpenRouterManager.getSelectedModel(context))
    }

    @Test
    fun testOpenRouterApiKeyStorage() {
        val testKey = "  sk-or-v1-testkey1234567890  "
        OpenRouterManager.setOpenRouterApiKey(context, testKey)
        assertEquals("sk-or-v1-testkey1234567890", OpenRouterManager.getOpenRouterApiKey(context))
    }

    @Test
    fun testGetCachedModelsReturnsFallbackWhenCacheEmpty() {
        val models = OpenRouterManager.getCachedModels(context)
        assertTrue(models.isNotEmpty())
        assertTrue(models.any { it.id.contains("gemini", ignoreCase = true) })
    }
}
