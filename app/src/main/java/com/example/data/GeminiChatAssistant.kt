package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiChatAssistant {

    private val client = OkHttpClient.Builder()
        .connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Retrieves the Gemini API Key strictly from user preferences stored locally on device.
     */
    fun getApiKey(context: Context, customApiKey: String? = null): String {
        if (!customApiKey.isNullOrBlank()) return customApiKey.trim()
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val k1 = (prefs.getString("gemini_api_key_1", "") ?: "").trim()
        if (k1.isNotBlank()) return k1
        val k2 = (prefs.getString("gemini_api_key_2", "") ?: "").trim()
        if (k2.isNotBlank()) return k2
        val k3 = (prefs.getString("gemini_api_key_3", "") ?: "").trim()
        if (k3.isNotBlank()) return k3
        return ""
    }

    suspend fun getDailyInspiration(
        context: Context,
        seenQuotes: Set<String> = emptySet(),
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val seenSnippet = if (seenQuotes.isNotEmpty()) {
            "\nDO NOT repeat or duplicate any of these previously shown quotes: " + seenQuotes.take(15).joinToString("; ")
        } else ""

        val prompt = """
            You are Lakshya AI, personal mentor to a hardworking student preparing for India's toughest competitive exams (NEET / JEE).
            Generate ONE fresh, profound, high-impact, genuinely inspirational quote about relentless discipline, focus, resilience, or scientific brilliance.
            $seenSnippet

            RULES:
            1. MUST be attributed to a real, legendary historical figure, scientist, philosopher, visionary, or leader (e.g. Dr. A.P.J. Abdul Kalam, Albert Einstein, Swami Vivekananda, Marie Curie, Steve Jobs, Nelson Mandela, Marcus Aurelius, Dr. B.R. Ambedkar, Leonardo da Vinci, Thomas Edison, Mahatma Gandhi, Benjamin Franklin, etc.).
            2. Never use 'Unknown', 'Anonymous', 'Inspiration', or 'Gemini' as the author.
            3. OUTPUT FORMAT: Respond ONLY with the single quote and author separated strictly by a vertical bar '|', with zero markdown or preamble:
            <Quote text>|<Real Author Name>
            Example:
            If you want to shine like a sun, first burn like a sun.|Dr. A.P.J. Abdul Kalam
        """.trimIndent()

        val geminiResult = callGemini(context, prompt, null, null, customApiKey)
        geminiResult.fold(
            onSuccess = { rawText ->
                val clean = rawText.trim()
                if (clean.contains("|") && clean.length > 10) {
                    val parts = clean.split("|")
                    val q = parts[0].trim().removeSurrounding("\"").removeSurrounding("'")
                    val a = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
                    val validAuthor = if (a.isBlank() || a.equals("Unknown", ignoreCase = true) || a.equals("Gemini", ignoreCase = true)) {
                        "Dr. A.P.J. Abdul Kalam"
                    } else a
                    Result.success("$q|$validAuthor")
                } else {
                    val offlineQuote = com.example.utils.MotivationalQuotes.getQuoteOfTheDay(seenQuotes)
                    Result.success("${offlineQuote.text}|${offlineQuote.author}")
                }
            },
            onFailure = {
                val offlineQuote = com.example.utils.MotivationalQuotes.getQuoteOfTheDay(seenQuotes)
                Result.success("${offlineQuote.text}|${offlineQuote.author}")
            }
        )
    }

    suspend fun generateErrorAnalysis(
        context: Context,
        subject: String,
        noteText: String,
        mediaUriStr: String?,
        mediaType: String?,
        customApiKey: String? = null,
        targetModel: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val effectiveModel = targetModel ?: GeminiModelManager.getSelectedModel(context)
        val prompt = """
            You are an elite National Ranker (AIR 1) and Academic Faculty for NEET and JEE (Model: $effectiveModel).
            Perform a deep, surgical Error Analysis for this student's recorded mistake:
            Subject: $subject
            Student's Recorded Mistake / Error Note:
            "$noteText"
            
            Provide a high-IQ diagnostic breakdown:
            🎯 **Mistake Diagnosis**: Exact categorization (Conceptual Gap vs Calculation Slip vs Question Misread vs Speed/Panic Trap).
            🧬 **Underlying NCERT / Core Principle**: The fundamental concept or formula that was violated.
            📝 **Correct Master Solution**: Precise mathematical steps or conceptual reasoning with zero skipped steps.
            ⚡ **Immediate Retention Rule**: A high-impact 1-line mental trigger or memory anchor to ensure 0% repeat probability in future tests.
            🔥 **Trap Warning**: Exactly how exam setters design wrong options based on this exact mistake.

            FORMATTING REQUIREMENT:
            - Write all math and chemistry formulas using clear LaTeX syntax wrapped in single dollar signs (inline) or double dollar signs (display equations).
            - Use emojis and crisp bullet points.
        """.trimIndent()

        callGemini(context, prompt, mediaUriStr, mediaType, customApiKey, targetModel)
    }

    suspend fun generateDetoxMotivation(
        context: Context,
        habitName: String,
        userFeeling: String? = null,
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val feelingContext = if (!userFeeling.isNullOrBlank()) {
            "\nThe student has shared their current inner feeling/urge: \"$userFeeling\". You MUST address this specific feeling, validate their struggle briefly, and then ruthlessly pull them back on track."
        } else ""

        val prompt = """
            You are a strict, highly motivating mentor and neuroscience-informed study coach for a student preparing for highly competitive exams like NEET/JEE.
            The student is feeling urges or asking for help to avoid breaking their detox/habit: "$habitName".
            $feelingContext
            
            Specifically if the habit is related to "Porn", "Fap", "Social Media", or "Distraction", you must heavily emphasize:
            1. Padhai me kya effect hota hai (Impact on Studies): Explain how dopamine depletion ruins focus, memory retention, and the ability to study difficult subjects like Physics/Chemistry.
            2. Soch kaisi hoti hai (Impact on Mindset): Explain the brain fog, lethargy, lack of motivation, and how it rewires the brain to seek cheap dopamine instead of hard-earned academic success.
            3. Act as a strict elder brother/mentor. Use strong, hard-hitting Hinglish (Hindi + English) to wake them up from the urge.
            4. Remind them of their ultimate goal (cracking the exam, making parents proud) and how a few minutes of cheap pleasure will destroy months of hard work.
            
            Format the response using clear markdown, bullet points, and high energy.
        """.trimIndent()
        
        callGemini(context, prompt, null, null, customApiKey)
    }

    suspend fun solveDoubt(
        context: Context,
        subject: String,
        doubtText: String,
        mediaUriStr: String?,
        mediaType: String?,
        customApiKey: String? = null,
        targetModel: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val effectiveModel = targetModel ?: GeminiModelManager.getSelectedModel(context)
        val prompt = """
            You are an elite Senior Academic Faculty & National Top Ranker (AIR 1) for NEET & JEE (Main + Advanced) running on $effectiveModel.
            Subject: $subject
            Student's Doubt / Problem:
            "$doubtText"
            
            Deliver a flawless, masterclass doubt resolution with 100% academic depth:
            🎯 **Problem Deconstruction & Given Data**: Identify key physical/chemical parameters, units, and what is specifically being asked.
            🧬 **Core Law / Governing Equations**: State the fundamental theorems, equations, or NCERT concepts (e.g. conservation of momentum, Nernst equation, L'Hôpital's rule).
            📝 **Detailed Step-by-Step Solution**: Complete derivation with algebraic steps, intermediate values, and substitution without skipping any step.
            ⚡ **30-Second Exam Shortcut / Elimination Trick**: A rapid technique (dimensional analysis, boundary value checks, symmetry, or mental ratio shortcut) to solve or verify the answer in seconds.
            🚫 **Examiner Trap & Common Blunder**: The exact trick or sign-convention mistake that causes students to choose the wrong option.
            💡 **High-Yield Takeaway**: One golden line to store in memory for the final exam.
            
            FORMATTING REQUIREMENT:
            - Write ALL mathematical formulas, derivations, variables, and fractions using MathJax LaTeX syntax.
            - Use single dollar signs (e.g. ${'$'}F = ma${'$'}) for inline math.
            - Use double dollar signs (${'$'}${'$'} ... ${'$'}${'$'}) for standalone equations and derivations.
            - Use structured step-by-step points, bullet lists, and markdown tables where appropriate.
        """.trimIndent()

        callGemini(context, prompt, mediaUriStr, mediaType, customApiKey, targetModel)
    }

    suspend fun explainConcept(
        context: Context,
        subject: String,
        conceptText: String,
        customApiKey: String? = null,
        targetModel: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val effectiveModel = targetModel ?: GeminiModelManager.getSelectedModel(context)
        val prompt = """
            You are an elite Senior Professor and Top Educator for NEET and JEE (Model: $effectiveModel).
            Explain this topic/concept from $subject with ultimate conceptual clarity and intuitive depth:
            "$conceptText"
            
            Structure your master explanation:
            🔍 **Intuitive Real-World Analogy & Visualization**: How to mentally visualize the phenomenon with concrete analogies, structured breakdown, and intuitive mental models.
            📐 **Mathematical Formulation & Derivation**: Precise laws, boundary conditions, and formulas in LaTeX syntax.
            💡 **NCERT Deep Insights & Exceptions**: Subtle points, edge cases, and high-yield questions asked in past 10 years (PYQs).
            ⚡ **Master Problem Solving Framework**: How to approach exam questions from this topic step-by-step.
            
            Format clearly with markdown, emojis, and LaTeX formulas.
        """.trimIndent()

        callGemini(context, prompt, null, null, customApiKey, targetModel)
    }

    suspend fun analyzeSyllabus(
        context: Context,
        syllabusText: String,
        mediaUriStr: String?,
        mediaType: String?,
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val prompt = """
            You are an expert AI exam syllabus analyst for NEET and JEE.
            I am providing a syllabus or specific instructions for an upcoming test or study block.
            User Input / Description: "$syllabusText"
            (I may also have provided an image or PDF of the syllabus).
            
            Please carefully read the user input/description and the provided PDF (if any).
            Analyze the syllabus and provide the following for the chapters/topics covered:
            1. 🎯 **Important Topics**: The core topics you must not miss.
            2. ❓ **Most Asked Questions**: The specific types of questions frequently asked from these chapters.
            3. 📈 **PYQ Trend Analysis**: The trend of questions over the last 5-10 years (weightage, difficulty).
            4. 🔥 **Highest Yield Areas**: The absolute highest weightage sub-topics in exam history for this syllabus.
            
            *Important: If the user explicitly asked for something specific in their description (e.g., "only fluid mechanics", "only physics", "give me numericals"), you MUST follow those instructions.*
            
            Format your response cleanly with markdown, bullet points, and emojis.
            Write ALL mathematical formulas, derivations, variables, and fractions using MathJax LaTeX syntax.
            Use single dollar signs (one before and one after the expression) for inline math and double dollar signs for display math.
        """.trimIndent()

        callGemini(context, prompt, mediaUriStr, mediaType, customApiKey)
    }

    suspend fun generateChecklistFromPDF(
        context: Context,
        pdfUriStr: String,
        subject: String,
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val prompt = """
            CRITICAL INSTRUCTION - READ 100% OF THE PDF DOCUMENT:
            You are an expert curriculum parsing AI. Read the attached PDF document thoroughly from the VERY FIRST PAGE to the VERY LAST PAGE.
            You must NOT skip, omit, abbreviate, or summarize ANY section, chapter, table row, timeline phase, or topic in this entire PDF.
            
            Extract ALL chapters/topics into a structured checklist:
            1. "subject": Subject name (e.g. "$subject", or extract "Physics", "Chemistry", "Biology" if multiple subjects exist).
            2. "phaseTitle": The exact phase or date range (e.g. "AUG - 15TH OCT", "16TH OCT - 30TH NOV", "Phase 1", "Mechanics", etc.). If no phase is mentioned, use "$subject Checklist".
            3. "topicName": Clean and exact chapter/topic name (e.g. "Kinematics", "Laws of Motion", "Work Power Energy"). Clean away any stray symbols or newlines.
            4. "days": The allotted days integer (e.g. 4, 3, 2, 5). Default to 4 if not specified.
            5. "columns": Comma-separated list of Year milestone column headings only (e.g. "2020,2021,2022,2023,2024,2025,2026" or "2020 PYQ,2021 PYQ,2022 PYQ,2023/24 PYQ"). Do NOT include DPPs or DPPS. Include ONLY the PYQ Years / Year columns. Keep labels concise so they fit cleanly.

            Output format MUST BE STRICTLY a JSON array of objects:
            [
              {
                "subject": "$subject",
                "phaseTitle": "AUG - 15TH OCT",
                "topicName": "Kinematics",
                "days": 4,
                "columns": "2020,2021,2022,2023,2024,2025,2026"
              }
            ]
            Ensure you extract EVERY single topic from ALL pages of the PDF. Output ONLY the raw JSON array.
        """.trimIndent()

        callGemini(context, prompt, pdfUriStr, "pdf", customApiKey)
    }

    suspend fun analyzeMockTestPdfWithGemini(
        context: Context,
        pdfUriStr: String,
        testName: String,
        userScore: Int,
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val examName = "NTA NEET UG"
        val totalMarks = 720
        val subject3Name = "🧬 Biology Analysis"
        val subject3Details = "- Statement-I & Statement-II questions, Assertion-Reason, and Match the Column density vs NTA NEET trend evolution."

        val prompt = """
            You are an expert $examName Examination Analysis AI. You are given a PDF containing a Mock Test paper ('$testName') taken by a student (User's Score: $userScore/$totalMarks).

            Analyze this PDF question paper thoroughly and compare its difficulty level with official $examName papers from 2013 all the way to 2026 (e.g. 2013, 2016, 2019, 2021, 2023, 2024, 2025, 2026).

            Provide a structured, highly detailed analysis covering:
            1. 🎯 **Overall Difficulty Rating**: Rating out of 10 (e.g., 7.5/10) vs Actual $examName trend from 2013 to 2026 (Is it Easier, Exact Level, or Tougher?).
            2. ⚡ **Physics Analysis**:
               - Conceptual vs Formula-based vs Numerical calculation ratio.
               - Compare with official $examName Physics paper trend evolution since 2013.
            3. 🧪 **Chemistry Analysis**:
               - Physical, Organic, and Inorganic balance & syllabus alignment.
               - Calculation intensity & tricky questions vs 2013-2026 $examName Chemistry level.
            4. $subject3Name:
               $subject3Details
            5. 📈 **Score & Percentile / Cutoff Impact**:
               - What score in this mock test corresponds to what rank/percentile/score in actual $examName (comparing historical cutoffs from 2013 to 2026)?
               - Personalized strategic recommendations for the student based on score $userScore/$totalMarks.

            Format your output using clean Markdown with bullet points and bold headers. For mathematical symbols and units, use plain text or standard $ inline math $ / $$ block math $$. Do not leave un-wrapped TeX commands like \text{} or \mathbf{} outside math delimiters.
        """.trimIndent()

        callGemini(context, prompt, pdfUriStr, "pdf", customApiKey)
    }

    suspend fun explainTestQuestionSolution(
        context: Context,
        questionText: String,
        options: List<String>,
        userSelectedOption: String?,
        correctOption: String,
        subject: String,
        chapter: String?,
        baseExplanation: String?,
        customStudentDoubt: String? = null,
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val optionsFormatted = options.mapIndexed { idx, opt ->
            val optLetter = ('A' + idx).toString()
            "Option $optLetter: $opt"
        }.joinToString("\n")

        val doubtSection = if (!customStudentDoubt.isNullOrBlank()) {
            "\n\nStudent's Specific Doubt / Follow-up:\n\"$customStudentDoubt\""
        } else ""

        val prompt = """
            You are an expert NEET and JEE (Main & Advanced) Master Faculty and AI Tutor.
            Provide an ultra-clear, deeply reasoned Step-by-Step AI Solution, Concept Derivation, and Exam Trick for this test question:

            Subject: $subject
            Chapter/Topic: ${chapter ?: "Syllabus Topic"}
            
            Question:
            "$questionText"

            Options:
            $optionsFormatted

            Correct Key: Option $correctOption
            Student's Selected Option: ${userSelectedOption ?: "Unattempted"}
            ${if (!baseExplanation.isNullOrBlank()) "Original Solution Key: $baseExplanation" else ""}
            $doubtSection

            Please structure your explanation cleanly with Markdown and clear headings:
            1. 💡 **Core NCERT / Syllabus Law & Principle**: Fundamental concept tested and exact NCERT connection.
            2. 📐 **Step-by-Step Solution & Mathematical Derivation**: Logical, crystal-clear derivation from fundamental principles.
            3. 🎯 **Option Elimination & Why Other Options Failed**: Clear analysis of Option $correctOption versus the traps in options A, B, C, D.
            4. ⚡ **Speed Trick / 30-Second NEET-JEE Shortcut**: Rapid visual or algebraic trick to solve without lengthy calculations.
            5. ⚠️ **Common Exam Mistake & Trap to Avoid**: Silly mistake that most students make on this problem.

            CRITICAL FORMATTING INSTRUCTIONS FOR MATH & FORMULAS:
            - Use proper Markdown headers (##, ###), bullet points, and bold text.
            - Write ALL mathematical formulas, fractions, powers, subscripts, and physics equations using LaTeX syntax:
              - Inline math: wrapped with single dollar signs, like ${'$'}F = \frac{G m_1 m_2}{r^2}${'$'} or ${'$'}v = u + at${'$'} or ${'$'}\lambda = \frac{h}{p}${'$'}.
              - Display / Multi-step math: wrapped with double dollar signs, like:
                ${'$'}${'$'} \Delta U = n C_v \Delta T ${'$'}${'$'}
            - Chemical equations: ${'$'}2H_2 + O_2 \rightarrow 2H_2O${'$'}, ${'$'}CaCO_3 \xrightarrow{\Delta} CaO + CO_2${'$'}.
        """.trimIndent()

        callGemini(context, prompt, null, null, customApiKey)
    }

    suspend fun solveMistakeWithAiChat(
        context: Context,
        subject: String,
        chapter: String,
        mistakeType: String,
        questionText: String,
        conversationHistory: List<MistakeChatMessage> = emptyList(),
        followUpDoubt: String? = null,
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val historyContext = if (conversationHistory.isNotEmpty()) {
            val formatted = conversationHistory.takeLast(10).joinToString("\n") { msg ->
                val role = if (msg.sender == "user") "Student" else "AI Tutor"
                "[$role]: ${msg.text}"
            }
            "\n[CONVERSATION HISTORY SO FAR]:\n$formatted\n"
        } else ""

        val prompt = if (followUpDoubt.isNullOrBlank() && conversationHistory.isEmpty()) {
            """
                You are a Master Faculty and AI Subject Expert for NEET and JEE competitive exams.
                Analyze the following mistake recorded in the student's Error Notebook and provide a complete, deeply educational Step-by-Step AI Solution and Master Guide.

                Subject: $subject
                Chapter: ${if (chapter.isNotBlank()) chapter else "General Syllabus"}
                Mistake Type: $mistakeType
                
                Question / Recorded Mistake Content:
                "$questionText"

                Please structure your solution using crisp, beautiful Markdown:
                1. 💡 **Core NCERT / Standard Law & Governing Equations**: The fundamental formula, law, or mechanism tested.
                2. 📝 **Complete Step-by-Step Mathematical & Conceptual Solution**: Detailed step-by-step resolution from basics to final answer.
                3. ⚠️ **Mistake Analysis & Why This Trap Occurred**: Pinpoint why '$mistakeType' happens and how to permanently avoid it.
                4. ⚡ **30-Second Exam Shortcut & Rapid Elimination Trick**: Shortcut, dimensional trick, or high-speed mental rule.
                5. 🎯 **Key Takeaway for Exam Day**: Single memorable rule to write in the short notes.

                FORMATTING INSTRUCTIONS FOR FORMULAS & MATH:
                - Write ALL mathematical formulas, fractions, powers, subscripts, and physics equations using LaTeX syntax with single dollar signs for inline math (${'$'}F = ma${'$'}) or double dollar signs for display math (${'$'}${'$'} E = mc^2 ${'$'}${'$'}).
                - Use clear headings (##, ###) and emojis for visual scanning.
            """.trimIndent()
        } else {
            """
                You are the AI Master Faculty continuing an interactive mentoring session on this recorded mistake from the student's Error Notebook:
                Subject: $subject
                Chapter: ${if (chapter.isNotBlank()) chapter else "General Syllabus"}
                Mistake Category: $mistakeType
                Original Question: "$questionText"
                $historyContext

                Student's Latest Follow-up Doubt / Question:
                "${followUpDoubt ?: ""}"

                Provide a direct, crystal-clear, highly educational answer addressing the student's specific doubt.
                - If they asked for a shortcut, give the fastest shortcut.
                - If they asked in Hindi/Hinglish, explain warmly in relatable Hinglish.
                - If they asked about a derivation or alternative method, provide step-by-step logic.
                - Write all math and chemistry formulas in LaTeX ($ for inline, $$ for block).
            """.trimIndent()
        }

        callGemini(context, prompt, null, null, customApiKey)
    }

    private suspend fun callGemini(
        context: Context,
        prompt: String,
        mediaUriStr: String?,
        mediaType: String?,
        customApiKey: String?,
        targetModel: String? = null
    ): Result<String> {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val providerPref = prefs.getString("ai_provider", AiProvider.NATIVE_GEMINI.name) ?: AiProvider.NATIVE_GEMINI.name
        val orKey = (prefs.getString("openrouter_api_key", "") ?: "").trim()
        val orModel = (prefs.getString("openrouter_selected_model", "google/gemini-2.5-flash") ?: "google/gemini-2.5-flash").trim()

        if (providerPref == AiProvider.OPENROUTER.name) {
            // STRICT ISOLATION: When OpenRouter is chosen, Native Gemini API is COMPLETELY OFF!
            if (orKey.isBlank()) {
                return Result.failure(Exception("OpenRouter API Key is missing! Please enter your OpenRouter key in Settings."))
            }
            return try {
                var base64Img: String? = null
                var mediaMime = "image/jpeg"
                if (!mediaUriStr.isNullOrBlank()) {
                    try {
                        val uri = Uri.parse(mediaUriStr)
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bytes = stream.readBytes()
                            if (bytes.isNotEmpty()) {
                                base64Img = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                mediaMime = if (mediaType == "pdf") "application/pdf" else "image/jpeg"
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val rawModel = if (targetModel != null && !targetModel.startsWith("gemini-3.") && targetModel.isNotBlank()) targetModel else orModel
                val activeModel = when {
                    rawModel.contains("/") -> rawModel
                    rawModel.contains("pro", ignoreCase = true) -> "google/gemini-2.5-pro"
                    rawModel.contains("flash", ignoreCase = true) || rawModel.contains("gemini", ignoreCase = true) -> "google/gemini-2.5-flash"
                    else -> rawModel
                }

                val text = OpenRouterManager.callOpenRouterChat(
                    apiKey = orKey,
                    model = activeModel,
                    prompt = prompt,
                    base64Image = base64Img,
                    mimeType = mediaMime,
                    context = context
                )
                if (text.isNotBlank()) {
                    Result.success(text)
                } else {
                    Result.failure(Exception("Empty response received from OpenRouter model: $activeModel"))
                }
            } catch (e: Exception) {
                // STRICT ISOLATION: DO NOT FALLBACK TO GEMINI 3.7! Return OpenRouter failure directly!
                Result.failure(e)
            }
        }

        if (providerPref == AiProvider.GROQ.name) {
            // STRICT ISOLATION: When Groq is chosen, OpenRouter & Native Gemini are COMPLETELY OFF!
            val groqKey = (prefs.getString(GroqManager.KEY_GROQ_KEY, "") ?: "").trim()
            val groqModel = (prefs.getString(GroqManager.KEY_GROQ_MODEL, GroqManager.DEFAULT_TEXT_MODEL) ?: GroqManager.DEFAULT_TEXT_MODEL).trim()

            if (groqKey.isBlank()) {
                return Result.failure(Exception("Groq API Key is missing! Please enter your Groq API key (starts with 'gsk_') in Settings."))
            }

            return try {
                var base64Img: String? = null
                var mediaMime = "image/jpeg"
                if (!mediaUriStr.isNullOrBlank()) {
                    try {
                        val uri = Uri.parse(mediaUriStr)
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bytes = stream.readBytes()
                            if (bytes.isNotEmpty()) {
                                base64Img = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                mediaMime = if (mediaType == "pdf") "application/pdf" else "image/jpeg"
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val activeModel = if (targetModel != null && !targetModel.startsWith("gemini-") && !targetModel.contains("openrouter") && targetModel.isNotBlank()) {
                    targetModel
                } else groqModel

                val text = GroqManager.callGroqChat(
                    apiKey = groqKey,
                    model = activeModel,
                    prompt = prompt,
                    base64Image = base64Img,
                    mimeType = mediaMime
                )
                if (text.isNotBlank()) {
                    Result.success(text)
                } else {
                    Result.failure(Exception("Empty response received from Groq model: $activeModel"))
                }
            } catch (e: Exception) {
                // STRICT ISOLATION: DO NOT FALLBACK! Return Groq failure directly!
                Result.failure(e)
            }
        }

        if (providerPref == AiProvider.CLOUDFLARE.name) {
            // STRICT ISOLATION: When Cloudflare is chosen, Gemini, OpenRouter & Groq are COMPLETELY OFF!
            val cfAccountId = (prefs.getString(CloudflareManager.KEY_CF_ACCOUNT_ID, "") ?: "").trim()
            val cfApiToken = (prefs.getString(CloudflareManager.KEY_CF_API_TOKEN, "") ?: "").trim()
            val cfModel = (prefs.getString(CloudflareManager.KEY_CF_MODEL, CloudflareManager.DEFAULT_TEXT_MODEL) ?: CloudflareManager.DEFAULT_TEXT_MODEL).trim()

            if (cfAccountId.isBlank()) {
                return Result.failure(Exception("Cloudflare Account ID is missing! Please enter your Account ID in Settings."))
            }
            if (cfApiToken.isBlank()) {
                return Result.failure(Exception("Cloudflare API Token is missing! Please enter your API Token in Settings."))
            }

            return try {
                var base64Img: String? = null
                var mediaMime = "image/jpeg"
                if (!mediaUriStr.isNullOrBlank()) {
                    try {
                        val uri = Uri.parse(mediaUriStr)
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val bytes = stream.readBytes()
                            if (bytes.isNotEmpty()) {
                                base64Img = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                mediaMime = if (mediaType == "pdf") "application/pdf" else "image/jpeg"
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val activeModel = if (targetModel != null && targetModel.startsWith("@cf/") && targetModel.isNotBlank()) {
                    targetModel
                } else cfModel

                val text = CloudflareManager.callCloudflareChat(
                    accountId = cfAccountId,
                    apiToken = cfApiToken,
                    model = activeModel,
                    prompt = prompt,
                    base64Image = base64Img,
                    mimeType = mediaMime
                )
                if (text.isNotBlank()) {
                    Result.success(text)
                } else {
                    Result.failure(Exception("Empty response received from Cloudflare model: $activeModel"))
                }
            } catch (e: Exception) {
                // STRICT ISOLATION: Return Cloudflare failure directly!
                Result.failure(e)
            }
        }

        val apiKey = getApiKey(context, customApiKey)
        if (apiKey.isBlank()) {
            return Result.failure(Exception("Gemini API Key is missing! Please configure your Gemini API Key in Settings."))
        }

        val model = if (!targetModel.isNullOrBlank()) {
            GeminiModelManager.resolveEffectiveEndpointModel(targetModel)
        } else {
            GeminiModelManager.getEffectiveModel(context)
        }

        // 1. Check Local Zero-Waste Cache first (Instant result, 0 tokens consumed)
        if (mediaUriStr.isNullOrBlank()) {
            val cached = GeminiRateLimiter.getCachedResponse(model, prompt, null)
            if (!cached.isNullOrBlank()) {
                return Result.success(cached)
            }
        }

        // 2. Client-side RPM Pacing Shield (Ensures Google 429 rate limit is never exceeded)
        GeminiRateLimiter.paceBeforeRequest(context, model)

        return try {
            val contentsArray = JSONArray()
            val partsArray = JSONArray()

            partsArray.put(JSONObject().apply {
                put("text", prompt)
            })

            if (!mediaUriStr.isNullOrBlank()) {
                try {
                    val uri = Uri.parse(mediaUriStr)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val bytes = inputStream.use { it.readBytes() }
                        if (bytes.isNotEmpty()) {
                            val mime = when (mediaType) {
                                "pdf" -> "application/pdf"
                                "image" -> "image/jpeg"
                                else -> "image/jpeg"
                            }
                            val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
                            partsArray.put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", mime)
                                    put("data", base64Data)
                                })
                            })
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            contentsArray.put(JSONObject().apply {
                put("parts", partsArray)
            })

            val standardRequestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 4096)
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val mediaTypeJson = "application/json; charset=utf-8".toMediaType()
            val body = standardRequestJson.toString().toRequestBody(mediaTypeJson)

            // Auto-retry up to 2 times on temporary 429
            var currentAttempt = 0
            val maxAttempts = 3
            var lastResponseCode = 0
            var lastErrorString = ""

            while (currentAttempt < maxAttempts) {
                currentAttempt++
                val request = Request.Builder().url(url).post(body).build()
                val response = client.newCall(request).await()
                val str = response.body?.string() ?: ""
                lastResponseCode = response.code

                if (response.isSuccessful && str.isNotBlank()) {
                    val jsonResponse = JSONObject(str)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val textOutput = candidates.getJSONObject(0)
                            .optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text", "") ?: ""
                        if (textOutput.isNotBlank()) {
                            val trimmedText = textOutput.trim()
                            // Record success in live quota tracker
                            GeminiRateLimiter.recordRequestSuccess(context, model)
                            if (mediaUriStr.isNullOrBlank()) {
                                GeminiRateLimiter.putCachedResponse(model, prompt, null, trimmedText)
                            }
                            return Result.success(trimmedText)
                        } else {
                            return Result.failure(Exception("Empty response from Gemini."))
                        }
                    } else {
                        return Result.failure(Exception("No candidate content returned by Gemini."))
                    }
                } else if (response.code == 429 && currentAttempt < maxAttempts) {
                    // Temporary burst limit hit: wait exponential backoff and retry without changing model
                    delay(3000L * currentAttempt)
                } else {
                    lastErrorString = str
                    break
                }
            }

            if (lastResponseCode == 429) {
                Result.failure(
                    Exception("Gemini Rate Limit (HTTP 429): Daily or minute limit for $model reached. Quota resets at Midnight UTC (5:30 AM IST). Your model remains unchanged.")
                )
            } else {
                val errorMsg = try {
                    JSONObject(lastErrorString).optJSONObject("error")?.optString("message") ?: "HTTP $lastResponseCode"
                } catch (e: Exception) {
                    "HTTP $lastResponseCode"
                }
                Result.failure(Exception("Gemini Error: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun executeTestingPrompt(
        context: Context,
        prompt: String,
        mediaUriStr: String? = null,
        mediaType: String? = null,
        customApiKey: String? = null,
        targetModel: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        callGemini(context, prompt, mediaUriStr, mediaType, customApiKey, targetModel)
    }

    suspend fun analyzePerformanceAndDownfall(
        context: Context,
        mockTests: List<MockTest>,
        practices: List<DailyPractice>,
        logs: List<StudyLog>,
        goals: List<Goal> = emptyList(),
        checklistTopics: List<DynamicChecklistTopic> = emptyList(),
        checklistState: Map<String, Boolean> = emptyMap(),
        dppItems: List<DppItem> = emptyList(),
        scheduledMockTests: List<ScheduledMockTest> = emptyList(),
        studyTubeTodayHours: Float = 0f,
        studyTubeTotalHours: Float = 0f,
        studyTubeLastWatched: String = "",
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        // Compile a high-fidelity summary of student progress for Gemini
        val testSummary = if (mockTests.isEmpty()) {
            "No mock tests taken yet."
        } else {
            mockTests.sortedBy { it.timestamp }.joinToString("\n") {
                "- Test: '${it.testName}' | Total Score: ${it.score}/${it.getMaxScore()} (P: ${it.physics}, C: ${it.chemistry}, B: ${it.biology}, Negatives: ${it.negative})"
            }
        }

        val upcomingTestsSummary = if (scheduledMockTests.isEmpty()) {
            "No upcoming mock tests scheduled."
        } else {
            scheduledMockTests.filter { !it.isCompleted }.joinToString("\n") {
                "- Upcoming Test: '${it.title}' | Date: ${it.scheduledDate} | Physics Syllabus: [${if (it.physicsSyllabus.isNotBlank()) it.physicsSyllabus else "Full"}] | Chemistry Syllabus: [${if (it.chemistrySyllabus.isNotBlank()) it.chemistrySyllabus else "Full"}] | Biology Syllabus: [${if (it.biologySyllabus.isNotBlank()) it.biologySyllabus else "Full"}] | Notes: ${it.syllabusNotes}"
            }
        }

        val practiceSummary = if (practices.isEmpty()) {
            "No practice sessions recorded yet."
        } else {
            practices.sortedBy { it.date }.takeLast(7).joinToString("\n") {
                "- Date: ${it.date} | Solved: Physics ${it.physicsSolved}/${it.physicsTarget}, Chemistry ${it.chemistrySolved}/${it.chemistryTarget}, Biology ${it.biologySolved}/${it.biologyTarget}"
            }
        }

        val studySummary = if (logs.isEmpty()) {
            "No study sessions timed yet."
        } else {
            logs.groupBy { it.subject }.mapValues { (_, sLogs) -> sLogs.sumOf { it.durationSeconds } / 3600f }.entries.joinToString("\n") {
                "- Subject: ${it.key} | Total Study Hours: ${String.format(java.util.Locale.US, "%.2f", it.value)} hours"
            }
        }

        val studyTubeSummary = """
            - StudyTube Watch Time Today: ${String.format(java.util.Locale.US, "%.1f", studyTubeTodayHours)} hours
            - Lifetime StudyTube Watched: ${String.format(java.util.Locale.US, "%.1f", studyTubeTotalHours)} hours
            - Recently Watched Lecture: ${if (studyTubeLastWatched.isNotBlank()) studyTubeLastWatched else "None recorded"}
        """.trimIndent()

        val completedGoalsCount = goals.filter { it.status == GoalStatus.COMPLETED.value }.size
        val totalGoalsCount = goals.size
        val goalsSummary = "Completed $completedGoalsCount out of $totalGoalsCount targets/goals configured by the student."

        val completedDppsCount = dppItems.filter { it.isCompleted }.size
        val totalDppsCount = dppItems.size
        val dppsSummary = "Completed $completedDppsCount out of $totalDppsCount Daily Practice Papers (DPPs)."

        val completedChecklistCount = checklistState.filterValues { it }.size
        var totalChecklistCount = 0
        checklistTopics.forEach { topic ->
            totalChecklistCount += topic.columns.split(",").map { it.trim() }.filter { it.isNotEmpty() }.size
        }
        val checklistSummary = "Completed $completedChecklistCount checklist checkpoints out of $totalChecklistCount total checkpoints in the Lakshya Study Checklist."

        val prompt = """
            You are "LAKSHYA AI Guardian", the student's personal Big Brother, IIT/NEET Topper Coach, and proactive academic mentor.
            Your role is NOT a strict principal or an emotionless bot; you are their trusted mentor who blends high empathy with sharp tactical execution.

            Here is the student's real-time, comprehensive academic database:

            [MOCK TEST TREND DATA]
            $testSummary

            [QUESTION SOLVING PRACTICE TREND (Last 7 Days)]
            $practiceSummary

            [STUDYTUBE VIDEO WATCH TRACKING & STUDY LOGS]
            $studyTubeSummary
            $studySummary

            [TARGETS & GOALS STATUS]
            $goalsSummary

            [DAILY PRACTICE PAPERS (DPP) RATIO]
            $dppsSummary

            [UPCOMING MOCK TESTS & TEST SYLLABUS]
            $upcomingTestsSummary

            [LAKSHYA STUDY CHECKLIST PROGRESS]
            $checklistSummary

            Mentor Training & Guidance Directives:
            1. **Empathy & Anti-Guilt Philosophy**:
               - Never guilt-trip or shame the student if they missed daily goals or had a score drop.
               - Acknowledge their effort, validate feelings of stress or fatigue, and immediately provide a practical 45-minute immediate comeback recovery plan.
            2. **Zombie Learning vs Active Recall (StudyTube Focus)**:
               - Inspect their StudyTube video watch hours against question practice count.
               - If StudyTube watch time is high (e.g., > 1.5 hours) but question practice is low, alert them lovingly: "Bhai, sirf screen dekhne se rank nahi aayegi! Ye 'Zombie Learning' trap hai. Turant video pause karo aur jo topic dekha hai uske 20 questions practice karo."
               - If they are balancing both lecture watching and numerical practice, celebrate their discipline!
            3. **Upcoming Tests & Syllabus Guardian Command**:
               - Specifically check their upcoming scheduled mock tests. Warn them if the test date is near and syllabus topics in Physics, Chemistry, or Biology are still pending!
               - Connect pending DPPs directly with the upcoming test syllabus.
            4. **App Remote Control & Automatic Timer Power**:
               - You have direct power over the app! When advising the student to study, set a timer, or take an action, include actionable command tags in your advice:
                 - `[ACTION:TIMER:<minutes>:<subject>:<chapter>]` (e.g. `[ACTION:TIMER:45:Physics:Kinematics]`)
                 - `[ACTION:NAV:mocks]` (to check upcoming tests & syllabus)
                 - `[ACTION:NAV:targets]` (to solve DPPs and targets)
                 - `[ACTION:NAV:timer]` (to open study timer)
                 - `[ACTION:NAV:studytube]` (to open lectures)
                 - `[ACTION:NAV:mistakes]` (to open error notebook)
               - The app will render interactive 1-tap action buttons directly for the student to execute!
            5. **39-Year Multi-Exam Strategy**:
               - Remind the student that in NEET Physics & Chemistry, practicing 39-Year JEE Main & AIEEE PYQs alongside NEET questions builds unstoppable calculation speed and conceptual depth for 680+ marks.
            6. **Silly Mistakes vs Conceptual Gaps**:
               - If mock test negatives are high, pinpoint whether they are losing marks to calculation rush (silly errors) or skipped topics (conceptual gaps), and give 3 crisp, high-yield action bullets.
            7. **Burnout Prevention**:
               - Remind them to protect 6-7 hours of quality sleep and take 5-minute hydration/breathing breaks.
            8. **Tone & Formatting**:
               - Write in motivating, natural, warm Hinglish (Hindi + English) with clean Markdown and emojis.
               - Keep it under 270 words so it is punchy, clear, and inspiring.
               - End with ONE sharp, diagnostic question that invites them to reflect and reply.
        """.trimIndent()

        callGemini(context, prompt, null, null, customApiKey)
    }

    suspend fun checkKeyStatus(apiKey: String): String = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) return@withContext "Not Configured"
        val testModels = listOf("gemini-3.8-flash", "gemini-3.5-flash")
        var lastErrorMsg = ""
        var isInvalid = false
        var isQuota = false

        for (model in testModels) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$cleanKey"
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", "Hi") })
                        })
                    })
                }
                val requestJson = JSONObject().apply {
                    put("contents", contentsArray)
                }
                val mediaTypeJson = "application/json; charset=utf-8".toMediaType()
                val body = requestJson.toString().toRequestBody(mediaTypeJson)
                val request = Request.Builder().url(url).post(body).build()
                val response = client.newCall(request).await()
                val str = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    return@withContext "Active (Ready) ✅"
                } else {
                    val errorMsg = try {
                        JSONObject(str).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                    } catch (e: Exception) {
                        "HTTP ${response.code}"
                    }
                    lastErrorMsg = errorMsg
                    if (response.code == 400 && (errorMsg.contains("API_KEY_INVALID", ignoreCase = true) || errorMsg.contains("invalid", ignoreCase = true))) {
                        isInvalid = true
                        break
                    }
                    if (response.code == 429 || errorMsg.contains("quota", ignoreCase = true) || errorMsg.contains("limit", ignoreCase = true) || errorMsg.contains("exhausted", ignoreCase = true)) {
                        isQuota = true
                    }
                }
            } catch (e: Exception) {
                lastErrorMsg = e.localizedMessage ?: "Connection Failed"
            }
        }

        if (isInvalid) {
            "Invalid Key ❌"
        } else if (isQuota) {
            "Limit Exceeded ⚠️ (Quota Exhausted)"
        } else if (lastErrorMsg.isNotBlank()) {
            "Limit/Error: $lastErrorMsg ⚠️"
        } else {
            "Verification Failed ⚠️"
        }
    }

    fun getCurrentlyWorkingApiKey(context: Context): String {
        return getApiKey(context)
    }

    suspend fun scanTestbookScorecardWithGeminiVision(
        context: Context,
        imageBytes: ByteArray?,
        imageUriStr: String?,
        customApiKey: String? = null
    ): Result<TestbookScorecardResult> = withContext(Dispatchers.IO) {
        val prompt = """
            You are an expert OCR & NEET/JEE Test Analyzer AI.
            Analyze the provided image of a Testbook Test / Mock Test scorecard, result summary, or long full-page report from TOP to BOTTOM with extreme precision.
            Examine all sections: Header title, summary cards, subject score tables, question matrices, accuracy gauges, and bottom mistake breakdown / solutions.

            CRITICAL EXTRACTION GOALS:
            1. Test Title (e.g. "Testbook NEET Full Mock Test #1", "Testbook Physics Sectional Test (180 Marks)", "Testbook DPP Chapter Test (45 Marks)", etc.)
            2. Total Score achieved AND EXACT Maximum Marks of this test (e.g. totalScore: 165, maxScore: 180 for a sectional test, or totalScore: 620, maxScore: 720 for a full test, or maxScore: 45, 90, 300, 360, 720 depending on what is written on the scorecard). NEVER assume maxScore is 720 if it's a chapter/sectional test!
            3. Subject-wise marks breakdown and individual maximum marks if applicable:
               - Physics score & max marks
               - Chemistry score & max marks
               - Biology (or Botany + Zoology combined) score & max marks
            4. Detailed Question Stats if visible across the entire page:
               - Physics correct, incorrect, unattempted count
               - Chemistry correct, incorrect, unattempted count
               - Biology correct, incorrect, unattempted count
            5. Negative marks deducted (if shown).
            6. Overall accuracy percentage (e.g. 88.5%).
            7. Rank and Percentile (e.g. "Rank: 342", "Percentile: 98.6%").
            8. Identified weak topics or chapters from the mistakes/summary list anywhere on the page.
            9. Specific mistake highlights or lessons to record in the student's Mistakes / Error Notebook.

            OUTPUT FORMAT (MANDATORY):
            You MUST return a valid JSON object wrapped in ```json ``` code fence matching this exact schema:
            ```json
            {
              "testTitle": "Testbook Mock Test",
              "totalScore": 140,
              "maxScore": 180,
              "physicsScore": 140,
              "physicsMax": 180,
              "chemistryScore": 0,
              "chemistryMax": 0,
              "biologyScore": 0,
              "biologyMax": 0,
              "negativeMarks": 5,
              "accuracyPercentage": 84.5,
              "rankOrPercentile": "AIR 1420 (97.8%ile)",
              "physicsCorrect": 36,
              "physicsIncorrect": 9,
              "chemistryCorrect": 0,
              "chemistryIncorrect": 0,
              "biologyCorrect": 0,
              "biologyIncorrect": 0,
              "weakTopics": ["Rotational Motion", "Work Power Energy"],
              "mistakeHighlights": [
                "Calculation slip in moment of inertia integration"
              ],
              "aiMentorAdvice": "Short 2-3 sentence strategic advice on how to improve this score in the next test."
            }
            ```

            NOTE ON MAXIMUM MARKS:
            - If it is a full length NEET mock test, maxScore is typically 720 (Physics 180, Chem 180, Bio 360).
            - If it is a Sectional / Subject test (e.g. Physics Only), maxScore is typically 180, 100, or 45.
            - If it is a Chapter/Topic DPP test, maxScore could be 40, 45, 80, 90, 100, etc. Look at the total questions and marks on the scorecard! Extract the TRUE max marks from the image.
        """.trimIndent()

        try {
            val responseResult = if (imageBytes != null && imageBytes.isNotEmpty()) {
                callGeminiWithBytes(context, prompt, imageBytes, "image/jpeg", customApiKey)
            } else {
                callGemini(context, prompt, imageUriStr, "image", customApiKey)
            }

            if (responseResult.isFailure) {
                return@withContext Result.failure(responseResult.exceptionOrNull() ?: Exception("Failed to analyze image"))
            }

            val text = responseResult.getOrNull() ?: ""
            val jsonString = extractJsonFromMarkdown(text)

            val parsedResult = try {
                val json = JSONObject(jsonString)
                val weakTopicsList = mutableListOf<String>()
                val weakTopicsArray = json.optJSONArray("weakTopics")
                if (weakTopicsArray != null) {
                    for (i in 0 until weakTopicsArray.length()) {
                        weakTopicsList.add(weakTopicsArray.getString(i))
                    }
                }

                val mistakeList = mutableListOf<String>()
                val mistakeArray = json.optJSONArray("mistakeHighlights")
                if (mistakeArray != null) {
                    for (i in 0 until mistakeArray.length()) {
                        mistakeList.add(mistakeArray.getString(i))
                    }
                }

                val extractedMaxScore = json.optInt("maxScore", 0)
                val phyScore = json.optInt("physicsScore", 0)
                val chemScore = json.optInt("chemistryScore", 0)
                val bioScore = json.optInt("biologyScore", 0)
                val phyMax = json.optInt("physicsMax", if (phyScore > 0) 180 else 0)
                val chemMax = json.optInt("chemistryMax", if (chemScore > 0) 180 else 0)
                val bioMax = json.optInt("biologyMax", if (bioScore > 0) 360 else 0)

                // Intelligent calculation of maxScore if not explicitly given
                val computedMax = if (extractedMaxScore > 0) {
                    extractedMaxScore
                } else if (phyMax + chemMax + bioMax > 0) {
                    phyMax + chemMax + bioMax
                } else {
                    720
                }

                TestbookScorecardResult(
                    testTitle = json.optString("testTitle", "Testbook Mock Test").takeIf { it.isNotBlank() } ?: "Testbook Mock Test",
                    totalScore = json.optInt("totalScore", 0),
                    maxScore = computedMax,
                    physicsScore = phyScore,
                    physicsMax = phyMax,
                    chemistryScore = chemScore,
                    chemistryMax = chemMax,
                    biologyScore = bioScore,
                    biologyMax = bioMax,
                    negativeMarks = json.optInt("negativeMarks", 0),
                    accuracyPercentage = json.optDouble("accuracyPercentage", 0.0).toFloat(),
                    rankOrPercentile = json.optString("rankOrPercentile", ""),
                    physicsCorrect = json.optInt("physicsCorrect", 0),
                    physicsIncorrect = json.optInt("physicsIncorrect", 0),
                    chemistryCorrect = json.optInt("chemistryCorrect", 0),
                    chemistryIncorrect = json.optInt("chemistryIncorrect", 0),
                    biologyCorrect = json.optInt("biologyCorrect", 0),
                    biologyIncorrect = json.optInt("biologyIncorrect", 0),
                    weakTopics = weakTopicsList,
                    mistakesIdentified = mistakeList,
                    rawAnalysisMarkdown = json.optString("aiMentorAdvice", text)
                )
            } catch (e: Exception) {
                // Fallback default if JSON parser had trouble
                TestbookScorecardResult(
                    testTitle = "Testbook Mock Test",
                    totalScore = 0,
                    rawAnalysisMarkdown = text
                )
            }

            Result.success(parsedResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun callGeminiWithBytes(
        context: Context,
        prompt: String,
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg",
        customApiKey: String? = null,
        targetModel: String? = null
    ): Result<String> = callGeminiWithMultiBytes(context, prompt, listOf(imageBytes), mimeType, customApiKey, targetModel)

    private suspend fun callGeminiWithMultiBytes(
        context: Context,
        prompt: String,
        images: List<ByteArray>,
        mimeType: String = "image/jpeg",
        customApiKey: String? = null,
        targetModel: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val providerPref = prefs.getString("ai_provider", AiProvider.NATIVE_GEMINI.name) ?: AiProvider.NATIVE_GEMINI.name
        val orKey = (prefs.getString("openrouter_api_key", "") ?: "").trim()
        val orModel = (prefs.getString("openrouter_selected_model", "google/gemini-2.5-flash") ?: "google/gemini-2.5-flash").trim()

        if (providerPref == AiProvider.OPENROUTER.name) {
            // STRICT ISOLATION: When OpenRouter is chosen, Native Gemini API is COMPLETELY OFF!
            if (orKey.isBlank()) {
                return@withContext Result.failure(Exception("OpenRouter API Key is missing! Please enter your OpenRouter key in Settings."))
            }
            return@withContext try {
                val rawModel = if (targetModel != null && !targetModel.startsWith("gemini-3.") && targetModel.isNotBlank()) targetModel else orModel
                val activeModel = when {
                    rawModel.contains("/") -> rawModel
                    rawModel.contains("pro", ignoreCase = true) -> "google/gemini-2.5-pro"
                    rawModel.contains("flash", ignoreCase = true) || rawModel.contains("gemini", ignoreCase = true) -> "google/gemini-2.5-flash"
                    else -> rawModel
                }

                val firstImg = images.firstOrNull { it.isNotEmpty() }
                val base64Data = if (firstImg != null) Base64.encodeToString(firstImg, Base64.NO_WRAP) else null

                val text = OpenRouterManager.callOpenRouterChat(
                    apiKey = orKey,
                    model = activeModel,
                    prompt = prompt,
                    base64Image = base64Data,
                    mimeType = mimeType,
                    context = context
                )
                if (text.isNotBlank()) {
                    Result.success(text.trim())
                } else {
                    Result.failure(Exception("Empty response received from OpenRouter model: $activeModel"))
                }
            } catch (e: Exception) {
                // STRICT ISOLATION: DO NOT FALLBACK TO GEMINI 3.7! Return OpenRouter failure directly!
                Result.failure(e)
            }
        }

        if (providerPref == AiProvider.GROQ.name) {
            // STRICT ISOLATION: When Groq is chosen, OpenRouter & Native Gemini are COMPLETELY OFF!
            val groqKey = (prefs.getString(GroqManager.KEY_GROQ_KEY, "") ?: "").trim()
            val groqModel = (prefs.getString(GroqManager.KEY_GROQ_MODEL, GroqManager.DEFAULT_VISION_MODEL) ?: GroqManager.DEFAULT_VISION_MODEL).trim()

            if (groqKey.isBlank()) {
                return@withContext Result.failure(Exception("Groq API Key is missing! Please enter your Groq API key (starts with 'gsk_') in Settings."))
            }

            return@withContext try {
                val firstImg = images.firstOrNull { it.isNotEmpty() }
                val base64Data = if (firstImg != null) Base64.encodeToString(firstImg, Base64.NO_WRAP) else null

                val text = GroqManager.callGroqChat(
                    apiKey = groqKey,
                    model = groqModel,
                    prompt = prompt,
                    base64Image = base64Data,
                    mimeType = mimeType
                )
                if (text.isNotBlank()) {
                    Result.success(text.trim())
                } else {
                    Result.failure(Exception("Empty response received from Groq model: $groqModel"))
                }
            } catch (e: Exception) {
                // STRICT ISOLATION: DO NOT FALLBACK! Return Groq failure directly!
                Result.failure(e)
            }
        }

        if (providerPref == AiProvider.CLOUDFLARE.name) {
            // STRICT ISOLATION: When Cloudflare is chosen, Gemini, OpenRouter & Groq are COMPLETELY OFF!
            val cfAccountId = (prefs.getString(CloudflareManager.KEY_CF_ACCOUNT_ID, "") ?: "").trim()
            val cfApiToken = (prefs.getString(CloudflareManager.KEY_CF_API_TOKEN, "") ?: "").trim()
            val cfModel = (prefs.getString(CloudflareManager.KEY_CF_MODEL, CloudflareManager.DEFAULT_VISION_MODEL) ?: CloudflareManager.DEFAULT_VISION_MODEL).trim()

            if (cfAccountId.isBlank()) {
                return@withContext Result.failure(Exception("Cloudflare Account ID is missing! Please enter your Account ID in Settings."))
            }
            if (cfApiToken.isBlank()) {
                return@withContext Result.failure(Exception("Cloudflare API Token is missing! Please enter your API Token in Settings."))
            }

            return@withContext try {
                val firstImg = images.firstOrNull { it.isNotEmpty() }
                val base64Data = if (firstImg != null) Base64.encodeToString(firstImg, Base64.NO_WRAP) else null

                val activeModel = if (targetModel != null && targetModel.startsWith("@cf/") && targetModel.isNotBlank()) {
                    targetModel
                } else cfModel

                val text = CloudflareManager.callCloudflareChat(
                    accountId = cfAccountId,
                    apiToken = cfApiToken,
                    model = activeModel,
                    prompt = prompt,
                    base64Image = base64Data,
                    mimeType = mimeType
                )
                if (text.isNotBlank()) {
                    Result.success(text.trim())
                } else {
                    Result.failure(Exception("Empty response received from Cloudflare model: $activeModel"))
                }
            } catch (e: Exception) {
                // STRICT ISOLATION: Return Cloudflare failure directly!
                Result.failure(e)
            }
        }

        val apiKey = getApiKey(context, customApiKey)
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("Gemini API Key is missing! Please configure your Gemini API Key in Settings."))
        }

        val model = GeminiModelManager.getEffectiveModel(context)
        GeminiRateLimiter.paceBeforeRequest(context, model)

        try {
            val contentsArray = JSONArray()
            val partsArray = JSONArray()

            partsArray.put(JSONObject().apply {
                put("text", prompt)
            })

            for (img in images) {
                if (img.isNotEmpty()) {
                    val base64Data = Base64.encodeToString(img, Base64.NO_WRAP)
                    partsArray.put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", mimeType)
                            put("data", base64Data)
                        })
                    })
                }
            }

            contentsArray.put(JSONObject().apply {
                put("parts", partsArray)
            })

            val standardRequestJson = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("maxOutputTokens", 4096)
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val mediaTypeJson = "application/json; charset=utf-8".toMediaType()
            val body = standardRequestJson.toString().toRequestBody(mediaTypeJson)

            var currentAttempt = 0
            val maxAttempts = 3
            var lastResponseCode = 0
            var lastErrorString = ""

            while (currentAttempt < maxAttempts) {
                currentAttempt++
                val request = Request.Builder().url(url).post(body).build()
                val response = client.newCall(request).await()
                val str = response.body?.string() ?: ""
                lastResponseCode = response.code

                if (response.isSuccessful && str.isNotBlank()) {
                    val jsonResponse = JSONObject(str)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val textOutput = candidates.getJSONObject(0)
                            .optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text", "") ?: ""
                        if (textOutput.isNotBlank()) {
                            val trimmedText = textOutput.trim()
                            GeminiRateLimiter.recordRequestSuccess(context, model)
                            return@withContext Result.success(trimmedText)
                        } else {
                            return@withContext Result.failure(Exception("Empty multimodal response from Gemini."))
                        }
                    } else {
                        return@withContext Result.failure(Exception("No candidates returned from Gemini."))
                    }
                } else if (response.code == 429 && currentAttempt < maxAttempts) {
                    delay(3000L * currentAttempt)
                } else {
                    lastErrorString = str
                    break
                }
            }

            if (lastResponseCode == 429) {
                Result.failure(
                    Exception("Gemini Rate Limit (HTTP 429): Limit for $model reached. Quota resets at Midnight UTC (5:30 AM IST).")
                )
            } else {
                val errorMsg = try {
                    JSONObject(lastErrorString).optJSONObject("error")?.optString("message") ?: "HTTP $lastResponseCode"
                } catch (e: Exception) {
                    "HTTP $lastResponseCode"
                }
                Result.failure(Exception("Gemini Error: $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun extractMistakesFromTestbookScreenshots(
        context: Context,
        imagesBytesList: List<ByteArray>,
        customApiKey: String? = null
    ): Result<TestbookMistakeExtractionResult> = withContext(Dispatchers.IO) {
        if (imagesBytesList.isEmpty()) {
            return@withContext Result.failure(Exception("No screenshot provided for analysis."))
        }

        val prompt = """
            You are an Elite NTA NEET & JEE AI Tutor, Vision OCR Specialist, and Error-Pattern Analyst.
            The user has provided screenshot(s) from Testbook or a Mock Test practice platform containing questions they solved or got wrong during a test/DPP.

            YOUR MISSION:
            1. Examine every question in the image(s) with extreme mathematical and scientific precision.
            2. Specifically identify questions that were:
               - INCORRECT / Marked Wrong (look for red cross ❌, red highlighted option, negative mark deduction, or student's chosen option vs the correct green option).
               - SKIPPED / Unattempted or confusing questions.
               - If multiple questions or a full test solution sheet is visible, EXTRACT ALL OF THEM SEPARATELY.
            3. For EACH question found, extract:
               - subject: STRICTLY one of "Physics", "Chemistry", or "Biology" (if Botany or Zoology, name "Biology").
               - chapter: Inferred or detected chapter name (e.g. "Ray Optics", "Thermodynamics", "Chemical Bonding", "Genetics & Evolution", "Human Physiology", "Current Electricity").
               - questionText: Complete, verbatim question statement with equations, chemical reactions, units, and clear representation of symbols.
               - options: Array of 4 choices (e.g. ["(A) 24 m/s", "(B) 18 m/s", "(C) 12 m/s", "(D) 36 m/s"]).
               - userWrongOption: The option marked wrong by the aspirant (e.g. "(A) 24 m/s") or "Unattempted / Left" if unattempted.
               - correctOption: The actual correct option (e.g. "(B) 18 m/s").
               - mistakeType: Classify accurately into one of:
                 "Silly Mistake", "Conceptual Gap", "Formula Error", "Calculation Slip", "Time Rush", "Question Misread", "Unit Conversion Error", "Sign Convention Error".
               - keyConceptMissed: Concise 1-2 sentence core formula, rule, or concept missed (e.g. "Forgot to convert wavelength from Angstroms to meters before computing kinetic energy K_max = h*nu - phi").
               - stepByStepSolution: Crisp step-by-step master solution explaining how to solve it in under 60 seconds.
               - formattedNotebookEntry: A complete, beautifully formatted entry for the student's Mistake Notebook. It should include:
                 **[Chapter Name]** Topic
                 **Q:** <Question Statement>
                 ❌ **My Answer:** <Wrong Option>
                 ✅ **Correct Answer:** <Correct Option>
                 ⚠️ **Mistake Reason:** <Mistake Type>
                 💡 **Key Concept:** <Core formula/concept>
                 
                 **Master Solution:**
                 <Crisp Step-by-step working>

            OUTPUT FORMAT (MANDATORY JSON):
            You MUST return a valid JSON object wrapped in ```json ``` code fence matching this schema:
            ```json
            {
              "totalDetected": 2,
              "summaryReport": "Extracted 2 incorrect questions from the screenshots.",
              "questions": [
                {
                  "subject": "Physics",
                  "chapter": "Current Electricity",
                  "questionText": "In a potentiometer circuit, a balance point is obtained at 56 cm...",
                  "options": ["(A) 2.25 V", "(B) 2.0 V", "(C) 1.8 V", "(D) 2.5 V"],
                  "userWrongOption": "(A) 2.25 V",
                  "correctOption": "(B) 2.0 V",
                  "mistakeType": "Calculation Slip",
                  "keyConceptMissed": "Direct proportionality E1/E2 = l1/l2, slipped in fractional cross-multiplication.",
                  "stepByStepSolution": "Using potentiometer relation E1/E2 = l1/l2 => E2 = E1 * (l2/l1) = 1.25 * (56.0/35.0) = 2.0 V.",
                  "formattedNotebookEntry": "**[Current Electricity]** Potentiometer Balance Point\\n\\n**Q:** In a potentiometer circuit, a balance point is obtained at 56 cm...\\n\\n❌ **My Answer:** (A) 2.25 V\\n✅ **Correct Answer:** (B) 2.0 V\\n⚠️ **Mistake Reason:** Calculation Slip\\n💡 **Key Concept:** E1/E2 = l1/l2\\n\\n**Master Solution:**\\nE2 = 1.25 * (56.0 / 35.0) = 2.0 V"
                }
              ]
            }
            ```
        """.trimIndent()

        try {
            val responseResult = callGeminiWithMultiBytes(context, prompt, imagesBytesList, "image/jpeg", customApiKey)
            if (responseResult.isFailure) {
                return@withContext Result.failure(responseResult.exceptionOrNull() ?: Exception("Failed to analyze screenshot"))
            }

            val text = responseResult.getOrNull() ?: ""
            val jsonString = extractJsonFromMarkdown(text)

            val parsedResult = try {
                val json = JSONObject(jsonString)
                val total = json.optInt("totalDetected", 0)
                val summary = json.optString("summaryReport", "Extracted questions")
                val questionsArray = json.optJSONArray("questions")
                val extractedList = mutableListOf<ExtractedIncorrectQuestion>()

                if (questionsArray != null) {
                    for (i in 0 until questionsArray.length()) {
                        val qObj = questionsArray.getJSONObject(i)
                        val subjectRaw = qObj.optString("subject", "Physics")
                        val subject = when {
                            subjectRaw.contains("chem", ignoreCase = true) -> "Chemistry"
                            subjectRaw.contains("bio", ignoreCase = true) || subjectRaw.contains("bot", ignoreCase = true) || subjectRaw.contains("zoo", ignoreCase = true) -> "Biology"
                            else -> "Physics"
                        }

                        val optionsList = mutableListOf<String>()
                        val optsArr = qObj.optJSONArray("options")
                        if (optsArr != null) {
                            for (j in 0 until optsArr.length()) {
                                optionsList.add(optsArr.getString(j))
                            }
                        }

                        extractedList.add(
                            ExtractedIncorrectQuestion(
                                id = java.util.UUID.randomUUID().toString(),
                                subject = subject,
                                chapter = qObj.optString("chapter", "General"),
                                questionText = qObj.optString("questionText", ""),
                                options = optionsList,
                                userWrongOption = qObj.optString("userWrongOption", "Marked Incorrect"),
                                correctOption = qObj.optString("correctOption", "Correct Option"),
                                mistakeType = qObj.optString("mistakeType", "Silly Mistake"),
                                keyConceptMissed = extractStringOrArray(qObj, "keyConceptMissed"),
                                stepByStepSolution = extractStringOrArray(qObj, "stepByStepSolution"),
                                formattedNotebookEntry = qObj.optString("formattedNotebookEntry", ""),
                                isSelectedForImport = true
                            )
                        )
                    }
                }

                TestbookMistakeExtractionResult(
                    totalDetected = if (total > 0) total else extractedList.size,
                    questions = extractedList,
                    summaryReport = summary,
                    rawText = text
                )
            } catch (e: Exception) {
                // Fallback if parsing failed
                TestbookMistakeExtractionResult(
                    totalDetected = 1,
                    questions = listOf(
                        ExtractedIncorrectQuestion(
                            subject = "Physics",
                            chapter = "Extracted Test Question",
                            questionText = text.take(500),
                            formattedNotebookEntry = text,
                            mistakeType = "Conceptual Gap"
                        )
                    ),
                    summaryReport = "Extracted 1 question",
                    rawText = text
                )
            }

            Result.success(parsedResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun extractMistakesFromPdfOrMedia(
        context: Context,
        bytes: ByteArray,
        mimeType: String = "application/pdf",
        fileName: String = "document.pdf",
        customApiKey: String? = null
    ): Result<TestbookMistakeExtractionResult> = withContext(Dispatchers.IO) {
        if (bytes.isEmpty()) {
            return@withContext Result.failure(Exception("File is empty."))
        }
        val isPdf = mimeType.equals("application/pdf", ignoreCase = true) || fileName.endsWith(".pdf", ignoreCase = true)
        val actualMime = if (isPdf) "application/pdf" else mimeType

        val prompt = """
            You are an Elite NTA NEET & JEE AI Tutor, Vision OCR Specialist, and Error-Pattern Analyst.
            The user has provided a ${if (isPdf) "PDF document (test paper, DPP, mock test solution, question sheet, or mistake notebook worksheet)" else "document/image"} named "$fileName".

            YOUR MISSION:
            1. Examine every question in the provided file with extreme mathematical, chemical, and biological precision.
            2. Identify questions from the document that were:
               - INCORRECT / Marked Wrong, skipped, or tricky questions that need revision.
               - If it's a test paper / DPP / mock test, EXTRACT the key questions with their core concepts, formulas, and step-by-step solutions so they can be logged into the student's Mistake Notebook.
               - Extract ALL distinct questions present (up to 15 key questions per document).
            3. For EACH question found, extract:
               - subject: STRICTLY one of "Physics", "Chemistry", or "Biology" (if Botany or Zoology, name "Biology").
               - chapter: Inferred or detected chapter name (e.g. "Ray Optics", "Thermodynamics", "Chemical Bonding", "Genetics & Evolution", "Human Physiology", "Current Electricity").
               - questionText: Complete, verbatim question statement with equations, chemical reactions, units, and clear representation of symbols.
               - options: Array of 4 choices (e.g. ["(A) 24 m/s", "(B) 18 m/s", "(C) 12 m/s", "(D) 36 m/s"]).
               - userWrongOption: The option marked wrong or attempted by the aspirant (e.g. "(A) 24 m/s") or "Unattempted / Left" if unattempted.
               - correctOption: The actual correct option (e.g. "(B) 18 m/s").
               - mistakeType: Classify accurately into one of:
                 "Silly Mistake", "Conceptual Gap", "Formula Error", "Calculation Slip", "Time Rush", "Question Misread", "Unit Conversion Error", "Sign Convention Error".
               - keyConceptMissed: Concise 1-2 sentence core formula, rule, or concept missed.
               - stepByStepSolution: Crisp step-by-step master solution explaining how to solve it in under 60 seconds.
               - formattedNotebookEntry: A complete, beautifully formatted entry for the student's Mistake Notebook.

            OUTPUT FORMAT (MANDATORY JSON):
            You MUST return a valid JSON object wrapped in ```json ``` code fence matching this schema:
            ```json
            {
              "totalDetected": 2,
              "summaryReport": "Extracted questions from $fileName.",
              "questions": [
                {
                  "subject": "Physics",
                  "chapter": "Current Electricity",
                  "questionText": "In a potentiometer circuit, a balance point is obtained at 56 cm...",
                  "options": ["(A) 2.25 V", "(B) 2.0 V", "(C) 1.8 V", "(D) 2.5 V"],
                  "userWrongOption": "(A) 2.25 V",
                  "correctOption": "(B) 2.0 V",
                  "mistakeType": "Calculation Slip",
                  "keyConceptMissed": "Direct proportionality E1/E2 = l1/l2, slipped in fractional cross-multiplication.",
                  "stepByStepSolution": "Using potentiometer relation E1/E2 = l1/l2 => E2 = E1 * (l2/l1) = 1.25 * (56.0/35.0) = 2.0 V.",
                  "formattedNotebookEntry": "**[Current Electricity]** Potentiometer Balance Point\n\n**Q:** In a potentiometer circuit, a balance point is obtained at 56 cm...\n\n❌ **My Answer:** (A) 2.25 V\n✅ **Correct Answer:** (B) 2.0 V\n⚠️ **Mistake Reason:** Calculation Slip\n💡 **Key Concept:** E1/E2 = l1/l2\n\n**Master Solution:**\nE2 = 1.25 * (56.0 / 35.0) = 2.0 V"
                }
              ]
            }
            ```
        """.trimIndent()

        try {
            val responseResult = callGeminiWithMultiBytes(context, prompt, listOf(bytes), actualMime, customApiKey)
            if (responseResult.isFailure) {
                return@withContext Result.failure(responseResult.exceptionOrNull() ?: Exception("Failed to analyze PDF/document"))
            }

            val text = responseResult.getOrNull() ?: ""
            val jsonString = extractJsonFromMarkdown(text)

            val parsedResult = try {
                val json = JSONObject(jsonString)
                val total = json.optInt("totalDetected", 0)
                val summary = json.optString("summaryReport", "Extracted questions from $fileName")
                val questionsArray = json.optJSONArray("questions")
                val extractedList = mutableListOf<ExtractedIncorrectQuestion>()

                if (questionsArray != null) {
                    for (i in 0 until questionsArray.length()) {
                        val qObj = questionsArray.getJSONObject(i)
                        val subjectRaw = qObj.optString("subject", "Physics")
                        val subject = when {
                            subjectRaw.contains("chem", ignoreCase = true) -> "Chemistry"
                            subjectRaw.contains("bio", ignoreCase = true) || subjectRaw.contains("bot", ignoreCase = true) || subjectRaw.contains("zoo", ignoreCase = true) -> "Biology"
                            else -> "Physics"
                        }

                        val optionsList = mutableListOf<String>()
                        val optsArr = qObj.optJSONArray("options")
                        if (optsArr != null) {
                            for (j in 0 until optsArr.length()) {
                                optionsList.add(optsArr.getString(j))
                            }
                        }

                        extractedList.add(
                            ExtractedIncorrectQuestion(
                                id = java.util.UUID.randomUUID().toString(),
                                subject = subject,
                                chapter = qObj.optString("chapter", "General"),
                                questionText = qObj.optString("questionText", ""),
                                options = optionsList,
                                userWrongOption = qObj.optString("userWrongOption", "Marked Incorrect"),
                                correctOption = qObj.optString("correctOption", "Correct Option"),
                                mistakeType = qObj.optString("mistakeType", "Silly Mistake"),
                                keyConceptMissed = extractStringOrArray(qObj, "keyConceptMissed"),
                                stepByStepSolution = extractStringOrArray(qObj, "stepByStepSolution"),
                                formattedNotebookEntry = qObj.optString("formattedNotebookEntry", ""),
                                isSelectedForImport = true
                            )
                        )
                    }
                }

                TestbookMistakeExtractionResult(
                    totalDetected = if (total > 0) total else extractedList.size,
                    questions = extractedList,
                    summaryReport = summary,
                    rawText = text
                )
            } catch (e: Exception) {
                TestbookMistakeExtractionResult(
                    totalDetected = 1,
                    questions = listOf(
                        ExtractedIncorrectQuestion(
                            subject = "Physics",
                            chapter = "Extracted PDF Question",
                            questionText = text.take(500),
                            formattedNotebookEntry = text,
                            mistakeType = "Conceptual Gap",
                            userWrongOption = "Needs Review",
                            correctOption = "Verified in Solution",
                            keyConceptMissed = "Extracted from PDF: $fileName",
                            stepByStepSolution = text
                        )
                    ),
                    summaryReport = "Extracted content from PDF",
                    rawText = text
                )
            }

            Result.success(parsedResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun syncGmcCutoffsWithGeminiAi(
        context: Context,
        userScore: Int,
        category: String,
        quota: String,
        preferredState: String,
        mockScoresHistory: List<Int> = emptyList(),
        customApiKey: String? = null
    ): Result<GmcAiAnalysisResult> = withContext(Dispatchers.IO) {
        val prompt = """
            You are India's premier MCC & State Quota Medical Counseling Expert and NEET UG Analytics AI.
            A NEET aspirant is analyzing their Medical College (GMC/AIIMS) admission prospects with the following profile:
            - Score: $userScore / 720
            - Category: $category
            - Target Quota: $quota (All India Quota 15% / State Quota 85%)
            - Domicile / Preferred State: $preferredState
            ${if (mockScoresHistory.isNotEmpty()) "- Recent Mock Test Score History: ${mockScoresHistory.takeLast(5).joinToString(", ")} (Average: ${mockScoresHistory.average().toInt()})" else ""}

            Perform a Live Real-Time Medical College Cutoff & Counseling Intelligence Analysis based on the latest NTA NEET UG and MCC Counseling seat matrix, historic cutoffs (2024, 2025) and 2026 projected inflation/normalization dynamics.

            Provide a structured JSON response in the following EXACT schema:
            {
              "expectedAirRankRange": "AIR 4,500 - 6,200",
              "admissionChancesSummary": "High chance for Top State GMCs (e.g. SMS Jaipur, BJMC Pune) and Moderate chance for AIIMS Tier-2 (e.g. AIIMS Patna/Bhopal)",
              "roundShiftForecast": "Round 1 AIQ General expected ~665+, Round 2 dips to ~652, Mop-up ~646",
              "topRecommendedColleges": [
                {
                  "collegeName": "AIIMS Bhopal",
                  "state": "Madhya Pradesh",
                  "quota": "AIQ 15%",
                  "safetyLevel": "Target",
                  "estimatedClosingScore": "668 - 675",
                  "reason": "Top clinical infra, moderate AIQ closing rank"
                },
                {
                  "collegeName": "Sawai Man Singh Medical College (SMS)",
                  "state": "Rajasthan",
                  "quota": "State Quota 85%",
                  "safetyLevel": "Safe",
                  "estimatedClosingScore": "655 - 662",
                  "reason": "High seat matrix, excellent clinical exposure"
                },
                {
                  "collegeName": "Maulana Azad Medical College (MAMC)",
                  "state": "Delhi",
                  "quota": "AIQ 15%",
                  "safetyLevel": "Reach",
                  "estimatedClosingScore": "700+",
                  "reason": "Apex college, very high cutoff threshold"
                }
              ],
              "strategicCounselingAdvice": "Key choice-filling strategy, bond insights (years & penalty), and AIQ vs State Quota comparison advice.",
              "rawAnalysisMarkdown": "Full detailed markdown analysis covering state quota advantages, category relaxation trends, and mock-to-NEET score conversion guidance."
            }

            Ensure the output is ONLY valid JSON.
        """.trimIndent()

        try {
            val geminiResult = callGemini(context, prompt, null, null, customApiKey)
            if (geminiResult.isFailure) {
                return@withContext Result.failure(geminiResult.exceptionOrNull() ?: Exception("Gemini sync failed"))
            }

            val text = geminiResult.getOrNull() ?: ""
            val jsonStr = extractJsonFromMarkdown(text)
            // ...
            val parsedResult = try {
                val json = JSONObject(jsonStr)
                val collegeArray = json.optJSONArray("topRecommendedColleges")
                val collegesList = mutableListOf<GmcAiCollegeRecommendation>()
                if (collegeArray != null) {
                    for (i in 0 until collegeArray.length()) {
                        val cObj = collegeArray.optJSONObject(i) ?: continue
                        collegesList.add(
                            GmcAiCollegeRecommendation(
                                collegeName = cObj.optString("collegeName", "GMC"),
                                state = cObj.optString("state", preferredState),
                                quota = cObj.optString("quota", quota),
                                safetyLevel = cObj.optString("safetyLevel", "Target"),
                                estimatedClosingScore = cObj.optString("estimatedClosingScore", "650+"),
                                reason = cObj.optString("reason", "")
                            )
                        )
                    }
                }

                GmcAiAnalysisResult(
                    expectedAirRankRange = json.optString("expectedAirRankRange", "AIR 5,000 - 8,000"),
                    admissionChancesSummary = json.optString("admissionChancesSummary", "Analysis completed successfully."),
                    roundShiftForecast = json.optString("roundShiftForecast", "Round trends analyzed based on latest counseling data."),
                    topRecommendedColleges = collegesList,
                    strategicCounselingAdvice = json.optString("strategicCounselingAdvice", ""),
                    rawAnalysisMarkdown = json.optString("rawAnalysisMarkdown", text),
                    syncTimestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                GmcAiAnalysisResult(
                    expectedAirRankRange = "AIR ~ ${(720 - userScore).coerceAtLeast(1) * 120}",
                    admissionChancesSummary = "Live counseling analysis completed.",
                    rawAnalysisMarkdown = text,
                    syncTimestamp = System.currentTimeMillis()
                )
            }

            Result.success(parsedResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Interactive Medical College Cutoff & Counselling Assistant Q&A
     * Answers candidate queries directly in conversational Hinglish or English with precise cutoff data,
     * round-by-round strategy, state quota vs AIQ rules, bond policies, and college rankings.
     */
    suspend fun askGmcCutoffQuestion(
        context: Context,
        userQuestion: String,
        userScore: Int,
        category: String,
        quota: String,
        preferredState: String,
        collegeContext: String? = null,
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val prompt = """
            You are India's premier MCC & State Quota Medical Counseling Expert and NEET UG Admission Consultant.
            Answer the NEET student's medical college and cutoff question with precise facts, historical benchmarks (2021, 2022, 2023, 2024, 2025), and 2026 realistic projections.

            [CANDIDATE PROFILE CONTEXT]
            - Current Score / Target Score: $userScore / 720
            - Category: $category (UR / OBC / EWS / SC / ST)
            - Target Quota: $quota (85% State Quota / 15% All India Quota)
            - Domicile / State: $preferredState
            ${if (!collegeContext.isNullOrBlank()) "- Currently Viewing College: $collegeContext" else ""}

            [STUDENT'S QUESTION]
            "$userQuestion"

            [GUIDELINES FOR YOUR RESPONSE]
            1. **Direct Answer & Clarity**: Answer the core question directly in friendly, encouraging Hinglish (or English if prompted) with clean Markdown formatting.
            2. **Cutoff Marks & Rank Reality Check**:
               - Give realistic closing marks and All India Rank (AIR) ranges.
               - Differentiate clearly between Round 1, Round 2, Round 3 (Mop-up), and Stray Vacancy rounds.
            3. **State Quota (85%) vs AIQ (15%) Rules**:
               - Explicitly explain if the student has a better chance via their state quota vs AIQ.
               - Mention state reservation nuances (e.g., UP Sub-categories, Maharashtra 70:30 removal, Rajasthan high cutoff, South low fees).
            4. **Bond, Fees & Clinical Exposure**:
               - Mention mandatory rural/service bond duration (e.g., 2 yrs in UP, 1 yr in MP, 5 yrs in Assam, 0 in AIIMS/Delhi).
               - Mention annual tuition fee range and patient flow/OPD quality.
            5. **Practical Actionable Tip / Choice Filling Order**:
               - Give 2-3 specific recommendations for their choice filling list.

            Format cleanly with bullet points, bold highlights, and emojis for great mobile readability.
        """.trimIndent()

        callGemini(context, prompt, null, null, customApiKey)
    }

    /**
     * Generates a structured personalized NEET Study Timetable using LAKSHYA AI.
     */
    suspend fun generateAiTimetable(
        context: Context,
        targetHours: Int,
        wakeTime: String,
        sleepTime: String,
        focusSubject: String,
        studentType: String,
        customRequirements: String,
        customApiKey: String? = null
    ): Result<GeneratedTimetablePlan> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                You are India's premier NEET-UG Top Ranker Mentor and Scientific Schedule Architect.
                Create a high-impact, scientifically optimized daily study timetable for a NEET aspirant.

                [ASPIRANT PARAMETERS]
                - Target Daily Study Hours: $targetHours Hours
                - Wake-up Time: $wakeTime
                - Sleep Time: $sleepTime
                - Primary Focus Subject / Weakness: $focusSubject
                - Aspirant Type: $studentType (Dropper Full Day, School Going, Coaching + Self Study, Revision Phase)
                - Custom Instructions / Goals: "$customRequirements"

                [INSTRUCTIONS]
                1. Structure the daily timetable with clear, sequential time blocks from morning wake-up to night bedtime.
                2. Balance Physics (numerical problem solving during high-alert hours), Chemistry (inorganic/organic memorization + physical numericals), Biology (NCERT deep reading + line-by-line MCQ testing), Revision blocks, and scheduled refreshing breaks (meals, power naps, physical walk).
                3. Allocate realistic duration (e.g., 60m to 180m blocks) and appropriate tags (Core Study, MCQ Practice, Active Revision, Break/Meal, Mock Analysis).
                4. Output MUST be ONLY valid, parsable JSON matching this schema:
                {
                  "title": "NEET AIR-100 $targetHours-Hour Power Schedule",
                  "targetHours": $targetHours,
                  "strategyNote": "A 2-3 sentence strategic tip explaining how to get maximum retention and speed with this schedule.",
                  "slots": [
                    {
                      "startTime": "06:00 AM",
                      "endTime": "07:30 AM",
                      "subject": "Biology",
                      "title": "NCERT Biology Deep Line-by-Line Reading",
                      "description": "High-retention reading for Botany/Zoology chapters with margin notes",
                      "tag": "Core Study",
                      "durationMinutes": 90
                    },
                    {
                      "startTime": "07:30 AM",
                      "endTime": "08:00 AM",
                      "subject": "Break",
                      "title": "Healthy Breakfast & Morning Freshness",
                      "description": "Nutritious meal, hydration, and mental priming",
                      "tag": "Break/Meal",
                      "durationMinutes": 30
                    }
                  ]
                }
                
                IMPORTANT: Return ONLY the JSON object. Do not include markdown preamble outside the code block.
            """.trimIndent()

            val geminiResult = callGemini(context, prompt, null, null, customApiKey)
            if (geminiResult.isFailure) {
                return@withContext Result.failure(geminiResult.exceptionOrNull() ?: Exception("Gemini generation failed"))
            }

            val text = geminiResult.getOrNull() ?: ""
            val jsonStr = extractJsonFromMarkdown(text)

            val parsedPlan = try {
                val json = JSONObject(jsonStr)
                val title = json.optString("title", "NEET $targetHours-Hour Mastery Schedule")
                val totalH = json.optDouble("targetHours", targetHours.toDouble()).toFloat()
                val strategy = json.optString("strategyNote", "Follow consistent daily cycles with active recall and timed question solving.")
                val slotsArray = json.optJSONArray("slots")
                val slotsList = mutableListOf<TimetableSlot>()

                if (slotsArray != null) {
                    for (i in 0 until slotsArray.length()) {
                        val sObj = slotsArray.optJSONObject(i) ?: continue
                        val slotSubject = sObj.optString("subject", "General")
                        val normalizedSubject = when {
                            slotSubject.contains("Phys", ignoreCase = true) -> "Physics"
                            slotSubject.contains("Chem", ignoreCase = true) -> "Chemistry"
                            slotSubject.contains("Bio", ignoreCase = true) -> "Biology"
                            slotSubject.contains("Rev", ignoreCase = true) -> "Revision"
                            slotSubject.contains("Mock", ignoreCase = true) -> "Mock Test"
                            slotSubject.contains("Break", ignoreCase = true) || slotSubject.contains("Meal", ignoreCase = true) || slotSubject.contains("Sleep", ignoreCase = true) -> "Break"
                            else -> slotSubject
                        }

                        slotsList.add(
                            TimetableSlot(
                                id = java.util.UUID.randomUUID().toString(),
                                startTime = sObj.optString("startTime", "06:00 AM"),
                                endTime = sObj.optString("endTime", "07:30 AM"),
                                subject = normalizedSubject,
                                title = sObj.optString("title", "Study Session"),
                                description = sObj.optString("description", ""),
                                tag = sObj.optString("tag", "Core Study"),
                                durationMinutes = sObj.optInt("durationMinutes", 90),
                                isCompleted = false
                            )
                        )
                    }
                }

                if (slotsList.isEmpty()) {
                    getDefaultTimetable(targetHours)
                } else {
                    GeneratedTimetablePlan(
                        title = title,
                        targetHours = totalH,
                        strategyNote = strategy,
                        slots = slotsList
                    )
                }
            } catch (e: Exception) {
                getDefaultTimetable(targetHours)
            }

            Result.success(parsedPlan)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getDefaultTimetable(targetHours: Int = 12): GeneratedTimetablePlan {
        val slots = when (targetHours) {
            14 -> listOf(
                TimetableSlot(startTime = "05:30 AM", endTime = "06:00 AM", subject = "Break", title = "Wake Up, Hydration & Light Exercise", description = "Mind activation and morning readiness", tag = "Break/Meal", durationMinutes = 30),
                TimetableSlot(startTime = "06:00 AM", endTime = "08:30 AM", subject = "Biology", title = "NCERT Biology Intensive Memorization", description = "High yield NCERT text reading & active recall", tag = "Core Study", durationMinutes = 150),
                TimetableSlot(startTime = "08:30 AM", endTime = "09:00 AM", subject = "Break", title = "Nutritious Breakfast & Rest", description = "Refuel body & brain", tag = "Break/Meal", durationMinutes = 30),
                TimetableSlot(startTime = "09:00 AM", endTime = "12:00 PM", subject = "Physics", title = "Physics Problem Solving & HCV/PYQ Drills", description = "Mechanics/Electrodynamics numericals (60+ Qs)", tag = "MCQ Practice", durationMinutes = 180),
                TimetableSlot(startTime = "12:00 PM", endTime = "01:00 PM", subject = "Break", title = "Lunch & 20-min Power Nap", description = "Energy restoration", tag = "Break/Meal", durationMinutes = 60),
                TimetableSlot(startTime = "01:00 PM", endTime = "04:00 PM", subject = "Chemistry", title = "Organic & Physical Chemistry Core Mastery", description = "Reaction mechanisms, named reactions & numerical practice", tag = "Core Study", durationMinutes = 180),
                TimetableSlot(startTime = "04:00 PM", endTime = "04:30 PM", subject = "Break", title = "Evening Walk, Tea & Brain Reset", description = "Mental recovery and relaxation", tag = "Break/Meal", durationMinutes = 30),
                TimetableSlot(startTime = "04:30 PM", endTime = "07:30 PM", subject = "Chemistry", title = "Inorganic NCERT + Timed MCQ Test", description = "Periodic trends, p-block, coordination compounds", tag = "MCQ Practice", durationMinutes = 180),
                TimetableSlot(startTime = "07:30 PM", endTime = "08:15 PM", subject = "Break", title = "Dinner & Family Time", description = "Healthy dinner", tag = "Break/Meal", durationMinutes = 45),
                TimetableSlot(startTime = "08:15 PM", endTime = "10:45 PM", subject = "Revision", title = "Daily Error Log & Eduniti Checklist Revision", description = "Fix mistakes from day's practice and flashcards", tag = "Active Revision", durationMinutes = 150),
                TimetableSlot(startTime = "10:45 PM", endTime = "11:15 PM", subject = "General", title = "Next Day Target Planning & Journaling", description = "Set daily goals and close notebook", tag = "Core Study", durationMinutes = 30),
                TimetableSlot(startTime = "11:15 PM", endTime = "05:30 AM", subject = "Break", title = "Deep Restorative Sleep (6.25 Hrs)", description = "Memory consolidation", tag = "Break/Meal", durationMinutes = 375)
            )
            10 -> listOf(
                TimetableSlot(startTime = "06:30 AM", endTime = "07:00 AM", subject = "Break", title = "Morning Freshness & Warmup", description = "Prepare for study sessions", tag = "Break/Meal", durationMinutes = 30),
                TimetableSlot(startTime = "07:00 AM", endTime = "09:30 AM", subject = "Physics", title = "Physics Concept Clarification & Formula Drills", description = "Formulas, derivations & textbook theory", tag = "Core Study", durationMinutes = 150),
                TimetableSlot(startTime = "09:30 AM", endTime = "10:00 AM", subject = "Break", title = "Breakfast & Tea", description = "Rest", tag = "Break/Meal", durationMinutes = 30),
                TimetableSlot(startTime = "10:00 AM", endTime = "01:00 PM", subject = "Biology", title = "NCERT Line-by-Line & 100 MCQ Challenge", description = "Botany + Zoology speed practice", tag = "MCQ Practice", durationMinutes = 180),
                TimetableSlot(startTime = "01:00 PM", endTime = "02:00 PM", subject = "Break", title = "Lunch & Relax", description = "Healthy meal", tag = "Break/Meal", durationMinutes = 60),
                TimetableSlot(startTime = "02:00 PM", endTime = "05:00 PM", subject = "Chemistry", title = "Physical & Organic Chemistry Practice", description = "Module questions + NCERT exemplar", tag = "Core Study", durationMinutes = 180),
                TimetableSlot(startTime = "05:00 PM", endTime = "05:30 PM", subject = "Break", title = "Snack & Fresh Air", description = "Step outdoors", tag = "Break/Meal", durationMinutes = 30),
                TimetableSlot(startTime = "05:30 PM", endTime = "07:30 PM", subject = "Physics", title = "Physics 45-Question Timed Speed Test", description = "Exam temperament & time management", tag = "MCQ Practice", durationMinutes = 120),
                TimetableSlot(startTime = "07:30 PM", endTime = "08:30 PM", subject = "Break", title = "Dinner Break", description = "Dinner & unwind", tag = "Break/Meal", durationMinutes = 60),
                TimetableSlot(startTime = "08:30 PM", endTime = "10:30 PM", subject = "Revision", title = "Flashcards & Mistake Notebook Consolidation", description = "Active recall before bed", tag = "Active Revision", durationMinutes = 120),
                TimetableSlot(startTime = "10:30 PM", endTime = "06:30 AM", subject = "Break", title = "8-Hour Peaceful Sleep", description = "Rest well for optimal memory retention", tag = "Break/Meal", durationMinutes = 480)
            )
            else -> listOf(
                TimetableSlot(startTime = "06:00 AM", endTime = "06:30 AM", subject = "Break", title = "Wake Up & Morning Readiness", description = "Hydrate & warm up", tag = "Break/Meal", durationMinutes = 30),
                TimetableSlot(startTime = "06:30 AM", endTime = "09:00 AM", subject = "Biology", title = "High-Yield NCERT Biology Mastery", description = "Complete reading & diagram recall", tag = "Core Study", durationMinutes = 150),
                TimetableSlot(startTime = "09:00 AM", endTime = "09:45 AM", subject = "Break", title = "Breakfast Break", description = "Breakfast & relax", tag = "Break/Meal", durationMinutes = 45),
                TimetableSlot(startTime = "09:45 AM", endTime = "12:45 PM", subject = "Physics", title = "Physics Mechanics/Optics Numerical Problem Solving", description = "Solve 50 standard numericals + PYQs", tag = "MCQ Practice", durationMinutes = 180),
                TimetableSlot(startTime = "12:45 PM", endTime = "02:00 PM", subject = "Break", title = "Lunch & Power Nap", description = "Rest and recharge", tag = "Break/Meal", durationMinutes = 75),
                TimetableSlot(startTime = "02:00 PM", endTime = "05:00 PM", subject = "Chemistry", title = "Chemistry Organic/Inorganic Core Block", description = "Named reactions & NCERT tables", tag = "Core Study", durationMinutes = 180),
                TimetableSlot(startTime = "05:00 PM", endTime = "05:45 PM", subject = "Break", title = "Evening Walk & Healthy Snack", description = "Outdoor light walk", tag = "Break/Meal", durationMinutes = 45),
                TimetableSlot(startTime = "05:45 PM", endTime = "08:15 PM", subject = "Chemistry", title = "Mixed NEET Subject DPP & Rapid MCQ Challenge", description = "100 questions timed solve across 3 subjects", tag = "MCQ Practice", durationMinutes = 150),
                TimetableSlot(startTime = "08:15 PM", endTime = "09:15 PM", subject = "Break", title = "Dinner & Free Time", description = "Dinner with family", tag = "Break/Meal", durationMinutes = 60),
                TimetableSlot(startTime = "09:15 PM", endTime = "11:15 PM", subject = "Revision", title = "Daily Mistake Analysis & Formula Revision", description = "Review mistakes made today and spaced repetition flashcards", tag = "Active Revision", durationMinutes = 120),
                TimetableSlot(startTime = "11:15 PM", endTime = "06:00 AM", subject = "Break", title = "Deep Restorative Sleep (6.75 Hrs)", description = "Full body recovery", tag = "Break/Meal", durationMinutes = 405)
            )
        }

        return GeneratedTimetablePlan(
            title = "NEET $targetHours-Hour AIR Top Ranker Timetable",
            targetHours = targetHours.toFloat(),
            strategyNote = "Follow timed slots with zero multitasking. Switch off social media during core study blocks.",
            slots = slots
        )
    }

    private fun extractJsonFromMarkdown(text: String): String {
        val jsonBlockRegex = """```(?:json)?\s*([\s\S]*?)\s*```""".toRegex()
        val match = jsonBlockRegex.find(text)
        if (match != null && match.groupValues.size > 1) {
            return match.groupValues[1].trim()
        }
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return text.substring(firstBrace, lastBrace + 1).trim()
        }
        return text.trim()
    }

    private fun extractStringOrArray(json: JSONObject, key: String): String {
        val obj = json.opt(key)
        if (obj is org.json.JSONArray) {
            val list = mutableListOf<String>()
            for (i in 0 until obj.length()) {
                list.add(obj.optString(i, ""))
            }
            return list.joinToString("\n")
        }
        return json.optString(key, "")
    }

    suspend fun generateStudyTubeAiNotes(
        context: Context,
        topic: String,
        customApiKey: String? = null
    ): Result<String> {
        val prompt = """
            You are an elite NEET & JEE Ranker and Senior Faculty.
            Generate a high-yield, comprehensive Revision & Formula Cheat-Sheet for the following topic/chapter:
            Topic: "$topic"
            
            Structure the output with clean, organized markdown:
            # 🎯 1. CORE CONCEPT SUMMARY
            (High-yield punchy bullet points covering theoretical foundation)
            
            # ⚡ 2. ESSENTIAL FORMULAS & EQUATIONS
            (Every must-know formula with standard SI units and variable definitions)
            
            # 🧬 3. CRITICAL EXCEPTIONS & TRICKS
            (Reaction mechanisms, sign conventions, shortcuts for speed in 1-minute solving)
            
            # ⚠️ 4. COMMON SILLY MISTAKE TRAPS
            (High-risk mistakes where NTA sets deceptive trap options)
            
            # 📈 5. MOST FREQUENT PYQ THEMES
            (Subtopics asked most frequently in NEET / JEE Main)
            
            Keep it crystal clear, concise, and structured for fast exam revision.
        """.trimIndent()
        return callGeminiWithMultiBytes(context, prompt, emptyList(), customApiKey = customApiKey)
    }

    suspend fun generateStudyTubeAiQuiz(
        context: Context,
        topic: String,
        customApiKey: String? = null
    ): Result<String> {
        val prompt = """
            You are an elite NEET & JEE Senior Examiner.
            Create 5 High-Yield Multiple Choice Practice Questions (MCQs) for the topic: "$topic".
            
            Format each question strictly as follows:
            Q1. [Question text based on recent NTA NEET/JEE Main pattern]
            A) [Option A]
            B) [Option B]
            C) [Option C]
            D) [Option D]
            ✅ Correct Answer: [A/B/C/D]
            💡 Rapid Explanation: [2-line clear step-by-step reasoning or formula used]
            
            (Repeat for Q2 to Q5). Ensure calculations are clean and realistic for 1-minute solving.
        """.trimIndent()
        return callGeminiWithMultiBytes(context, prompt, emptyList(), customApiKey = customApiKey)
    }

    suspend fun verifyStudyPortalWithGemini(
        context: Context,
        rawUrl: String,
        titleHint: String? = null,
        customApiKey: String? = null
    ): Result<StudyWebVerificationResult> = withContext(Dispatchers.IO) {
        val cleanUrl = rawUrl.trim()
        val normalizedUrl = if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            "https://$cleanUrl"
        } else cleanUrl

        val lowerUrl = normalizedUrl.lowercase()

        // 1. Immediate local heuristic blocklist for high-certainty distraction & non-study domains
        // 1. Instant Guard Check for Prohibited Entertainment/Video/Social Media Domains
        val guardCheck = StudyPortalGuard.checkUrl(normalizedUrl)
        if (guardCheck.isBlocked) {
            return@withContext Result.success(
                StudyWebVerificationResult(
                    isStudyWeb = false,
                    reason = guardCheck.blockReason,
                    suggestedTitle = titleHint.takeIf { !it.isNullOrBlank() } ?: guardCheck.platformName,
                    suggestedTag = "Blocked",
                    suggestedEmoji = "🚫"
                )
            )
        }

        // 2. Call Gemini AI to analyze the domain and purpose
        val prompt = """
            You are an elite Academic Gatekeeper AI for a NEET/JEE medical & engineering exam prep app.
            Verify if the given website URL is an authentic study, educational, academic, coaching, test series, college counseling, learning tool, or research resource for students.

            Website URL: "$normalizedUrl"
            User Title / Note: "${titleHint ?: "None"}"

            ALLOWLIST CRITERIA (is_study_web = true):
            - Online coaching / classroom batches (PW / pwthor, Allen, Unacademy, Vedantu, Aakash, Resonance, Motion, Khan Academy, Coursera, edX, NPTEL, Swayam, etc.)
            - Mock test & test series portals (Testbook, Marks App, MathonGo, Embibe, Quizlet, NTA Abhyas, ExamGoal, etc.)
            - Official exam & education boards (nta.ac.in, mcc.nic.in, nmc.org.in, cbse.gov.in, ncert.nic.in, jeeadv.ac.in, state counseling portals)
            - Science, medical, mathematics & reference (Medscape, Amboss, PubMed, NCBI, LibreTexts, WolframAlpha, Desmos, GeoGebra, Wikipedia academic articles)
            - Programming & academic tools for students (LeetCode, GeeksforGeeks, W3Schools, GitHub educational repos)

            STRICT BLOCKLIST CRITERIA (is_study_web = false):
            - YouTube, Dailymotion, Vimeo, Twitch, Rumble, BitChute or any open public video sharing platform
            - Social media, short videos, dating (Instagram, Facebook, TikTok, Twitter/X, Snapchat, Reddit, Threads)
            - Entertainment, movies, anime, music streaming (Netflix, Prime Video, Disney+ Hotstar, JioCinema, SonyLIV, Zee5, Torrent, anime sites)
            - Gaming, sports betting, gambling (Steam, Roblox, Dream11, casino)
            - E-commerce & shopping (Amazon, Flipkart, Myntra)
            - General entertainment blogs, gossip, adult content, distraction websites

            RESPONSE FORMAT:
            Output ONLY a JSON object:
            {
              "is_study_web": true,
              "reason": "1-2 crisp sentences explaining why this portal is approved as an academic resource or why it is blocked.",
              "title": "Concise portal title",
              "tag": "Short badge tag (e.g., 'Live Batches', 'Test Series', 'Lectures', 'Notes', 'Self Study', 'Reference')",
              "icon_emoji": "Single suitable emoji (e.g. ⚡, 📝, 📚, 🔬, 🧬, 🏛️, 🎯, 🌐)"
            }
        """.trimIndent()

        try {
            val geminiResult = callGemini(context, prompt, null, null, customApiKey)
            geminiResult.fold(
                onSuccess = { responseText ->
                    val cleanedJson = responseText
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()

                    val startIdx = cleanedJson.indexOf('{')
                    val endIdx = cleanedJson.lastIndexOf('}')
                    if (startIdx != -1 && endIdx > startIdx) {
                        val jsonSub = cleanedJson.substring(startIdx, endIdx + 1)
                        val json = JSONObject(jsonSub)
                        val isStudy = json.optBoolean("is_study_web", false)
                        val reason = json.optString("reason", if (isStudy) "Verified educational portal" else "Non-educational site")
                        val title = json.optString("title", titleHint.takeIf { !it.isNullOrBlank() } ?: cleanUrl)
                        val tag = json.optString("tag", if (isStudy) "Study Portal" else "Blocked")
                        val emoji = json.optString("icon_emoji", if (isStudy) "🌐" else "🚫")

                        Result.success(
                            StudyWebVerificationResult(
                                isStudyWeb = isStudy,
                                reason = reason,
                                suggestedTitle = title,
                                suggestedTag = tag,
                                suggestedEmoji = emoji
                            )
                        )
                    } else {
                        fallbackEducationalCheck(normalizedUrl, titleHint)
                    }
                },
                onFailure = {
                    fallbackEducationalCheck(normalizedUrl, titleHint)
                }
            )
        } catch (e: Exception) {
            fallbackEducationalCheck(normalizedUrl, titleHint)
        }
    }

    private fun fallbackEducationalCheck(url: String, titleHint: String?): Result<StudyWebVerificationResult> {
        val guardCheck = StudyPortalGuard.checkUrl(url)
        if (guardCheck.isBlocked) {
            return Result.success(
                StudyWebVerificationResult(
                    isStudyWeb = false,
                    reason = guardCheck.blockReason,
                    suggestedTitle = titleHint ?: guardCheck.platformName,
                    suggestedTag = "Blocked",
                    suggestedEmoji = "🚫"
                )
            )
        }

        val lower = url.lowercase()
        val knownStudyKeywords = listOf(
            "edu", "study", "exam", "test", "academy", "allen", "pw.", "pwthor", "physicswallah",
            "unacademy", "khanacademy", "testbook", "vedantu", "embibe", "marks.app", "mathongo",
            "examgoal", "nta.", "ncert", "cbse", "aiims", "medscape", "amboss", "pubmed", "ncbi",
            "scholar", "wikipedia.org", "geeksforgeeks", "leetcode", "coursera", "edx", "swayam",
            "nptel", "chem", "physics", "bio", "math", "learn", "class", "notes"
        )

        val matches = knownStudyKeywords.any { lower.contains(it) }
        return if (matches) {
            Result.success(
                StudyWebVerificationResult(
                    isStudyWeb = true,
                    reason = "✅ Verified: Recognized educational and study domain.",
                    suggestedTitle = titleHint.takeIf { !it.isNullOrBlank() } ?: "Custom Study Portal",
                    suggestedTag = "Study Web",
                    suggestedEmoji = "🌐"
                )
            )
        } else {
            Result.success(
                StudyWebVerificationResult(
                    isStudyWeb = false,
                    reason = "🚫 LAKSHYA AI Shield: This website does not match verified academic/study criteria. Only genuine study portals are permitted.",
                    suggestedTitle = titleHint ?: "Web Portal",
                    suggestedTag = "Blocked",
                    suggestedEmoji = "🚫"
                )
            )
        }
    }

    /**
     * Scans a question image (photo/screenshot/crop) using Gemini Vision,
     * extracts verbatim question text, subject, chapter, topic, core concept,
     * and generates targeted YouTube search queries for video solutions from top NEET educators.
     */
    suspend fun scanQuestionForVideoSolutions(
        context: Context,
        imageBytes: ByteArray,
        contextText: String? = null,
        customApiKey: String? = null,
        targetModel: String? = null
    ): Result<ScannedQuestionVideoSolutionResult> = withContext(Dispatchers.IO) {
        if (imageBytes.isEmpty()) {
            return@withContext Result.failure(Exception("Image is empty. Please capture or select a question photo."))
        }

        val contextInfo = if (!contextText.isNullOrBlank()) {
            """
            
            ADDITIONAL VERIFIED QUESTION STATEMENT & OPTIONS FROM CBT ENGINE:
            "$contextText"
            (Important: Use both the provided question text/options and the visual image/diagram to extract 100% accurate mathematical details, diagrams, and search queries!)
            """.trimIndent()
        } else ""

        val prompt = """
            You are an Elite NTA NEET & JEE AI Tutor, Vision OCR Specialist, and Video Lecture Curator.
            The user took a photo, diagram, or screenshot of a question using StudyTube's Lakshya Lens Question Scanner.$contextInfo

            YOUR MISSION:
            1. Read the image with extreme mathematical, chemical, and biological precision.
            2. Extract:
               - questionText: Complete, verbatim question statement EXACTLY as it appears, including all equations, reactions, numerical data, and option choices (A, B, C, D) without omitting a single word.
               - subject: Strictly one of "Physics", "Chemistry", "Biology", or "Mathematics".
               - chapter: Exact NCERT NEET/JEE chapter name.
               - topic: The specific subtopic or formula involved.
               - coreConcept: A clear 1-2 sentence explanation of the fundamental concept.
               - searchQueries: 6 diverse, HIGH-PRECISION YouTube search queries designed to find the EXACT video solution for this specific question (like Doubtnut, Physics Wallah):
                 1. First 8-12 words of the question statement with exact numbers/values (DO NOT include Q number like "Q.1" or option choices like "(a) (b)").
                 2. First 8-12 words of the question + "Doubtnut" (Doubtnut titles exact questions on YouTube).
                 3. First 8-12 words of the question + "Physics Wallah".
                 4. First 8-12 words of the question + "solution".
                 5. Unique numerical/chemical values from the problem + "question solution".
                 6. NCERT chapter + topic + unique numbers + "PYQ solution".
               - stepByStepExplanation: 3-5 concise bullet points showing the methodical solution steps, formulas, and final answer derivation.
               - formulaOrTrick: Shortcut trick, mnemonic, or high-yield exam tip to solve this type of question in under 60 seconds.

            Respond STRICTLY in valid JSON format:
            {
              "questionText": "...",
              "subject": "Physics",
              "chapter": "...",
              "topic": "...",
              "coreConcept": "...",
              "searchQueries": [
                "...",
                "...",
                "...",
                "...",
                "...",
                "..."
              ],
              "stepByStepExplanation": "...",
              "formulaOrTrick": "..."
            }
        """.trimIndent()

        try {
            val geminiResult = callGeminiWithBytes(context, prompt, imageBytes, "image/jpeg", customApiKey, targetModel)
            if (geminiResult.isFailure) {
                return@withContext Result.failure(geminiResult.exceptionOrNull() ?: Exception("Lakshya Magic analysis failed"))
            }

            val rawText = geminiResult.getOrNull() ?: ""
            val parsed = parseScannedQuestionResult(rawText, fallbackQuery = "")
            Result.success(parsed)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Fallback to analyze question text if image OCR was already completed or user typed query.
     */
    suspend fun analyzeQuestionTextForVideoSolutions(
        context: Context,
        questionText: String,
        customApiKey: String? = null,
        targetModel: String? = null
    ): Result<ScannedQuestionVideoSolutionResult> = withContext(Dispatchers.IO) {
        val prompt = """
            You are an Elite NTA NEET & JEE AI Tutor and Video Lecture Curator.
            The user entered or pasted this exam question into StudyTube:
            "${questionText.take(1500)}"

            Analyze the question and respond STRICTLY in JSON:
            {
              "questionText": "${questionText.replace("\"", "\\\"").take(500)}",
              "subject": "Physics",
              "chapter": "exact chapter name",
              "topic": "specific topic",
              "coreConcept": "1-2 sentence core concept explanation",
              "searchQueries": [
                "Exact question statement verbatim",
                "question snippet question solution",
                "Exact question snippet Doubtnut",
                "subject topic numerical solution",
                "Physics Wallah chapter topic solution"
              ],
              "stepByStepExplanation": "3-5 bullet points solution summary",
              "formulaOrTrick": "key formula or trick"
            }
        """.trimIndent()

        try {
            val geminiResult = callGemini(context, prompt, null, null, customApiKey, targetModel)
            if (geminiResult.isFailure) {
                return@withContext Result.failure(geminiResult.exceptionOrNull() ?: Exception("Analysis failed"))
            }

            val rawText = geminiResult.getOrNull() ?: ""
            val parsed = parseScannedQuestionResult(rawText, fallbackQuery = questionText)
            Result.success(parsed)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun generatePostTestAiStrategy(
        context: Context,
        exam: String,
        testTitle: String,
        score: Int,
        maxScore: Int,
        accuracy: Float,
        timeTakenSeconds: Int,
        weakTopicsWithMarks: List<String>,
        strongTopics: List<String>,
        customUserPrompt: String? = null,
        customApiKey: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val prompt = """
            You are Lakshya AI Doctor — an Elite NTA NEET/JEE AI Clinical Test Diagnostic Specialist.
            The student just finished an AI CBT Mock Test. Here is their performance dossier:
            - Exam: $exam
            - Test: $testTitle
            - Score: $score / $maxScore (Accuracy: ${String.format(java.util.Locale.getDefault(), "%.1f", accuracy)}%)
            - Total Time: ${timeTakenSeconds / 60}m ${timeTakenSeconds % 60}s
            - Negative Marks / Weak Chapters: ${weakTopicsWithMarks.joinToString(", ").ifBlank { "None (Zero Mistakes!)" }}
            - Mastered Chapters: ${strongTopics.joinToString(", ").ifBlank { "None" }}
            ${if (!customUserPrompt.isNullOrBlank()) "\nStudent Doubt/Query: \"$customUserPrompt\"" else ""}

            CRITICAL FORMATTING RULES:
            - Be razor-sharp, ultra-smart, compact and actionable (STRICTLY UNDER 120-150 WORDS TOTAL).
            - DO NOT write lengthy generic paragraphs or essays.
            - Format cleanly like an elite doctor's Clinical Rx Prescription:

            🩺 **Clinical Diagnosis**: 1 punchy sentence identifying the exact leak cause (formula slip vs concept trap vs negative guessing).
            🎯 **High-Priority Weak Spots** (Top 1-2 chapters only):
            • **[Chapter Name]** (PYQ Weightage) — Exact trap & core concept to revise today.
            🚫 **Negative Mark Shield**: 1 concrete rule for when to skip vs attempt to protect marks.
            💊 **Immediate Rx Dose**: 1 specific action (e.g., revise NCERT formula table + solve 15 targeted PYQs in Lakshya).
        """.trimIndent()

        callGemini(context, prompt, null, null, customApiKey)
    }

    /**
     * Resilient 4-tier parser for LLM question extraction responses.
     * Guarantees extracting question text, subject, chapter, and video search queries
     * even with raw LaTeX, invalid JSON escapes, or unformatted text.
     */
    private fun parseScannedQuestionResult(
        rawText: String,
        fallbackQuery: String = ""
    ): ScannedQuestionVideoSolutionResult {
        if (rawText.isBlank()) {
            val defaultQ = fallbackQuery.ifBlank { "Scanned Question" }
            return ScannedQuestionVideoSolutionResult(
                questionText = defaultQ,
                subject = "Physics",
                chapter = "NEET Core Chapter",
                topic = "Question Solution",
                coreConcept = "",
                searchQueries = generateDefaultSearchQueries(defaultQ, "Physics", "Chapter", "Topic"),
                stepByStepExplanation = "",
                formulaOrTrick = ""
            )
        }

        val jsonString = extractJsonFromMarkdown(rawText)

        // Tier 1: Sanitized JSON parsing
        try {
            val sanitized = com.example.util.MathFormatter.sanitizeAndRepairJson(jsonString)
            val json = JSONObject(sanitized)
            val qText = json.optString("questionText", "").ifBlank {
                json.optString("question", "").ifBlank { fallbackQuery }
            }
            val rawSubject = json.optString("subject", "Physics")
            val subject = normalizeSubject(rawSubject, qText)
            val chapter = json.optString("chapter", "NEET Core Chapter")
            val topic = json.optString("topic", chapter)
            val coreConcept = json.optString("coreConcept", "")
            val explanation = extractStringOrArray(json, "stepByStepExplanation")
            val formula = extractStringOrArray(json, "formulaOrTrick")

            val queries = mutableListOf<String>()
            val queriesJson = json.optJSONArray("searchQueries")
            if (queriesJson != null) {
                for (i in 0 until queriesJson.length()) {
                    val q = queriesJson.optString(i, "").trim()
                    if (q.isNotBlank()) queries.add(q)
                }
            }
            if (queries.isEmpty()) {
                queries.addAll(generateDefaultSearchQueries(qText, subject, chapter, topic))
            }

            if (qText.isNotBlank()) {
                return ScannedQuestionVideoSolutionResult(
                    questionText = qText,
                    subject = subject,
                    chapter = chapter,
                    topic = topic,
                    coreConcept = coreConcept,
                    searchQueries = queries,
                    stepByStepExplanation = explanation,
                    formulaOrTrick = formula
                )
            }
        } catch (e: Exception) {
            android.util.Log.w("GeminiChatAssistant", "JSON parse tier 1 failed: ${e.message}")
        }

        // Tier 2: Repaired literal newlines inside JSON strings
        try {
            val repaired = repairJsonStringLiterals(jsonString)
            val sanitized = com.example.util.MathFormatter.sanitizeAndRepairJson(repaired)
            val json = JSONObject(sanitized)
            val qText = json.optString("questionText", "").ifBlank { fallbackQuery }
            val rawSubject = json.optString("subject", "Physics")
            val subject = normalizeSubject(rawSubject, qText)
            val chapter = json.optString("chapter", "NEET Core Chapter")
            val topic = json.optString("topic", chapter)
            val coreConcept = json.optString("coreConcept", "")
            val explanation = extractStringOrArray(json, "stepByStepExplanation")
            val formula = extractStringOrArray(json, "formulaOrTrick")

            val queries = mutableListOf<String>()
            val queriesJson = json.optJSONArray("searchQueries")
            if (queriesJson != null) {
                for (i in 0 until queriesJson.length()) {
                    val q = queriesJson.optString(i, "").trim()
                    if (q.isNotBlank()) queries.add(q)
                }
            }
            if (queries.isEmpty()) {
                queries.addAll(generateDefaultSearchQueries(qText, subject, chapter, topic))
            }

            if (qText.isNotBlank()) {
                return ScannedQuestionVideoSolutionResult(
                    questionText = qText,
                    subject = subject,
                    chapter = chapter,
                    topic = topic,
                    coreConcept = coreConcept,
                    searchQueries = queries,
                    stepByStepExplanation = explanation,
                    formulaOrTrick = formula
                )
            }
        } catch (e: Exception) {
            android.util.Log.w("GeminiChatAssistant", "JSON parse tier 2 failed: ${e.message}")
        }

        // Tier 3: Regex and Token Boundary Extraction
        try {
            val qText = extractFieldByRegex(rawText, "questionText")
                .ifBlank { extractFieldByRegex(rawText, "question") }
                .ifBlank { extractMainQuestionFromText(rawText).ifBlank { fallbackQuery } }

            val rawSubject = extractFieldByRegex(rawText, "subject")
            val subject = normalizeSubject(rawSubject, rawText)
            val chapter = extractFieldByRegex(rawText, "chapter").ifBlank { "NEET Core Chapter" }
            val topic = extractFieldByRegex(rawText, "topic").ifBlank { chapter }
            val coreConcept = extractFieldByRegex(rawText, "coreConcept")
            val explanation = extractFieldByRegex(rawText, "stepByStepExplanation")
                .ifBlank { extractFieldByRegex(rawText, "solution") }
            val formula = extractFieldByRegex(rawText, "formulaOrTrick")

            val extractedQueries = mutableListOf<String>()
            val queriesBlock = Regex(""""searchQueries"\s*:\s*\[([\s\S]*?)\]""").find(rawText)?.groupValues?.get(1)
            if (queriesBlock != null) {
                val stringItems = Regex(""""([^"\\]*(?:\\.[^"\\]*)*)"""").findAll(queriesBlock)
                for (m in stringItems) {
                    val q = m.groupValues[1].replace("\\\"", "\"").replace("\\n", " ").trim()
                    if (q.isNotBlank()) extractedQueries.add(q)
                }
            }
            if (extractedQueries.isEmpty()) {
                extractedQueries.addAll(generateDefaultSearchQueries(qText, subject, chapter, topic))
            }

            val finalQText = if (qText.isNotBlank()) qText else if (rawText.isNotBlank()) rawText.take(500) else fallbackQuery
            return ScannedQuestionVideoSolutionResult(
                questionText = finalQText.ifBlank { "Scanned Question" },
                subject = subject,
                chapter = chapter,
                topic = topic,
                coreConcept = coreConcept,
                searchQueries = extractedQueries,
                stepByStepExplanation = explanation.ifBlank { rawText.take(600) },
                formulaOrTrick = formula
            )
        } catch (e: Exception) {
            android.util.Log.e("GeminiChatAssistant", "Regex fallback parse failed: ${e.message}")
        }

        // Tier 4: Fallback Plain Text Strategy
        val cleanRaw = rawText.replace("```json", "").replace("```", "").trim()
        val finalQuestion = if (cleanRaw.isNotBlank()) cleanRaw.take(500) else fallbackQuery.ifBlank { "Scanned Question" }
        val subject = normalizeSubject("", finalQuestion)
        return ScannedQuestionVideoSolutionResult(
            questionText = finalQuestion,
            subject = subject,
            chapter = "NEET Core Chapter",
            topic = "Question Solution",
            coreConcept = "Scan extracted question successfully.",
            searchQueries = generateDefaultSearchQueries(finalQuestion, subject, "NEET Chapter", "Question Solution"),
            stepByStepExplanation = cleanRaw,
            formulaOrTrick = ""
        )
    }

    private fun normalizeSubject(rawSubject: String, contextText: String): String {
        val s = (rawSubject + " " + contextText).lowercase()
        return when {
            s.contains("chem") || s.contains("reaction") || s.contains("mole") || s.contains("equilibrium") || s.contains("acid") || s.contains("pcl") -> "Chemistry"
            s.contains("bio") || s.contains("botany") || s.contains("zoology") || s.contains("cell") || s.contains("gene") || s.contains("plant") || s.contains("dna") -> "Biology"
            s.contains("math") || s.contains("calculus") || s.contains("integral") || s.contains("matrix") -> "Mathematics"
            else -> "Physics"
        }
    }

    private fun generateDefaultSearchQueries(
        questionText: String,
        subject: String,
        chapter: String,
        topic: String
    ): List<String> {
        val cleanInfo = com.example.util.QuestionVideoSolutionMatcher.extractCleanQuestionInfo(questionText)
        return com.example.util.QuestionVideoSolutionMatcher.generateHighPrecisionVideoQueries(
            info = cleanInfo,
            subject = subject,
            chapter = chapter,
            topic = topic
        )
    }

    private fun extractFieldByRegex(text: String, fieldName: String): String {
        val pattern = Regex(""""$fieldName"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        val match = pattern.find(text)
        if (match != null) {
            return match.groupValues[1]
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .trim()
        }
        val multiPattern = Regex("\"" + fieldName + "\"\\s*:\\s*\"\"\"([\\s\\S]*?)\"\"\"")
        val multiMatch = multiPattern.find(text)
        if (multiMatch != null) {
            return multiMatch.groupValues[1].trim()
        }
        return ""
    }

    private fun extractMainQuestionFromText(text: String): String {
        val clean = text.replace("```json", "").replace("```", "").trim()
        val lines = clean.lines().filter { it.isNotBlank() && !it.startsWith("{") && !it.startsWith("}") && !it.startsWith("\"") }
        val qLines = lines.filter { line ->
            line.contains("?") || line.matches(Regex("""^\s*(?:\d+[\.\)]|Q(?:uestion)?[\.\:\s]).*""", RegexOption.IGNORE_CASE))
        }
        if (qLines.isNotEmpty()) {
            return qLines.joinToString("\n")
        }
        return lines.take(4).joinToString("\n")
    }

    private fun repairJsonStringLiterals(json: String): String {
        val sb = StringBuilder(json.length + 32)
        var inString = false
        var i = 0
        while (i < json.length) {
            val c = json[i]
            if (c == '"' && (i == 0 || json[i - 1] != '\\')) {
                inString = !inString
                sb.append(c)
                i++
                continue
            }
            if (inString) {
                when (c) {
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    else -> sb.append(c)
                }
            } else {
                sb.append(c)
            }
            i++
        }
        return sb.toString()
    }
}

data class TestbookScorecardResult(
    val testTitle: String = "Testbook Mock Test",
    val totalScore: Int = 0,
    val maxScore: Int = 720,
    val physicsScore: Int = 0,
    val physicsMax: Int = 180,
    val chemistryScore: Int = 0,
    val chemistryMax: Int = 180,
    val biologyScore: Int = 0,
    val biologyMax: Int = 360,
    val negativeMarks: Int = 0,
    val accuracyPercentage: Float = 0f,
    val rankOrPercentile: String = "",
    val physicsCorrect: Int = 0,
    val physicsIncorrect: Int = 0,
    val chemistryCorrect: Int = 0,
    val chemistryIncorrect: Int = 0,
    val biologyCorrect: Int = 0,
    val biologyIncorrect: Int = 0,
    val weakTopics: List<String> = emptyList(),
    val mistakesIdentified: List<String> = emptyList(),
    val rawAnalysisMarkdown: String = ""
)

data class GmcAiCollegeRecommendation(
    val collegeName: String = "",
    val state: String = "",
    val quota: String = "",
    val safetyLevel: String = "Target",
    val estimatedClosingScore: String = "",
    val reason: String = ""
)

data class GmcAiAnalysisResult(
    val expectedAirRankRange: String = "",
    val admissionChancesSummary: String = "",
    val roundShiftForecast: String = "",
    val topRecommendedColleges: List<GmcAiCollegeRecommendation> = emptyList(),
    val strategicCounselingAdvice: String = "",
    val rawAnalysisMarkdown: String = "",
    val syncTimestamp: Long = System.currentTimeMillis()
)

data class ExtractedIncorrectQuestion(
    val id: String = java.util.UUID.randomUUID().toString(),
    val subject: String = "Physics",
    val chapter: String = "General",
    val questionText: String = "",
    val options: List<String> = emptyList(),
    val userWrongOption: String = "",
    val correctOption: String = "",
    val mistakeType: String = "Silly Mistake",
    val keyConceptMissed: String = "",
    val stepByStepSolution: String = "",
    val formattedNotebookEntry: String = "",
    var isSelectedForImport: Boolean = true
)

data class TestbookMistakeExtractionResult(
    val totalDetected: Int = 0,
    val questions: List<ExtractedIncorrectQuestion> = emptyList(),
    val summaryReport: String = "",
    val rawText: String = ""
)

data class TimetableSlot(
    val id: String = java.util.UUID.randomUUID().toString(),
    val startTime: String = "06:00 AM",
    val endTime: String = "07:30 AM",
    val subject: String = "Biology",
    val title: String = "Core Study Block",
    val description: String = "",
    val tag: String = "Core Study",
    var isCompleted: Boolean = false,
    val durationMinutes: Int = 90
)

data class GeneratedTimetablePlan(
    val title: String = "NEET Mastery Schedule",
    val targetHours: Float = 12f,
    val strategyNote: String = "",
    val slots: List<TimetableSlot> = emptyList(),
    val generatedDate: String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
)

data class MistakeChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String = "ai", // "user" or "ai"
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class ScannedQuestionVideoSolutionResult(
    val questionText: String = "",
    val subject: String = "Physics",
    val chapter: String = "",
    val topic: String = "",
    val coreConcept: String = "",
    val searchQueries: List<String> = emptyList(),
    val stepByStepExplanation: String = "",
    val formulaOrTrick: String = ""
)




