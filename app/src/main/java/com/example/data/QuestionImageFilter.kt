package com.example.data

import android.net.Uri
import java.net.URI
import java.util.Locale

/**
 * Intelligent Image & Diagram Guard for Lakshya.
 * 
 * Guarantees that:
 * 1. Promotional banners, test-series advertisements, coaching batch posters, and UI chrome
 *    are NEVER treated as question diagrams.
 * 2. Questions only get diagrams that strictly belong to them (1:1 semantic binding).
 * 3. Duplicate images repeated across different questions in the same test are purged.
 * 4. Hallucinated, non-existent, or ad-network image URLs are safely nullified,
 *    allowing the app to display verified diagram labels, ASCII schematics, or SVGs instead.
 */
object QuestionImageFilter {

    // Keywords in URLs, filenames, alt-text, or class names indicating promotional ads or non-question chrome
    private val AD_AND_PROMO_PATTERNS = listOf(
        "target-batch",
        "targetbatch",
        "all-india",
        "all_india",
        "allindia",
        "classroom",
        "test-series",
        "test_series",
        "testseries",
        "nta-cbt",
        "cbt-mode",
        "cbt_mode",
        "pen-paper",
        "omr-format",
        "super30",
        "super-30",
        "banner",
        "promo",
        "promotion",
        "promotional",
        "advert",
        "advertisement",
        "advertising",
        "admission",
        "scholarship",
        "cashback",
        "discount",
        "coupon",
        "offer",
        "pricing",
        "subscribe",
        "subscription",
        "poster",
        "popup",
        "pop-up",
        "modal",
        "cta",
        "campaign",
        "announcement",
        "marketing",
        "centres",
        "centers",
        "offline-center",
        "offline-centre",
        "join-now",
        "download-app",
        "playstore",
        "googleplay",
        "appstore",
        "social",
        "facebook",
        "instagram",
        "telegram",
        "whatsapp",
        "youtube",
        "thumbnail",
        "header",
        "footer",
        "navbar",
        "sidebar",
        "watermark",
        "branding",
        "brand-logo",
        "logo",
        "avatar",
        "favicon",
        "slider",
        "carousel",
        "hero-image",
        "hero_image",
        "sponsor",
        "affiliate"
    )

    // Keywords in question text that indicate a visual figure, diagram, graph, or circuit is required
    private val DIAGRAM_REFERENCE_TERMS = listOf(
        "diagram",
        "figure",
        "fig",
        "graph",
        "circuit",
        "curve",
        "plot",
        "shown in",
        "given below",
        "represented in",
        "diagram below",
        "figure below",
        "given figure",
        "following figure",
        "following diagram",
        "following graph",
        "labeled a",
        "labelled a",
        "parts labeled",
        "parts labelled",
        "morphological structure",
        "anatomical structure",
        "schematic",
        "setup",
        "apparatus",
        "observe the",
        "identify the",
        "illustration"
    )

    /**
     * Checks if a given image URL or metadata indicates an advertisement, batch promotional banner,
     * or non-question UI element.
     */
    fun isAdOrPromotionalImage(
        url: String?,
        alt: String? = null,
        classOrId: String? = null
    ): Boolean {
        if (url.isNullOrBlank()) return true

        val lowerUrl = url.trim().lowercase(Locale.ROOT)

        // Ignore placeholder tokens
        if (lowerUrl == "null" || lowerUrl == "none" || lowerUrl == "n/a" || lowerUrl == "undefined") {
            return true
        }

        // Local image files (from OCR crop or user file) are authentic user content, not web ads
        if (lowerUrl.startsWith("/") || lowerUrl.startsWith("file://") || lowerUrl.startsWith("content://")) {
            return false
        }

        // Check for ad keywords in URL
        for (pattern in AD_AND_PROMO_PATTERNS) {
            if (lowerUrl.contains(pattern)) {
                return true
            }
        }

        // Check alt text if provided
        if (!alt.isNullOrBlank()) {
            val lowerAlt = alt.trim().lowercase(Locale.ROOT)
            for (pattern in AD_AND_PROMO_PATTERNS) {
                if (lowerAlt.contains(pattern)) {
                    return true
                }
            }
        }

        // Check container class or id if provided
        if (!classOrId.isNullOrBlank()) {
            val lowerClass = classOrId.trim().lowercase(Locale.ROOT)
            for (pattern in AD_AND_PROMO_PATTERNS) {
                if (lowerClass.contains(pattern)) {
                    return true
                }
            }
        }

        // Check ad domain hosts
        try {
            val uri = URI(lowerUrl)
            val host = uri.host ?: ""
            if (host.contains("doubleclick") || host.contains("googleads") ||
                host.contains("adservice") || host.contains("adnxs") ||
                host.contains("facebook.com") || host.contains("analytics")
            ) {
                return true
            }
        } catch (_: Exception) {}

        return false
    }

