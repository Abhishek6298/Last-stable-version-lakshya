package com.example.data

import org.junit.Assert.*
import org.junit.Test

class QuestionImageVerificationTest {

    @Test
    fun testAuthenticDiagramUrlsAreAllowed() {
        val authenticUrls = listOf(
            "https://cdn.neetprep.com/questions/optics_prism_refraction.jpg",
            "https://d10lpgp6xz60nq.cloudfront.net/images/questions/physics_circuit_bridge.png",
            "https://media.doubtnut.com/q-images/biology_nephron_structure.webp",
            "https://res.cloudinary.com/examgoal/image/upload/v1/physics/pv_graph.jpg",
            "https://static.pw.live/question-images/ray_optics_lens.png"
        )

        for (url in authenticUrls) {
            assertTrue("URL should be plausible diagram: $url", QuestionImageFilter.isPlausibleDiagramUrl(url))
            assertFalse("URL should NOT be flagged as ad: $url", QuestionImageFilter.isAdOrPromotionalImage(url))
        }
    }

    @Test
    fun testPromotionalBannersAreFilteredOut() {
        val adUrls = listOf(
            "https://example.com/banners/target-batch-promo.jpg",
            "https://example.com/all_india_test_series_discount.png",
            "https://example.com/assets/subscription_pricing_offer.webp"
        )

        for (url in adUrls) {
            assertTrue("Ad URL should be flagged as promo: $url", QuestionImageFilter.isAdOrPromotionalImage(url))
        }
    }

    @Test
    fun testSanitizeTestQuestionsPreservesGenuineImages() {
        val testQuestions = listOf(
            AiTestQuestion(
                id = 1,
                subject = "Physics",
                chapter = "Current Electricity",
                pyqYear = "NEET 2025 PYQ",
                questionText = "Find equivalent resistance across AB in the given circuit diagram.",
                optionA = "2 Ω",
                optionB = "4 Ω",
                optionC = "6 Ω",
                optionD = "8 Ω",
                correctOption = "B",
                explanation = "Bridge is balanced, so central resistor can be removed.",
                hasImage = true,
                imageUrl = "https://cdn.neetprep.com/questions/circuit_wheatstone.jpg",
                diagramLabel = "Figure: Wheatstone bridge circuit diagram",
                diagramType = "CIRCUIT"
            ),
            AiTestQuestion(
                id = 2,
                subject = "Physics",
                chapter = "Current Electricity",
                pyqYear = "NEET 2024 PYQ",
                questionText = "State Kirchhoff's junction law.",
                optionA = "ΣI = 0",
                optionB = "ΣV = 0",
                optionC = "ΣR = 0",
                optionD = "None",
                correctOption = "A",
                explanation = "Based on conservation of charge.",
                hasImage = false,
                imageUrl = null,
                diagramLabel = null,
                diagramType = null
            )
        )

        val sanitized = QuestionImageFilter.sanitizeTestQuestions(testQuestions)

        assertEquals(2, sanitized.size)
        // Question 1 must have authentic image preserved
        assertTrue("Question 1 must have hasImage = true", sanitized[0].hasImage)
        assertEquals("https://cdn.neetprep.com/questions/circuit_wheatstone.jpg", sanitized[0].imageUrl)

        // Question 2 must not have fake image
        assertFalse("Question 2 must have hasImage = false", sanitized[1].hasImage)
        assertNull("Question 2 imageUrl must be null", sanitized[1].imageUrl)
    }
}
