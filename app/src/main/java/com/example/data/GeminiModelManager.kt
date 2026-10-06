package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeminiModel(
    val id: String,
    val name: String,
    val description: String,
    val contextLength: Int,
    val isVision: Boolean,
    val recommendedFor: String,
    val category: String // "Lite", "Flagship", "Reasoning", "Vision"
)

object GeminiModelManager {

    private const val TAG = "GeminiModelManager"
    private const val PREFS_NAME = "user_prefs"
    const val KEY_SELECTED_GEMINI_MODEL = "selected_gemini_model"
    private const val KEY_CACHED_GEMINI_MODELS = "gemini_cached_models_json"

    const val DEFAULT_MODEL = "gemini-3.8-flash"

    private val client = OkHttpClient.Builder()
        .connectionPool(okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun isDeprecatedModel(modelId: String): Boolean {
        val clean = modelId.trim().lowercase()
        return clean.contains("2.5") || clean.contains("2.0") || clean.contains("1.5") || clean == "gemini-pro"
    }

    val popularGeminiModels = listOf(
        GeminiModel(
            id = "gemini-3.8-flash",
            name = "Gemini 3.8 Flash",
            description = "Google's latest flagship model with cutting-edge reasoning speed, superior token throughput, and full multimodal vision for NEET & JEE.",
            contextLength = 1048576,
            isVision = true,
            recommendedFor = "🌟 Flagship NEET/JEE Theory & Problem Solving (Recommended)",
            category = "Flagship"
        ),
        GeminiModel(
            id = "gemini-3.5-flash",
            name = "Gemini 3.5 Flash",
            description = "Fast, highly efficient core Flash model balanced for quick Q&A, NCERT syllabus revisions, and doubt resolution.",
            contextLength = 1048576,
            isVision = true,
            recommendedFor = "⚡ High-Speed Daily Doubt Clarification",
            category = "Flagship"
        ),
        GeminiModel(
            id = "gemini-3.1-pro-preview",
            name = "Gemini 3.1 Pro",
            description = "Deep reasoning model for complex multi-step physics derivations, chemical reaction mechanisms, and tough JEE Advanced numericals.",
            contextLength = 2097152,
            isVision = true,
            recommendedFor = "🧠 High Academic Rigor & Deep Concept Derivations",
            category = "Reasoning"
        ),
        GeminiModel(
            id = "gemini-3.5-flash-lite",
            name = "Gemini 3.5 Flash Lite",
            description = "High-speed, optimized 3.5 Flash Lite engine with ultra-low latency, zero queue delays, and high quota throughput.",
            contextLength = 524288,
            isVision = true,
            recommendedFor = "⚡ Ultra-Fast DPPs, Quick Hints & High-Speed Solving",
            category = "Lite"
        ),
        GeminiModel(
            id = "gemini-3.1-flash-lite-preview",
            name = "Gemini 3.1 Flash Lite",
            description = "High-speed, optimized Flash Lite engine with ultra-low latency, zero queue delays, and high quota throughput.",
            contextLength = 524288,
            isVision = true,
            recommendedFor = "⚡ Rapid DPPs, Quick Hints & High-Volume Q&A",
            category = "Lite"
        ),
        GeminiModel(
            id = "gemini-flash-latest",
            name = "Gemini Flash (Latest)",
            description = "Dynamic pointer always referencing Google's latest stable Flash production release.",
            contextLength = 1048576,
            isVision = true,
            recommendedFor = "🔄 Always Latest Auto-Updated Engine",
            category = "Flagship"
        ),
        GeminiModel(
            id = "gemini-3.7-flash",
            name = "Gemini 3.7 Flash",
            description = "Google multimodal Flash model with hybrid reasoning and high benchmark scores.",
            contextLength = 1048576,
            isVision = true,
            recommendedFor = "🌟 Hybrid Reasoning & Test Analysis",
            category = "Flagship"
        )
    )

    fun getSelectedModel(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_SELECTED_GEMINI_MODEL, null)?.trim()
        if (!saved.isNullOrBlank()) {
            return saved
        }
        return DEFAULT_MODEL
    }

    /**
     * Strictly uses the exact user-selected model without altering or overriding it.
     */
    fun getEffectiveModel(context: Context): String {
        val selected = getSelectedModel(context)
        return resolveEffectiveEndpointModel(selected)
    }

    fun resolveEffectiveEndpointModel(modelId: String): String {
        val clean = modelId.trim().removePrefix("models/").trim()
        if (clean.isBlank()) return DEFAULT_MODEL
        val lower = clean.lowercase()

        when (lower) {
            "gemini flash" -> return "gemini-flash-latest"
            "gemini lite" -> return "gemini-3.1-flash-lite-preview"
            "gemini pro" -> return "gemini-3.1-pro-preview"
            "gemini-3.8" -> return "gemini-3.8-flash"
        }

        if (isDeprecatedModel(clean)) {
            return if (lower.contains("pro")) {
                "gemini-3.1-pro-preview"
            } else {
                "gemini-3.8-flash"
            }
        }

        return clean
    }

