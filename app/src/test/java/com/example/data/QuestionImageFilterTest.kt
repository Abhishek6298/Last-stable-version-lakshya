package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionImageFilterTest {

    @Test
    fun testAdAndPromotionalBannerDetection() {
        // NEETprep / Coaching batch promotional banners (reported by user in screenshots 1, 2, 3)
        assertTrue(QuestionImageFilter.isAdOrPromotionalImage("https://cdn.neetprep.com/images/target-batch-nta-cbt.png"))
        assertTrue(QuestionImageFilter.isAdOrPromotionalImage("https://neetprep.com/assets/banner_targetbatch.jpg"))
        assertTrue(QuestionImageFilter.isAdOrPromotionalImage("https://cdn.neetprep.com/all-india-classroom-test-series.png"))
        assertTrue(QuestionImageFilter.isAdOrPromotionalImage("https://example.com/promo/discount-batch.png"))
        assertTrue(QuestionImageFilter.isAdOrPromotionalImage("https://example.com/img/super30-ad.jpg"))
        assertTrue(QuestionImageFilter.isAdOrPromotionalImage("https://example.com/assets/header-banner.png"))
        assertTrue(QuestionImageFilter.isAdOrPromotionalImage("https://example.com/cta-admission-open.png"))

        // Null / none / placeholder tokens
        assertTrue(QuestionImageFilter.isAdOrPromotionalImage(null))
        assertTrue(QuestionImageFilter.isAdOrPromotionalImage(""))
        assertTrue(QuestionImageFilter.isAdOrPromotionalImage("null"))
        assertTrue(QuestionImageFilter.isAdOrPromotionalImage("none"))

        // Legitimate diagram URLs
        assertFalse(QuestionImageFilter.isAdOrPromotionalImage("https://examgoal.com/images/question_1203_tube_particle.png"))
        assertFalse(QuestionImageFilter.isAdOrPromotionalImage("https://cdn.example.com/q_img/resistor_circuit.jpg"))
        assertFalse(QuestionImageFilter.isAdOrPromotionalImage("https://example.com/uploads/figures/euglena_structure.svg"))
        assertFalse(QuestionImageFilter.isAdOrPromotionalImage("/storage/emulated/0/DCIM/cropped_diagram.jpg"))
        assertFalse(QuestionImageFilter.isAdOrPromotionalImage("file:///data/user/0/com.example/cache/diag_1.png"))
    }

    @Test
    fun testDuplicateImageAcrossDifferentQuestionsIsPurged() {
        val bannerUrl = "https://cdn.neetprep.com/img/target-batch-nta-cbt.png"
        val q1 = AiTestQuestion(
            id = 1,
            subject = "Physics",
            chapter = "Kinematics",
            pyqYear = "NEET 2024",
            questionText = "A particle moves along a straight line. Its velocity-time graph is represented in the diagram below.",
            optionA = "10 m",
            optionB = "20 m",
            optionC = "30 m",
            optionD = "40 m",
            correctOption = "C",
            explanation = "Area under v-t graph.",
            hasImage = true,
            imageUrl = bannerUrl,
            diagramLabel = "Velocity-time graph"
        )
        val q2 = AiTestQuestion(
            id = 2,
            subject = "Biology",
            chapter = "Biological Classification",
            pyqYear = "NEET 2023",
            questionText = "Observe the given morphological structure of a freshwater flagellate. Which feature is correct?",
            optionA = "Pellicle present",
            optionB = "Cellulose wall",
            optionC = "Silica wall",
            optionD = "None",
            correctOption = "A",
            explanation = "Euglenoids possess pellicle.",
            hasImage = true,
            imageUrl = bannerUrl,
            diagramLabel = "Euglena morphology"
        )

        val sanitized = QuestionImageFilter.sanitizeTestQuestions(listOf(q1, q2))

        // Both questions must have the ad banner purged to null
        assertNull(sanitized[0].imageUrl)
        assertNull(sanitized[1].imageUrl)
        // Diagram label should remain intact so student sees explanation rather than ad
        assertEquals("Velocity-time graph", sanitized[0].diagramLabel)
        assertEquals("Euglena morphology", sanitized[1].diagramLabel)
    }

    @Test
    fun testAuthenticSingleDiagramPreserved() {
        val authenticUrl = "https://examgoal.com/question/tube_elastic_collision.png"
        val q1 = AiTestQuestion(
            id = 1,
            subject = "Physics",
            chapter = "Work Power Energy",
            pyqYear = "JEE Advanced 2019",
            questionText = "A small particle of mass m moves inside a straight tube shown in the figure below.",
            optionA = "KE increases",
            optionB = "KE decreases",
            optionC = "Momentum zero",
            optionD = "None",
            correctOption = "A",
            explanation = "Piston moves inward.",
            hasImage = true,
            imageUrl = authenticUrl,
            diagramLabel = "Particle in tube"
        )

        val sanitized = QuestionImageFilter.sanitizeTestQuestions(
            listOf(q1),
            verifiedWebImages = setOf(authenticUrl)
        )

        assertEquals(authenticUrl, sanitized[0].imageUrl)
        assertTrue(sanitized[0].hasImage)
    }

    @Test
    fun testUnreferencedDiagramPurgedFromTextualQuestion() {
        // Question is purely textual with zero diagram reference
        val q1 = AiTestQuestion(
            id = 1,
            subject = "Physics",
            chapter = "Electrostatics",
            pyqYear = "NEET 2021",
            questionText = "The electric flux through a closed Gaussian surface depends only on the charge enclosed.",
            optionA = "True",
            optionB = "False",
            optionC = "Depends on shape",
            optionD = "Depends on medium only",
            correctOption = "A",
            explanation = "Gauss's law.",
            hasImage = false,
            imageUrl = "https://examgoal.com/images/spring_block.png", // Mismatched unsolicited image
            diagramLabel = null
        )

        val sanitized = QuestionImageFilter.sanitizeTestQuestions(listOf(q1))
        assertNull(sanitized[0].imageUrl)
    }
}
