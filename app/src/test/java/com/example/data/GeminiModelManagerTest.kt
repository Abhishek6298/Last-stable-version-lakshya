package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiModelManagerTest {

    @Test
    fun testDefaultModelIs38Flash() {
        assertEquals("gemini-3.8-flash", GeminiModelManager.DEFAULT_MODEL)
    }

    @Test
    fun testModernModelsPassThroughUnmodified() {
        assertEquals("gemini-3.8-flash", GeminiModelManager.resolveEffectiveEndpointModel("gemini-3.8-flash"))
        assertEquals("gemini-3.5-flash", GeminiModelManager.resolveEffectiveEndpointModel("gemini-3.5-flash"))
        assertEquals("gemini-3.1-pro-preview", GeminiModelManager.resolveEffectiveEndpointModel("gemini-3.1-pro-preview"))
        assertEquals("gemini-3.1-flash-lite-preview", GeminiModelManager.resolveEffectiveEndpointModel("gemini-3.1-flash-lite-preview"))
        assertEquals("gemini-flash-latest", GeminiModelManager.resolveEffectiveEndpointModel("gemini-flash-latest"))
        assertEquals("gemini-3.7-flash", GeminiModelManager.resolveEffectiveEndpointModel("gemini-3.7-flash"))
    }

    @Test
    fun testModelsPrefixIsSanitized() {
        assertEquals("gemini-3.8-flash", GeminiModelManager.resolveEffectiveEndpointModel("models/gemini-3.8-flash"))
        assertEquals("gemini-3.5-flash", GeminiModelManager.resolveEffectiveEndpointModel("models/gemini-3.5-flash"))
    }

    @Test
    fun testDeprecatedModelsAreSafelyUpgraded() {
        // Discontinued 2.5 and 2.0 and 1.5 must NEVER be sent to the API
        assertEquals("gemini-3.8-flash", GeminiModelManager.resolveEffectiveEndpointModel("gemini-2.5-flash"))
        assertEquals("gemini-3.1-pro-preview", GeminiModelManager.resolveEffectiveEndpointModel("gemini-2.5-pro"))
        assertEquals("gemini-3.8-flash", GeminiModelManager.resolveEffectiveEndpointModel("gemini-2.0-flash"))
        assertEquals("gemini-3.8-flash", GeminiModelManager.resolveEffectiveEndpointModel("gemini-2.0-flash-lite"))
        assertEquals("gemini-3.8-flash", GeminiModelManager.resolveEffectiveEndpointModel("gemini-1.5-flash"))
        assertEquals("gemini-3.1-pro-preview", GeminiModelManager.resolveEffectiveEndpointModel("gemini-1.5-pro"))
    }

    @Test
    fun testAliasesAreProperlyMapped() {
        assertEquals("gemini-flash-latest", GeminiModelManager.resolveEffectiveEndpointModel("gemini flash"))
        assertEquals("gemini-3.1-flash-lite-preview", GeminiModelManager.resolveEffectiveEndpointModel("gemini lite"))
        assertEquals("gemini-3.5-flash-lite", GeminiModelManager.resolveEffectiveEndpointModel("gemini-3.5-flash-lite"))
        assertEquals("gemini-3.1-pro-preview", GeminiModelManager.resolveEffectiveEndpointModel("gemini pro"))
        assertEquals("gemini-3.8-flash", GeminiModelManager.resolveEffectiveEndpointModel("gemini-3.8"))
    }

    @Test
    fun testIsDeprecatedModel() {
        assertTrue(GeminiModelManager.isDeprecatedModel("gemini-2.5-flash"))
        assertTrue(GeminiModelManager.isDeprecatedModel("gemini-2.0-flash"))
        assertTrue(GeminiModelManager.isDeprecatedModel("gemini-1.5-pro"))
        assertTrue(GeminiModelManager.isDeprecatedModel("gemini-pro"))

        assertFalse(GeminiModelManager.isDeprecatedModel("gemini-3.8-flash"))
        assertFalse(GeminiModelManager.isDeprecatedModel("gemini-3.5-flash"))
        assertFalse(GeminiModelManager.isDeprecatedModel("gemini-3.1-pro-preview"))
        assertFalse(GeminiModelManager.isDeprecatedModel("gemini-3.1-flash-lite-preview"))
    }

    @Test
    fun testPopularModelsDoNotContainDeprecatedModels() {
        val popular = GeminiModelManager.popularGeminiModels
        for (m in popular) {
            assertFalse(
                "Popular models must not include deprecated model: ${m.id}",
                GeminiModelManager.isDeprecatedModel(m.id)
            )
            // Verify that switching to each popular model resolves directly to its valid endpoint
            assertEquals(m.id, GeminiModelManager.resolveEffectiveEndpointModel(m.id))
        }
    }
}
