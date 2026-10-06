package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeneratedCard(
    val front: String,
    val back: String
)

object GeminiFlashcardGenerator {

    private val client = OkHttpClient.Builder()
        .connectionPool(okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES))
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
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
     * Powerful AI Flashcard Generator for any NEET Topic or Chapter
     */
    suspend fun generateFlashcardsFromTopic(
        context: Context,
        topicName: String,
        subjectCategory: String = "Physics",
        cardStyle: String = "Comprehensive Mixed",
        cardCount: Int = 10,
        customApiKey: String? = null
    ): Result<List<GeneratedCard>> = withContext(Dispatchers.IO) {
        try {
            val keys = getApiKeys(context, customApiKey)
            if (keys.isEmpty()) {
                return@withContext Result.failure(Exception("Lakshya AI Key missing. Please enter your Lakshya AI Key in Settings."))
            }

            val promptText = """
                You are an elite NEET/JEE AI master tutor and top ranker mentor specializing in active-recall flashcard curation.
                
                Subject: $subjectCategory
                Chapter / Topic: "$topicName"
                Flashcard Style Focus: $cardStyle
                Target Number of Cards: $cardCount
                
                Guidelines:
                1. Craft high-yield, exam-oriented flashcards tailored for NEET UG & JEE Main top scoring.
                2. Front: Include clear, challenging recall questions, formula identifiers, tricky NCERT exceptions, assertion-reason setups, or reaction mechanisms.
                3. Back: Provide crisp, memorable explanations, exact NCERT line references, mnemonics/shortcuts, and common traps to avoid.
                4. Return ONLY a valid JSON array of objects. Do NOT include markdown ticks, intros, or commentary.
                
                JSON Format:
                [
                  {
                    "front": "Front question, formula name, or concept challenge",
                    "back": "Concise high-yield explanation, key formula with LaTeX, or mnemonic tip"
                  }
                ]
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 8192)
                }
                put("generationConfig", generationConfig)
            }

            callGeminiWithFallback(context, keys, requestJson)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates Flashcards from an attached PDF document
     */
    suspend fun generateFlashcardsFromPdf(
        context: Context,
        pdfUri: Uri,
        customApiKey: String? = null,
        subjectCategory: String = "Physics",
        cardCount: Int = 12
    ): Result<List<GeneratedCard>> = withContext(Dispatchers.IO) {
        try {
            val keys = getApiKeys(context, customApiKey)
            if (keys.isEmpty()) {
                return@withContext Result.failure(Exception("Lakshya AI Key missing. Please enter your Lakshya AI Key in Settings."))
            }

            val inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: return@withContext Result.failure(Exception("Could not open selected PDF file."))

            val pdfBytes = inputStream.use { it.readBytes() }
            if (pdfBytes.isEmpty()) {
                return@withContext Result.failure(Exception("Selected PDF file is empty."))
            }

            val pdfBase64 = Base64.encodeToString(pdfBytes, Base64.NO_WRAP)

            val promptText = """
                You are an elite NEET/JEE AI tutor for $subjectCategory exam preparation.
                Analyze the attached PDF document thoroughly and generate $cardCount high-yield active-recall flashcards.
                
                Extract:
                - Core principles, high-yield definitions, and essential formulas.
                - Tricky NCERT exceptions and assertion-reason relationships.
                - Exam question patterns and shortcuts.
                
                Return ONLY a valid JSON array of objects. Do NOT include markdown ticks, intro, or concluding text.
                Format:
                [
                  {
                    "front": "Front question or conceptual challenge",
                    "back": "Detailed answer, formula derivation, or mnemonic"
                  }
                ]
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "application/pdf")
                                    put("data", pdfBase64)
                                })
                            })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    put("temperature", 0.2)
                    put("maxOutputTokens", 8192)
                }
                put("generationConfig", generationConfig)
            }

            callGeminiWithFallback(context, keys, requestJson)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun callGeminiWithFallback(
        context: Context,
        keys: List<String>,
        requestJson: JSONObject
    ): Result<List<GeneratedCard>> {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val providerPref = prefs.getString("ai_provider", AiProvider.NATIVE_GEMINI.name) ?: AiProvider.NATIVE_GEMINI.name
        val isOr = providerPref == AiProvider.OPENROUTER.name
        val orKey = (prefs.getString("openrouter_api_key", "") ?: "").trim()
        val orModel = (prefs.getString("openrouter_selected_model", "google/gemini-2.5-flash") ?: "google/gemini-2.5-flash").trim()

        if (isOr) {
            if (orKey.isBlank()) {
                return Result.failure(Exception("OpenRouter API key is missing. Please configure it in AI Engine settings."))
            }
            return try {
                val contents = requestJson.optJSONArray("contents")
                val parts = contents?.optJSONObject(0)?.optJSONArray("parts")
                val promptText = parts?.optJSONObject(0)?.optString("text") ?: ""
                val rawText = OpenRouterManager.callOpenRouterChat(orKey, orModel, promptText)
                val cleanJson = rawText
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                val jsonArrayStart = cleanJson.indexOf('[')
                val jsonArrayEnd = cleanJson.lastIndexOf(']')
                val jsonToParse = if (jsonArrayStart != -1 && jsonArrayEnd != -1 && jsonArrayEnd > jsonArrayStart) {
                    cleanJson.substring(jsonArrayStart, jsonArrayEnd + 1)
                } else {
                    cleanJson
                }

                val jsonArray = JSONArray(jsonToParse)
                val cardsList = mutableListOf<GeneratedCard>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val front = obj.optString("front", "").trim()
                    val back = obj.optString("back", "").trim()
                    if (front.isNotBlank() && back.isNotBlank()) {
                        cardsList.add(GeneratedCard(front, back))
                    }
                }
                if (cardsList.isNotEmpty()) {
                    Result.success(cardsList)
                } else {
                    Result.failure(Exception("Could not parse flashcards from OpenRouter response"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        if (providerPref == AiProvider.GROQ.name) {
            val groqKey = GroqManager.getGroqApiKey(context)
            val groqModel = GroqManager.getSelectedModel(context)
            if (groqKey.isBlank()) {
                return Result.failure(Exception("Groq API key is missing. Please configure it in Settings."))
            }
            return try {
                val contents = requestJson.optJSONArray("contents")
                val parts = contents?.optJSONObject(0)?.optJSONArray("parts")
                val promptText = parts?.optJSONObject(0)?.optString("text") ?: ""
                val rawText = GroqManager.callGroqChat(groqKey, groqModel, promptText)
                val cleanJson = rawText.replace("```json", "").replace("```", "").trim()
                val jsonArrayStart = cleanJson.indexOf('[')
                val jsonArrayEnd = cleanJson.lastIndexOf(']')
                val jsonToParse = if (jsonArrayStart != -1 && jsonArrayEnd != -1 && jsonArrayEnd > jsonArrayStart) {
                    cleanJson.substring(jsonArrayStart, jsonArrayEnd + 1)
                } else cleanJson

                val jsonArray = JSONArray(jsonToParse)
                val cardsList = mutableListOf<GeneratedCard>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val front = obj.optString("front", "").trim()
                    val back = obj.optString("back", "").trim()
                    if (front.isNotBlank() && back.isNotBlank()) {
                        cardsList.add(GeneratedCard(front, back))
                    }
                }
                if (cardsList.isNotEmpty()) Result.success(cardsList) else Result.failure(Exception("Could not parse flashcards from Groq response"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        if (providerPref == AiProvider.CLOUDFLARE.name) {
            val cfAccountId = (prefs.getString(CloudflareManager.KEY_CF_ACCOUNT_ID, "") ?: "").trim()
            val cfToken = (prefs.getString(CloudflareManager.KEY_CF_API_TOKEN, "") ?: "").trim()
            val cfModel = CloudflareManager.getSelectedModel(context)
            if (cfAccountId.isBlank() || cfToken.isBlank()) {
                return Result.failure(Exception("Cloudflare Account ID or API Token is missing. Please configure it in Settings."))
            }
            return try {
                val contents = requestJson.optJSONArray("contents")
                val parts = contents?.optJSONObject(0)?.optJSONArray("parts")
                val promptText = parts?.optJSONObject(0)?.optString("text") ?: ""
                val rawText = CloudflareManager.callCloudflareChat(
                    accountId = cfAccountId,
                    apiToken = cfToken,
                    model = cfModel,
                    prompt = promptText
                )
                val cleanJson = rawText.replace("```json", "").replace("```", "").trim()
                val jsonArrayStart = cleanJson.indexOf('[')
                val jsonArrayEnd = cleanJson.lastIndexOf(']')
                val jsonToParse = if (jsonArrayStart != -1 && jsonArrayEnd != -1 && jsonArrayEnd > jsonArrayStart) {
                    cleanJson.substring(jsonArrayStart, jsonArrayEnd + 1)
                } else cleanJson

                val jsonArray = JSONArray(jsonToParse)
                val cardsList = mutableListOf<GeneratedCard>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val front = obj.optString("front", "").trim()
                    val back = obj.optString("back", "").trim()
                    if (front.isNotBlank() && back.isNotBlank()) {
                        cardsList.add(GeneratedCard(front, back))
                    }
                }
                if (cardsList.isNotEmpty()) Result.success(cardsList) else Result.failure(Exception("Could not parse flashcards from Cloudflare response"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val apiKey = getApiKey(context)
        if (apiKey.isBlank()) {
            return Result.failure(Exception("Gemini API Key missing. Please enter your Gemini API Key in Settings."))
        }

        try {
            val model = GeminiModelManager.getEffectiveModel(context)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).await()
            val responseBodyString = response.body?.string() ?: ""

            if (response.isSuccessful && responseBodyString.isNotBlank()) {
                val responseJson = JSONObject(responseBodyString)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val rawText = candidates.getJSONObject(0)
                        .optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text", "") ?: ""

                    val cleanJson = rawText
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()

                    val jsonArrayStart = cleanJson.indexOf('[')
                    val jsonArrayEnd = cleanJson.lastIndexOf(']')
                    val jsonToParse = if (jsonArrayStart != -1 && jsonArrayEnd != -1 && jsonArrayEnd > jsonArrayStart) {
                        cleanJson.substring(jsonArrayStart, jsonArrayEnd + 1)
                    } else {
                        cleanJson
                    }

                    val jsonArray = JSONArray(jsonToParse)
                    val cardsList = mutableListOf<GeneratedCard>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val front = obj.optString("front", "").trim()
                        val back = obj.optString("back", "").trim()
                        if (front.isNotBlank() && back.isNotBlank()) {
                            cardsList.add(GeneratedCard(front, back))
                        }
                    }

                    if (cardsList.isNotEmpty()) {
                        return Result.success(cardsList)
                    }
                }
            } else {
                val errorMsg = try {
                    JSONObject(responseBodyString).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}"
                }
                return Result.failure(Exception("Gemini Error: $errorMsg"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }

        return Result.failure(Exception("Failed to generate flashcards from Gemini."))
    }
}