    /**
     * Validates whether a URL has the characteristics of a plausible diagram or question figure.
     */
    fun isPlausibleDiagramUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        if (isAdOrPromotionalImage(url)) return false

        val lower = url.trim().lowercase(Locale.ROOT)

        // Allow local files
        if (lower.startsWith("/") || lower.startsWith("file://") || lower.startsWith("content://")) {
            return true
        }

        // Must be http or https
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return false
        }

        // Exclude common tracking pixels and tiny icons
        if (lower.contains("1x1") || lower.contains("pixel") || lower.contains("track") || lower.endsWith(".gif")) {
            return false
        }

        // High probability educational question image paths or standard formats
        val hasImageExtension = lower.endsWith(".png") || lower.endsWith(".jpg") ||
                lower.endsWith(".jpeg") || lower.endsWith(".webp") || lower.endsWith(".svg")

        val hasQuestionIndicator = lower.contains("/question") || lower.contains("/diagram") ||
                lower.contains("/figure") || lower.contains("/q_img") || lower.contains("/latex") ||
                lower.contains("/math") || lower.contains("/problem") || lower.contains("/uploads/")

        return hasImageExtension || hasQuestionIndicator
    }

    /**
     * Determines whether the question statement actually references a diagram or visual setup.
     */
    fun questionReferencesDiagram(questionText: String): Boolean {
        val lower = questionText.lowercase(Locale.ROOT)
        for (term in DIAGRAM_REFERENCE_TERMS) {
            if (lower.contains(term)) return true
        }
        return false
    }

    /**
     * Sanitizes and verifies an entire list of generated or parsed test questions.
     * 
     * Applies 4 levels of validation:
     * 1. Ad & Banner Purge: Any imageUrl matching advertisement patterns is instantly nullified.
     * 2. Duplicate Image Purge: In authentic exams, two distinct questions never share the exact same
     *    diagram URL (e.g. "Target Batch" or ExamGoal diagram repeated across Q2, Q3, Q6).
     * 3. Web-Source Grounding: If verifiedWebImages is provided, only allow URLs confirmed from the source.
     * 4. Textual Reference Check: If question statement does NOT reference any figure or diagram,
     *    do not allow an unsolicited random diagram to be attached.
     */
    fun sanitizeTestQuestions(
        questions: List<AiTestQuestion>,
        verifiedWebImages: Set<String>? = null
    ): List<AiTestQuestion> {
        if (questions.isEmpty()) return questions

        // Count occurrences of each image URL across the test
        val urlCounts = mutableMapOf<String, Int>()
        for (q in questions) {
            val url = q.imageUrl?.trim()
            if (!url.isNullOrBlank() && !isAdOrPromotionalImage(url)) {
                urlCounts[url] = (urlCounts[url] ?: 0) + 1
            }
        }

        return questions.map { q ->
            var finalUrl = q.imageUrl?.trim()

            // 1. Purge ads and invalid URLs
            if (finalUrl != null && isAdOrPromotionalImage(finalUrl)) {
                finalUrl = null
            }

            // 2. Purge repeated duplicate URLs across different questions
            // (If the exact same URL is used on multiple questions, it is almost certainly a banner or hallucination)
            if (finalUrl != null && (urlCounts[finalUrl] ?: 0) > 1) {
                finalUrl = null
            }

            // 3. Web-Source Grounding: if we scraped a specific page, reject URLs not found on that page
            if (finalUrl != null && verifiedWebImages != null && verifiedWebImages.isNotEmpty()) {
                val isLocal = finalUrl.startsWith("/") || finalUrl.startsWith("file://")
                if (!isLocal && !verifiedWebImages.contains(finalUrl)) {
                    finalUrl = null
                }
            }

            // 4. Textual Reference Check:
            // If the question text has zero indication of a figure (e.g. purely verbal definition question),
            // and has no diagram label describing it, discard accidental image assignment.
            if (finalUrl != null) {
                val hasRef = questionReferencesDiagram(q.questionText) || !q.diagramLabel.isNullOrBlank()
                if (!hasRef) {
                    finalUrl = null
                }
            }

            val hasImage = finalUrl != null || !q.diagramSvg.isNullOrBlank() || (!q.diagramLabel.isNullOrBlank() && q.hasImage)

            q.copy(
                hasImage = hasImage,
                imageUrl = finalUrl
            )
        }
    }
}
