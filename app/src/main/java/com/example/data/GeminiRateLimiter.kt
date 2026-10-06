package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.util.SimpleLruCache
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Intelligent Rate Limiter and Quota Shield for Google Gemini API.
 * Prevents HTTP 429 Too Many Requests and RESOURCE_EXHAUSTED errors without altering the user's chosen model.
 */
object GeminiRateLimiter {

    data class ModelQuotaConfig(
        val rpm: Int,
        val rpd: Int,
        val minIntervalMs: Long
    )

    data class ModelQuotaUsage(
        val modelId: String,
        val modelDisplayName: String,
        val todayRequests: Int,
        val maxRpd: Int,
        val remainingToday: Int,
        val rpmUsedLastMinute: Int,
        val maxRpm: Int,
        val percentUsedToday: Float,
        val isNearLimit: Boolean,
        val isExhausted: Boolean,
        val resetTimeText: String
    )

    private const val PREFS_NAME = "gemini_quota_prefs"
    private const val KEY_LAST_DATE = "last_quota_date"

    private val mutex = Mutex()
    private val slidingWindowMap = mutableMapOf<String, ArrayDeque<Long>>()

    // In-memory 100-entry response cache to prevent duplicate queries from consuming quota
    data class CachedResponse(val response: String, val timestamp: Long)
    private val responseCache = SimpleLruCache<String, CachedResponse>(100)
    private const val CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000L // 24 hours

    private val _currentUsageFlow = MutableStateFlow<ModelQuotaUsage?>(null)
    val currentUsageFlow: StateFlow<ModelQuotaUsage?> = _currentUsageFlow.asStateFlow()

    fun getModelConfig(modelId: String): ModelQuotaConfig {
        val sanitized = GeminiModelManager.resolveEffectiveEndpointModel(modelId).lowercase()
        return when {
            sanitized.contains("pro") -> {
                // Gemini Pro has strict free tier limits: 2 RPM, 50 RPD
                ModelQuotaConfig(rpm = 2, rpd = 50, minIntervalMs = 28_000L)
            }
            sanitized.contains("flash-lite") || sanitized.contains("lite") -> {
                // Flash-lite free tier: 15 RPM, 1,000 RPD (Updated Google policy)
                ModelQuotaConfig(rpm = 15, rpd = 1000, minIntervalMs = 3_800L)
            }
            sanitized.contains("2.5-flash") -> {
                // Gemini 2.5 Flash free tier: 10 RPM, 250 RPD (Updated Google policy)
                ModelQuotaConfig(rpm = 10, rpd = 250, minIntervalMs = 5_800L)
            }
            else -> {
                // Gemini 3.8 Flash, 3.5 Flash, 3.7 Flash, Flash-Latest: 15 RPM, 1500 RPD
                ModelQuotaConfig(rpm = 15, rpd = 1500, minIntervalMs = 3_800L)
            }
        }
    }

    private fun getUtcDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Checks if a cached response exists for this prompt and model to save 100% API quota.
     */
    fun getCachedResponse(modelId: String, prompt: String, mediaUri: String? = null): String? {
        val cacheKey = generateCacheKey(modelId, prompt, mediaUri)
        val cached = responseCache.get(cacheKey) ?: return null
        val now = System.currentTimeMillis()
        return if (now - cached.timestamp < CACHE_EXPIRY_MS) {
            cached.response
        } else {
            responseCache.remove(cacheKey)
            null
        }
    }

    /**
     * Caches a successful Gemini response.
     */
    fun putCachedResponse(modelId: String, prompt: String, mediaUri: String?, response: String) {
        if (response.isBlank()) return
        val cacheKey = generateCacheKey(modelId, prompt, mediaUri)
        responseCache.put(cacheKey, CachedResponse(response, System.currentTimeMillis()))
    }

    private fun generateCacheKey(modelId: String, prompt: String, mediaUri: String?): String {
        val raw = "$modelId|${mediaUri ?: ""}|${prompt.trim()}"
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(raw.toByteArray(Charsets.UTF_8))
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (_: Exception) {
            raw.hashCode().toString()
        }
    }

