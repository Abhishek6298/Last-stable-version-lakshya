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

enum class AiProvider(val displayName: String, val badge: String) {
    NATIVE_GEMINI("Native Gemini ✦", "Gemini"),
    OPENROUTER("OpenRouter Hub 🌐", "OpenRouter"),
    GROQ("Groq LPU ⚡", "Groq Ultra-Fast"),
    CLOUDFLARE("Cloudflare Workers AI ☁️", "Cloudflare")
}

data class OpenRouterModel(
    val id: String,
    val name: String,
    val description: String,
    val contextLength: Int,
    val promptPricing: Double,      // cost per 1M tokens or per token
    val completionPricing: Double,
    val isFree: Boolean,
    val isVision: Boolean,
    val topProvider: String,
    val created: Long = 0L
)

object OpenRouterManager {

    private const val TAG = "OpenRouterManager"
    private const val PREFS_NAME = "user_prefs"
    private const val KEY_CACHED_MODELS = "openrouter_cached_models_json"

    private val client = OkHttpClient.Builder()
        .connectionPool(okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(100, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Curated high-performance latest 2025/2026 models for NEET/JEE & OCR
    val fallbackModels = listOf(
        OpenRouterModel(
            id = "google/gemini-2.5-flash",
            name = "Google: Gemini 2.5 Flash",
            description = "High-performance multimodal flagship model on OpenRouter for reasoning, diagrams & doubts.",
            contextLength = 1048576,
            promptPricing = 0.075,
            completionPricing = 0.3,
            isFree = false,
            isVision = true,
            topProvider = "Google"
        ),
        OpenRouterModel(
            id = "google/gemini-2.5-pro",
            name = "Google: Gemini 2.5 Pro",
            description = "Google's flagship deep reasoning frontier model on OpenRouter.",
            contextLength = 1048576,
            promptPricing = 1.25,
            completionPricing = 5.0,
            isFree = false,
            isVision = true,
            topProvider = "Google"
        ),
        OpenRouterModel(
            id = "deepseek/deepseek-r1:free",
            name = "DeepSeek: R1 Reasoning (Free)",
            description = "State-of-the-art chain-of-thought reasoning model for Physics & Chemistry numericals.",
            contextLength = 65536,
            promptPricing = 0.0,
            completionPricing = 0.0,
            isFree = true,
            isVision = false,
            topProvider = "DeepSeek"
        ),
        OpenRouterModel(
            id = "qwen/qwen-2.5-vl-72b-instruct:free",
            name = "Qwen: 2.5 VL 72B Instruct (Free)",
            description = "Flagship vision-language model with top-tier chart, diagram, and NCERT figure analysis.",
            contextLength = 32768,
            promptPricing = 0.0,
            completionPricing = 0.0,
            isFree = true,
            isVision = true,
            topProvider = "Alibaba / Qwen"
        ),
        OpenRouterModel(
            id = "meta-llama/llama-3.3-70b-instruct:free",
            name = "Meta: Llama 3.3 70B Instruct (Free)",
            description = "Latest high-capacity open-weights model for comprehensive chapter and concept explanations.",
            contextLength = 131072,
            promptPricing = 0.0,
            completionPricing = 0.0,
            isFree = true,
            isVision = false,
            topProvider = "Meta"
        ),
        OpenRouterModel(
            id = "anthropic/claude-3.7-sonnet",
            name = "Anthropic: Claude 3.7 Sonnet",
            description = "Elite hybrid reasoning and multimodal engine for highest accuracy test paper generation.",
            contextLength = 200000,
            promptPricing = 3.0,
            completionPricing = 15.0,
            isFree = false,
            isVision = true,
            topProvider = "Anthropic"
        ),
        OpenRouterModel(
            id = "deepseek/deepseek-chat",
            name = "DeepSeek: V3 (671B MoE)",
            description = "Exceptional speed, math ability, and low cost for full syllabus mock tests.",
            contextLength = 65536,
            promptPricing = 0.14,
            completionPricing = 0.28,
            isFree = false,
            isVision = false,
            topProvider = "DeepSeek"
        ),
        OpenRouterModel(
            id = "mistralai/pixtral-large-2411",
            name = "Mistral: Pixtral Large (Vision)",
            description = "124B parameter multimodal specialist model for dense text & high-resolution diagrams.",
            contextLength = 128000,
            promptPricing = 2.0,
            completionPricing = 6.0,
            isFree = false,
            isVision = true,
            topProvider = "Mistral"
        )
    )

    /**
     * Fetches the complete, live list of all models available on OpenRouter (300+ models).
     * Automatically tags free models and vision models, then caches them locally.
     */
    suspend fun fetchLiveModels(context: Context, apiKey: String? = null): Result<List<OpenRouterModel>> = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url("https://openrouter.ai/api/v1/models")
                .get()

            if (!apiKey.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${apiKey.trim()}")
            }

            val response = client.newCall(requestBuilder.build()).await()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful || bodyString.isBlank()) {
                return@withContext Result.failure(Exception("Failed to fetch models from OpenRouter (HTTP ${response.code})"))
            }

            val root = JSONObject(bodyString)
            val dataArray = root.optJSONArray("data") ?: return@withContext Result.failure(Exception("Invalid response format from OpenRouter"))

            val parsedModels = mutableListOf<OpenRouterModel>()

            for (i in 0 until dataArray.length()) {
                val obj = dataArray.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                if (id.isBlank()) continue

                val name = obj.optString("name", id).trim()
                val description = obj.optString("description", "").trim()
                val contextLength = obj.optInt("context_length", 32768)

                val pricingObj = obj.optJSONObject("pricing")
                val promptPrice = pricingObj?.optDouble("prompt", 0.0) ?: 0.0
                val completionPrice = pricingObj?.optDouble("completion", 0.0) ?: 0.0

                val isFree = id.endsWith(":free", ignoreCase = true) || (promptPrice == 0.0 && completionPrice == 0.0)

                // Detect Vision capability
                val architectureObj = obj.optJSONObject("architecture")
                val modality = architectureObj?.optString("modality", "")?.lowercase() ?: ""
                val idLower = id.lowercase()
                val nameLower = name.lowercase()

                val isVision = modality.contains("image") ||
                        idLower.contains("vision") ||
                        idLower.contains("-vl") ||
                        idLower.contains("pixtral") ||
                        idLower.contains("flash") ||
                        idLower.contains("gpt-4o") ||
                        idLower.contains("gemini") ||
                        nameLower.contains("vision") ||
                        nameLower.contains("multimodal")

                val topProvider = obj.optJSONObject("top_provider")?.optString("name", "")
                    ?: id.substringBefore("/", "OpenRouter")

                val created = obj.optLong("created", 0L)

                parsedModels.add(
                    OpenRouterModel(
                        id = id,
                        name = name,
                        description = description,
                        contextLength = contextLength,
                        promptPricing = promptPrice,
                        completionPricing = completionPrice,
                        isFree = isFree,
                        isVision = isVision,
                        topProvider = topProvider.replaceFirstChar { it.uppercase() },
                        created = created
                    )
                )
            }

            if (parsedModels.isNotEmpty()) {
                // Sort: Free models first, then vision models, then alphabetically / by creation
                val sorted = parsedModels.sortedWith(
                    compareByDescending<OpenRouterModel> { it.isFree }
                        .thenByDescending { it.isVision }
                        .thenBy { it.name }
                )

                // Save to cache
                saveModelsToCache(context, bodyString)
                Result.success(sorted)
            } else {
                Result.success(fallbackModels)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching OpenRouter models: ${e.message}", e)
            val cached = getCachedModels(context)
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.success(fallbackModels)
            }
        }
    }

    private fun saveModelsToCache(context: Context, jsonString: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CACHED_MODELS, jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCachedModels(context: Context): List<OpenRouterModel> {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonString = prefs.getString(KEY_CACHED_MODELS, null) ?: return fallbackModels

            val root = JSONObject(jsonString)
            val dataArray = root.optJSONArray("data") ?: return fallbackModels
            val list = mutableListOf<OpenRouterModel>()

            for (i in 0 until dataArray.length()) {
                val obj = dataArray.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                if (id.isBlank()) continue

                val name = obj.optString("name", id).trim()
                val description = obj.optString("description", "").trim()
                val contextLength = obj.optInt("context_length", 32768)

                val pricingObj = obj.optJSONObject("pricing")
                val promptPrice = pricingObj?.optDouble("prompt", 0.0) ?: 0.0
                val completionPrice = pricingObj?.optDouble("completion", 0.0) ?: 0.0

                val isFree = id.endsWith(":free", ignoreCase = true) || (promptPrice == 0.0 && completionPrice == 0.0)

                val architectureObj = obj.optJSONObject("architecture")
                val modality = architectureObj?.optString("modality", "")?.lowercase() ?: ""
                val idLower = id.lowercase()
                val nameLower = name.lowercase()

                val isVision = modality.contains("image") ||
                        idLower.contains("vision") ||
                        idLower.contains("-vl") ||
                        idLower.contains("pixtral") ||
                        idLower.contains("flash") ||
                        idLower.contains("gpt-4o") ||
                        idLower.contains("gemini") ||
                        nameLower.contains("vision") ||
                        nameLower.contains("multimodal")

                val topProvider = obj.optJSONObject("top_provider")?.optString("name", "")
                    ?: id.substringBefore("/", "OpenRouter")

                val created = obj.optLong("created", 0L)

                list.add(
                    OpenRouterModel(
                        id = id,
                        name = name,
                        description = description,
                        contextLength = contextLength,
                        promptPricing = promptPrice,
                        completionPricing = completionPrice,
                        isFree = isFree,
                        isVision = isVision,
                        topProvider = topProvider.replaceFirstChar { it.uppercase() },
                        created = created
                    )
                )
            }

            return if (list.isNotEmpty()) {
                list.sortedWith(
                    compareByDescending<OpenRouterModel> { it.isFree }
                        .thenByDescending { it.isVision }
                        .thenBy { it.name }
                )
            } else fallbackModels
        } catch (e: Exception) {
            return fallbackModels
        }
    }

    /**
     * Verifies the user's OpenRouter API key using their official auth/key endpoint.
     */
    suspend fun verifyApiKey(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmed = apiKey.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(Exception("API Key cannot be blank"))
        }

        try {
            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/auth/key")
                .addHeader("Authorization", "Bearer $trimmed")
                .get()
                .build()

            val response = client.newCall(request).await()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful && body.isNotBlank()) {
                val root = JSONObject(body)
                val data = root.optJSONObject("data")
                val label = data?.optString("label", "Active Key") ?: "Active Key"
                val limit = data?.optDouble("limit")
                val usage = data?.optDouble("usage", 0.0) ?: 0.0
                val isFreeTier = data?.optBoolean("is_free_tier", false) ?: false

                val statusText = buildString {
                    append("✅ Connected: $label")
                    if (isFreeTier) append(" (Free Tier)")
                    if (limit != null && limit > 0) {
                        append(" • Credits: $${String.format(java.util.Locale.US, "%.2f", (limit - usage).coerceAtLeast(0.0))}")
                    }
                }
                Result.success(statusText)
            } else {
                val err = try {
                    JSONObject(body).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (e: Exception) {
                    "HTTP ${response.code}: Invalid OpenRouter key"
                }
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.localizedMessage ?: e.message}"))
        }
    }

    @Volatile
    var configuredMaxTokens: Int = 8192

    fun getMaxTokens(context: Context): Int {
        val tokens = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getInt("openrouter_max_tokens", 8192)
        configuredMaxTokens = tokens
        return tokens
    }

    fun setMaxTokens(context: Context, tokens: Int) {
        val safeTokens = tokens.coerceIn(1024, 65536)
        configuredMaxTokens = safeTokens
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("openrouter_max_tokens", safeTokens)
            .apply()
    }

    /**
     * Calls OpenRouter Chat Completions endpoint (supports multimodal text + image).
     */
    suspend fun callOpenRouterChat(
        apiKey: String,
        model: String,
        prompt: String,
        base64Image: String? = null,
        mimeType: String = "image/jpeg",
        temperature: Double = 0.2,
        context: Context? = null,
        maxTokens: Int? = null,
        systemPrompt: String? = null
    ): String = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) {
            throw Exception("OpenRouter API key is missing. Please configure it in Lakshya Settings.")
        }

        val resolvedModel = when {
            model.contains("/") -> model.trim()
            model.contains("pro", ignoreCase = true) -> "google/gemini-2.5-pro"
            model.contains("flash", ignoreCase = true) -> "google/gemini-2.5-flash"
            model.contains("gemini", ignoreCase = true) -> "google/gemini-2.5-flash"
            else -> model.trim()
        }

        val effectiveMaxTokens = maxTokens ?: context?.let { getMaxTokens(it) } ?: configuredMaxTokens

        val requestBodyJson = JSONObject().apply {
            put("model", resolvedModel)
            put("temperature", temperature)
            put("max_tokens", effectiveMaxTokens)

            // Prioritize highest throughput & lowest latency provider
            val providerObj = JSONObject().apply {
                put("sort", "throughput")
            }
            put("provider", providerObj)

            val messagesArr = JSONArray()

            if (!systemPrompt.isNullOrBlank()) {
                messagesArr.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt.trim())
                })
            }

            val userMsg = JSONObject().apply {
                put("role", "user")

                if (base64Image != null && base64Image.isNotBlank()) {
                    val contentArr = JSONArray().apply {
                        // Text part
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", prompt)
                        })
                        // Image part
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
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer $trimmedKey")
            .addHeader("HTTP-Referer", "https://lakshya.neet.ai")
            .addHeader("X-Title", "Lakshya NEET Preparation")
            .addHeader("Content-Type", "application/json")
            .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).await()
        val responseString = response.body?.string() ?: ""

        if (!response.isSuccessful || responseString.isBlank()) {
            val errorMsg = try {
                val root = JSONObject(responseString)
                val errObj = root.optJSONObject("error")
                errObj?.optString("message") ?: "HTTP ${response.code}"
            } catch (e: Exception) {
                "HTTP ${response.code}"
            }
            throw Exception("OpenRouter API Error ($model): $errorMsg")
        }

        val root = JSONObject(responseString)
        val choices = root.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.optJSONObject("message")
            val content = message?.optString("content", "") ?: ""
            if (content.isNotBlank()) {
                return@withContext content
            }
        }

        throw Exception("Empty response received from OpenRouter model: $model")
    }

    const val KEY_AI_PROVIDER = "ai_provider"
    const val KEY_OPENROUTER_KEY = "openrouter_api_key"
    const val KEY_OPENROUTER_MODEL = "openrouter_selected_model"
    const val DEFAULT_MODEL = "google/gemini-2.5-flash"

    fun getAiProvider(context: Context): AiProvider {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_AI_PROVIDER, AiProvider.NATIVE_GEMINI.name) ?: AiProvider.NATIVE_GEMINI.name
        return try {
            AiProvider.valueOf(name)
        } catch (e: Exception) {
            AiProvider.NATIVE_GEMINI
        }
    }

    fun setAiProvider(context: Context, provider: AiProvider) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_AI_PROVIDER, provider.name).apply()
    }

    fun getOpenRouterApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_OPENROUTER_KEY, "") ?: ""
    }

    fun setOpenRouterApiKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_OPENROUTER_KEY, key.trim()).apply()
    }

    fun getSelectedModel(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_OPENROUTER_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    fun setSelectedModel(context: Context, model: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_OPENROUTER_MODEL, model.trim()).apply()
    }
}
