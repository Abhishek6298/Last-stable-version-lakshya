package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GroqModel(
    val id: String,
    val name: String,
    val description: String,
    val contextLength: Int,
    val isVision: Boolean,
    val recommendedFor: String
)

object GroqManager {

    private const val TAG = "GroqManager"
    private const val PREFS_NAME = "user_prefs"
    const val KEY_GROQ_KEY = "groq_api_key"
    const val KEY_GROQ_MODEL = "groq_selected_model"
    private const val KEY_CACHED_GROQ_MODELS = "groq_cached_models_json"

    const val DEFAULT_TEXT_MODEL = "llama-3.3-70b-versatile"
    const val DEFAULT_VISION_MODEL = "llama-3.2-11b-vision-preview"

    private val client = OkHttpClient.Builder()
        .connectionPool(okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val popularGroqModels = listOf(
        GroqModel(
            id = "qwen/qwen3.8-27b",
            name = "Qwen 3.8 27B Vision",
            description = "Alibaba Qwen multimodal vision architecture on Groq LPU hardware with high context.",
            contextLength = 131072,
            isVision = true,
            recommendedFor = "📐 Multimodal Vision & OCR"
        ),
        GroqModel(
            id = "llama-3.2-90b-vision-preview",
            name = "Llama 3.2 90B Vision",
            description = "Flagship high-parameter vision engine with top visual reasoning for complex paper layouts.",
            contextLength = 128000,
            isVision = true,
            recommendedFor = "🔬 High-Accuracy Vision & OCR"
        ),
        GroqModel(
            id = "llama-3.2-11b-vision-preview",
            name = "Llama 3.2 11B Vision",
            description = "High-speed multimodal vision model for diagram solving, test screenshots & OCR.",
            contextLength = 128000,
            isVision = true,
            recommendedFor = "🖼️ Vision, Diagrams & OCR"
        ),
        GroqModel(
            id = "deepseek-r1-distill-llama-70b",
            name = "DeepSeek R1 Distill 70B",
            description = "State-of-the-art chain-of-thought reasoning for tough Physics numericals & Chemistry mechanisms.",
            contextLength = 128000,
            isVision = false,
            recommendedFor = "🧠 Deep Reasoning & Numericals"
        ),
        GroqModel(
            id = "llama-3.3-70b-versatile",
            name = "Llama 3.3 70B Versatile",
            description = "Meta's flagship 70B model with top benchmark scores for NEET & JEE Physics/Chemistry.",
            contextLength = 128000,
            isVision = false,
            recommendedFor = "⚡ Fast Theory & PYQ Solving"
        ),
        GroqModel(
            id = "llama-3.1-70b-versatile",
            name = "Llama 3.1 70B Versatile",
            description = "High-capacity 70B model for in-depth explanations and complete revision notes.",
            contextLength = 128000,
            isVision = false,
            recommendedFor = "📚 In-Depth Notes & Revision"
        ),
        GroqModel(
            id = "llama-3.1-8b-instant",
            name = "Llama 3.1 8B Instant",
            description = "Ultra-fast low-latency LPU model for immediate flashcards, quick doubt resolution & hints.",
            contextLength = 128000,
            isVision = false,
            recommendedFor = "🚀 Instant Quick Answers (800 tps)"
        ),
        GroqModel(
            id = "gemma2-9b-it",
            name = "Google Gemma 2 9B",
            description = "Google's lightweight, high-performance model for crisp doubt clarification.",
            contextLength = 8192,
            isVision = false,
            recommendedFor = "💡 Concept Clarification"
        ),
        GroqModel(
            id = "mixtral-8x7b-32768",
            name = "Mixtral 8x7B (32k)",
            description = "Mistral AI mixture-of-experts model on LPU speed with 32k context window.",
            contextLength = 32768,
            isVision = false,
            recommendedFor = "⚡ MoE High Speed Solving"
        )
    )

    fun getGroqApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return (prefs.getString(KEY_GROQ_KEY, "") ?: "").trim()
    }