    /**
     * Suspends if necessary to enforce the model's RPM and burst spacing limits,
     * guaranteeing Google will not reject the request with HTTP 429.
     */
    suspend fun paceBeforeRequest(
        context: Context,
        modelId: String,
        onPacingWait: ((waitSeconds: Int) -> Unit)? = null
    ) {
        val config = getModelConfig(modelId)
        val sanitized = GeminiModelManager.resolveEffectiveEndpointModel(modelId)
        val now = System.currentTimeMillis()

        var waitMs = 0L

        mutex.withLock {
            val queue = slidingWindowMap.getOrPut(sanitized) { ArrayDeque() }

            // Prune timestamps older than 60 seconds
            val cutoff = now - 60_000L
            while (queue.isNotEmpty() && queue.peekFirst()!! < cutoff) {
                queue.pollFirst()
            }

            // Check if sliding window is saturated
            if (queue.size >= config.rpm) {
                val oldest = queue.peekFirst() ?: now
                val timeToSlotAvailable = (oldest + 60_000L) - now + 200L
                if (timeToSlotAvailable > waitMs) {
                    waitMs = timeToSlotAvailable
                }
            }

            // Check minimum interval since last request to avoid rapid bursts
            if (queue.isNotEmpty()) {
                val lastCall = queue.peekLast() ?: now
                val timeSinceLast = now - lastCall
                if (timeSinceLast < config.minIntervalMs) {
                    val burstWait = config.minIntervalMs - timeSinceLast
                    if (burstWait > waitMs) {
                        waitMs = burstWait
                    }
                }
            }
        }

        if (waitMs > 0L) {
            val waitSeconds = ((waitMs + 999L) / 1000L).toInt()
            onPacingWait?.invoke(waitSeconds)
            delay(waitMs)
        }
    }

    /**
     * Records a completed request for today's quota and current sliding window.
     */
    suspend fun recordRequestSuccess(context: Context, modelId: String) {
        val sanitized = GeminiModelManager.resolveEffectiveEndpointModel(modelId)
        val now = System.currentTimeMillis()
        val today = getUtcDateString()

        mutex.withLock {
            val queue = slidingWindowMap.getOrPut(sanitized) { ArrayDeque() }
            queue.addLast(now)
            // Prune old
            val cutoff = now - 60_000L
            while (queue.isNotEmpty() && queue.peekFirst()!! < cutoff) {
                queue.pollFirst()
            }
        }

        val prefs = getPrefs(context)
        val storedDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
        val editor = prefs.edit()
        if (storedDate != today) {
            // New day in UTC - reset daily counters
            editor.putString(KEY_LAST_DATE, today)
        }
        val key = "usage_${today}_$sanitized"
        val count = prefs.getInt(key, 0) + 1
        editor.putInt(key, count)
        editor.apply()

        // Update Live Usage StateFlow
        refreshUsageState(context, modelId)
    }

    /**
     * Reads current quota usage for a model.
     */
    fun getUsage(context: Context, modelId: String): ModelQuotaUsage {
        val config = getModelConfig(modelId)
        val sanitized = GeminiModelManager.resolveEffectiveEndpointModel(modelId)
        val today = getUtcDateString()
        val prefs = getPrefs(context)

        val storedDate = prefs.getString(KEY_LAST_DATE, "") ?: ""
        val todayRequests = if (storedDate == today) {
            prefs.getInt("usage_${today}_$sanitized", 0)
        } else {
            0
        }

        val remainingToday = (config.rpd - todayRequests).coerceAtLeast(0)
        val percentUsed = (todayRequests.toFloat() / config.rpd.toFloat()).coerceIn(0f, 1f)

        val now = System.currentTimeMillis()
        val cutoff = now - 60_000L
        val rpmUsed = synchronized(slidingWindowMap) {
            val q = slidingWindowMap[sanitized] ?: ArrayDeque()
            q.count { it >= cutoff }
        }

        val isNearLimit = percentUsed >= 0.85f || remainingToday <= (if (config.rpd > 100) 25 else 5)
        val isExhausted = remainingToday <= 0

        val displayName = when {
            sanitized.contains("3.8") -> "Gemini 3.8 Flash"
            sanitized.contains("3.5") -> "Gemini 3.5 Flash"
            sanitized.contains("3.1-pro") || sanitized.contains("pro") -> "Gemini 3.1 Pro"
            sanitized.contains("flash-lite") -> "Gemini 3.1 Flash Lite"
            else -> "Gemini Flash"
        }

        return ModelQuotaUsage(
            modelId = sanitized,
            modelDisplayName = displayName,
            todayRequests = todayRequests,
            maxRpd = config.rpd,
            remainingToday = remainingToday,
            rpmUsedLastMinute = rpmUsed,
            maxRpm = config.rpm,
            percentUsedToday = percentUsed,
            isNearLimit = isNearLimit,
            isExhausted = isExhausted,
            resetTimeText = "Midnight UTC (5:30 AM IST)"
        )
    }

    fun refreshUsageState(context: Context, modelId: String) {
        val usage = getUsage(context, modelId)
        _currentUsageFlow.value = usage
    }
}
