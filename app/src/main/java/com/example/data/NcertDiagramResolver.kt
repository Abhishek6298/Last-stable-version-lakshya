package com.example.data

import java.util.Locale

/**
 * Authentic educational diagram resolver and phantom phrasing cleaner.
 * 
 * Strict Zero-Fake-Diagram Policy:
 * 1. NEVER invent, hallucinate, or guess image URLs via keywords.
 * 2. NEVER attach historical portraits of scientists (e.g. Charles Wheatstone sketch)
 *    or antique book covers to physics/chemistry/biology questions.
 * 3. Only preserve genuine, verified diagram URLs directly extracted from the source website.
 * 4. If a question does not have an authentic source diagram, imageUrl MUST be null and
 *    hasImage MUST be false. Clean phantom phrasing ("Observe the given figure...") so the
 *    problem statement is 100% self-contained and clear without any image.
 */
object NcertDiagramResolver {

    /**
     * Enriches an AiTestQuestion:
     * - If imageUrl is valid and authentic (from source extraction), preserves it.
     * - If imageUrl is null or invalid, leaves imageUrl = null and hasImage = false.
     *   NEVER attaches fake or irrelevant images.
     * - Cleans up phantom phrasing so the question doesn't refer to a non-existent diagram.
     */
    fun ensureAuthenticDiagram(question: AiTestQuestion): AiTestQuestion {
        val rawUrl = question.imageUrl?.trim()
        val isAuthenticSourceImage = !rawUrl.isNullOrBlank() &&
                !rawUrl.equals("null", ignoreCase = true) &&
                !rawUrl.equals("none", ignoreCase = true) &&
                !QuestionImageFilter.isAdOrPromotionalImage(rawUrl) &&
                QuestionImageFilter.isPlausibleDiagramUrl(rawUrl)

        if (isAuthenticSourceImage) {
            return question.copy(
                hasImage = true,
                imageUrl = rawUrl
            )
        }

        // Strictly NO fake image injection. If no authentic source diagram exists, imageUrl MUST be null.
        val cleanedText = cleanPhantomPhrasing(question.questionText)
        return question.copy(
            questionText = cleanedText,
            hasImage = false,
            imageUrl = null,
            diagramLabel = null
        )
    }

    /**
     * Cleans up questions that mistakenly say "Observe the given figure..." when no figure exists,
     * transforming them into complete, self-contained conceptual problems.
     */
    private fun cleanPhantomPhrasing(text: String): String {
        return text
            .replace(Regex("""(?i)^Observe the given figure of\s+"""), "In ")
            .replace(Regex("""(?i)^The given diagram shows\s+"""), "In ")
            .replace(Regex("""(?i)^In the given figure,?\s*"""), "Consider ")
            .replace(Regex("""(?i)^In the given circuit,?\s*"""), "In an electrical circuit, ")
            .replace(Regex("""(?i)^In the given setup,?\s*"""), "In an experimental setup, ")
            .replace(Regex("""(?i)^From the given graph,?\s*"""), "Based on standard theory, ")
            .replace(Regex("""(?i)marked as ['"]?A['"]?"""), "the primary functional part")
            .replace(Regex("""(?i)labelled ['"]?A['"]?,?\s*['"]?B['"]?,?\s*and\s*['"]?C['"]?"""), "the key components")
            .trim()
    }
}