    fun setGroqApiKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_GROQ_KEY, key.trim()).apply()
    }

    fun getSelectedModel(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_GROQ_MODEL, DEFAULT_TEXT_MODEL) ?: DEFAULT_TEXT_MODEL
    }

    fun setSelectedModel(context: Context, model: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_GROQ_MODEL, model.trim()).apply()
    }

    fun getCachedModels(context: Context): List<GroqModel> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedJson = prefs.getString(KEY_CACHED_GROQ_MODELS, null)
        if (!cachedJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(cachedJson)
                val list = mutableListOf<GroqModel>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        GroqModel(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            description = obj.optString("description", ""),
                            contextLength = obj.optInt("contextLength", 128000),
                            isVision = obj.optBoolean("isVision", false),
                            recommendedFor = obj.optString("recommendedFor", "")
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return popularGroqModels
    }

    suspend fun fetchLiveModels(context: Context, apiKey: String): Result<List<GroqModel>> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim().ifBlank { getGroqApiKey(context) }
        if (trimmedKey.isBlank()) {
            return@withContext Result.failure(Exception("Groq API key required to fetch live models from Groq server."))
        }

        try {
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/models")
                .addHeader("Authorization", "Bearer $trimmedKey")
                .get()
                .build()

            val response = client.newCall(request).await()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful || responseString.isBlank()) {
                val errorMsg = try {
                    JSONObject(responseString).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}"
                }
                return@withContext Result.failure(Exception("Groq server returned: $errorMsg"))
            }

            val root = JSONObject(responseString)
            val dataArr = root.optJSONArray("data") ?: return@withContext Result.success(popularGroqModels)

            val knownMap = popularGroqModels.associateBy { it.id }
            val parsedList = mutableListOf<GroqModel>()

            for (i in 0 until dataArr.length()) {
                val item = dataArr.getJSONObject(i)
                val id = item.optString("id", "")
                if (id.isBlank()) continue

                val idLower = id.lowercase()
                if (idLower.contains("whisper") || idLower.contains("tts") || idLower.contains("embedding") || idLower.contains("distil-whisper")) {
                    continue
                }

                val active = item.optBoolean("active", true)
                if (!active) continue

                val isVision = idLower.contains("vision") || idLower.contains("vl") || idLower.contains("qwen")
                val contextWindow = item.optInt("context_window", 128000)

                val known = knownMap[id]
                val name = known?.name ?: formatModelName(id)
                val desc = known?.description ?: "Official Groq LPU model (${contextWindow / 1000}k context)"
                val rec = known?.recommendedFor ?: if (isVision) "🖼️ Live Vision & OCR" else "⚡ Fast Groq Inference"

                parsedList.add(
                    GroqModel(
                        id = id,
                        name = name,
                        description = desc,
                        contextLength = contextWindow,
                        isVision = isVision,
                        recommendedFor = rec
                    )
                )
            }

            val sortedList = parsedList.sortedWith(
                compareByDescending<GroqModel> { it.isVision }
                    .thenByDescending { it.id.contains("70b") || it.id.contains("90b") }
                    .thenBy { it.name }
            )

            try {
                val cacheArr = JSONArray()
                for (m in sortedList) {
                    val obj = JSONObject().apply {
                        put("id", m.id)
                        put("name", m.name)
                        put("description", m.description)
                        put("contextLength", m.contextLength)
                        put("isVision", m.isVision)
                        put("recommendedFor", m.recommendedFor)
                    }
                    cacheArr.put(obj)
                }
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_CACHED_GROQ_MODELS, cacheArr.toString())
                    .apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Result.success(if (sortedList.isNotEmpty()) sortedList else popularGroqModels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatModelName(id: String): String {
        return id.split("-", "_", "/")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    suspend fun verifyGroqKey(context: Context, apiKey: String): String = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim().trim('"', '\'', ' ')
        if (trimmed.isBlank()) return@withContext "Not Configured"

        try {
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/models")
                .addHeader("Authorization", "Bearer $trimmed")
                .get()
                .build()

            val response = client.newCall(request).await()
            val str = response.body?.string() ?: ""

            if (response.isSuccessful) {
                // Auto-refresh models in background on successful verification
                try {
                    fetchLiveModels(context, trimmed)
                } catch (_: Exception) {}
                "Active (Groq LPU Ready) ⚡"
            } else {
                val errorMsg = try {
                    JSONObject(str).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}"
                }
                "Invalid Key: $errorMsg"
            }
        } catch (e: Exception) {
            "Connection Error: ${e.localizedMessage ?: "Failed to connect to Groq"}"
        }
    }

    suspend fun callGroqChat(
        apiKey: String,
        model: String,
        prompt: String,
        base64Image: String? = null,
        mimeType: String = "image/jpeg",
        temperature: Double = 0.3,
        maxTokens: Int = 4096,
        systemPrompt: String? = null
    ): String = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            throw Exception("Groq API key is missing. Please enter your Groq API key (starts with 'gsk_') in Settings.")
        }

        // Auto-select vision model if an image is provided and current model is text-only
        val hasImage = !base64Image.isNullOrBlank()
        val resolvedModel = if (hasImage) {
            val isCurrentVision = model.contains("vision", ignoreCase = true) || model.contains("qwen", ignoreCase = true)
            if (isCurrentVision) model.trim() else DEFAULT_VISION_MODEL
        } else {
            model.trim().ifBlank { DEFAULT_TEXT_MODEL }
        }

        val requestBodyJson = JSONObject().apply {
            put("model", resolvedModel)
            put("temperature", temperature)
            put("max_tokens", maxTokens)

            val messagesArr = JSONArray()

            // Optional System message (crucial for JSON formatting and role instructions)
            if (!systemPrompt.isNullOrBlank()) {
                messagesArr.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt.trim())
                })
            }

            val userMsg = JSONObject().apply {
                put("role", "user")

                if (hasImage) {
                    val contentArr = JSONArray().apply {
                        // Text prompt
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", prompt)
                        })
                        // Image URL
                        put(JSONObject().apply {
                            put("type", "image_url")
                            val imgUrlObj = JSONObject().apply {
                                put("url", "data:$mimeType;base64,$base64Image")
                            }
                            put("image_url", imgUrlObj)
                        })
                    }
                    put("content", contentArr)
                } else {
                    put("content", prompt)
                }
            }
            messagesArr.put(userMsg)
            put("messages", messagesArr)
        }

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $trimmedKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).await()
        val responseString = response.body?.string() ?: ""

        if (!response.isSuccessful || responseString.isBlank()) {
            val errorMsg = try {
                val root = JSONObject(responseString)
                val err = root.optJSONObject("error")
                err?.optString("message") ?: "Groq API HTTP ${response.code}"
            } catch (_: Exception) {
                "Groq API HTTP ${response.code}"
            }

            // Adapt parameters for THIS EXACT SAME MODEL if Groq limits max_tokens (e.g. models capped at 4096)
            if (response.code == 400 && (errorMsg.contains("max_tokens", ignoreCase = true) || errorMsg.contains("max_completion_tokens", ignoreCase = true) || errorMsg.contains("reduce", ignoreCase = true)) && maxTokens > 4096) {
                Log.w(TAG, "Model '$resolvedModel' requested reduced max_tokens ($errorMsg). Retrying with max_tokens = 4096 on same model.")
                return@withContext callGroqChat(
                    apiKey = apiKey,
                    model = resolvedModel,
                    prompt = prompt,
                    base64Image = base64Image,
                    mimeType = mimeType,
                    temperature = temperature,
                    maxTokens = 4096,
                    systemPrompt = systemPrompt
                )
            }

            // Adapt if this exact model does not support a separate system message (e.g. some reasoning models)
            if (response.code == 400 && (errorMsg.contains("system", ignoreCase = true) || errorMsg.contains("developer", ignoreCase = true)) && !systemPrompt.isNullOrBlank()) {
                Log.w(TAG, "Model '$resolvedModel' does not support system role. Merging into prompt on same model.")
                val mergedPrompt = "$systemPrompt\n\n$prompt"
                return@withContext callGroqChat(
                    apiKey = apiKey,
                    model = resolvedModel,
                    prompt = mergedPrompt,
                    base64Image = base64Image,
                    mimeType = mimeType,
                    temperature = temperature,
                    maxTokens = maxTokens,
                    systemPrompt = null
                )
            }

            if (response.code == 429) {
                throw Exception("Groq Rate Limit Exceeded (429) for '$resolvedModel': $errorMsg. Please retry in a few seconds.")
            } else if (response.code == 401) {
                throw Exception("Groq Authentication Error (401): Invalid API Key. Please verify your Groq key (gsk_...) in Settings.")
            } else {
                throw Exception("Groq API Error ($resolvedModel): $errorMsg")
            }
        }

        val jsonResponse = JSONObject(responseString)
        val choices = jsonResponse.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val messageObj = choices.getJSONObject(0).optJSONObject("message")
            val content = messageObj?.optString("content", "") ?: ""
            if (content.isNotBlank()) {
                return@withContext content
            }
        }

        throw Exception("Empty response received from Groq model: $resolvedModel")
    }

    private suspend fun okhttp3.Call.await(): okhttp3.Response = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : okhttp3.Callback {
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                continuation.resumeWith(Result.success(response))
            }
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                continuation.resumeWith(Result.failure(e))
            }
        })
    }
}
