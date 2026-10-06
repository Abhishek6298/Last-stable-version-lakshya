package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * High-Capacity Smart OCR Pipeline for 30-40 Page Test Papers
 * Features:
 * - Chunked/Multi-Page batch processing preventing token exhaustion
 * - Vision-based diagram, circuit, and graph detection & auto-screenshot cropping
 * - Institute tagging (Allen, Aakash, PW, Motion, etc.)
 * - Automatic step-by-step NCERT solutions and difficulty classification
 */
object SmartOcrMultiPageEngine {

    data class ExtractionProgress(
        val currentBatch: Int,
        val totalBatches: Int,
        val currentPageIndex: Int,
        val totalPages: Int,
        val questionsExtracted: Int,
        val diagramsExtracted: Int,
        val statusMessage: String
    )

    data class ParsedQuestionWithBbox(
        val question: AiTestQuestion,
        val bbox: List<Number>? // [ymin, xmin, ymax, xmax] in normalized (0-1, 0-100, 0-1000) or pixel coordinates
    )

    private val client = OkHttpClient.Builder()
        .connectionPool(okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun getApiKey(context: Context, customApiKey: String? = null): String {
        if (!customApiKey.isNullOrBlank()) return customApiKey.trim()
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return (prefs.getString("gemini_api_key_1", "") ?: "").trim()
    }

    fun getApiKeys(context: Context, customApiKey: String? = null): List<String> {
        val singleKey = getApiKey(context, customApiKey)
        return if (singleKey.isNotBlank()) listOf(singleKey) else emptyList()
    }

    /**
     * Inspects a PDF to quickly return its page count without loading the whole file into memory.
     */
    fun getPdfTotalPages(context: Context, uri: Uri): Int {
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return 1
            pfd.use { descriptor ->
                val renderer = PdfRenderer(descriptor)
                val count = renderer.pageCount
                renderer.close()
                count
            }
        } catch (e: Exception) {
            1
        }
    }

    suspend fun extractTest(
        context: Context,
        exam: ExamCategory,
        uri: Uri,
        fileName: String,
        institute: String = "Self/General",
        startPage: Int = 1,
        endPage: Int? = null,
        scanLanguage: String = "English",
        autoCropDiagrams: Boolean = true,
        customApiKey: String? = null,
        answerKeyUri: Uri? = null,
        answerKeyText: String? = null,
        aiProvider: AiProvider? = null,
        openRouterModel: String? = null,
        openRouterApiKey: String? = null,
        onProgress: ((ExtractionProgress) -> Unit)? = null
    ): Result<List<AiTestQuestion>> = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val providerPref = prefs.getString("ai_provider", AiProvider.NATIVE_GEMINI.name) ?: AiProvider.NATIVE_GEMINI.name
            val resolvedProvider = aiProvider ?: try { AiProvider.valueOf(providerPref) } catch (e: Exception) { AiProvider.NATIVE_GEMINI }
            val resolvedOrKey = (openRouterApiKey ?: prefs.getString("openrouter_api_key", "") ?: "").trim()
            val resolvedOrModel = (openRouterModel ?: prefs.getString("openrouter_selected_model", "google/gemini-2.5-flash") ?: "google/gemini-2.5-flash").trim()

            val keys = getApiKeys(context, customApiKey)
            if (resolvedProvider == AiProvider.OPENROUTER && resolvedOrKey.isBlank() && keys.isEmpty()) {
                return@withContext Result.failure(Exception("OpenRouter API key is required. Please configure your OpenRouter key in Settings or switch to Native Gemini."))
            } else if (resolvedProvider == AiProvider.GROQ && GroqManager.getGroqApiKey(context).isBlank()) {
                return@withContext Result.failure(Exception("Groq API key is required. Please configure your Groq key in Settings."))
            } else if (resolvedProvider == AiProvider.CLOUDFLARE && (prefs.getString(CloudflareManager.KEY_CF_ACCOUNT_ID, "")?.trim().isNullOrBlank() || prefs.getString(CloudflareManager.KEY_CF_API_TOKEN, "")?.trim().isNullOrBlank())) {
                return@withContext Result.failure(Exception("Cloudflare Account ID and API Token are required for OCR. Please configure them in Settings."))
            } else if (resolvedProvider == AiProvider.NATIVE_GEMINI && keys.isEmpty()) {
                return@withContext Result.failure(Exception("Lakshya AI Key is required to scan and extract questions. Please configure your key in Settings."))
            }

            // Extract Answer Key first if provided
            val answerMap = if (answerKeyUri != null || !answerKeyText.isNullOrBlank()) {
                onProgress?.invoke(
                    ExtractionProgress(
                        currentBatch = 0,
                        totalBatches = 1,
                        currentPageIndex = 0,
                        totalPages = 1,
                        questionsExtracted = 0,
                        diagramsExtracted = 0,
                        statusMessage = "🔑 Reading official Answer Key..."
                    )
                )
                parseAnswerKey(
                    context = context,
                    keys = keys,
                    answerKeyUri = answerKeyUri,
                    answerKeyText = answerKeyText,
                    aiProvider = resolvedProvider,
                    openRouterModel = resolvedOrModel,
                    openRouterApiKey = resolvedOrKey
                )
            } else emptyMap()

            val mimeType = context.contentResolver.getType(uri) ?: when {
                fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                fileName.endsWith(".png", ignoreCase = true) -> "image/png"
                fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
                else -> "image/jpeg"
            }

            val isPdf = mimeType.equals("application/pdf", ignoreCase = true) || fileName.endsWith(".pdf", ignoreCase = true)

            val rawResult = if (isPdf) {
                extractFromPdfMultiPage(
                    context = context,
                    exam = exam,
                    uri = uri,
                    fileName = fileName,
                    institute = institute,
                    startPage = startPage,
                    endPage = endPage,
                    scanLanguage = scanLanguage,
                    autoCropDiagrams = autoCropDiagrams,
                    keys = keys,
                    aiProvider = resolvedProvider,
                    openRouterModel = resolvedOrModel,
                    openRouterApiKey = resolvedOrKey,
                    onProgress = onProgress
                )
            } else {
                extractFromSingleImage(
                    context = context,
                    exam = exam,
                    uri = uri,
                    fileName = fileName,
                    institute = institute,
                    mimeType = mimeType,
                    scanLanguage = scanLanguage,
                    autoCropDiagrams = autoCropDiagrams,
                    keys = keys,
                    aiProvider = resolvedProvider,
                    openRouterModel = resolvedOrModel,
                    openRouterApiKey = resolvedOrKey,
                    onProgress = onProgress
                )
            }

