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

data class CloudflareModel(
    val id: String,                     // e.g. "@cf/meta/llama-3.3-70b-instruct-fp8-fast"
    val name: String,                   // Clean formatted name
    val description: String,
    val task: String,                   // "Text Generation", "Image-to-Text", etc.
    val isVision: Boolean,
    val isBeta: Boolean,
    val recommendedFor: String
)

object CloudflareManager {

    private const val TAG = "CloudflareManager"
    private const val PREFS_NAME = "user_prefs"
    const val KEY_CF_ACCOUNT_ID = "cloudflare_account_id"
    const val KEY_CF_API_TOKEN = "cloudflare_api_token"
    const val KEY_CF_MODEL = "cloudflare_selected_model"
    private const val KEY_CACHED_CF_MODELS = "cloudflare_cached_models_json"

    const val DEFAULT_TEXT_MODEL = "@cf/meta/llama-3.3-70b-instruct-fp8-fast"
    const val DEFAULT_REASONING_MODEL = "@cf/deepseek-ai/deepseek-r1-distill-qwen-32b"
    const val DEFAULT_VISION_MODEL = "@cf/meta/llama-3.2-11b-vision-instruct"

    private val client = OkHttpClient.Builder()
        .connectionPool(okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(100, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    val popularCloudflareModels = listOf(
        CloudflareModel(
            id = "@cf/deepseek-ai/deepseek-r1-distill-qwen-32b",
            name = "DeepSeek R1 Distill Qwen 32B",
            description = "State-of-the-art chain-of-thought mathematical reasoning model for tough Physics and Chemistry problems.",
            task = "Text Generation",
            isVision = false,
            isBeta = true,
            recommendedFor = "🧠 Deep Reasoning & Numericals"
        ),
        CloudflareModel(
            id = "@cf/meta/llama-3.3-70b-instruct-fp8-fast",
            name = "Llama 3.3 70B Instruct (Fast FP8)",
            description = "Meta's flagship 70B model running accelerated on Cloudflare edge GPUs for full CBT tests & detailed notes.",
            task = "Text Generation",
            isVision = false,
            isBeta = false,
            recommendedFor = "⚡ Flagship High-Yield Solving"
        ),
        CloudflareModel(
            id = "@cf/meta/llama-3.2-11b-vision-instruct",
            name = "Llama 3.2 11B Vision Instruct",
            description = "Multimodal vision model for scanning diagrams, formulas, circuit schematics, and question paper crops.",
            task = "Image-to-Text",
            isVision = true,
            isBeta = false,
            recommendedFor = "🖼️ Diagrams & Vision OCR"
        ),
        CloudflareModel(
            id = "@cf/qwen/qwen2.5-72b-instruct",
            name = "Qwen 2.5 72B Instruct",
            description = "High-accuracy multilingual model with exceptional performance in science, calculations, and structured derivation.",
            task = "Text Generation",
            isVision = false,
            isBeta = false,
            recommendedFor = "📐 Maths, Formulas & Derivations"
        ),
        CloudflareModel(
            id = "@cf/google/gemma-2-9b-it",
            name = "Google Gemma 2 9B IT",
            description = "Google's lightweight, high-precision architecture designed for rapid doubt answering and NCERT theory revision.",
            task = "Text Generation",
            isVision = false,
            isBeta = false,
            recommendedFor = "✦ Google Gemma Foundation"
        ),
        CloudflareModel(
            id = "@cf/meta/llama-3.1-8b-instruct",
            name = "Llama 3.1 8B Instruct",
            description = "Ultra-fast low-latency edge model for instant flashcards, rapid doubt solving, and formula lookups.",
            task = "Text Generation",
            isVision = false,
            isBeta = false,
            recommendedFor = "🚀 Instant Speed (10k Free Neurons)"
        ),
        CloudflareModel(
            id = "@cf/mistral/mistral-7b-instruct-v0.2",
            name = "Mistral 7B Instruct v0.2",
            description = "High-performing European open model with clean reasoning and concise step-by-step guidance.",
            task = "Text Generation",
            isVision = false,
            isBeta = false,
            recommendedFor = "📚 Rapid Concept Clarification"
        ),
        CloudflareModel(
            id = "@cf/qwen/qwen2.5-coder-32b-instruct",
            name = "Qwen 2.5 Coder 32B",
            description = "High-precision logical structuring model excels at multi-step problem solving and algorithmic analysis.",
            task = "Text Generation",
            isVision = false,
            isBeta = false,
            recommendedFor = "⚙️ Structured Step Logic"
        )
    )

    fun getSelectedModel(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CF_MODEL, DEFAULT_TEXT_MODEL) ?: DEFAULT_TEXT_MODEL
    }

    fun setSelectedModel(context: Context, modelId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CF_MODEL, modelId).apply()
    }