    fun setSelectedModel(context: Context, modelId: String) {
        val trimmed = modelId.trim()
        if (trimmed.isNotBlank()) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_SELECTED_GEMINI_MODEL, trimmed)
                .apply()
        }
    }

    fun getCachedModels(context: Context): List<GeminiModel> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CACHED_GEMINI_MODELS, null)
        if (!json.isNullOrBlank()) {
            try {
                val arr = JSONArray(json)
                val list = mutableListOf<GeminiModel>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.getString("id")
                    if (isDeprecatedModel(id)) continue
                    list.add(
                        GeminiModel(
                            id = id,
                            name = obj.getString("name"),
                            description = obj.optString("description", ""),
                            contextLength = obj.optInt("contextLength", 1048576),
                            isVision = obj.optBoolean("isVision", true),
                            recommendedFor = obj.optString("recommendedFor", ""),
                            category = obj.optString("category", "Flagship")
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return popularGeminiModels
    }

    suspend fun fetchLiveModels(context: Context, apiKey: String): Result<List<GeminiModel>> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim().ifBlank {
            GeminiChatAssistant.getApiKey(context)
        }
        if (trimmedKey.isBlank()) {
            return@withContext Result.failure(Exception("Gemini API key is required to fetch live models from Google server."))
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$trimmedKey"
            val request = Request.Builder()
                .url(url)
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
                return@withContext Result.failure(Exception("Google server returned: $errorMsg"))
            }

            val root = JSONObject(responseString)
            val modelsArr = root.optJSONArray("models") ?: return@withContext Result.success(popularGeminiModels)

            val knownMap = popularGeminiModels.associateBy { it.id }
            val parsedList = mutableListOf<GeminiModel>()

            for (i in 0 until modelsArr.length()) {
                val item = modelsArr.getJSONObject(i)
                val rawName = item.optString("name", "") // e.g., "models/gemini-1.5-flash"
                val id = rawName.removePrefix("models/").trim()
                if (id.isBlank() || id.contains("embedding") || id.contains("aqa") || id.contains("imagen") || isDeprecatedModel(id)) {
                    continue
                }

                val displayName = item.optString("displayName", id)
                val desc = item.optString("description", "")
                val inputTokenLimit = item.optInt("inputTokenLimit", 1048576)
                val supportedMethods = item.optJSONArray("supportedGenerationMethods")
                val isContentGen = supportedMethods != null && (0 until supportedMethods.length()).any { idx ->
                    supportedMethods.optString(idx) == "generateContent"
                }
                if (!isContentGen && supportedMethods != null && supportedMethods.length() > 0) continue

                val idLower = id.lowercase()
                val isLite = idLower.contains("lite") || idLower.contains("8b")
                val isReasoning = idLower.contains("pro") || idLower.contains("thinking")
                val category = when {
                    isLite -> "Lite"
                    isReasoning -> "Reasoning"
                    idLower.contains("flash") -> "Flagship"
                    else -> "Vision"
                }

                val known = knownMap[id]
                val name = known?.name ?: displayName.ifBlank { formatModelName(id) }
                val fullDesc = known?.description ?: desc.ifBlank { "Official Google Gemini model (${inputTokenLimit / 1000}k context)" }
                val rec = known?.recommendedFor ?: if (isLite) "⚡ Fast & High Quota" else "🌟 Google Gemini Multimodal"

                parsedList.add(
                    GeminiModel(
                        id = id,
                        name = name,
                        description = fullDesc,
                        contextLength = inputTokenLimit,
                        isVision = true,
                        recommendedFor = rec,
                        category = category
                    )
                )
            }

            // Merge with popular presets if any user-requested models are not yet in public list
            for (p in popularGeminiModels) {
                if (parsedList.none { it.id == p.id }) {
                    parsedList.add(p)
                }
            }

            val sortedList = parsedList.sortedWith(
                compareByDescending<GeminiModel> { it.id.contains("3.8") }
                    .thenByDescending { it.id.contains("3.5") }
                    .thenByDescending { it.id.contains("3.1") }
                    .thenByDescending { it.id.contains("3.7") }
                    .thenByDescending { it.id.contains("latest") }
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
                        put("category", m.category)
                    }
                    cacheArr.put(obj)
                }
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_CACHED_GEMINI_MODELS, cacheArr.toString())
                    .apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Result.success(if (sortedList.isNotEmpty()) sortedList else popularGeminiModels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatModelName(id: String): String {
        return id.split("-", "_")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