            if (rawResult.isSuccess) {
                val questions = rawResult.getOrNull() ?: emptyList()
                val reconciled = reconcilePassagesAndDiagrams(questions)
                val finalQuestions = applyAnswerKey(reconciled, answerMap)
                Result.success(finalQuestions)
            } else {
                rawResult
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Abhi Magic 🪄 Pipeline Error: ${e.localizedMessage}"))
        }
    }

    suspend fun parseAnswerKey(
        context: Context,
        keys: List<String>,
        answerKeyUri: Uri?,
        answerKeyText: String?,
        aiProvider: AiProvider = AiProvider.NATIVE_GEMINI,
        openRouterModel: String = "google/gemini-2.5-flash",
        openRouterApiKey: String = ""
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val answerMap = mutableMapOf<String, String>()

        // 1. Parse text directly if provided
        if (!answerKeyText.isNullOrBlank()) {
            parseAnswerKeyTextDirectly(answerKeyText, answerMap)
        }

        // 2. If answerKeyUri is provided, use AI Vision to OCR the answer key sheet / table
        if (answerKeyUri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(answerKeyUri)?.use { it.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) {
                    val mimeType = context.contentResolver.getType(answerKeyUri) ?: "image/jpeg"
                    val isPdf = mimeType.contains("pdf", ignoreCase = true)
                    val base64 = if (isPdf) {
                        val pfd = context.contentResolver.openFileDescriptor(answerKeyUri, "r")
                        if (pfd != null) {
                            val renderer = android.graphics.pdf.PdfRenderer(pfd)
                            val page = renderer.openPage(0)
                            val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bmp)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            renderer.close()
                            pfd.close()
                            val stream = java.io.ByteArrayOutputStream()
                            bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                            bmp.recycle()
                            Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                        } else Base64.encodeToString(bytes, Base64.NO_WRAP)
                    } else {
                        Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }

                    val prompt = """
                        You are an expert Answer Key OCR reader for Indian entrance exams (NEET, JEE Main, JEE Advanced).
                        Read ALL question numbers and their corresponding correct answers (e.g., A, B, C, D or numerical numbers like 25, 4.5, 0) from this Answer Key sheet/table.
                        Return ONLY a JSON object mapping question number strings to correct answer strings.
                        Example:
                        {
                          "1": "A",
                          "2": "C",
                          "3": "D",
                          "4": "B",
                          "21": "25",
                          "22": "4.5"
                        }
                    """.trimIndent()

                    val rawJson = callAiVisionDoc(
                        context = context,
                        keys = keys,
                        prompt = prompt,
                        base64Data = base64,
                        mimeType = "image/jpeg",
                        aiProvider = aiProvider,
                        openRouterModel = openRouterModel,
                        openRouterApiKey = openRouterApiKey
                    )
                    val cleaned = rawJson.replace("```json", "").replace("```", "").trim()
                    val jsonObject = JSONObject(cleaned)
                    val keysIter: Iterator<String> = jsonObject.keys()
                    while (keysIter.hasNext()) {
                        val qNo = keysIter.next()
                        val ans = jsonObject.optString(qNo).trim().uppercase()
                        if (ans.isNotBlank()) {
                            answerMap[qNo.trim()] = ans
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        answerMap
    }

    private fun parseAnswerKeyTextDirectly(text: String, map: MutableMap<String, String>) {
        // Support: "1. A", "1-A", "1: (2)", "21: 25.4", "22 = 10", "Q1 A", "Q21 4.5", "1 -> B", multi-column lines
        val pattern = Regex("""(?:Q\.?|Question\s*)?(\d+)\s*[:.\-–=)>\]]*\s*(?:\(?([A-Da-d]|-?\d+(?:\.\d+)?)\)?)""")
        pattern.findAll(text).forEach { match ->
            val qNum = match.groupValues[1].trim()
            var ans = match.groupValues[2].trim()
            if (qNum.isNotBlank() && ans.isNotBlank()) {
                // If it's a single digit 1, 2, 3, 4 without decimals, map to A, B, C, D for MCQs
                val normalizedAns = when (ans.uppercase()) {
                    "1", "(1)" -> "A"
                    "2", "(2)" -> "B"
                    "3", "(3)" -> "C"
                    "4", "(4)" -> "D"
                    else -> ans
                }
                map[qNum] = normalizedAns
            }
        }
    }

    fun applyAnswerKey(questions: List<AiTestQuestion>, answerMap: Map<String, String>): List<AiTestQuestion> {
        if (answerMap.isEmpty()) return questions
        return questions.mapIndexed { index, q ->
            val officialAns = answerMap[q.paperQNo?.trim()]
                ?: answerMap[q.id.toString()]
                ?: answerMap[(index + 1).toString()]
            if (officialAns != null && officialAns.isNotBlank()) {
                val isNum = !listOf("A", "B", "C", "D").contains(officialAns.trim().uppercase()) && officialAns.trim().toDoubleOrNull() != null
                q.copy(
                    correctOption = officialAns.trim(),
                    isNumerical = isNum || q.isNumerical
                )
            } else {
                q
            }
        }
    }

    private suspend fun extractFromPdfMultiPage(
        context: Context,
        exam: ExamCategory,
        uri: Uri,
        fileName: String,
        institute: String,
        startPage: Int,
        endPage: Int?,
        scanLanguage: String,
        autoCropDiagrams: Boolean,
        keys: List<String>,
        aiProvider: AiProvider = AiProvider.NATIVE_GEMINI,
        openRouterModel: String = "google/gemini-2.5-flash",
        openRouterApiKey: String = "",
        onProgress: ((ExtractionProgress) -> Unit)?
    ): Result<List<AiTestQuestion>> {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: return Result.failure(Exception("Could not open PDF file descriptor: $fileName"))

        return pfd.use { descriptor ->
            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val effectiveOrModel = (prefs.getString("openrouter_selected_model", "")?.takeIf { it.isNotBlank() } ?: openRouterModel).trim()
            val effectiveAiProvider = try {
                AiProvider.valueOf(prefs.getString("ai_provider", aiProvider.name) ?: aiProvider.name)
            } catch (_: Exception) { aiProvider }
            val effectiveOrKey = (prefs.getString("openrouter_api_key", "")?.takeIf { it.isNotBlank() } ?: openRouterApiKey).trim()

            var renderer: PdfRenderer? = null
            try {
                renderer = PdfRenderer(descriptor)
                val totalPdfPages = renderer.pageCount
                if (totalPdfPages <= 0) {
                    return Result.failure(Exception("PDF has 0 readable pages."))
                }

                val sPage = (startPage - 1).coerceIn(0, totalPdfPages - 1)
                val ePage = ((endPage ?: totalPdfPages) - 1).coerceIn(sPage, totalPdfPages - 1)
                val pagesToProcess = (sPage..ePage).toList()

                val allQuestions = mutableListOf<AiTestQuestion>()
                val seenPaperQNos = mutableSetOf<String>()
                var totalDiagramsSnapped = 0

                val langRule = """
                    CRITICAL LANGUAGE & COLUMN EXTRACTION (ENGLISH ONLY & STRICT ZERO-SKIP FULL SCAN):
                    - ONLY EXTRACT ENGLISH QUESTIONS. Do not extract Hindi or other languages.
                    - ZERO SKIPPING MANDATE: Exhaustively scan the ENTIRE page from top to bottom across ALL COLUMNS and extract 100% of all questions.
                    - TWO-COLUMN PAPER RULE: If the page has multiple columns (e.g., Left and Right columns typical in Allen, Aakash, PW, Motion, Resonance papers), YOU MUST SCAN AND EXTRACT ALL QUESTIONS FROM BOTH COLUMNS top-to-bottom.
                    - NEVER STOP AT COLUMN 1: Finish the Left column completely, then immediately scan the Right column completely!
                    - BILINGUAL SIDE-BY-SIDE RULE: If the page contains both English and Hindi versions of the same question, ONLY extract the English version and ignore the Hindi translation.
                    - CHECK ALL QUESTION NUMBERS: Look for question numbers carefully (e.g. 1, 2, 3... or Q.1, Q.2... or Section A / Section B).
                    - NEVER skip, omit, or drop any question from top to bottom of this page.
                """.trimIndent()

                var lastCaughtError: String? = null
                var pendingPassageText: String? = null
                var pendingPassageRange: IntRange? = null
                var pendingPassageImageUrl: String? = null

                for ((idx, pageIndex) in pagesToProcess.withIndex()) {
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()

                    onProgress?.invoke(
                        ExtractionProgress(
                            currentBatch = idx + 1,
                            totalBatches = pagesToProcess.size,
                            currentPageIndex = pageIndex + 1,
                            totalPages = totalPdfPages,
                            questionsExtracted = allQuestions.size,
                            diagramsExtracted = totalDiagramsSnapped,
                            statusMessage = "Scanning Page ${pageIndex + 1} of $totalPdfPages (Zero-Skip Multi-Column Abhi Magic 🪄 OCR • ${allQuestions.size} Extracted)..."
                        )
                    )

                    val page = renderer.openPage(pageIndex)
                    val renderScale = 2.4f
                    val targetW = (page.width * renderScale).toInt().coerceIn(1200, 2200)
                    val targetH = (page.height * renderScale).toInt().coerceIn(1600, 3200)
                    val pageBitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(pageBitmap)
                    canvas.drawColor(AndroidColor.WHITE)
                    page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val stream = ByteArrayOutputStream()
                    pageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                    val pageBytes = stream.toByteArray()
                    val base64Img = Base64.encodeToString(pageBytes, Base64.NO_WRAP)

                    val promptText = """
                        You are an Elite NTA NEET & JEE Exam Paper Vision OCR Specialist with ZERO-SKIPPING ACCURACY.
                        Document: "$fileName"
                        Institute/Coaching: "$institute"
                        Page: ${pageIndex + 1} of $totalPdfPages
                        Exam Target: ${exam.displayName}
                        Target Language: "$scanLanguage"

                        MANDATE - ABSOLUTE 100% EXHAUSTIVE QUESTION EXTRACTION (ZERO SKIPPING):
                        Every single numbered question printed on this page MUST be extracted into the JSON array.
                        Under NO circumstances should any question be skipped, omitted, summarized, or truncated.
                        If this page contains NO questions at all (e.g. only cover page or blank rough space), return [].

                        $langRule

                        EXHAUSTIVE TWO-COLUMN WORKFLOW:
                        1. Scan Left Column from top to bottom. Extract every single question (e.g. Q1 to Q7).
                        2. Immediately scan Right Column from top to bottom. Extract every single question (e.g. Q8 to Q15).
                        3. DO NOT STOP AFTER COLUMN 1! Both columns MUST be present in the output array.
                        4. Keep metadata concise so you NEVER run out of tokens. Focus on verbatim question text and choices!

                        QUESTION FIELDS:
                        - "paperQNo": Exact printed number on paper (e.g. "1", "12", "73").
                        - "id": Sequential integer starting at ${allQuestions.size + 1}.
                        - "subject": "Physics", "Chemistry", or "Biology".
                        - "chapter": NCERT Chapter name.
                        - "passage": If this question is part of a Reading Paragraph, Comprehension, or Passage (e.g. 'Paragraph for Question 17 to 19'):
                          * STRICT PASSAGE RULE: The passage MUST ONLY be attached to the exact questions specified in its header (e.g. Q17, Q18, Q19). NEVER attach it to preceding questions (like Q16)!
                          * If a Paragraph is printed at the bottom of this page for questions on the next page (e.g. 'Paragraph for Question 17 to 19' at bottom of Page 1): extract it with "paperQNo": "17", include the full text in "passage", and crop its diagram bbox so the diagram is preserved for Q17!
                        - "questionText": Full verbatim question statement with all equations, numbers, units, and clear LaTeX math notation.
                        - "is_numerical": false. ALL questions must strictly be 4-option MCQs. No typing or keyboards allowed.
                        - "optionA", "optionB", "optionC", "optionD": The 4 choices. If question was fill-in-the-blank or numerical on paper, generate 4 realistic choices where exactly 1 option is the verified correct answer.
                        - "correctOption": "A", "B", "C", or "D" corresponding to the verified answer choice. Solve accurately based on NCERT.
                        - "explanation": 1 concise sentence core formula or concept.
                        - "hasImage": true if question has a diagram/figure/circuit/graph, otherwise false.
                        - "diagram_bbox": [ymin, xmin, ymax, xmax] (0 to 1000).
                          * CRITICAL DATA-INCLUSIVE RULE: If the question has a drawing (circuit, graph, pulley, ray diagram, chemical structure, or visual matching figures), the bounding box MUST include the drawing PLUS all attached values, component parameters (e.g. R=5Ω, C=2μF), graph axes markings & units, and labels. Do NOT cut off the values belonging to the diagram!
                          * STRICT MANDATE: Never crop the whole page or multiple questions into diagram_bbox! Crop the specific visual diagram/figure.
                          * If no diagram drawing: "hasImage": false, "diagram_bbox": null.

                        OUTPUT FORMAT:
                        Return ONLY a valid JSON array of question objects containing ALL questions on this page:
                        [
                          {
                            "id": ${allQuestions.size + 1},
                            "paperQNo": "1",
                            "subject": "Physics",
                            "chapter": "Units and Measurements",
                            "questionText": "...",
                            "optionA": "...",
                            "optionB": "...",
                            "optionC": "...",
                            "optionD": "...",
                            "correctOption": "A",
                            "explanation": "...",
                            "hasImage": false,
                            "diagram_bbox": null
                          }
                        ]
                    """.trimIndent()

                    try {
                        val rawJson = callAiVisionDoc(
                            context = context,
                            keys = keys,
                            prompt = promptText,
                            base64Data = base64Img,
                            mimeType = "image/jpeg",
                            aiProvider = effectiveAiProvider,
                            openRouterModel = effectiveOrModel,
                            openRouterApiKey = effectiveOrKey
                        )
                        var parsed = parseQuestionsWithBboxFromJson(rawJson, institute)

                        // Fallback retry if 0 questions extracted on non-empty page
                        if (parsed.isEmpty()) {
                            try {
                                val retryPrompt = """
                                    Extract ALL multiple choice questions visible on this test paper page into a JSON array:
                                    [{"id": ${allQuestions.size + 1}, "paperQNo": "1", "questionText": "...", "optionA": "...", "optionB": "...", "optionC": "...", "optionD": "...", "correctOption": "A", "explanation": "..."}]
                                    Scan both Left and Right columns thoroughly. Do not skip any question!
                                """.trimIndent()
                                val retryJson = callAiVisionDoc(
                                    context = context,
                                    keys = keys,
                                    prompt = retryPrompt,
                                    base64Data = base64Img,
                                    mimeType = "image/jpeg",
                                    aiProvider = effectiveAiProvider,
                                    openRouterModel = effectiveOrModel,
                                    openRouterApiKey = effectiveOrKey
                                )
                                parsed = parseQuestionsWithBboxFromJson(retryJson, institute)
                            } catch (reEx: Exception) {
                                lastCaughtError = reEx.localizedMessage ?: reEx.message
                            }
                        }

                        val pageSeenQuestions = mutableListOf<AiTestQuestion>()

                        for (item in parsed) {
                            val rawQNo = item.question.paperQNo
                            // True duplicate check: only filter if BOTH full question text and choices are truly identical (prevents dropping distinct questions starting with common phrases)
                            val isDuplicate = pageSeenQuestions.any { existing ->
                                val existingClean = existing.questionText.trim().lowercase().replace(Regex("""\s+"""), " ")
                                val newClean = item.question.questionText.trim().lowercase().replace(Regex("""\s+"""), " ")
                                val exactTextMatch = existingClean.isNotBlank() && existingClean == newClean
                                val exactOptionsMatch = existing.optionA.trim().equals(item.question.optionA.trim(), ignoreCase = true) &&
                                        existing.optionB.trim().equals(item.question.optionB.trim(), ignoreCase = true)
                                exactTextMatch && exactOptionsMatch
                            }

                            if (isDuplicate && scanLanguage != "Both") {
                                continue
                            }
                            pageSeenQuestions.add(item.question)

                            var q = item.question.copy(id = allQuestions.size + 1)
                            val bbox = item.bbox
                            if (autoCropDiagrams && bbox != null && bbox.size == 4) {
                                val croppedPath = cropDiagramFromBitmap(context, pageBitmap, bbox, q.id)
                                if (croppedPath != null) {
                                    q = q.copy(hasImage = true, imageUrl = croppedPath)
                                    totalDiagramsSnapped++
                                } else {
                                    // Preserve question's detected image status even if cropping couldn't complete
                                    q = q.copy(hasImage = item.question.hasImage || !item.question.imageUrl.isNullOrBlank())
                                }
                            } else {
                                q = q.copy(hasImage = item.question.hasImage || !item.question.imageUrl.isNullOrBlank())
                            }

                            val qNum = q.paperQNo?.toIntOrNull()

                            // Check if this item has a passage meant for future questions (e.g. Paragraph for Q17-19 on Page 1 where this item is Q16)
                            val itemPassage = (q.passage ?: if (q.questionText.contains("Paragraph for Question", ignoreCase = true)) q.questionText else null)?.trim()
                            val itemPassageRange = if (!itemPassage.isNullOrBlank()) parsePassageQuestionRange(itemPassage) else null

                            if (itemPassage != null && itemPassageRange != null && qNum != null && qNum !in itemPassageRange) {
                                // This passage was attached to Q16, but belongs to Q17..Q19!
                                pendingPassageText = itemPassage
                                pendingPassageRange = itemPassageRange
                                if (!q.imageUrl.isNullOrBlank()) {
                                    pendingPassageImageUrl = q.imageUrl
                                }
                                // Detach the future passage from Q16!
                                q = q.copy(passage = null)
                            }

                            // Check if we have a pending carried passage from a previous page that belongs to this question:
                            if (pendingPassageText != null && pendingPassageRange != null && qNum != null && qNum in pendingPassageRange) {
                                if (q.passage.isNullOrBlank()) {
                                    q = q.copy(passage = pendingPassageText)
                                }
                                if (q.imageUrl.isNullOrBlank() && !pendingPassageImageUrl.isNullOrBlank()) {
                                    q = q.copy(hasImage = true, imageUrl = pendingPassageImageUrl)
                                }
                            }

                            allQuestions.add(q)
                        }
                    } catch (e: Exception) {
                        lastCaughtError = e.localizedMessage ?: e.message
                        e.printStackTrace()
                    } finally {
                        pageBitmap.recycle()
                    }
                }

                val finalQuestions = reconcilePassagesAndDiagrams(allQuestions)
                if (finalQuestions.isNotEmpty()) {
                    onProgress?.invoke(
                        ExtractionProgress(
                            currentBatch = pagesToProcess.size,
                            totalBatches = pagesToProcess.size,
                            currentPageIndex = ePage + 1,
                            totalPages = totalPdfPages,
                            questionsExtracted = finalQuestions.size,
                            diagramsExtracted = totalDiagramsSnapped,
                            statusMessage = "Done: ${finalQuestions.size} Questions Extracted with $totalDiagramsSnapped Diagrams!"
                        )
                    )
                    Result.success(finalQuestions)
                } else {
                    val msg = if (!lastCaughtError.isNullOrBlank()) {
                        "Gemini OCR: $lastCaughtError. Please check your Gemini API key in Settings."
                    } else {
                        "Could not extract MCQs from this PDF. Please ensure the document pages contain readable test questions."
                    }
                    Result.failure(Exception(msg))
                }
            } finally {
                try {
                    renderer?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun extractFromSingleImage(
        context: Context,
        exam: ExamCategory,
        uri: Uri,
        fileName: String,
        institute: String,
        mimeType: String,
        scanLanguage: String,
        autoCropDiagrams: Boolean,
        keys: List<String>,
        aiProvider: AiProvider = AiProvider.NATIVE_GEMINI,
        openRouterModel: String = "google/gemini-2.5-flash",
        openRouterApiKey: String = "",
        onProgress: ((ExtractionProgress) -> Unit)?
    ): Result<List<AiTestQuestion>> {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return Result.failure(Exception("Could not read image file: $fileName"))

        onProgress?.invoke(
            ExtractionProgress(
                currentBatch = 1,
                totalBatches = 1,
                currentPageIndex = 1,
                totalPages = 1,
                questionsExtracted = 0,
                diagramsExtracted = 0,
                statusMessage = "Analyzing test paper image (Zero-Skip Multi-Column Abhi Magic 🪄 OCR)..."
            )
        )

        val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: return Result.failure(Exception("Could not decode image file."))

        // Optimize resolution: Downscale smoothly if > 1920px to maximize upload & OCR speed without losing clarity
        val maxDim = 1920
        val origW = originalBitmap.width
        val origH = originalBitmap.height
        val bitmap = if (origW > maxDim || origH > maxDim) {
            val scale = maxDim.toFloat() / maxOf(origW, origH)
            val newW = (origW * scale).toInt()
            val newH = (origH * scale).toInt()
            val scaled = Bitmap.createScaledBitmap(originalBitmap, newW, newH, true)
            if (scaled != originalBitmap) originalBitmap.recycle()
            scaled
        } else {
            originalBitmap
        }

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val optimizedBytes = stream.toByteArray()
        val base64Img = Base64.encodeToString(optimizedBytes, Base64.NO_WRAP)

        val langRule = """
            CRITICAL LANGUAGE & COLUMN EXTRACTION (ENGLISH ONLY & STRICT ZERO-SKIP FULL SCAN):
            - ONLY EXTRACT ENGLISH QUESTIONS. Do not extract Hindi or other languages.
            - ZERO SKIPPING MANDATE: Exhaustively scan the ENTIRE image from top to bottom across ALL COLUMNS and extract 100% of all questions.
            - TWO-COLUMN PAPER RULE: If the page has multiple columns (e.g., Left and Right columns typical in Allen, Aakash, PW, Motion, Resonance papers), YOU MUST SCAN AND EXTRACT ALL QUESTIONS FROM BOTH COLUMNS top-to-bottom.
            - NEVER STOP AT COLUMN 1: Finish the Left column completely, then immediately scan the Right column completely!
            - BILINGUAL SIDE-BY-SIDE RULE: If the page contains both English and Hindi versions of the same question, ONLY extract the English version and ignore the Hindi translation.
            - CHECK ALL QUESTION NUMBERS: Look for question numbers carefully (e.g. 1, 2, 3... or Q.1, Q.2... or Section A / Section B).
            - NEVER skip, omit, or drop any question from top to bottom of this image.
        """.trimIndent()

        val promptText = """
            You are an Elite NTA NEET & JEE Exam Paper Vision OCR Specialist with ZERO-SKIPPING ACCURACY.
            Image File: "$fileName"
            Institute/Category: "$institute"
            Target Exam: ${exam.displayName}
            Target Language: "$scanLanguage"

            MANDATE - ABSOLUTE ZERO SKIPPING GUARANTEE:
            Exhaustively parse and extract 100% OF ALL Multiple Choice Questions (MCQs), Assertion-Reasons, Match-the-Columns, and Numerical Questions visible on this image.
            Under NO circumstances should any numbered question be skipped, omitted, summarized, or truncated.
            
            $langRule

            SYSTEMATIC COLUMN-BY-COLUMN SCANNING PROTOCOL:
            1. QUESTION NUMBER CENSUS:
               - First, scan across the page to identify ALL printed question numbers (e.g. 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12...).
               - Note both Left Column and Right Column question numbers.
            2. TWO-COLUMN DENSE PAPERS:
               - Coaching test papers typically feature a 2-COLUMN layout.
               - Scan Left column completely from top to bottom. Extract every single question.
               - Scan Right column completely from top to bottom. Extract every single question.
               - Extract EVERY single numbered question. DO NOT skip any questions!
            3. QUESTION TYPES TO FULLY EXTRACT:
               - Standard MCQs: Single Correct with 4 choices A, B, C, D.
               - Assertion - Reason: Include Assertion (A) and Reason (R) statements fully in "questionText".
               - Match the Column / Matching List (List-I & List-II): Include all rows in "questionText".
               - Statement I & Statement II: Include both statements completely.
               - Fill-in-the-blank / Integer Questions: Convert into 4-option MCQs with AI-generated realistic choices.
               - CRITICAL MANDATORY 4-OPTION RULE FOR ALL QUESTIONS (NO KEYBOARDS / NO FILL-IN-THE-BLANK TYPING):
                 * EVERY single question (including Fill-in-the-blank, Integer Type, or Numerical questions) MUST HAVE 4 COMPLETE OPTIONS: "optionA", "optionB", "optionC", "optionD".
                 * If choices are missing or question is fill-in-the-blank on paper: YOU MUST SOLVE THE QUESTION AND CRAFT 4 PLAUSIBLE, HIGH-QUALITY MULTIPLE CHOICE OPTIONS with 1 verified correct answer (A, B, C, or D).
                 * NEVER leave options blank, empty, or 'not provided'! Keyboard systems are completely disabled in CBT.
            4. "paperQNo": The printed question number on the paper (e.g. "1", "12", "73", "180").
            5. "id": Sequential integer starting at 1.
            6. "questionText": Full verbatim statement with LaTeX formulas and notations.
            7. "optionA", "optionB", "optionC", "optionD": All 4 choices. Never leave any choice blank.
            8. "correctOption": "A", "B", "C", or "D". Solve with 100% academic precision based on NCERT standards.
            9. "explanation": Concise 1-2 sentence step-by-step NCERT solution and formula.
            10. "subject": "Physics", "Chemistry", or "Biology".
            11. "chapter": Relevant NCERT Chapter (e.g. "Equilibrium", "Thermodynamics", "Some Basic Concepts of Chemistry", "Ray Optics", etc.).
            12. "subtopic": Specific NCERT topic tested (e.g. "Conjugate Acid-Base Pairs", "Reversible Isothermal Work", "Mole Concept").
            13. "ncertReference": Authentic NCERT textbook reference (e.g. "NCERT Class 11 Chemistry, Part 1, Ch 7, Sec 7.11").
            14. "conceptKey": 1-2 sentence core formula, scientific law, or concept required to solve the question.
            15. "difficulty": "Easy", "Medium", or "Hard".
            16. "institute": "$institute".
            17. DIAGRAM / CIRCUIT / GRAPH / FIGURE BOUNDING BOX:
                - DATA-INCLUSIVE BOUNDING BOX:
                  * When the question contains ANY diagram, circuit, graph, apparatus, ray optics, visual matching figure, or chemical structure:
                    "hasImage": true
                    "diagram_bbox": [ymin, xmin, ymax, xmax] (normalized 0 to 1000).
                  * MUST INCLUDE: The drawing PLUS all its attached data values, component parameters (e.g. R=10Ω, V=20V), graph axes numbers & units (e.g. x(m), t(s)), angle values, and figure labels. Do NOT cut off the numbers or units belonging to the diagram!
                  * STRICT NO-FULL-PAGE MANDATE: NEVER crop the whole page or multiple questions into diagram_bbox! Crop ONLY the specific drawing/figure.
                - If no diagram drawing: "hasImage": false, "diagram_bbox": null.
            18. ANTI-SPOILER RULE:
                - In "diagramLabel", NEVER state the answer or name mystery labels (A, B, C). Keep it strictly neutral.
            19. LATEX BACKSLASH RULE:
                - In JSON strings, ALWAYS escape backslashes for LaTeX (e.g. "\\\\Delta", "\\\\alpha", "\\\\frac", "\\\\text", "\\\\sqrt") so the JSON is strictly RFC 8259 valid.
            20. "passage": If this question is part of a Reading Paragraph, Comprehension, or Passage (e.g. "Paragraph for Questions 15 & 16" or "Passage 1"), include the full common passage/context text in "passage". Attach the SAME "passage" to each question belonging to this block so context is never lost. If no passage: "passage": null.

            Return ONLY a valid JSON array of question objects. Every question visible MUST be in the output array.
        """.trimIndent()

        val rawJson = callAiVisionDoc(
            context = context,
            keys = keys,
            prompt = promptText,
            base64Data = base64Img,
            mimeType = "image/jpeg",
            aiProvider = aiProvider,
            openRouterModel = openRouterModel,
            openRouterApiKey = openRouterApiKey
        )
        var parsed = parseQuestionsWithBboxFromJson(rawJson, institute)

        // Retry once if 0 questions parsed from image
        if (parsed.isEmpty()) {
            try {
                val retryPrompt = """
                    Extract ALL multiple choice questions visible on this test paper image into a JSON array:
                    [{"id": 1, "paperQNo": "1", "questionText": "...", "optionA": "...", "optionB": "...", "optionC": "...", "optionD": "...", "correctOption": "A", "explanation": "..."}]
                    Scan both Left and Right columns thoroughly. Do not skip any question!
                """.trimIndent()
                val retryJson = callAiVisionDoc(
                    context = context,
                    keys = keys,
                    prompt = retryPrompt,
                    base64Data = base64Img,
                    mimeType = "image/jpeg",
                    aiProvider = aiProvider,
                    openRouterModel = openRouterModel,
                    openRouterApiKey = openRouterApiKey
                )
                parsed = parseQuestionsWithBboxFromJson(retryJson, institute)
            } catch (_: Exception) {}
        }

        val allQuestions = mutableListOf<AiTestQuestion>()
        val pageSeenQuestions = mutableListOf<AiTestQuestion>()
        var diagramsCount = 0

        for (item in parsed) {
            val rawQNo = item.question.paperQNo
            val qTextPrefix = item.question.questionText.take(35).trim().lowercase()

            // Smart duplicate check (drops duplicate bilingual entries while keeping separate questions with same number across sections)
            val isDuplicate = pageSeenQuestions.any { existing ->
                val existingPrefix = existing.questionText.take(35).trim().lowercase()
                (qTextPrefix.isNotBlank() && qTextPrefix == existingPrefix) ||
                (!rawQNo.isNullOrBlank() && rawQNo == existing.paperQNo && qTextPrefix.take(20) == existingPrefix.take(20))
            }

            if (isDuplicate && scanLanguage != "Both") {
                continue
            }
            pageSeenQuestions.add(item.question)

            var q = item.question.copy(id = allQuestions.size + 1)
            val bbox = item.bbox
            if (autoCropDiagrams && bbox != null && bbox.size == 4) {
                val croppedPath = cropDiagramFromBitmap(context, bitmap, bbox, q.id)
                if (croppedPath != null) {
                    q = q.copy(hasImage = true, imageUrl = croppedPath)
                    diagramsCount++
                } else {
                    q = q.copy(hasImage = item.question.hasImage || !item.question.imageUrl.isNullOrBlank())
                }
            } else {
                q = q.copy(hasImage = item.question.hasImage || !item.question.imageUrl.isNullOrBlank())
            }
            allQuestions.add(q)
        }
        bitmap.recycle()

        if (allQuestions.isNotEmpty()) {
            return Result.success(allQuestions)
        } else {
            return Result.failure(Exception("Could not detect readable MCQs in this image. Please ensure photo is sharp and well-lit."))
        }
    }

    private fun synthesizeMissingOptions(
        qText: String,
        expl: String,
        corr: String,
        subject: String
    ): List<String> {
        val corrLetter = when (corr.trim().uppercase().take(1)) {
            "A", "1" -> "A"
            "B", "2" -> "B"
            "C", "3" -> "C"
            "D", "4" -> "D"
            else -> "A"
        }

        val directNum = corr.trim().replace(Regex("[^0-9.-]"), "").toDoubleOrNull()
        val numberMatch = Regex("""\b(\d+(?:\.\d+)?)\s*(?:m\/s|m\/s²|kg|N|J|W|V|A|Ω|Hz|mol|K|°C|cm|mm|nm|pm|s)?\b""").find(expl)
        val baseVal = directNum ?: numberMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull()

        if (baseVal != null && baseVal != 0.0) {
            val unit = numberMatch?.value?.replace(baseVal.toString(), "")?.trim() ?: ""
            fun fmt(v: Double) = if (v % 1.0 == 0.0) "${v.toInt()} $unit".trim() else "${String.format(Locale.US, "%.2f", v)} $unit".trim()
            val v1 = fmt(baseVal)
            val v2 = fmt(baseVal * 2)
            val v3 = fmt(baseVal / 2)
            val v4 = fmt(baseVal * 4)

            return when (corrLetter) {
                "A" -> listOf(v1, v2, v3, v4)
                "B" -> listOf(v2, v1, v3, v4)
                "C" -> listOf(v3, v2, v1, v4)
                else -> listOf(v4, v2, v3, v1)
            }
        }

        val firstSentence = expl.split(".").firstOrNull { it.trim().length > 10 }?.trim()
        val correctConcept = firstSentence ?: "Directly proportional to the applied constraint"
        return when (corrLetter) {
            "A" -> listOf(correctConcept, "Inversely proportional to square of distance", "Independent of external thermodynamic changes", "Remains zero in all inertial reference frames")
            "B" -> listOf("Remains constant and independent of state", correctConcept, "Decreases exponentially with time", "Directly proportional to temperature gradient")
            "C" -> listOf("Equal to unity under standard conditions", "Approaches infinity asymptotically", correctConcept, "Negligible for non-ideal parameters")
            else -> listOf("Strictly zero everywhere", "Linearly increasing with potential", "Oscillates with natural resonance frequency", correctConcept)
        }
    }

    /**
     * Extracts the target question range from comprehension/passage headers.
     * Examples:
     * - "Paragraph for Question 17 to 19" -> 17..19
     * - "Passage for Questions 17 - 19" -> 17..19
     * - "Questions 17 to 19 are based on" -> 17..19
     */
    fun parsePassageQuestionRange(text: String): IntRange? {
        if (text.isBlank()) return null

        val p1 = Regex(
            """(?:Paragraph|Passage|Comprehension|Case\s+Study|Directions?|Common\s+Data|Linked\s+Answer|Questions?)\s+(?:for\s+)?(?:Questions?|Q\.?)?\s*(\d{1,3})\s*(?:to|-|through|and|&)\s*(?:Q\.?)?\s*(\d{1,3})""",
            RegexOption.IGNORE_CASE
        )
        val m1 = p1.find(text)
        if (m1 != null) {
            val s = m1.groupValues[1].toIntOrNull()
            val e = m1.groupValues[2].toIntOrNull()
            if (s != null && e != null && s in 1..300 && e in s..(s + 20)) {
                return s..e
            }
        }

        val p2 = Regex(
            """\b(?:Q\.?|Questions?)\s*(\d{1,3})\s*(?:to|-|through|and|&)\s*(?:Q\.?)?\s*(\d{1,3})\b""",
            RegexOption.IGNORE_CASE
        )
        val m2 = p2.find(text)
        if (m2 != null) {
            val s = m2.groupValues[1].toIntOrNull()
            val e = m2.groupValues[2].toIntOrNull()
            if (s != null && e != null && s in 1..300 && e in s..(s + 20)) {
                return s..e
            }
        }

        val p3 = Regex(
            """(?:Paragraph|Passage|Comprehension)\s+for\s+(?:Question|Q\.?)\s*(\d{1,3})\b""",
            RegexOption.IGNORE_CASE
        )
        val m3 = p3.find(text)
        if (m3 != null) {
            val s = m3.groupValues[1].toIntOrNull()
            if (s != null && s in 1..300) {
                return s..s
            }
        }

        return null
    }

    /**
     * Cross-Page & Single-Page Comprehension Passage Reconciler.
     * Ensures:
     * 1. Paragraphs with ranges (e.g. Q17-19) are NEVER wrongly assigned to preceding questions (like Q16).
     * 2. All questions within the range (e.g. Q17, Q18, Q19) receive the full paragraph text and its diagram.
     * 3. Erroneous passage copies are stripped from questions that fall outside the target range.
     */
    fun reconcilePassagesAndDiagrams(questions: List<AiTestQuestion>): List<AiTestQuestion> {
        if (questions.isEmpty()) return questions

        data class DetectedPassage(
            val passageText: String,
            val range: IntRange,
            val imageUrl: String?,
            val sourceQNo: Int?
        )

        val detectedList = mutableListOf<DetectedPassage>()

        for (q in questions) {
            val qNum = q.paperQNo?.toIntOrNull() ?: q.id

            // Check if q.passage has a valid range
            val pass = q.passage?.trim()
            if (!pass.isNullOrBlank()) {
                val range = parsePassageQuestionRange(pass)
                if (range != null) {
                    detectedList.add(DetectedPassage(pass, range, q.imageUrl, qNum))
                }
            }

            // Check if questionText itself has the paragraph (in case OCR merged it into questionText)
            val qTextRange = parsePassageQuestionRange(q.questionText)
            if (qTextRange != null && q.questionText.contains(Regex("""(?:Paragraph|Passage|Comprehension)""", RegexOption.IGNORE_CASE))) {
                val qText = q.questionText.trim()
                val splitRegex = Regex("""(?:(?<=\n)\s*\d{1,3}\.|\bFind\b|\bWhat\b|\bCalculate\b|\bWhich\b|\bA\s+body\b|\bTwo\s+blocks\b)""", RegexOption.IGNORE_CASE)
                val parts = qText.split(splitRegex, limit = 2)
                val passText = if (parts.size > 1 && parts[0].length > 25) parts[0].trim() else qText
                detectedList.add(DetectedPassage(passText, qTextRange, q.imageUrl, qNum))
            }
        }

        // Clean out any dummy questions created solely for the paragraph header
        val filteredQuestions = questions.filterNot { q ->
            val isHeaderOnly = q.questionText.contains("Paragraph for Question", ignoreCase = true) &&
                    (q.optionA.isBlank() || q.optionA.equals("Option (A)", ignoreCase = true)) &&
                    (q.explanation.isBlank() || q.explanation.contains("passage", ignoreCase = true)) &&
                    !q.questionText.contains(Regex("""\b(?:find|calculate|what|which|determine|ratio)\b""", RegexOption.IGNORE_CASE))
            isHeaderOnly
        }

        if (detectedList.isEmpty()) {
            return filteredQuestions
        }

        // Apply detected passages to questions in their target ranges
        return filteredQuestions.map { q ->
            val qNum = q.paperQNo?.toIntOrNull() ?: q.id

            // Find matching passage whose range includes this question
            val matching = detectedList.firstOrNull { qNum in it.range }

            if (matching != null) {
                // Belongs to the paragraph!
                val cleanQText = if (q.questionText.contains("Paragraph for Question", ignoreCase = true)) {
                    q.questionText.replace(Regex("""Paragraph for Question.*?(?=(?:\n\s*\d{1,3}\.|\bFind\b|\bWhat\b|\bWhich\b|\bCalculate\b|$))""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "").trim()
                        .removePrefix("${qNum}.").removePrefix("Q${qNum}.").trim()
                } else {
                    q.questionText
                }

                q.copy(
                    passage = matching.passageText,
                    imageUrl = q.imageUrl ?: matching.imageUrl,
                    hasImage = q.hasImage || !matching.imageUrl.isNullOrBlank(),
                    questionText = cleanQText.ifBlank { q.questionText }
                )
            } else {
                // Does NOT belong to any paragraph range!
                // If it was wrongly given a passage whose range does NOT include qNum (e.g. Q16 given Q17-Q19 passage):
                val pass = q.passage
                val passRange = if (!pass.isNullOrBlank()) parsePassageQuestionRange(pass) else null
                if (passRange != null && qNum !in passRange) {
                    // Strip the misplaced passage from Q16!
                    q.copy(passage = null)
                } else {
                    q
                }
            }
        }
    }

    private fun cropDiagramFromBitmap(
        context: Context,
        pageBitmap: Bitmap,
        bbox: List<Number>,
        questionId: Int
    ): String? {
        try {
            if (bbox.size != 4) return null
            var ymin = bbox[0].toFloat()
            var xmin = bbox[1].toFloat()
            var ymax = bbox[2].toFloat()
            var xmax = bbox[3].toFloat()

            // Ensure coordinates are min <= max
            if (ymax < ymin) {
                val t = ymin
                ymin = ymax
                ymax = t
            }
            if (xmax < xmin) {
                val t = xmin
                xmin = xmax
                xmax = t
            }
            if (ymax <= ymin || xmax <= xmin) return null

            val bmpW = pageBitmap.width
            val bmpH = pageBitmap.height

            // Scale Auto-Detection:
            // Case 1: Normalized 0.0 to 1.0 (standard vision model output e.g. [0.12, 0.35, 0.45, 0.70])
            if (ymax <= 1.05f && xmax <= 1.05f && (ymax > 0.01f || xmax > 0.01f)) {
                ymin *= 1000f
                xmin *= 1000f
                ymax *= 1000f
                xmax *= 1000f
            } else if (ymax <= 100f && xmax <= 100f && (ymax > 1.05f || xmax > 1.05f)) {
                // Case 2: Percentage 0 to 100
                ymin *= 10f
                xmin *= 10f
                ymax *= 10f
                xmax *= 10f
            }

            val isDirectPixels = ymax > 1000f || xmax > 1000f

            val (left, top, right, bottom) = if (isDirectPixels) {
                val l = xmin.toInt().coerceIn(0, bmpW - 1)
                val t = ymin.toInt().coerceIn(0, bmpH - 1)
                val r = xmax.toInt().coerceIn(l + 1, bmpW)
                val b = ymax.toInt().coerceIn(t + 1, bmpH)
                listOf(l, t, r, b)
            } else {
                val rawW = xmax - xmin
                val rawH = ymax - ymin

                // Reject full-page hallucinated bounding boxes (> 85% width AND height)
                if (rawW > 850f && rawH > 850f) return null

                // Generous margin padding (12% of width and height) so labels, axes, numbers, and symbols are never chopped
                val padX = (rawW * 0.12f).coerceIn(8f, 50f)
                val padY = (rawH * 0.12f).coerceIn(8f, 50f)

                val expXmin = (xmin - padX).coerceAtLeast(0f)
                val expYmin = (ymin - padY).coerceAtLeast(0f)
                val expXmax = (xmax + padX).coerceAtMost(1000f)
                val expYmax = (ymax + padY).coerceAtMost(1000f)

                val l = (expXmin * bmpW / 1000f).toInt().coerceIn(0, bmpW - 1)
                val t = (expYmin * bmpH / 1000f).toInt().coerceIn(0, bmpH - 1)
                val r = (expXmax * bmpW / 1000f).toInt().coerceIn(l + 1, bmpW)
                val b = (expYmax * bmpH / 1000f).toInt().coerceIn(t + 1, bmpH)
                listOf(l, t, r, b)
            }

            val cropW = right - left
            val cropH = bottom - top

            if (cropW < 20 || cropH < 20) return null
            // Reject whole-page false positives only if it covers > 92% of both width and height
            if (cropW > bmpW * 0.92f && cropH > bmpH * 0.92f) return null

            val cropped = Bitmap.createBitmap(pageBitmap, left, top, cropW, cropH)
            val dir = File(context.filesDir, "cbt_diagrams").apply { mkdirs() }
            val file = File(dir, "diag_${System.currentTimeMillis()}_q${questionId}.png")
            FileOutputStream(file).use { out ->
                cropped.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            cropped.recycle()
            return if (file.exists() && file.length() > 0) file.absolutePath else null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Repairs raw JSON from LLM:
     * - Fixes invalid LaTeX backslashes (\Delta, \alpha, \sqrt, \text) which crash org.json with 'Bad escape'
     * - Closes truncated arrays/objects if output was cut off
     * - Strips trailing commas
     */
    fun sanitizeAndRepairJson(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("```json", ignoreCase = true)) {
            s = s.substring(7).trim()
        } else if (s.startsWith("```JSON", ignoreCase = true)) {
            s = s.substring(7).trim()
        } else if (s.startsWith("```")) {
            s = s.substring(3).trim()
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length - 3).trim()
        }

        val firstBracket = s.indexOf('[')
        val firstBrace = s.indexOf('{')
        if (firstBracket != -1 && (firstBrace == -1 || firstBracket < firstBrace)) {
            val lastBracket = s.lastIndexOf(']')
            if (lastBracket != -1 && lastBracket > firstBracket) {
                s = s.substring(firstBracket, lastBracket + 1)
            } else {
                // Truncated array: find last complete object and close it
                val lastCloseBrace = s.lastIndexOf('}')
                if (lastCloseBrace != -1 && lastCloseBrace > firstBracket) {
                    s = s.substring(firstBracket, lastCloseBrace + 1) + "]"
                } else {
                    s = s.substring(firstBracket) + "]"
                }
            }
        } else if (firstBrace != -1) {
            val lastBrace = s.lastIndexOf('}')
            if (lastBrace != -1 && lastBrace > firstBrace) {
                s = s.substring(firstBrace, lastBrace + 1)
            }
        }

        // Escape unescaped backslashes inside JSON string literals
        val sb = StringBuilder(s.length + 128)
        var inString = false
        var i = 0
        val len = s.length
        while (i < len) {
            val c = s[i]
            if (c == '"') {
                var slashCount = 0
                var j = i - 1
                while (j >= 0 && s[j] == '\\') {
                    slashCount++
                    j--
                }
                if (slashCount % 2 == 0) {
                    inString = !inString
                }
                sb.append(c)
                i++
            } else if (inString && c == '\\') {
                if (i + 1 < len) {
                    val next = s[i + 1]
                    val isStandardEscape = next == '"' || next == '\\' || next == '/' ||
                            next == 'b' || next == 'f' || next == 'n' || next == 'r' || next == 't'
                    val isUnicodeEscape = next == 'u' && i + 5 < len &&
                            (2..5).all { offset ->
                                val hex = s[i + offset]
                                hex in '0'..'9' || hex in 'a'..'f' || hex in 'A'..'F'
                            }

                    if (isStandardEscape || isUnicodeEscape) {
                        sb.append(c)
                        sb.append(next)
                        i += 2
                    } else {
                        // Raw LaTeX backslash like \Delta, \frac, \alpha -> escape backslash!
                        sb.append("\\\\")
                        i++
                    }
                } else {
                    sb.append("\\\\")
                    i++
                }
            } else {
                sb.append(c)
                i++
            }
        }

        var result = sb.toString()
        // Strip trailing commas before closing braces/brackets
        result = result.replace(Regex(",\\s*([\\]\\}])"), "$1")
        return result
    }

    /**
     * Fallback extractor that recovers each balanced { ... } question object individually from raw text.
     * Guarantees that even if one question is malformed, 100% of other questions are preserved!
     */
    fun extractJsonObjectsFromRawText(text: String): List<JSONObject> {
        val results = mutableListOf<JSONObject>()
        var depth = 0
        var startIndex = -1
        var inString = false
        var i = 0
        val len = text.length

        while (i < len) {
            val c = text[i]
            if (c == '"') {
                var slashCount = 0
                var j = i - 1
                while (j >= 0 && text[j] == '\\') {
                    slashCount++
                    j--
                }
                if (slashCount % 2 == 0) {
                    inString = !inString
                }
            } else if (!inString) {
                if (c == '{') {
                    if (depth == 0) {
                        startIndex = i
                    }
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0 && startIndex != -1) {
                        val objStr = text.substring(startIndex, i + 1)
                        if (objStr.contains("question", ignoreCase = true) || objStr.contains("option", ignoreCase = true) || objStr.contains("statement", ignoreCase = true)) {
                            try {
                                val repaired = sanitizeAndRepairJson(objStr)
                                results.add(JSONObject(repaired))
                            } catch (_: Exception) {}
                        }
                        startIndex = -1
                    } else if (depth < 0) {
                        depth = 0
                        startIndex = -1
                    }
                }
            }
            i++
        }
        return results
    }

    private fun parseSingleQuestionObject(obj: JSONObject, fallbackId: Int, defaultInstitute: String): ParsedQuestionWithBbox? {
        try {
            val id = obj.optInt("id", fallbackId)
            val paperQNo = obj.optString("paperQNo", "").ifBlank {
                obj.optString("questionNumber", "").ifBlank {
                    obj.optString("qNo", "").ifBlank {
                        obj.optString("number", "")
                    }
                }
            }.trim().ifBlank { null }

            val subject = obj.optString("subject", "").ifBlank {
                obj.optString("subjectName", "Physics")
            }.trim()

            val chapter = obj.optString("chapter", "").ifBlank {
                obj.optString("topic", "").ifBlank {
                    obj.optString("chapterName", "High Yield Topic")
                }
            }.trim()

            val pyqYear = obj.optString("pyqYear", "PYQ Standard")
            val rawNcert = obj.optString("ncertReference", "").ifBlank { obj.optString("ncert", "") }.trim()
            val rawConcept = obj.optString("conceptKey", "").ifBlank { obj.optString("concept", "") }.trim()
            val rawSubtopic = obj.optString("subtopic", "").trim()

            var qText = obj.optString("questionText", "").ifBlank {
                obj.optString("question", "").ifBlank {
                    obj.optString("statement", "").ifBlank {
                        obj.optString("q", "").ifBlank {
                            obj.optString("problem", "").ifBlank {
                                obj.optString("text", "").ifBlank {
                                    obj.optString("content", "").ifBlank {
                                        obj.optString("qText", "")
                                    }
                                }
                            }
                        }
                    }
                }
            }.trim()

            var optA = obj.optString("optionA", "").ifBlank {
                obj.optString("option_a", "").ifBlank {
                    obj.optString("option1", "").ifBlank {
                        obj.optString("optA", "").ifBlank {
                            obj.optString("opt1", "").ifBlank {
                                obj.optString("A", "").ifBlank {
                                    obj.optString("(A)", "").ifBlank {
                                        obj.optString("1", "")
                                    }
                                }
                            }
                        }
                    }
                }
            }.trim()

            var optB = obj.optString("optionB", "").ifBlank {
                obj.optString("option_b", "").ifBlank {
                    obj.optString("option2", "").ifBlank {
                        obj.optString("optB", "").ifBlank {
                            obj.optString("opt2", "").ifBlank {
                                obj.optString("B", "").ifBlank {
                                    obj.optString("(B)", "").ifBlank {
                                        obj.optString("2", "")
                                    }
                                }
                            }
                        }
                    }
                }
            }.trim()

            var optC = obj.optString("optionC", "").ifBlank {
                obj.optString("option_c", "").ifBlank {
                    obj.optString("option3", "").ifBlank {
                        obj.optString("optC", "").ifBlank {
                            obj.optString("opt3", "").ifBlank {
                                obj.optString("C", "").ifBlank {
                                    obj.optString("(C)", "").ifBlank {
                                        obj.optString("3", "")
                                    }
                                }
                            }
                        }
                    }
                }
            }.trim()

            var optD = obj.optString("optionD", "").ifBlank {
                obj.optString("option_d", "").ifBlank {
                    obj.optString("option4", "").ifBlank {
                        obj.optString("optD", "").ifBlank {
                            obj.optString("opt4", "").ifBlank {
                                obj.optString("D", "").ifBlank {
                                    obj.optString("(D)", "").ifBlank {
                                        obj.optString("4", "")
                                    }
                                }
                            }
                        }
                    }
                }
            }.trim()

            // Check if options are in a nested JSONObject (e.g. "options": { "A": "...", "B": "..." })
            val optionsObj = obj.optJSONObject("options") ?: obj.optJSONObject("choices") ?: obj.optJSONObject("answers") ?: obj.optJSONObject("mcq_options")
            if (optionsObj != null) {
                if (optA.isBlank()) optA = optionsObj.optString("A", "").ifBlank { optionsObj.optString("1", "").ifBlank { optionsObj.optString("(1)", "").ifBlank { optionsObj.optString("(A)", "") } } }
                if (optB.isBlank()) optB = optionsObj.optString("B", "").ifBlank { optionsObj.optString("2", "").ifBlank { optionsObj.optString("(2)", "").ifBlank { optionsObj.optString("(B)", "") } } }
                if (optC.isBlank()) optC = optionsObj.optString("C", "").ifBlank { optionsObj.optString("3", "").ifBlank { optionsObj.optString("(3)", "").ifBlank { optionsObj.optString("(C)", "") } } }
                if (optD.isBlank()) optD = optionsObj.optString("D", "").ifBlank { optionsObj.optString("4", "").ifBlank { optionsObj.optString("(4)", "").ifBlank { optionsObj.optString("(D)", "") } } }
            }

            // Check if options are in a JSONArray (e.g. "options": ["...", "..."] or "options": [{"text": "..."}, ...])
            val optsArr = obj.optJSONArray("options") ?: obj.optJSONArray("choices") ?: obj.optJSONArray("answers")
            if (optsArr != null && optsArr.length() >= 2) {
                fun getArrOpt(idx: Int): String {
                    val rawItem = optsArr.opt(idx)
                    return when (rawItem) {
                        is String -> rawItem
                        is JSONObject -> rawItem.optString("text", "").ifBlank { rawItem.optString("option", "").ifBlank { rawItem.optString("value", "").ifBlank { rawItem.optString("choice", "") } } }
                        else -> rawItem?.toString() ?: ""
                    }
                }
                if (optA.isBlank()) optA = getArrOpt(0)
                if (optB.isBlank()) optB = getArrOpt(1)
                if (optC.isBlank() && optsArr.length() > 2) optC = getArrOpt(2)
                if (optD.isBlank() && optsArr.length() > 3) optD = getArrOpt(3)
            }

            fun cleanOptionStr(s: String): String {
                val t = s.trim()
                val lower = t.lowercase().replace(Regex("[^a-z0-9]"), "")
                if (lower.isBlank() || lower == "null" || lower == "none" || lower == "na" || 
                    lower == "nil" || lower == "undefined" || lower.contains("notprovided") || 
                    lower.contains("notgiven") || lower.contains("notavailable") || lower.contains("nooption")) {
                    return ""
                }
                return t
            }

            optA = cleanOptionStr(optA)
            optB = cleanOptionStr(optB)
            optC = cleanOptionStr(optC)
            optD = cleanOptionStr(optD)

            val isFillInTheBlank = qText.contains(
                Regex("""_{1,}|\.{2,}|\bnearest integer\b|\binteger value\b|\bvalue of\b|\bfill in the blank\b|\bfill in the blanks\b|\bblank\b|\bblanks\b|\[\s*\]|\(\s*_{1,}\s*\)|\(\s*\.{2,}\s*\)|—|–|\banswer is\b|\bequal to\s*_{1,}""", RegexOption.IGNORE_CASE)
            )

            val isNumerical = isFillInTheBlank || 
                obj.optBoolean("is_numerical", false) || 
                obj.optBoolean("isNumerical", false) || 
                obj.optString("questionType", "").contains("NUMERICAL", ignoreCase = true) ||
                obj.optString("questionType", "").contains("FILL", ignoreCase = true) ||
                obj.optString("questionType", "").contains("NAT", ignoreCase = true) ||
                obj.optString("type", "").contains("NUMERICAL", ignoreCase = true) ||
                obj.optString("type", "").contains("BLANK", ignoreCase = true) ||
                (obj.optString("correctOption", "").trim().toDoubleOrNull() != null && obj.optString("correctOption", "").trim().uppercase() !in listOf("A", "B", "C", "D"))

            var correct = obj.optString("correctOption", "").ifBlank {
                obj.optString("answer", "").ifBlank {
                    obj.optString("correct_answer", "").ifBlank {
                        obj.optString("correct", "")
                    }
                }
            }.trim()

            // GUARANTEE 4 complete options for EVERY question (including Fill-in-the-blank and Numerical questions)
            if (isNumerical || optA.isBlank() || optB.isBlank() || optC.isBlank() || optD.isBlank()) {
                val rawExpl = obj.optString("explanation", "").ifBlank { obj.optString("solution", "") }
                val targetCorr = if (correct.isNotBlank()) correct else obj.optString("answer", "A")
                val synthesized = synthesizeMissingOptions(qText, rawExpl, targetCorr, subject)
                if (optA.isBlank()) optA = synthesized[0]
                if (optB.isBlank()) optB = synthesized[1]
                if (optC.isBlank()) optC = synthesized[2]
                if (optD.isBlank()) optD = synthesized[3]

                if (correct.uppercase() !in listOf("A", "B", "C", "D")) {
                    val matchIdx = synthesized.indexOfFirst { it.trim().equals(correct.trim(), ignoreCase = true) }
                    correct = if (matchIdx >= 0) ('A' + matchIdx).toString() else "A"
                }
            }

            correct = correct.uppercase()
            when {
                correct.startsWith("1") || correct.contains("(1)") || correct.contains("(A)") -> correct = "A"
                correct.startsWith("2") || correct.contains("(2)") || correct.contains("(B)") -> correct = "B"
                correct.startsWith("3") || correct.contains("(3)") || correct.contains("(C)") -> correct = "C"
                correct.startsWith("4") || correct.contains("(4)") || correct.contains("(D)") -> correct = "D"
                correct.length > 1 -> correct = correct.take(1)
            }
            if (correct !in listOf("A", "B", "C", "D")) correct = "A"

            val expl = obj.optString("explanation", "").ifBlank {
                obj.optString("solution", "").ifBlank {
                    obj.optString("reason", "Standard NCERT conceptual solution.")
                }
            }.trim()

            val rawImg = obj.optString("imageUrl", "").trim()
            val rawLabel = obj.optString("diagramLabel", "").trim()
            val rawSvg = obj.optString("diagramSvg", "").trim()
            val rawType = obj.optString("diagramType", "").trim()

            val rawDiff = obj.optString("difficulty", "Medium").trim()
            val diff = when {
                rawDiff.contains("easy", ignoreCase = true) -> "Easy"
                rawDiff.contains("hard", ignoreCase = true) -> "Hard"
                else -> "Medium"
            }
            val inst = obj.optString("institute", defaultInstitute).ifBlank { defaultInstitute }

            val imgUrl = if (rawImg.isNotBlank() && !rawImg.equals("null", true) && !rawImg.equals("none", true)) rawImg else null
            val diagLabel = if (rawLabel.isNotBlank() && !rawLabel.equals("null", true) && !rawLabel.equals("none", true)) rawLabel else null
            val diagSvg = if (rawSvg.isNotBlank() && !rawSvg.equals("null", true) && !rawSvg.equals("none", true)) rawSvg else null
            val diagType = if (rawType.isNotBlank() && !rawType.equals("null", true) && !rawType.equals("none", true)) rawType else if (imgUrl != null || diagSvg != null) "DIAGRAM" else null

            val rawPassage = obj.optString("passage", "").ifBlank {
                obj.optString("comprehensionText", "").ifBlank {
                    obj.optString("paragraph", "").ifBlank {
                        obj.optString("passageText", "").ifBlank {
                            obj.optString("context", "")
                        }
                    }
                }
            }.trim()
            val passage = if (rawPassage.isNotBlank() && !rawPassage.equals("null", true) && !rawPassage.equals("none", true)) rawPassage else null

            val bboxArr = obj.optJSONArray("diagram_bbox")
                ?: obj.optJSONArray("diagramBbox")
                ?: obj.optJSONArray("bbox")
                ?: obj.optJSONArray("box")
                ?: obj.optJSONArray("diagram_box")
                ?: obj.optJSONArray("coordinates")
                ?: obj.optJSONArray("diagramCoordinates")
                ?: obj.optJSONArray("image_bbox")

            val bbox: List<Number>? = if (bboxArr != null && bboxArr.length() >= 4) {
                val list = mutableListOf<Number>()
                for (k in 0 until 4) {
                    val d = bboxArr.optDouble(k, Double.NaN)
                    if (!d.isNaN()) {
                        list.add(d)
                    } else {
                        val num = bboxArr.optInt(k, -1)
                        if (num >= 0) list.add(num)
                    }
                }
                if (list.size == 4) list else null
            } else {
                val rawBboxStr = obj.optString("diagram_bbox", "").ifBlank {
                    obj.optString("bbox", "").ifBlank {
                        obj.optString("diagramBbox", "")
                    }
                }.trim()
                if (rawBboxStr.isNotBlank() && rawBboxStr != "null" && rawBboxStr != "none") {
                    try {
                        val nums = Regex("""[-+]?[0-9]*\.?[0-9]+""").findAll(rawBboxStr).mapNotNull {
                            it.value.toDoubleOrNull()
                        }.take(4).toList()
                        if (nums.size == 4) nums else null
                    } catch (_: Exception) { null }
                } else null
            }

            val hasImage = imgUrl != null || diagSvg != null || bbox != null || (diagLabel != null && obj.optBoolean("hasImage", false)) || obj.optBoolean("hasImage", false)

            if (qText.isBlank() && (optA.isNotBlank() || diagLabel != null)) {
                qText = diagLabel ?: "Question ${paperQNo ?: fallbackId} (Refer to the figure/options below)"
            }

            if (qText.isNotBlank()) {
                val q = AiTestQuestion(
                    id = id,
                    subject = if (subject.isNotBlank()) subject else "Physics",
                    chapter = if (chapter.isNotBlank()) chapter else "High Yield Topic",
                    pyqYear = pyqYear,
                    questionText = qText,
                    optionA = optA,
                    optionB = optB,
                    optionC = optC,
                    optionD = optD,
                    correctOption = correct,
                    explanation = expl,
                    passage = passage,
                    hasImage = hasImage,
                    imageUrl = imgUrl,
                    diagramLabel = diagLabel,
                    diagramSvg = diagSvg,
                    diagramType = diagType,
                    difficulty = diff,
                    institute = inst,
                    paperQNo = paperQNo,
                    ncertReference = rawNcert,
                    conceptKey = rawConcept,
                    subtopic = rawSubtopic,
                    isNumerical = false
                )
                val enriched = NcertConceptRegistry.enrichQuestionWithNcertDetails(q)
                return ParsedQuestionWithBbox(enriched, bbox)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun parseQuestionsWithBboxFromJson(rawJson: String, defaultInstitute: String = ""): List<ParsedQuestionWithBbox> {
        val list = mutableListOf<ParsedQuestionWithBbox>()
        val cleaned = sanitizeAndRepairJson(rawJson)

        // Primary Attempt: Parse entire array/object
        try {
            if (cleaned.startsWith("[")) {
                val array = JSONArray(cleaned)
                for (i in 0 until array.length()) {
                    val qObj = array.optJSONObject(i) ?: continue
                    val parsed = parseSingleQuestionObject(qObj, i + 1, defaultInstitute)
                    if (parsed != null) {
                        list.add(parsed)
                    }
                }
            } else if (cleaned.startsWith("{")) {
                val obj = JSONObject(cleaned)
                var foundArr: JSONArray? = obj.optJSONArray("questions")
                    ?: obj.optJSONArray("items")
                    ?: obj.optJSONArray("mcqs")
                    ?: obj.optJSONArray("mcqList")
                    ?: obj.optJSONArray("testQuestions")
                    ?: obj.optJSONArray("extractedQuestions")
                    ?: obj.optJSONArray("paper")
                    ?: obj.optJSONArray("results")
                    ?: obj.optJSONArray("problems")
                    ?: obj.optJSONArray("data")
                    ?: obj.optJSONArray("list")

                if (foundArr == null) {
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val candidate = obj.optJSONArray(k)
                        if (candidate != null && candidate.length() > 0) {
                            foundArr = candidate
                            break
                        }
                    }
                }

                if (foundArr != null) {
                    for (i in 0 until foundArr.length()) {
                        val qObj = foundArr.optJSONObject(i) ?: continue
                        val parsed = parseSingleQuestionObject(qObj, i + 1, defaultInstitute)
                        if (parsed != null) {
                            list.add(parsed)
                        }
                    }
                } else {
                    // Check if keys are question numbers (e.g. "1", "2", "3")
                    val keys = obj.keys()
                    var count = 1
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val childObj = obj.optJSONObject(k)
                        if (childObj != null) {
                            val parsed = parseSingleQuestionObject(childObj, count++, defaultInstitute)
                            if (parsed != null) {
                                list.add(parsed)
                            }
                        }
                    }
                    // If root object itself is a single question
                    if (list.isEmpty()) {
                        val parsedSingle = parseSingleQuestionObject(obj, 1, defaultInstitute)
                        if (parsedSingle != null) {
                            list.add(parsedSingle)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Secondary Attempt: Fallback resilient regex extractor (never drops questions on parse errors!)
        if (list.isEmpty()) {
            try {
                val extractedObjects = extractJsonObjectsFromRawText(rawJson)
                for ((idx, qObj) in extractedObjects.withIndex()) {
                    val parsed = parseSingleQuestionObject(qObj, idx + 1, defaultInstitute)
                    if (parsed != null) {
                        list.add(parsed)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Post-processing: Propagate common passage to follow-up questions in a comprehension block
        var activePassage: String? = null
        val finalizedList = mutableListOf<ParsedQuestionWithBbox>()
        for (item in list) {
            val q = item.question
            if (!q.passage.isNullOrBlank()) {
                activePassage = q.passage
                finalizedList.add(item)
            } else if (activePassage != null && q.questionText.contains(Regex("""\babove\s+(?:passage|paragraph|comprehension|case|text|experiment|data)\b|\brefer to\b|\bfrom the given\b|\bwith reference to\b|\bin the above\b""", RegexOption.IGNORE_CASE))) {
                finalizedList.add(item.copy(question = q.copy(passage = activePassage)))
            } else {
                activePassage = null
                finalizedList.add(item)
            }
        }

        return finalizedList
    }

    private suspend fun callAiVisionDoc(
        context: Context,
        keys: List<String>,
        prompt: String,
        base64Data: String,
        mimeType: String,
        aiProvider: AiProvider,
        openRouterModel: String,
        openRouterApiKey: String
    ): String {
        if (aiProvider == AiProvider.OPENROUTER) {
            if (openRouterApiKey.isBlank()) {
                throw Exception("OpenRouter API key is missing. Please configure it in AI Engine settings.")
            }
            // STRICT ISOLATION: Call OpenRouter directly with chosen model. Gemini 3.7 is COMPLETELY OFF!
            return OpenRouterManager.callOpenRouterChat(
                apiKey = openRouterApiKey,
                model = openRouterModel,
                prompt = prompt,
                base64Image = base64Data,
                mimeType = mimeType,
                context = context
            )
        } else if (aiProvider == AiProvider.GROQ) {
            val groqKey = GroqManager.getGroqApiKey(context)
            val groqModel = GroqManager.getSelectedModel(context)
            if (groqKey.isBlank()) {
                throw Exception("Groq API key is missing. Please configure your Groq key (starts with 'gsk_') in Settings.")
            }
            // STRICT ISOLATION: Call Groq LPU Vision directly. Gemini 3.7 & OpenRouter are COMPLETELY OFF!
            return GroqManager.callGroqChat(
                apiKey = groqKey,
                model = groqModel,
                prompt = prompt,
                base64Image = base64Data,
                mimeType = mimeType
            )
        } else if (aiProvider == AiProvider.CLOUDFLARE) {
            val prefs = context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
            val cfAccountId = (prefs.getString(CloudflareManager.KEY_CF_ACCOUNT_ID, "") ?: "").trim()
            val cfToken = (prefs.getString(CloudflareManager.KEY_CF_API_TOKEN, "") ?: "").trim()
            val cfModel = (prefs.getString(CloudflareManager.KEY_CF_MODEL, CloudflareManager.DEFAULT_VISION_MODEL) ?: CloudflareManager.DEFAULT_VISION_MODEL).trim()
            if (cfAccountId.isBlank() || cfToken.isBlank()) {
                throw Exception("Cloudflare Account ID or API Token is missing. Please configure your Cloudflare credentials in Settings.")
            }
            // STRICT ISOLATION: Call Cloudflare Workers AI directly. Gemini, OpenRouter & Groq are COMPLETELY OFF!
            return CloudflareManager.callCloudflareChat(
                accountId = cfAccountId,
                apiToken = cfToken,
                model = cfModel,
                prompt = prompt,
                base64Image = base64Data,
                mimeType = mimeType
            )
        }
        return callGeminiMultimodalDoc(context, keys, prompt, base64Data, mimeType)
    }

    private suspend fun callGeminiMultimodalDoc(context: Context, keys: List<String>, prompt: String, base64Data: String, mimeType: String): String {
        val apiKey = keys.firstOrNull { it.isNotBlank() } ?: getApiKey(context)
        if (apiKey.isBlank()) {
            throw Exception("Gemini API Key is missing. Please configure your key in Settings.")
        }
        val model = GeminiModelManager.getEffectiveModel(context)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val jsonBody = JSONObject().apply {
            val contentsArr = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArr = JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        put(JSONObject().apply {
                            val inlineData = JSONObject().apply {
                                put("mimeType", mimeType)
                                put("data", base64Data)
                            }
                            put("inlineData", inlineData)
                        })
                    }
                    put("parts", partsArr)
                }
                put(contentObj)
            }
            put("contents", contentsArr)
            val genConfig = JSONObject().apply {
                put("temperature", 0.1)
                put("maxOutputTokens", 16384)
                put("responseMimeType", "application/json")
            }
            put("generationConfig", genConfig)
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).await()
        val responseString = response.body?.string() ?: ""
        if (response.isSuccessful && responseString.isNotBlank()) {
            val root = JSONObject(responseString)
            val candidates = root.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    if (text.isNotBlank()) return text
                }
            }
            throw Exception("Empty response from Gemini OCR engine")
        } else {
            // Fallback retry without responseMimeType if 400 Bad Request was returned
            if (response.code == 400) {
                try {
                    val fallbackBody = JSONObject().apply {
                        val contentsArr = JSONArray().apply {
                            val contentObj = JSONObject().apply {
                                val partsArr = JSONArray().apply {
                                    put(JSONObject().apply { put("text", prompt) })
                                    put(JSONObject().apply {
                                        val inlineData = JSONObject().apply {
                                            put("mimeType", mimeType)
                                            put("data", base64Data)
                                        }
                                        put("inlineData", inlineData)
                                    })
                                }
                                put("parts", partsArr)
                            }
                            put(contentObj)
                        }
                        put("contents", contentsArr)
                        val genConfig = JSONObject().apply {
                            put("temperature", 0.1)
                            put("maxOutputTokens", 16384)
                        }
                        put("generationConfig", genConfig)
                    }
                    val fallbackReq = Request.Builder()
                        .url(url)
                        .post(fallbackBody.toString().toRequestBody("application/json".toMediaType()))
                        .build()
                    val fbResponse = client.newCall(fallbackReq).await()
                    val fbString = fbResponse.body?.string() ?: ""
                    if (fbResponse.isSuccessful && fbString.isNotBlank()) {
                        val root = JSONObject(fbString)
                        val candidates = root.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val firstCandidate = candidates.getJSONObject(0)
                            val content = firstCandidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val text = parts.getJSONObject(0).optString("text", "")
                                if (text.isNotBlank()) return text
                            }
                        }
                    }
                } catch (_: Exception) { }
            }

            val errorMsg = try {
                JSONObject(responseString).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
            } catch (e: Exception) {
                "HTTP ${response.code}"
            }
            throw Exception("Gemini Error: $errorMsg")
        }
    }
}