    fun getCachedModels(context: Context): List<CloudflareModel> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_CACHED_CF_MODELS, null) ?: return popularCloudflareModels
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<CloudflareModel>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CloudflareModel(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.optString("description", ""),
                        task = obj.optString("task", "Text Generation"),
                        isVision = obj.optBoolean("isVision", false),
                        isBeta = obj.optBoolean("isBeta", false),
                        recommendedFor = obj.optString("recommendedFor", "⚡ Cloudflare Edge AI")
                    )
                )
            }
            if (list.isNotEmpty()) list else popularCloudflareModels
        } catch (e: Exception) {
            e.printStackTrace()
            popularCloudflareModels
        }
    }

    /**
     * Live Dynamic Sync: Queries Cloudflare's real-time model catalog.
     * Any new models released by Cloudflare in the future automatically sync here!
     */
    suspend fun fetchLiveModels(
        context: Context,
        accountId: String,
        apiToken: String
    ): Result<List<CloudflareModel>> = withContext(Dispatchers.IO) {
        val trimmedAccountId = accountId.trim()
        val trimmedToken = apiToken.trim().trim('"', '\'', ' ')

        if (trimmedAccountId.isBlank() || trimmedToken.isBlank()) {
            return@withContext Result.failure(Exception("Cloudflare Account ID and API Token are required to fetch live models."))
        }

        try {
            val url = "https://api.cloudflare.com/client/v4/accounts/$trimmedAccountId/ai/models/search"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $trimmedToken")
                .get()
                .build()

            val response = client.newCall(request).await()
            val jsonStr = response.body?.string() ?: ""

            if (!response.isSuccessful || jsonStr.isBlank()) {
                val errorMsg = try {
                    val root = JSONObject(jsonStr)
                    val errorsArr = root.optJSONArray("errors")
                    if (errorsArr != null && errorsArr.length() > 0) {
                        errorsArr.getJSONObject(0).optString("message", "HTTP ${response.code}")
                    } else "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}"
                }
                return@withContext Result.failure(Exception("Cloudflare sync failed: $errorMsg"))
            }

            val root = JSONObject(jsonStr)
            val resultArr = root.optJSONArray("result") ?: JSONArray()
            val list = mutableListOf<CloudflareModel>()

            for (i in 0 until resultArr.length()) {
                val item = resultArr.getJSONObject(i)
                val rawName = item.optString("name", "").trim()
                if (rawName.isBlank()) continue

                val taskObj = item.optJSONObject("task")
                val taskName = taskObj?.optString("name", "Text Generation") ?: "Text Generation"
                val desc = item.optString("description", "")

                // Filter to Text Generation, Image-to-Text, Code, or relevant AI models
                val isTextGen = taskName.contains("text", ignoreCase = true) || taskName.contains("chat", ignoreCase = true)
                val isVision = taskName.contains("image", ignoreCase = true) || rawName.contains("vision", ignoreCase = true) || rawName.contains("vl", ignoreCase = true)
                val isTranslation = taskName.contains("translation", ignoreCase = true)

                if (!isTextGen && !isVision && !isTranslation) {
                    // Skip embedding / audio models for chat picker
                    continue
                }

                val propertiesArr = item.optJSONArray("properties")
                var isBeta = false
                if (propertiesArr != null) {
                    for (p in 0 until propertiesArr.length()) {
                        val prop = propertiesArr.optJSONObject(p)
                        if (prop?.optString("property_id") == "beta") {
                            isBeta = prop.optString("value") == "true"
                        }
                    }
                }

                val displayName = formatModelDisplayName(rawName)
                val recommendedFor = when {
                    rawName.contains("r1", ignoreCase = true) || rawName.contains("reason", ignoreCase = true) -> "🧠 Deep Reasoning & Numericals"
                    isVision -> "🖼️ Diagrams & Vision OCR"
                    rawName.contains("70b", ignoreCase = true) || rawName.contains("72b", ignoreCase = true) -> "⚡ Flagship High-Yield Solving"
                    rawName.contains("qwen", ignoreCase = true) -> "📐 Maths & Derivations"
                    rawName.contains("gemma", ignoreCase = true) -> "✦ Google Gemma Foundation"
                    rawName.contains("8b", ignoreCase = true) || rawName.contains("mini", ignoreCase = true) -> "🚀 Instant Speed (10k Free Neurons)"
                    else -> "⚡ Cloudflare Edge AI"
                }

                list.add(
                    CloudflareModel(
                        id = rawName,
                        name = displayName,
                        description = desc.ifBlank { "Cloudflare Workers AI model running on serverless edge GPU." },
                        task = taskName,
                        isVision = isVision,
                        isBeta = isBeta,
                        recommendedFor = recommendedFor
                    )
                )
            }

            // Sort: Reasoning first, then 70B+, then vision, then others
            val sortedList = list.sortedWith(
                compareByDescending<CloudflareModel> { it.id.contains("r1", ignoreCase = true) }
                    .thenByDescending { it.id.contains("70b", ignoreCase = true) || it.id.contains("72b", ignoreCase = true) }
                    .thenByDescending { it.isVision }
                    .thenBy { it.name }
            )

            // Cache models to SharedPreferences
            try {
                val cacheArr = JSONArray()
                for (m in sortedList) {
                    val obj = JSONObject().apply {
                        put("id", m.id)
                        put("name", m.name)
                        put("description", m.description)
                        put("task", m.task)
                        put("isVision", m.isVision)
                        put("isBeta", m.isBeta)
                        put("recommendedFor", m.recommendedFor)
                    }
                    cacheArr.put(obj)
                }
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_CACHED_CF_MODELS, cacheArr.toString())
                    .apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Result.success(if (sortedList.isNotEmpty()) sortedList else popularCloudflareModels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatModelDisplayName(id: String): String {
        // e.g. "@cf/meta/llama-3.3-70b-instruct-fp8-fast" -> "Llama 3.3 70B Instruct Fast"
        val clean = id.removePrefix("@cf/").substringAfterLast("/")
        return clean.split("-", "_")
            .filter { it.isNotBlank() && it.lowercase() != "fp8" }
            .joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }

    suspend fun verifyCloudflareCredentials(
        context: Context,
        accountId: String,
        apiToken: String
    ): String = withContext(Dispatchers.IO) {
        val trimmedAccountId = accountId.trim()
        val trimmedToken = apiToken.trim().trim('"', '\'', ' ')

        if (trimmedAccountId.isBlank()) return@withContext "Account ID Required"
        if (trimmedToken.isBlank()) return@withContext "API Token Required"

        try {
            val url = "https://api.cloudflare.com/client/v4/accounts/$trimmedAccountId/ai/models/search?per_page=1"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $trimmedToken")
                .get()
                .build()

            val response = client.newCall(request).await()
            val str = response.body?.string() ?: ""

            if (response.isSuccessful) {
                // Auto-refresh models in background on successful verification
                try {
                    fetchLiveModels(context, trimmedAccountId, trimmedToken)
                } catch (_: Exception) {}

                // Pre-accept license agreements for Llama 3.2 vision & reasoning models
                try {
                    acceptModelAgreement(trimmedAccountId, trimmedToken, DEFAULT_VISION_MODEL)
                } catch (_: Exception) {}

                "Active (10,000 Free Daily Neurons Ready) ☁️"
            } else {
                val errorMsg = try {
                    val root = JSONObject(str)
                    val errorsArr = root.optJSONArray("errors")
                    if (errorsArr != null && errorsArr.length() > 0) {
                        errorsArr.getJSONObject(0).optString("message", "HTTP ${response.code}")
                    } else "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}"
                }
                "Auth Error: $errorMsg"
            }
        } catch (e: Exception) {
            "Connection Error: ${e.localizedMessage ?: "Failed to connect to Cloudflare"}"
        }
    }

    suspend fun acceptModelAgreement(
        accountId: String,
        apiToken: String,
        modelId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val trimmedAccountId = accountId.trim()
            val trimmedToken = apiToken.trim().trim('"', '\'', ' ')
            val resolvedModel = when {
                modelId.startsWith("@") -> modelId
                modelId.contains("/") -> "@cf/$modelId"
                else -> modelId
            }
            val url = "https://api.cloudflare.com/client/v4/accounts/$trimmedAccountId/ai/run/$resolvedModel"

            // Cloudflare requires sending prompt: "agree" to accept the Meta Community License
            val bodyJson = JSONObject().apply {
                put("prompt", "agree")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "agree")
                    })
                })
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $trimmedToken")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).await()
            val responseString = response.body?.string() ?: ""
            Log.d(TAG, "acceptModelAgreement for $resolvedModel result: HTTP ${response.code} $responseString")
            if (response.isSuccessful) {
                Result.success("Meta License Agreement accepted for $resolvedModel")
            } else {
                Result.failure(Exception("Agreement response: HTTP ${response.code} - $responseString"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun callCloudflareChat(
        accountId: String,
        apiToken: String,
        model: String,
        prompt: String,
        base64Image: String? = null,
        mimeType: String = "image/jpeg",
        temperature: Double = 0.3,
        maxTokens: Int = 4096,
        systemPrompt: String? = null
    ): String = withContext(Dispatchers.IO) {
        val trimmedAccountId = accountId.trim()
        val trimmedToken = apiToken.trim()

        if (trimmedAccountId.isBlank()) {
            throw Exception("Cloudflare Account ID is missing. Please enter your Account ID in Settings.")
        }
        if (trimmedToken.isBlank()) {
            throw Exception("Cloudflare API Token is missing. Please enter your API Token in Settings.")
        }

        val hasImage = !base64Image.isNullOrBlank()
        val rawModel = if (hasImage) {
            val isCurrentVision = model.contains("vision", ignoreCase = true) || model.contains("llava", ignoreCase = true)
            if (isCurrentVision) model.trim() else DEFAULT_VISION_MODEL
        } else {
            model.trim().ifBlank { DEFAULT_TEXT_MODEL }
        }
        val resolvedModel = when {
            rawModel.startsWith("@") -> rawModel
            rawModel.contains("/") -> "@cf/$rawModel"
            else -> rawModel
        }

        fun buildRequestBody(): JSONObject = JSONObject().apply {
            put("max_tokens", maxTokens)
            put("temperature", temperature)

            val messagesArr = JSONArray()

            // System prompt
            if (!systemPrompt.isNullOrBlank()) {
                messagesArr.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt.trim())
                })
            }

            // User prompt
            val userMsg = JSONObject().apply {
                put("role", "user")
                if (hasImage) {
                    val contentArr = JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", prompt)
                        })
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

        // Direct Cloudflare Workers AI execution endpoint
        val url = "https://api.cloudflare.com/client/v4/accounts/$trimmedAccountId/ai/run/$resolvedModel"

        fun createRequest(): Request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $trimmedToken")
            .addHeader("Content-Type", "application/json")
            .post(buildRequestBody().toString().toRequestBody("application/json".toMediaType()))
            .build()

        var response = client.newCall(createRequest()).await()
        var responseString = response.body?.string() ?: ""

        // Check if Cloudflare asks for Model Agreement (e.g. Meta Llama 3.2 license)
        val isAgreementRequired = !response.isSuccessful && (
            responseString.contains("Model Agreement", ignoreCase = true) ||
            responseString.contains("prompt 'agree'", ignoreCase = true) ||
            responseString.contains("Community License", ignoreCase = true)
        )

        if (isAgreementRequired) {
            Log.i(TAG, "Cloudflare model $resolvedModel requires agreement acceptance. Auto-accepting now...")
            acceptModelAgreement(trimmedAccountId, trimmedToken, resolvedModel)
            // Retry the original query
            response = client.newCall(createRequest()).await()
            responseString = response.body?.string() ?: ""
        }

        if (!response.isSuccessful || responseString.isBlank()) {
            val errorMsg = try {
                val root = JSONObject(responseString)
                val errorsArr = root.optJSONArray("errors")
                if (errorsArr != null && errorsArr.length() > 0) {
                    errorsArr.getJSONObject(0).optString("message", "HTTP ${response.code}")
                } else {
                    root.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                }
            } catch (_: Exception) {
                "HTTP ${response.code}"
            }
            throw Exception("Cloudflare error ($resolvedModel): $errorMsg")
        }

        try {
            val root = JSONObject(responseString)
            val resultObj = root.optJSONObject("result")
            if (resultObj != null) {
                val respText = resultObj.optString("response", "")
                if (respText.isNotBlank()) return@withContext respText

                // Try choices format (if Cloudflare returns OpenAI compatible choices)
                val choices = resultObj.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val choice = choices.getJSONObject(0)
                    val content = choice.optJSONObject("message")?.optString("content", "")
                        ?: choice.optString("text", "")
                    if (content.isNotBlank()) return@withContext content
                }

                // Fallback fields
                val text = resultObj.optString("text", "")
                if (text.isNotBlank()) return@withContext text
                val output = resultObj.optString("output", "")
                if (output.isNotBlank()) return@withContext output
            }

            // Direct root response
            val directResp = root.optString("response", "")
            if (directResp.isNotBlank()) return@withContext directResp

            throw Exception("Could not parse output from Cloudflare model: $resolvedModel")
        } catch (e: Exception) {
            throw Exception("Cloudflare output parsing error: ${e.message}")
        }
    }
}
