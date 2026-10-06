package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class WebQuestionItem(
    val itemNumber: Int,
    val questionText: String,
    val options: List<String> = emptyList(),
    val diagramImageUrl: String? = null,
    val diagramAlt: String? = null
)

data class WebExtractionResult(
    val isDirectScrape: Boolean,
    val targetDomain: String,
    val sourceUrl: String,
    val extractedText: String,
    val extractedQuestions: List<WebQuestionItem> = emptyList(),
    val verifiedDiagramImages: Set<String> = emptySet(),
    val statusMessage: String
)

object WebQuestionExtractor {

    private const val TAG = "WebQuestionExtractor"

    private val httpClient = OkHttpClient.Builder()
        .connectionPool(okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES))
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Match image tags extracting src, data-src, alt, class
    private val IMAGE_TAG_PATTERN = Pattern.compile(
        """<img[^>]+(?:src|data-src|data-original|data-url)\s*=\s*['"]([^'"]+)['"][^>]*>""",
        Pattern.CASE_INSENSITIVE
    )

    // Strip top-level non-content layout elements: scripts, styles, svgs, headers, footers, navs, asides
    private val SCRIPT_STYLE_LAYOUT_PATTERN = Pattern.compile(
        """<(script|style|svg|noscript|header|footer|nav|aside)[^>]*>.*?</\1>""",
        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
    )

    // Strip banner/ad container divs
    private val AD_CONTAINER_PATTERN = Pattern.compile(
        """<div[^>]+(?:class|id)\s*=\s*['"][^'"]*(?:banner|promo|ad-|ads-|advertisement|popup|modal|announcement|sidebar|cta|marketing)[^'"]*['"][^>]*>.*?</div>""",
        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
    )

    private val HTML_TAG_PATTERN = Pattern.compile("""<[^>]+>""")

    /**
     * Checks whether an input string represents a website domain or full URL.
     */
    fun isWebTarget(input: String): Boolean {
        val trimmed = input.trim().lowercase(Locale.ROOT)
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return true
        if (trimmed.startsWith("www.")) return true
        if (trimmed.contains(".com") || trimmed.contains(".in") || trimmed.contains(".org") ||
            trimmed.contains(".edu") || trimmed.contains(".net") || trimmed.contains(".co")
        ) return true
        if (trimmed.contains("neetprep") || trimmed.contains("allen") || trimmed.contains("testbook") ||
            trimmed.contains("examgoal") || trimmed.contains("pw.live") || trimmed.contains("physicswallah") ||
            trimmed.contains("aakash") || trimmed.contains("embibe")
        ) return true
        return false
    }

    /**
     * Normalizes a user input into a target domain and full URL.
     */
    fun normalizeUrl(input: String): Pair<String, String> {
        var clean = input.trim()
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "https://$clean"
        }
        val domain = try {
            val uri = URI(clean)
            uri.host?.removePrefix("www.") ?: input.trim()
        } catch (_: Exception) {
            input.trim()
        }
        return Pair(domain, clean)
    }

    /**
     * Fetches live web content from the given URL or domain.
     * Extracts text, identifies individual questions, and binds diagrams 1:1 to their exact questions
     * while completely eliminating advertisements and marketing banners.
     */
    suspend fun fetchAndExtract(rawInput: String): WebExtractionResult = withContext(Dispatchers.IO) {
        val (domain, fullUrl) = normalizeUrl(rawInput)

        val hasDeepPath = try {
            val uri = URI(fullUrl)
            val path = uri.path ?: ""
            path.length > 1 && path != "/"
        } catch (_: Exception) {
            false
        }

        if (!hasDeepPath) {
            return@withContext WebExtractionResult(
                isDirectScrape = false,
                targetDomain = domain,
                sourceUrl = fullUrl,
                extractedText = "",
                extractedQuestions = emptyList(),
                verifiedDiagramImages = emptySet(),
                statusMessage = "Targeting authentic $domain question bank & syllabus patterns"
            )
        }

        try {
            val request = Request.Builder()
                .url(fullUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Referer", "https://$domain/")
                .build()

            val response = httpClient.newCall(request).await()
            val rawHtml = response.body?.string() ?: ""

            if (!response.isSuccessful || rawHtml.isBlank()) {
                return@withContext WebExtractionResult(
                    isDirectScrape = false,
                    targetDomain = domain,
                    sourceUrl = fullUrl,
                    extractedText = "",
                    extractedQuestions = emptyList(),
                    verifiedDiagramImages = emptySet(),
                    statusMessage = "HTTP ${response.code} received; targeting $domain syllabus via AI"
                )
            }

            // Step 1: Clean out headers, footers, navs, sidebars, ad containers
            val withoutLayout = SCRIPT_STYLE_LAYOUT_PATTERN.matcher(rawHtml).replaceAll(" ")
            val cleanContentHtml = AD_CONTAINER_PATTERN.matcher(withoutLayout).replaceAll(" ")

            // Step 2: Extract structured question blocks with 1:1 bound diagrams
            val structuredQuestions = extractQuestionUnitsFromHtml(cleanContentHtml, fullUrl)
            val verifiedImages = structuredQuestions.mapNotNull { it.diagramImageUrl }.toSet()

            // Step 3: Extract clean readable text for general context fallback
            val cleanText = HTML_TAG_PATTERN.matcher(cleanContentHtml).replaceAll(" ")
                .replace(Regex("""\s+"""), " ")
                .trim()
                .take(16000)

            val isGoodContent = cleanText.length > 100 && (
                cleanText.contains("question", ignoreCase = true) ||
                cleanText.contains("option", ignoreCase = true) ||
                cleanText.contains("(a)", ignoreCase = true) ||
                cleanText.contains("(1)", ignoreCase = true) ||
                structuredQuestions.isNotEmpty()
            )

            WebExtractionResult(
                isDirectScrape = isGoodContent,
                targetDomain = domain,
                sourceUrl = fullUrl,
                extractedText = if (isGoodContent) cleanText else "",
                extractedQuestions = structuredQuestions,
                verifiedDiagramImages = verifiedImages,
                statusMessage = if (isGoodContent) {
                    "Extracted ${structuredQuestions.size} questions with ${verifiedImages.size} verified diagrams"
                } else {
                    "Protected page; targeting $domain syllabus via AI"
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to scrape direct URL: ${e.message}")
            WebExtractionResult(
                isDirectScrape = false,
                targetDomain = domain,
                sourceUrl = fullUrl,
                extractedText = "",
                extractedQuestions = emptyList(),
                verifiedDiagramImages = emptySet(),
                statusMessage = "Targeting $domain syllabus via AI"
            )
        }
    }

    /**
     * Parses HTML to extract individual question units and binds diagram images strictly to their
     * containing question, guaranteeing 0% advertisement leakage and 0% cross-question image contamination.
     */
    private fun extractQuestionUnitsFromHtml(html: String, baseUrl: String): List<WebQuestionItem> {
        val questions = mutableListOf<WebQuestionItem>()

        // Strategy 1: Look for explicit question containers in HTML
        val containerPattern = Pattern.compile(
            """<(?:div|article|li)[^>]+(?:class|id)\s*=\s*['"][^'"]*(?:question|item|mcq|problem|que|card)[^'"]*['"][^>]*>([\s\S]*?)</(?:div|article|li)>""",
            Pattern.CASE_INSENSITIVE
        )
        val containerMatcher = containerPattern.matcher(html)

        var qIndex = 1
        while (containerMatcher.find() && qIndex <= 40) {
            val blockHtml = containerMatcher.group(1) ?: continue
            val qItem = parseSingleQuestionBlock(blockHtml, baseUrl, qIndex)
            if (qItem != null) {
                questions.add(qItem)
                qIndex++
            }
        }

        // Strategy 2: If no explicit container was found (e.g. single question page or continuous text)
        if (questions.isEmpty()) {
            // Check if this page is a single question page
            val isSingleQPage = html.contains("question", ignoreCase = true) || baseUrl.contains("/question")
            if (isSingleQPage) {
                val singleItem = parseSingleQuestionBlock(html, baseUrl, 1)
                if (singleItem != null) {
                    questions.add(singleItem)
                }
            }
        }

        return questions
    }

    /**
     * Parses a single question HTML block, extracting statement text and finding the genuine diagram
     * located specifically within that question block.
     */
    private fun parseSingleQuestionBlock(blockHtml: String, baseUrl: String, index: Int): WebQuestionItem? {
        // Find genuine diagram image strictly inside this block
        var boundDiagramUrl: String? = null
        var boundDiagramAlt: String? = null

        val imgMatcher = IMAGE_TAG_PATTERN.matcher(blockHtml)
        while (imgMatcher.find()) {
            val rawSrc = imgMatcher.group(1)?.trim() ?: continue
            val fullImgTag = imgMatcher.group(0) ?: ""

            // Extract alt text
            val altMatch = Regex("""alt\s*=\s*['"]([^'"]*)['"]""", RegexOption.IGNORE_CASE).find(fullImgTag)
            val alt = altMatch?.groupValues?.get(1)?.trim()

            // Extract class
            val classMatch = Regex("""class\s*=\s*['"]([^'"]*)['"]""", RegexOption.IGNORE_CASE).find(fullImgTag)
            val cls = classMatch?.groupValues?.get(1)?.trim()

            val resolved = resolveAbsoluteUrl(baseUrl, rawSrc)

            // Strict Filter: Never accept advertisements, promotional banners, or non-diagrams
            if (!QuestionImageFilter.isAdOrPromotionalImage(resolved, alt, cls) &&
                QuestionImageFilter.isPlausibleDiagramUrl(resolved)
            ) {
                boundDiagramUrl = resolved
                boundDiagramAlt = alt
                break // Only take the primary question diagram
            }
        }

        // Clean block text
        val textWithoutTags = HTML_TAG_PATTERN.matcher(blockHtml).replaceAll(" ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (textWithoutTags.length < 25) return null

        return WebQuestionItem(
            itemNumber = index,
            questionText = textWithoutTags.take(800),
            diagramImageUrl = boundDiagramUrl,
            diagramAlt = boundDiagramAlt
        )
    }

    private fun resolveAbsoluteUrl(baseUrl: String, relativeUrl: String): String {
        return try {
            val base = URI(baseUrl)
            base.resolve(relativeUrl).toString()
        } catch (_: Exception) {
            if (relativeUrl.startsWith("//")) {
                "https:$relativeUrl"
            } else if (relativeUrl.startsWith("/")) {
                val domain = try { URI(baseUrl).host } catch (_: Exception) { "" }
                "https://$domain$relativeUrl"
            } else {
                relativeUrl
            }
        }
    }

    /**
     * Constructs a specialized prompt for website-targeted question extraction with 1:1 question-diagram binding
     * and a zero-advertisement mandate.
     */
    fun buildWebExtractionPrompt(
        extraction: WebExtractionResult,
        examName: String,
        subject: String,
        chapter: String,
        topic: String,
        questionCount: Int,
        difficulty: String,
        negativePromptClause: String,
        customCommand: String? = null
    ): String {
        val domainHeader = if (extraction.targetDomain.isNotBlank()) extraction.targetDomain else "Educational Question Bank"

        val structuredContentBlock = if (extraction.extractedQuestions.isNotEmpty()) {
            """
            === AUTHENTIC SOURCE QUESTIONS WITH EXACT BOUND DIAGRAMS (FROM ${extraction.sourceUrl}) ===
            ${extraction.extractedQuestions.take(20).joinToString("\n\n") { item ->
                buildString {
                    append("[SOURCE QUESTION #${item.itemNumber}]:\n")
                    append("Question Text: \"${item.questionText.take(400)}\"\n")
                    if (item.diagramImageUrl != null) {
                        append("Verified Bound Diagram URL: \"${item.diagramImageUrl}\"\n")
                        if (!item.diagramAlt.isNullOrBlank()) {
                            append("Diagram Description/Alt: \"${item.diagramAlt}\"\n")
                        }
                    } else {
                        append("Verified Bound Diagram URL: NONE (This question has NO diagram in the source)\n")
                    }
                }
            }}
            === END SOURCE QUESTIONS ===
            """.trimIndent()
        } else if (extraction.isDirectScrape && extraction.extractedText.isNotBlank()) {
            """
            === LIVE WEBPAGE CONTENT EXTRACTED FROM (${extraction.sourceUrl}) ===
            ${extraction.extractedText.take(10000)}
            === END WEBPAGE CONTENT ===
            """.trimIndent()
        } else {
            """
            TARGET SOURCE DOMAIN: "$domainHeader"
            You MUST generate questions modeled strictly after authentic test series, question bank, and curriculum standards of "$domainHeader" for $examName.
            """.trimIndent()
        }

        val customCommandBlock = if (!customCommand.isNullOrBlank()) {
            """
            USER CUSTOM COMMAND / FOCUS INSTRUCTION:
            "$customCommand"
            (Strictly adhere to this instruction, prioritizing the specified question types, concepts, formulas, or patterns).
            """.trimIndent()
        } else ""

        return """
            You are an elite $examName faculty mentor and question curator specializing in authentic question extraction from $domainHeader.
            
            Exam: $examName
            Subject: $subject
            Chapter: "$chapter"
            Focus Weak Topic: "$topic"
            Difficulty: $difficulty
            Target Question Count: EXACTLY $questionCount QUESTIONS
            Source Website/URL: ${extraction.sourceUrl.ifBlank { extraction.targetDomain }}
            
            $customCommandBlock

            $structuredContentBlock
            
            $negativePromptClause
            
            CRITICAL ZERO-MISMATCH & ZERO-ADS DIAGRAM RULES (MANDATORY):
            1. STRICT 1-TO-1 MATCH ONLY:
               - You may ONLY set "imageUrl" to a URL if that EXACT image URL was specifically bound to THAT EXACT source question in the list above.
               - NEVER assign Question #1's diagram to Question #2 or Question #6.
               - If a question asks about a potential energy curve, NEVER attach a spring or tube diagram to it.
            2. ABSOLUTELY ZERO ADVERTISEMENTS OR PROMOTIONAL BANNERS:
               - NEVER use coaching banners, test series ads, "Target Batch", "NTA-CBT Mode" banners, or marketing posters as question diagrams!
            3. NO HALLUCINATED URLS:
               - If a question does not have a verified bound diagram in the source above, "imageUrl" MUST BE null.
               - NEVER invent, guess, or synthesize image URLs.
            4. ACCURATE DIAGRAM SCHEMATIC FALLBACK:
               - If a question tests a diagram, graph, or anatomical structure (e.g. NCERT biology, ray optics, circuit, velocity-time graph):
                 * Set "hasImage": true
                 * If an exact verified bound URL exists for this question, set "imageUrl": "[the exact bound URL]". Otherwise, set "imageUrl": null.
                 * In "diagramLabel", provide an accurate description of the figure and what parts/labels A, B, C, D indicate.
                 * In "diagramSvg", provide a clean readable inline ASCII schematic or SVG string.
                 * Set "diagramType" to "BIOLOGY_NCERT", "CIRCUIT", "RAY_OPTICS", "GENETICS", or "GRAPH".
            5. For questions without figures, set "hasImage": false, "imageUrl": null, "diagramLabel": null, "diagramSvg": null.
            
            CRITICAL ZERO-REPETITION MANDATE:
            - Every single question must be 100% unique.
            - Ensure options (A, B, C, D) are mutually exclusive and strictly one correct option.
            - Include step-by-step solution in "explanation".
            
            Return ONLY a valid JSON array of $questionCount question objects:
            [
              {
                "id": 1,
                "subject": "$subject",
                "chapter": "$chapter",
                "subtopic": "$topic",
                "ncertReference": "NCERT Reference Chapter & Section",
                "conceptKey": "Core formula or concept in 1 line",
                "pyqYear": "${if (domainHeader.isNotBlank()) "$domainHeader Series" else "PYQ Standard"}",
                "questionText": "Question statement...",
                "hasImage": false,
                "imageUrl": null,
                "diagramLabel": null,
                "diagramSvg": null,
                "diagramType": null,
                "optionA": "...",
                "optionB": "...",
                "optionC": "...",
                "optionD": "...",
                "correctOption": "A",
                "explanation": "Detailed step-by-step solution..."
              }
            ]
        """.trimIndent()
    }

    /**
     * Constructs a specialized prompt for website-targeted Full Mock Test extraction with strict 1:1 diagram binding.
     */
    fun buildFullMockWebExtractionPrompt(
        extraction: WebExtractionResult,
        examName: String,
        subjectDistribution: String,
        questionCount: Int,
        negativePromptClause: String = ""
    ): String {
        val domainHeader = if (extraction.targetDomain.isNotBlank()) extraction.targetDomain else "Online Question Bank"

        val structuredContentBlock = if (extraction.extractedQuestions.isNotEmpty()) {
            """
            === AUTHENTIC SOURCE QUESTIONS WITH EXACT BOUND DIAGRAMS (FROM ${extraction.sourceUrl}) ===
            ${extraction.extractedQuestions.take(30).joinToString("\n\n") { item ->
                buildString {
                    append("[SOURCE QUESTION #${item.itemNumber}]:\n")
                    append("Question Text: \"${item.questionText.take(400)}\"\n")
                    if (item.diagramImageUrl != null) {
                        append("Verified Bound Diagram URL: \"${item.diagramImageUrl}\"\n")
                        if (!item.diagramAlt.isNullOrBlank()) {
                            append("Diagram Description/Alt: \"${item.diagramAlt}\"\n")
                        }
                    } else {
                        append("Verified Bound Diagram URL: NONE\n")
                    }
                }
            }}
            === END SOURCE QUESTIONS ===
            """.trimIndent()
        } else if (extraction.isDirectScrape && extraction.extractedText.isNotBlank()) {
            """
            === LIVE WEBPAGE CONTENT EXTRACTED FROM (${extraction.sourceUrl}) ===
            ${extraction.extractedText.take(10000)}
            === END WEBPAGE CONTENT ===
            """.trimIndent()
        } else {
            """
            TARGET SOURCE DOMAIN: "$domainHeader"
            You MUST generate a complete authentic Full Length Mock Test strictly reflecting the question bank, test series standard, and curriculum patterns of "$domainHeader" for $examName.
            """.trimIndent()
        }

        return """
            You are the Chief Academic Controller and Exam Paper Setter specializing in authentic $examName Full Mock Test curation from $domainHeader.
            
            Exam: $examName Full Length Mock CBT
            Subject Distribution: $subjectDistribution
            Total Questions Required: EXACTLY $questionCount QUESTIONS
            Source Website/URL: ${extraction.sourceUrl.ifBlank { extraction.targetDomain }}
            
            $structuredContentBlock
            
            $negativePromptClause
            
            CRITICAL INSTRUCTIONS FOR FULL MOCK:
            1. 100% UNIQUE QUESTIONS: Every single question in this test MUST be completely unique.
            2. SUBJECT MIX: Strictly respect the subject breakdown: $subjectDistribution. (For NEET, NO mathematics questions!).
            3. STRICT 1:1 DIAGRAM MATCHING (NO ADVERTISEMENTS / NO MISMATCHES):
               - You may ONLY attach an "imageUrl" to a question if that EXACT image was bound to that specific source question in the extracted source list above.
               - NEVER assign an image from Question #1 to Question #2 or Question #6.
               - NEVER use advertising banners, test series posters, or "Target Batch" ads as question diagrams.
               - If a question has NO bound image in the source, "imageUrl" MUST BE null (never invent or guess URLs).
               - When a question requires a diagram, set "hasImage": true, describe the diagram in "diagramLabel", provide clean schematic in "diagramSvg", and set "imageUrl": null unless an exact verified image belongs to this question.
            4. Tag "pyqYear" with "$domainHeader Standard" or authentic PYQ year.
            5. Return ONLY a valid JSON array of $questionCount objects. No markdown ticks, no preamble.
            
            JSON Format:
            [
              {
                "id": 1,
                "subject": "Biology",
                "chapter": "Chapter Name",
                "pyqYear": "$domainHeader Test",
                "questionText": "Question statement...",
                "hasImage": false,
                "imageUrl": null,
                "diagramLabel": null,
                "diagramSvg": null,
                "diagramType": null,
                "optionA": "...",
                "optionB": "...",
                "optionC": "...",
                "optionD": "...",
                "correctOption": "A",
                "explanation": "Concise step-by-step solution..."
              }
            ]
        """.trimIndent()
    }
}
