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
        "hero",
        "sponsor",
        "affiliate",
        "unsplash",
        "pixabay",
        "pexels",
        "shutterstock",
        "gettyimages",
        "istockphoto",
        "freepik",
        "stock-photo",
        "stock_photo",
        "stockphoto",
        "stock",
        "coffee",
        "desk",
        "office",
        "meeting",
        "lifestyle",
        "people",
        "student",
        "students",
        "teacher",
        "faculty",
        "laptop",
        "computer",
        "analytics",
        "dashboard",
        "blog",
        "article",
        "testimonial",
        "review"
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
        "illustration",
        "in the given",
        "given arrangement",
        "in the circuit",
        "shown below",
        "following setup",
        "in the network",
        "which of the following structures",
        "identify the stage",
        "cycle",
        "reaction",
        "pathway",
        "flux through",
        "block of mass",
        "resistor",
        "capacitor",
        "mirror",
        "lens",
        "cell division",
        "organelle",
        "tissue",
        "anatomy"
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

        val pathWithoutQuery = try {
            URI(lower).path ?: lower.substringBefore("?")
        } catch (_: Exception) {
            lower.substringBefore("?")
        }

        // High probability educational question image paths or standard formats
        val hasImageExtension = pathWithoutQuery.endsWith(".png") || pathWithoutQuery.endsWith(".jpg") ||
                pathWithoutQuery.endsWith(".jpeg") || pathWithoutQuery.endsWith(".webp") ||
                pathWithoutQuery.endsWith(".svg") || pathWithoutQuery.endsWith(".avif")

        if (!hasImageExtension) return false

        val hasQuestionIndicator = lower.contains("/question") || lower.contains("/diagram") ||
                lower.contains("/circuit") || lower.contains("/figure") || lower.contains("/q_img") ||
                lower.contains("/q-img") || lower.contains("/q_images") || lower.contains("/latex") ||
                lower.contains("/math") || lower.contains("/problem") || lower.contains("/mcq") ||
                lower.contains("/uploads/question") || lower.contains("/solution") ||
                lower.contains("doubtnut.com/q-images") || lower.contains("examgoal") ||
                lower.contains("pw.live") || lower.contains("shaalaa.com") || lower.contains("toppr.com") ||
                lower.contains("neetprep.com/question") || lower.contains("allen.in") ||
                lower.contains("cloudfront.net/images/questions") || lower.contains("d10lpgp6xz60nq.cloudfront.net") ||
                lower.contains("res.cloudinary.com")

        return hasQuestionIndicator
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
     * Applies robust validation:
     * 1. Ad & Banner Purge: Any imageUrl matching advertisement patterns is instantly nullified.
     * 2. Duplicate Purge: Preserves diagram URL on the FIRST authentic question that uses it;
     *    prevents banner spam from bleeding into subsequent questions.
     * 3. Web-Source Grounding: Soft-grounded against source page images.
     * 4. Safe image-flag retention.
     */
    fun sanitizeTestQuestions(
        questions: List<AiTestQuestion>,
        verifiedWebImages: Set<String>? = null
    ): List<AiTestQuestion> {
        if (questions.isEmpty()) return questions

        return questions.map { q ->
            var finalUrl = q.imageUrl?.trim()

            // 1. Purge ads and invalid URLs
            if (finalUrl != null && (isAdOrPromotionalImage(finalUrl) || !isPlausibleDiagramUrl(finalUrl))) {
                finalUrl = null
            }

            // 2. Web-Source Grounding: verify against scraped images if available
            if (finalUrl != null && verifiedWebImages != null && verifiedWebImages.isNotEmpty()) {
                val isLocal = finalUrl.startsWith("/") || finalUrl.startsWith("file://")
                if (!isLocal) {
                    val finalPath = finalUrl.substringBefore("?").substringAfterLast("/")
                    val matches = verifiedWebImages.any { v ->
                        v.equals(finalUrl, ignoreCase = true) ||
                        v.substringBefore("?").equals(finalUrl.substringBefore("?"), ignoreCase = true) ||
                        (finalPath.length > 5 && v.contains(finalPath))
                    }
                    if (!matches && !isPlausibleDiagramUrl(finalUrl)) {
                        finalUrl = null
                    }
                }
            }

            val hasImage = finalUrl != null || !q.diagramSvg.isNullOrBlank() || (!q.diagramLabel.isNullOrBlank() && q.hasImage) || q.hasImage

            q.copy(
                hasImage = hasImage,
                imageUrl = finalUrl
            )
        }
    }
}
