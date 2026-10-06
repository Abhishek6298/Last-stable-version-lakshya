package com.example.ui
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.*
import org.json.JSONArray
import org.json.JSONObject
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat


data class GuardianMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "ai" or "user"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class GuardianUpcomingTestInfo(
    val id: Int,
    val title: String,
    val scheduledDate: String,
    val daysRemaining: Int,
    val physicsSyllabus: String,
    val chemistrySyllabus: String,
    val biologySyllabus: String,
    val syllabusNotes: String,
    val isCompleted: Boolean,
    val isPinned: Boolean = false
)

data class GuardianPulseState(
    val readinessScore: Int = 75,
    val readinessStatus: String = "Analyzing Preparation...",
    val readinessColorHex: Long = 0xFF10B981,
    val studyHoursToday: Float = 0f,
    val studyTargetHours: Float = 3f,
    val dppCompletedCount: Int = 0,
    val dppTotalCount: Int = 0,
    val dppPendingCount: Int = 0,
    val upcomingTests: List<GuardianUpcomingTestInfo> = emptyList(),
    val nextUpcomingTest: GuardianUpcomingTestInfo? = null,
    val averageMockScore: Int = 0,
    val latestMockScore: Int? = null,
    val weakestSubject: String = "Physics",
    val isZombieRisk: Boolean = false,
    val proactiveDiagnosis: String = "LAKSHYA AI Guardian is actively observing your preparation radar...",
    val actionableGuidance: String = "Solve high-yield problems and revise target test chapters."
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)

    var pendingMistakeImageUri: String? = null
    var pendingAiBotImageUri: String? = null
    var pendingAiBotSubject: String? = null
    var pendingStudyTubeImageUri: String? = null
    var pendingStudyTubeQuery: String? = null


    private val _edunitiChecklistState = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val edunitiChecklistState: StateFlow<Map<String, Boolean>> = _edunitiChecklistState.asStateFlow()

    fun loadEdunitiChecklist() {
        val allPrefs = prefs.all
        val state = mutableMapOf<String, Boolean>()
        for ((key, value) in allPrefs) {
            if (key.startsWith("eduniti_chk_") && value is Boolean) {
                state[key.removePrefix("eduniti_chk_")] = value
            }
        }
        _edunitiChecklistState.value = state
    }

    fun toggleEdunitiChecklistItem(key: String, value: Boolean, chapterName: String? = null, milestone: String? = null) {
        prefs.edit().putBoolean("eduniti_chk_$key", value).apply()
        _edunitiChecklistState.value = _edunitiChecklistState.value.toMutableMap().apply {
            put(key, value)
        }
        if (value && !chapterName.isNullOrBlank()) {
            // autoSyncChecklistChapterToRevision(chapterName, milestone) removed per user request
        }
    }
    
    private val defaultNeetTargetMillis = Calendar.getInstance().apply {
        set(2027, Calendar.MAY, 3, 14, 0, 0)
    }.timeInMillis

    private val _neetTargetMillis = kotlinx.coroutines.flow.MutableStateFlow(prefs.getLong("neet_target_millis", defaultNeetTargetMillis))
    val neetTargetMillis: StateFlow<Long> = _neetTargetMillis

    fun setNeetTargetMillis(millis: Long) {
        prefs.edit().putLong("neet_target_millis", millis).apply()
        _neetTargetMillis.value = millis
        com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
    }

    private val _isSidebarOpen = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isSidebarOpen: StateFlow<Boolean> = _isSidebarOpen

    fun openSidebar() {
        _isSidebarOpen.value = true
    }

    fun closeSidebar() {
        _isSidebarOpen.value = false
    }

    fun toggleSidebar() {
        _isSidebarOpen.value = !_isSidebarOpen.value
    }

    private fun getInitialAvatarUri(): String? {
        val persistentFile = java.io.File(getApplication<Application>().filesDir, "user_avatar_persistent.jpg")
        if (persistentFile.exists() && persistentFile.length() > 0) {
            return Uri.fromFile(persistentFile).toString()
        }
        return prefs.getString("user_avatar_uri", null)
    }

    private val _userName = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("user_name", "Future Doctor 👨‍⚕️") ?: "Future Doctor 👨‍⚕️")
    val userName: StateFlow<String> = _userName
    
    private val _userClass = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("user_class", "Dropper") ?: "Dropper")
    val userClass: StateFlow<String> = _userClass

    private val _userAvatarUri = kotlinx.coroutines.flow.MutableStateFlow(getInitialAvatarUri())
    val userAvatarUri: StateFlow<String?> = _userAvatarUri

    fun setUserNameAndClass(name: String, userClass: String) {
        prefs.edit().putString("user_name", name).putString("user_class", userClass).apply()
        _userName.value = name
        _userClass.value = userClass
    }

    fun setUserProfile(name: String, userClass: String, avatarUri: String?) {
        var finalAvatarUri: String? = avatarUri
        val context = getApplication<Application>()
        val persistentFile = java.io.File(context.filesDir, "user_avatar_persistent.jpg")

        if (!avatarUri.isNullOrBlank()) {
            if (avatarUri.startsWith("content://")) {
                try {
                    val srcUri = Uri.parse(avatarUri)
                    context.contentResolver.openInputStream(srcUri)?.use { input ->
                        persistentFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (persistentFile.exists() && persistentFile.length() > 0) {
                        finalAvatarUri = Uri.fromFile(persistentFile).toString()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (avatarUri.startsWith("file://")) {
                val file = java.io.File(Uri.parse(avatarUri).path ?: "")
                if (file.exists() && file.absolutePath != persistentFile.absolutePath) {
                    try {
                        file.inputStream().use { input ->
                            persistentFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        finalAvatarUri = Uri.fromFile(persistentFile).toString()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            try {
                if (persistentFile.exists()) {
                    persistentFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            finalAvatarUri = null
        }

        val editor = prefs.edit().putString("user_name", name).putString("user_class", userClass)
        if (finalAvatarUri != null) {
            editor.putString("user_avatar_uri", finalAvatarUri)
        } else {
            editor.remove("user_avatar_uri")
        }
        editor.apply()
        _userName.value = name
        _userClass.value = userClass
        _userAvatarUri.value = finalAvatarUri
    }

    private val defaultStudyPortals = listOf(
        StudyPortal(
            id = "pw_thor",
            name = "PW Thor",
            url = "https://pwthor.live",
            tag = "Live Batches",
            description = "pwthor.live • Classes, Live Streams & DPPs",
            iconEmoji = "⚡",
            isDefault = true,
            isVerified = true,
            aiVerificationReason = "Physics Wallah Live Learning & Batch Streaming Portal"
        ),
        StudyPortal(
            id = "pw_live",
            name = "Physics Wallah",
            url = "https://www.pw.live",
            tag = "Batches & Tests",
            description = "pw.live • Official Batches, Notes & DPPs",
            iconEmoji = "🎓",
            isDefault = true,
            isVerified = true,
            aiVerificationReason = "Official Physics Wallah Web Portal"
        ),
        StudyPortal(
            id = "testbook",
            name = "Testbook",
            url = "https://testbook.com/test-series",
            tag = "Test Series",
            description = "testbook.com • Full NEET Mock Tests & Past Papers",
            iconEmoji = "📝",
            isDefault = true,
            isVerified = true,
            aiVerificationReason = "National Mock Test & Exam Preparation Platform"
        ),
        StudyPortal(
            id = "nta_neet",
            name = "NTA NEET",
            url = "https://neet.nta.nic.in",
            tag = "Official NTA",
            description = "neet.nta.nic.in • Official Exam Notices & Syllabus",
            iconEmoji = "🏛️",
            isDefault = true,
            isVerified = true,
            aiVerificationReason = "National Testing Agency Official NEET Web Portal"
        ),
        StudyPortal(
            id = "allen_digital",
            name = "Allen Digital",
            url = "https://allendigital.in",
            tag = "Allen Tests",
            description = "allendigital.in • Online Test Series & Modules",
            iconEmoji = "📘",
            isDefault = true,
            isVerified = true,
            aiVerificationReason = "Allen Career Institute Online NEET Platform"
        ),
        StudyPortal(
            id = "embibe_neet",
            name = "Embibe AI",
            url = "https://www.embibe.com/medical/neet-test-series",
            tag = "AI Analytics",
            description = "embibe.com • AI Mock Practice & Rank Analysis",
            iconEmoji = "🎯",
            isDefault = true,
            isVerified = true,
            aiVerificationReason = "AI Powered Adaptive Test & Practice Engine"
        ),
        StudyPortal(
            id = "unacademy_neet",
            name = "Unacademy",
            url = "https://unacademy.com/goal/neet-ug/YOTXM",
            tag = "Live Prep",
            description = "unacademy.com • Live Lectures & All India Tests",
            iconEmoji = "🟢",
            isDefault = true,
            isVerified = true,
            aiVerificationReason = "Unacademy NEET UG Learning & Mock Portal"
        )
    )

    private val _studyPortals = MutableStateFlow<List<StudyPortal>>(defaultStudyPortals)
    val studyPortals: StateFlow<List<StudyPortal>> = _studyPortals.asStateFlow()

    fun loadStudyPortals() {
        val isInitialized = prefs.getBoolean("study_portals_initialized_v2", false)
        val jsonStr = prefs.getString("study_portals_v2", null)

        if (!isInitialized && jsonStr.isNullOrBlank()) {
            prefs.edit().putBoolean("study_portals_initialized_v2", true).apply()
            _studyPortals.value = defaultStudyPortals
            saveStudyPortals(defaultStudyPortals)
            return
        }

        if (jsonStr.isNullOrBlank()) {
            _studyPortals.value = emptyList()
            return
        }

        try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<StudyPortal>()
            val deletedUrls = prefs.getStringSet("deleted_study_portal_urls", emptySet()) ?: emptySet()
            val deletedIds = prefs.getStringSet("deleted_study_portal_ids", emptySet()) ?: emptySet()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                val url = obj.optString("url", "").trim()
                val normUrl = url.lowercase().trimEnd('/')

                // Respect user deletion: never resurrect deleted portals
                if (id in deletedIds || (normUrl.isNotBlank() && normUrl in deletedUrls)) {
                    continue
                }

                list.add(
                    StudyPortal(
                        id = id,
                        name = obj.optString("name", "Study Portal"),
                        url = url,
                        tag = obj.optString("tag", "Study Portal"),
                        description = obj.optString("description", ""),
                        iconEmoji = obj.optString("iconEmoji", "🌐"),
                        isDefault = obj.optBoolean("isDefault", false),
                        isVerified = obj.optBoolean("isVerified", true),
                        aiVerificationReason = obj.optString("aiVerificationReason", "")
                    )
                )
            }
            _studyPortals.value = list
        } catch (e: Exception) {
            e.printStackTrace()
            _studyPortals.value = emptyList()
        }
    }

    private fun saveStudyPortals(list: List<StudyPortal>) {
        try {
            val arr = JSONArray()
            for (portal in list) {
                val obj = JSONObject().apply {
                    put("id", portal.id)
                    put("name", portal.name)
                    put("url", portal.url)
                    put("tag", portal.tag)
                    put("description", portal.description)
                    put("iconEmoji", portal.iconEmoji)
                    put("isDefault", portal.isDefault)
                    put("isVerified", portal.isVerified)
                    put("aiVerificationReason", portal.aiVerificationReason)
                }
                arr.put(obj)
            }
            prefs.edit()
                .putString("study_portals_v2", arr.toString())
                .putBoolean("study_portals_initialized_v2", true)
                .apply()
            _studyPortals.value = list
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addStudyPortal(portal: StudyPortal) {
        val normUrl = portal.url.lowercase().trimEnd('/')
        val deletedUrls = prefs.getStringSet("deleted_study_portal_urls", emptySet())?.toMutableSet() ?: mutableSetOf()
        val deletedIds = prefs.getStringSet("deleted_study_portal_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (normUrl in deletedUrls || portal.id in deletedIds) {
            deletedUrls.remove(normUrl)
            deletedIds.remove(portal.id)
            prefs.edit()
                .putStringSet("deleted_study_portal_urls", deletedUrls)
                .putStringSet("deleted_study_portal_ids", deletedIds)
                .apply()
        }
        val current = _studyPortals.value.filter { it.id != portal.id }
        val updated = current + portal
        saveStudyPortals(updated)
    }

    fun updateStudyPortal(portal: StudyPortal) {
        val updated = _studyPortals.value.map {
            if (it.id == portal.id) portal else it
        }
        saveStudyPortals(updated)
    }

    fun deleteStudyPortal(portalId: String) {
        val target = _studyPortals.value.find { it.id == portalId }
        val updated = _studyPortals.value.filter { it.id != portalId }
        if (target != null) {
            val deletedUrls = prefs.getStringSet("deleted_study_portal_urls", emptySet())?.toMutableSet() ?: mutableSetOf()
            val deletedIds = prefs.getStringSet("deleted_study_portal_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
            val normUrl = target.url.lowercase().trimEnd('/')
            if (normUrl.isNotBlank()) deletedUrls.add(normUrl)
            deletedIds.add(target.id)
            prefs.edit()
                .putStringSet("deleted_study_portal_urls", deletedUrls)
                .putStringSet("deleted_study_portal_ids", deletedIds)
                .apply()
        }
        saveStudyPortals(updated)
    }

    fun resetStudyPortalsToDefault() {
        prefs.edit()
            .remove("deleted_study_portal_urls")
            .remove("deleted_study_portal_ids")
            .putBoolean("study_portals_initialized_v2", true)
            .apply()
        saveStudyPortals(defaultStudyPortals)
    }

    private fun getNotificationTimes(): Map<String, Int> {
        return mapOf(
            "Daily Reminder" to prefs.getInt("pref_time_daily_reminder", 8),
            "Daily Target" to prefs.getInt("pref_time_daily_target", 9),
            "New Content" to prefs.getInt("pref_time_new_content", 12),
            "Revision" to prefs.getInt("pref_time_revision", 14),
            "Practice Test" to prefs.getInt("pref_time_practice_test", 17),
            "Timetable" to prefs.getInt("pref_time_timetable", 18),
            "Mock Test" to prefs.getInt("pref_time_mock_test", 20)
        )
    }

    private val _notificationTimes = kotlinx.coroutines.flow.MutableStateFlow(getNotificationTimes())
    val notificationTimes: kotlinx.coroutines.flow.StateFlow<Map<String, Int>> = _notificationTimes

    fun updateNotificationTime(key: String, hour: Int, context: android.content.Context) {
        val prefKey = when(key) {
            "Daily Reminder" -> "pref_time_daily_reminder"
            "Daily Target" -> "pref_time_daily_target"
            "New Content" -> "pref_time_new_content"
            "Revision" -> "pref_time_revision"
            "Practice Test" -> "pref_time_practice_test"
            "Timetable" -> "pref_time_timetable"
            "Mock Test" -> "pref_time_mock_test"
            else -> return
        }
        prefs.edit().putInt(prefKey, hour).apply()
        _notificationTimes.value = getNotificationTimes()
        // Reschedule notifications with the new times
        com.example.workers.NotificationScheduler.scheduleAllReminders(context, forceUpdate = true)
    }

    // Custom 3 Gemini API Keys State
    private val _geminiApiKey1 = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("gemini_api_key_1", "") ?: "")
    val geminiApiKey1: StateFlow<String> = _geminiApiKey1

    private val _geminiApiKey2 = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("gemini_api_key_2", "") ?: "")
    val geminiApiKey2: StateFlow<String> = _geminiApiKey2

    private val _geminiApiKey3 = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("gemini_api_key_3", "") ?: "")
    val geminiApiKey3: StateFlow<String> = _geminiApiKey3

    // ==========================================
    // 🌐 OPENROUTER & DUAL AI ENGINE ARCHITECTURE
    // ==========================================
    private val _aiProvider = MutableStateFlow(
        try {
            val saved = prefs.getString("ai_provider", AiProvider.NATIVE_GEMINI.name) ?: AiProvider.NATIVE_GEMINI.name
            AiProvider.valueOf(saved)
        } catch (_: Exception) {
            AiProvider.NATIVE_GEMINI
        }
    )
    val aiProvider: StateFlow<AiProvider> = _aiProvider.asStateFlow()

    fun setAiProvider(provider: AiProvider) {
        prefs.edit().putString("ai_provider", provider.name).apply()
        _aiProvider.value = provider
    }

    private val _groqApiKey = MutableStateFlow(
        prefs.getString(com.example.data.GroqManager.KEY_GROQ_KEY, "") ?: ""
    )
    val groqApiKey: StateFlow<String> = _groqApiKey.asStateFlow()

    fun setGroqApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(com.example.data.GroqManager.KEY_GROQ_KEY, trimmed).apply()
        _groqApiKey.value = trimmed
        if (trimmed.isNotBlank()) {
            fetchGroqLiveModels()
        }
    }

    private val _groqSelectedModel = MutableStateFlow(
        prefs.getString(com.example.data.GroqManager.KEY_GROQ_MODEL, com.example.data.GroqManager.DEFAULT_TEXT_MODEL) ?: com.example.data.GroqManager.DEFAULT_TEXT_MODEL
    )
    val groqSelectedModel: StateFlow<String> = _groqSelectedModel.asStateFlow()

    fun setGroqSelectedModel(model: String) {
        val trimmed = model.trim()
        if (trimmed.isNotBlank()) {
            prefs.edit().putString(com.example.data.GroqManager.KEY_GROQ_MODEL, trimmed).apply()
            com.example.data.GroqManager.setSelectedModel(getApplication(), trimmed)
            _groqSelectedModel.value = trimmed
            setAiProvider(AiProvider.GROQ)
        }
    }

    private val _groqModels = MutableStateFlow<List<com.example.data.GroqModel>>(
        com.example.data.GroqManager.getCachedModels(getApplication())
    )
    val groqModels: StateFlow<List<com.example.data.GroqModel>> = _groqModels.asStateFlow()

    private val _isGroqLoadingModels = MutableStateFlow(false)
    val isGroqLoadingModels: StateFlow<Boolean> = _isGroqLoadingModels.asStateFlow()

    fun fetchGroqLiveModels(onResult: ((Boolean, String) -> Unit)? = null) {
        val app = getApplication<android.app.Application>()
        val key = _groqApiKey.value.ifBlank { com.example.data.GroqManager.getGroqApiKey(app) }
        viewModelScope.launch {
            _isGroqLoadingModels.value = true
            val res = com.example.data.GroqManager.fetchLiveModels(app, key)
            _isGroqLoadingModels.value = false
            res.fold(
                onSuccess = { models ->
                    if (models.isNotEmpty()) {
                        _groqModels.value = models
                    }
                    onResult?.invoke(true, "✅ Synced ${models.size} live models from Groq!")
                },
                onFailure = { err ->
                    val fallback = com.example.data.GroqManager.popularGroqModels
                    _groqModels.value = fallback
                    val msg = if (key.isBlank()) {
                        "⚠️ Please enter your Groq API Key in Settings first (starts with 'gsk_')"
                    } else {
                        "Sync error: ${err.message}. Showing ${fallback.size} default models."
                    }
                    onResult?.invoke(false, msg)
                }
            )
        }
    }

    private val _cloudflareSelectedModel = MutableStateFlow(
        prefs.getString(com.example.data.CloudflareManager.KEY_CF_MODEL, com.example.data.CloudflareManager.DEFAULT_TEXT_MODEL) ?: com.example.data.CloudflareManager.DEFAULT_TEXT_MODEL
    )
    val cloudflareSelectedModel: StateFlow<String> = _cloudflareSelectedModel.asStateFlow()

    fun setCloudflareSelectedModel(model: String) {
        val trimmed = model.trim()
        if (trimmed.isNotBlank()) {
            prefs.edit().putString(com.example.data.CloudflareManager.KEY_CF_MODEL, trimmed).apply()
            com.example.data.CloudflareManager.setSelectedModel(getApplication(), trimmed)
            _cloudflareSelectedModel.value = trimmed
            setAiProvider(AiProvider.CLOUDFLARE)
        }
    }

    private val _cloudflareModels = MutableStateFlow<List<com.example.data.CloudflareModel>>(
        com.example.data.CloudflareManager.getCachedModels(getApplication())
    )
    val cloudflareModels: StateFlow<List<com.example.data.CloudflareModel>> = _cloudflareModels.asStateFlow()

    private val _isCloudflareLoadingModels = MutableStateFlow(false)
    val isCloudflareLoadingModels: StateFlow<Boolean> = _isCloudflareLoadingModels.asStateFlow()

    fun fetchCloudflareLiveModels(onResult: ((Boolean, String) -> Unit)? = null) {
        val app = getApplication<android.app.Application>()
        val prefs = app.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val accId = (prefs.getString(com.example.data.CloudflareManager.KEY_CF_ACCOUNT_ID, "") ?: "").trim()
        val token = (prefs.getString(com.example.data.CloudflareManager.KEY_CF_API_TOKEN, "") ?: "").trim()
        viewModelScope.launch {
            _isCloudflareLoadingModels.value = true
            val res = com.example.data.CloudflareManager.fetchLiveModels(app, accId, token)
            _isCloudflareLoadingModels.value = false
            res.fold(
                onSuccess = { models ->
                    if (models.isNotEmpty()) {
                        _cloudflareModels.value = models
                    }
                    onResult?.invoke(true, "Synced ${models.size} live models from Cloudflare Workers AI!")
                },
                onFailure = { err ->
                    val fallback = com.example.data.CloudflareManager.popularCloudflareModels
                    _cloudflareModels.value = fallback
                    onResult?.invoke(false, "Sync error: ${err.message}. Showing ${fallback.size} default models.")
                }
            )
        }
    }

    private val _openrouterApiKey = MutableStateFlow(prefs.getString("openrouter_api_key", "") ?: "")
    val openrouterApiKey: StateFlow<String> = _openrouterApiKey.asStateFlow()

    fun setOpenRouterApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString("openrouter_api_key", trimmed).apply()
        _openrouterApiKey.value = trimmed
    }

    private val _openrouterMaxTokens = MutableStateFlow(
        prefs.getInt("openrouter_max_tokens", 8192)
    )
    val openrouterMaxTokens: StateFlow<Int> = _openrouterMaxTokens.asStateFlow()

    fun setOpenRouterMaxTokens(tokens: Int) {
        val safeTokens = tokens.coerceIn(1024, 65536)
        prefs.edit().putInt("openrouter_max_tokens", safeTokens).apply()
        _openrouterMaxTokens.value = safeTokens
        OpenRouterManager.setMaxTokens(getApplication(), safeTokens)
    }

    private val _openrouterSelectedModel = MutableStateFlow(
        prefs.getString("openrouter_selected_model", "google/gemini-2.5-flash") ?: "google/gemini-2.5-flash"
    )
    val openrouterSelectedModel: StateFlow<String> = _openrouterSelectedModel.asStateFlow()
    val openRouterSelectedModel: StateFlow<String> get() = _openrouterSelectedModel.asStateFlow()

    fun setOpenRouterSelectedModel(modelId: String) {
        val trimmed = modelId.trim()
        if (trimmed.isNotBlank()) {
            val normalized = when {
                trimmed.contains("/") -> trimmed
                trimmed.contains("pro", ignoreCase = true) -> "google/gemini-2.5-pro"
                trimmed.contains("flash", ignoreCase = true) || trimmed.contains("gemini", ignoreCase = true) -> "google/gemini-2.5-flash"
                else -> trimmed
            }
            prefs.edit().putString("openrouter_selected_model", normalized).apply()
            _openrouterSelectedModel.value = normalized
            // Automatically switch active provider to OpenRouter so user's chosen model is strictly used!
            setAiProvider(com.example.data.AiProvider.OPENROUTER)
        }
    }

    private val _openrouterModels = MutableStateFlow<List<OpenRouterModel>>(
        OpenRouterManager.getCachedModels(application)
    )
    val openrouterModels: StateFlow<List<OpenRouterModel>> = _openrouterModels.asStateFlow()
    val openRouterModels: StateFlow<List<OpenRouterModel>> get() = _openrouterModels.asStateFlow()

    private val _isOpenRouterLoadingModels = MutableStateFlow(false)
    val isOpenRouterLoadingModels: StateFlow<Boolean> = _isOpenRouterLoadingModels.asStateFlow()

    private val _openrouterKeyStatus = MutableStateFlow("")
    val openrouterKeyStatus: StateFlow<String> = _openrouterKeyStatus.asStateFlow()

    fun fetchOpenRouterLiveModels() {
        if (_isOpenRouterLoadingModels.value) return
        _isOpenRouterLoadingModels.value = true
        viewModelScope.launch {
            val result = OpenRouterManager.fetchLiveModels(getApplication(), _openrouterApiKey.value)
            _isOpenRouterLoadingModels.value = false
            result.onSuccess { models ->
                if (models.isNotEmpty()) {
                    _openrouterModels.value = models
                }
            }
        }
    }

    fun verifyOpenRouterKey(onResult: ((Boolean, String) -> Unit)? = null) {
        val key = _openrouterApiKey.value.trim()
        if (key.isBlank()) {
            _openrouterKeyStatus.value = "⚠️ Key is empty"
            onResult?.invoke(false, "API Key is empty")
            return
        }
        _openrouterKeyStatus.value = "Verifying..."
        viewModelScope.launch {
            val result = OpenRouterManager.verifyApiKey(key)
            result.fold(
                onSuccess = { msg ->
                    _openrouterKeyStatus.value = msg
                    onResult?.invoke(true, msg)
                    // Auto-refresh models with the newly validated key
                    fetchOpenRouterLiveModels()
                },
                onFailure = { err ->
                    val msg = "❌ ${err.localizedMessage ?: err.message}"
                    _openrouterKeyStatus.value = msg
                    onResult?.invoke(false, msg)
                }
            )
        }
    }

    private val _youtubeApiKey = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("youtube_api_key", "") ?: "")
    val youtubeApiKey: StateFlow<String> = _youtubeApiKey

    private val _savedPlaylists = kotlinx.coroutines.flow.MutableStateFlow(prefs.getStringSet("saved_youtube_playlists", emptySet()) ?: emptySet())
    val savedPlaylists: StateFlow<Set<String>> = _savedPlaylists

    fun addCustomPlaylist(id: String, name: String) {
        val entry = "$id|$name"
        val current = _savedPlaylists.value.toMutableSet()
        // Remove existing if any with same id
        current.removeAll { it.startsWith("$id|") }
        current.add(entry)
        prefs.edit().putStringSet("saved_youtube_playlists", current).apply()
        _savedPlaylists.value = current
    }

    fun removeCustomPlaylist(id: String) {
        val current = _savedPlaylists.value.toMutableSet()
        current.removeAll { it.startsWith("$id|") }
        prefs.edit().putStringSet("saved_youtube_playlists", current).apply()
        _savedPlaylists.value = current
    }

    // StudyTube Bookmarks & Notes
    data class StudyTubeLaunchRequest(
        val urlOrIdOrQuery: String,
        val title: String = "",
        val subject: String = "",
        val isPlaylist: Boolean = true,
        val timestampSeconds: Int = 0
    )

    private val _pendingStudyTubeLaunch = kotlinx.coroutines.flow.MutableStateFlow<StudyTubeLaunchRequest?>(null)
    val pendingStudyTubeLaunch: StateFlow<StudyTubeLaunchRequest?> = _pendingStudyTubeLaunch

    fun openInStudyTube(urlOrIdOrQuery: String, title: String = "", subject: String = "", isPlaylist: Boolean = true, timestampSeconds: Int = 0) {
        _pendingStudyTubeLaunch.value = StudyTubeLaunchRequest(urlOrIdOrQuery, title, subject, isPlaylist, timestampSeconds)
    }

    fun openQuestionInStudyTubeLens(questionText: String, imageUri: String? = null) {
        pendingStudyTubeQuery = questionText
        pendingStudyTubeImageUri = imageUri
    }

    fun clearPendingStudyTubeLaunch() {
        _pendingStudyTubeLaunch.value = null
    }

    private val _studyTubeBookmarks = kotlinx.coroutines.flow.MutableStateFlow(prefs.getStringSet("studytube_bookmarks", emptySet()) ?: emptySet())
    val studyTubeBookmarks: StateFlow<Set<String>> = _studyTubeBookmarks

    fun toggleStudyTubeBookmark(videoId: String, title: String, channelTitle: String, thumbUrl: String) {
        val current = _studyTubeBookmarks.value.toMutableSet()
        val existing = current.find { it.startsWith("$videoId|||") }
        if (existing != null) {
            current.remove(existing)
        } else {
            current.add("$videoId|||$title|||$channelTitle|||$thumbUrl")
        }
        prefs.edit().putStringSet("studytube_bookmarks", current).apply()
        _studyTubeBookmarks.value = current
    }

    private val _studyTubeNotes = kotlinx.coroutines.flow.MutableStateFlow(prefs.getStringSet("studytube_notes", emptySet()) ?: emptySet())
    val studyTubeNotes: StateFlow<Set<String>> = _studyTubeNotes

    fun addStudyTubeNote(videoId: String, videoTitle: String, timestampSec: Int, noteText: String) {
        val current = _studyTubeNotes.value.toMutableSet()
        val id = System.currentTimeMillis()
        current.add("$id|||$videoId|||$timestampSec|||$noteText|||$videoTitle")
        prefs.edit().putStringSet("studytube_notes", current).apply()
        _studyTubeNotes.value = current
    }

    fun removeStudyTubeNote(noteEntry: String) {
        val current = _studyTubeNotes.value.toMutableSet()
        current.remove(noteEntry)
        prefs.edit().putStringSet("studytube_notes", current).apply()
        _studyTubeNotes.value = current
    }

    // StudyTube Organized Chapter Videos: "subject|||chapterName|||videoId|||title|||channelTitle|||thumbUrl"
    private val _studyTubeChapterVideos = kotlinx.coroutines.flow.MutableStateFlow(prefs.getStringSet("studytube_chapter_videos", emptySet()) ?: emptySet())
    val studyTubeChapterVideos: StateFlow<Set<String>> = _studyTubeChapterVideos

    fun addVideoToChapter(subject: String, chapterName: String, videoId: String, title: String, channelTitle: String, thumbUrl: String) {
        val current = _studyTubeChapterVideos.value.toMutableSet()
        val entry = "$subject|||$chapterName|||$videoId|||$title|||$channelTitle|||$thumbUrl"
        current.add(entry)
        prefs.edit().putStringSet("studytube_chapter_videos", current).apply()
        _studyTubeChapterVideos.value = current
    }

    fun removeVideoFromChapter(entry: String) {
        val current = _studyTubeChapterVideos.value.toMutableSet()
        current.remove(entry)
        prefs.edit().putStringSet("studytube_chapter_videos", current).apply()
        _studyTubeChapterVideos.value = current
    }

    // StudyTube Recently Viewed
    data class RecentlyViewedVideo(
        val id: String,
        val title: String,
        val channelTitle: String,
        val thumbnailUrl: String,
        val subjectTag: String = "General",
        val timestamp: Long = System.currentTimeMillis()
    )

    private fun loadRecentlyViewedVideos(): List<RecentlyViewedVideo> {
        val jsonStr = prefs.getString("studytube_recently_viewed", null) ?: return emptyList()
        return try {
            val list = mutableListOf<RecentlyViewedVideo>()
            val arr = org.json.JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    RecentlyViewedVideo(
                        id = obj.optString("id", ""),
                        title = obj.optString("title", ""),
                        channelTitle = obj.optString("channelTitle", ""),
                        thumbnailUrl = obj.optString("thumbnailUrl", ""),
                        subjectTag = obj.optString("subjectTag", "General"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list.filter { it.id.isNotBlank() && it.title.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private val _recentlyViewedVideos = kotlinx.coroutines.flow.MutableStateFlow<List<RecentlyViewedVideo>>(loadRecentlyViewedVideos())
    val recentlyViewedVideos: StateFlow<List<RecentlyViewedVideo>> = _recentlyViewedVideos.asStateFlow()

    fun recordRecentlyViewed(videoId: String, title: String, channelTitle: String, thumbUrl: String, subjectTag: String = "General") {
        if (videoId.isBlank() || title.isBlank()) return
        val current = _recentlyViewedVideos.value.toMutableList()
        current.removeAll { it.id == videoId }
        current.add(0, RecentlyViewedVideo(
            id = videoId,
            title = title,
            channelTitle = channelTitle,
            thumbnailUrl = thumbUrl,
            subjectTag = subjectTag,
            timestamp = System.currentTimeMillis()
        ))
        val trimmed = current.take(30)
        _recentlyViewedVideos.value = trimmed
        saveRecentlyViewedVideos(trimmed)
    }

    fun removeRecentlyViewedItem(videoId: String) {
        val current = _recentlyViewedVideos.value.toMutableList()
        current.removeAll { it.id == videoId }
        _recentlyViewedVideos.value = current
        saveRecentlyViewedVideos(current)
    }

    fun clearRecentlyViewed() {
        _recentlyViewedVideos.value = emptyList()
        prefs.edit().remove("studytube_recently_viewed").apply()
    }

    private fun saveRecentlyViewedVideos(list: List<RecentlyViewedVideo>) {
        try {
            val arr = org.json.JSONArray()
            list.forEach { item ->
                val obj = org.json.JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("channelTitle", item.channelTitle)
                    put("thumbnailUrl", item.thumbnailUrl)
                    put("subjectTag", item.subjectTag)
                    put("timestamp", item.timestamp)
                }
                arr.put(obj)
            }
            prefs.edit().putString("studytube_recently_viewed", arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // StudyTube Custom Hall of Fame Channels & Hidden/Deleted Channels
    data class CustomStudyChannel(
        val id: String = java.util.UUID.randomUUID().toString(),
        val name: String,
        val handle: String = "",
        val channelId: String = "",
        val subject: String = "General",
        val description: String = "",
        val colorHex: String = "#00F0FF"
    )

    private fun loadCustomHallOfFameChannels(): List<CustomStudyChannel> {
        val jsonStr = prefs.getString("custom_hall_of_fame_channels_json", null) ?: return emptyList()
        return try {
            val list = mutableListOf<CustomStudyChannel>()
            val arr = org.json.JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CustomStudyChannel(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = obj.optString("name", "Custom Channel"),
                        handle = obj.optString("handle", ""),
                        channelId = obj.optString("channelId", ""),
                        subject = obj.optString("subject", "General"),
                        description = obj.optString("description", ""),
                        colorHex = obj.optString("colorHex", "#00F0FF")
                    )
                )
            }
            list.filter { it.name.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private val _customHallOfFameChannels = kotlinx.coroutines.flow.MutableStateFlow<List<CustomStudyChannel>>(loadCustomHallOfFameChannels())
    val customHallOfFameChannels: StateFlow<List<CustomStudyChannel>> = _customHallOfFameChannels.asStateFlow()

    private val _hiddenHallOfFameChannelIds = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(
        prefs.getStringSet("hidden_hall_of_fame_channels", emptySet()) ?: emptySet()
    )
    val hiddenHallOfFameChannelIds: StateFlow<Set<String>> = _hiddenHallOfFameChannelIds.asStateFlow()

    fun addCustomHallOfFameChannel(channel: CustomStudyChannel) {
        val current = _customHallOfFameChannels.value.filter { it.id != channel.id && !it.name.equals(channel.name, ignoreCase = true) }.toMutableList()
        current.add(0, channel)
        _customHallOfFameChannels.value = current
        saveCustomHallOfFameChannels(current)
        // If it was previously hidden, unhide it
        if (_hiddenHallOfFameChannelIds.value.contains(channel.id) || _hiddenHallOfFameChannelIds.value.contains(channel.name)) {
            val unhidden = _hiddenHallOfFameChannelIds.value.toMutableSet()
            unhidden.remove(channel.id)
            unhidden.remove(channel.name)
            prefs.edit().putStringSet("hidden_hall_of_fame_channels", unhidden).apply()
            _hiddenHallOfFameChannelIds.value = unhidden
        }
    }

    fun deleteHallOfFameChannel(channelIdOrName: String) {
        // 1. Remove from custom list if present
        val customCurrent = _customHallOfFameChannels.value.filter { it.id != channelIdOrName && !it.name.equals(channelIdOrName, ignoreCase = true) }
        if (customCurrent.size != _customHallOfFameChannels.value.size) {
            _customHallOfFameChannels.value = customCurrent
            saveCustomHallOfFameChannels(customCurrent)
        }

        // 2. Add to hidden list (covers both custom and default channels)
        val hidden = _hiddenHallOfFameChannelIds.value.toMutableSet()
        hidden.add(channelIdOrName)
        prefs.edit().putStringSet("hidden_hall_of_fame_channels", hidden).apply()
        _hiddenHallOfFameChannelIds.value = hidden
    }

    fun restoreDefaultHallOfFameChannels() {
        prefs.edit().remove("hidden_hall_of_fame_channels").apply()
        _hiddenHallOfFameChannelIds.value = emptySet()
    }

    private fun saveCustomHallOfFameChannels(list: List<CustomStudyChannel>) {
        try {
            val arr = org.json.JSONArray()
            list.forEach { item ->
                val obj = org.json.JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("handle", item.handle)
                    put("channelId", item.channelId)
                    put("subject", item.subject)
                    put("description", item.description)
                    put("colorHex", item.colorHex)
                }
                arr.put(obj)
            }
            prefs.edit().putString("custom_hall_of_fame_channels_json", arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadStudyTubeTodaySeconds(): Long {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val savedDate = prefs.getString("studytube_today_date", "")
        return if (savedDate == todayStr) {
            prefs.getLong("studytube_today_seconds", 0L)
        } else {
            0L
        }
    }

    private val _studyTubeTodaySeconds = MutableStateFlow(loadStudyTubeTodaySeconds())
    val studyTubeTodaySeconds: StateFlow<Long> = _studyTubeTodaySeconds.asStateFlow()

    private val _studyTubeTotalSeconds = MutableStateFlow(prefs.getLong("studytube_total_seconds", 0L))
    val studyTubeTotalSeconds: StateFlow<Long> = _studyTubeTotalSeconds.asStateFlow()

    private val _studyTubeLastWatchedInfo = MutableStateFlow(prefs.getString("studytube_last_watched_info", "No lectures watched yet") ?: "No lectures watched yet")
    val studyTubeLastWatchedInfo: StateFlow<String> = _studyTubeLastWatchedInfo.asStateFlow()

    fun logStudyTubeSession(subject: String, lectureTitle: String, durationSeconds: Long) {
        if (durationSeconds < 5) return // Ignore trivial ticks
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val savedDate = prefs.getString("studytube_today_date", "")
        val currentToday = if (savedDate == todayStr) prefs.getLong("studytube_today_seconds", 0L) else 0L
        val updatedToday = currentToday + durationSeconds
        val updatedTotal = prefs.getLong("studytube_total_seconds", 0L) + durationSeconds
        val infoStr = "$subject • $lectureTitle"

        prefs.edit()
            .putString("studytube_today_date", todayStr)
            .putLong("studytube_today_seconds", updatedToday)
            .putLong("studytube_total_seconds", updatedTotal)
            .putString("studytube_last_watched_info", infoStr)
            .apply()

        _studyTubeTodaySeconds.value = updatedToday
        _studyTubeTotalSeconds.value = updatedTotal
        _studyTubeLastWatchedInfo.value = infoStr

        viewModelScope.launch {
            try {
                dao.insertStudyLog(
                    com.example.data.StudyLog(
                        subject = subject,
                        chapter = lectureTitle.take(60),
                        durationSeconds = durationSeconds.toInt(),
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val _selectedGeminiModel = kotlinx.coroutines.flow.MutableStateFlow(
        com.example.data.GeminiModelManager.getSelectedModel(application)
    )
    val selectedGeminiModel: StateFlow<String> = _selectedGeminiModel

    val geminiQuotaUsage: StateFlow<com.example.data.GeminiRateLimiter.ModelQuotaUsage?> =
        com.example.data.GeminiRateLimiter.currentUsageFlow

    private val _showGeminiQuotaBadge = MutableStateFlow(
        getApplication<Application>().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getBoolean("show_gemini_quota_badge", true)
    )
    val showGeminiQuotaBadge: StateFlow<Boolean> = _showGeminiQuotaBadge.asStateFlow()

    fun setShowGeminiQuotaBadge(enabled: Boolean) {
        _showGeminiQuotaBadge.value = enabled
        getApplication<Application>().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("show_gemini_quota_badge", enabled)
            .apply()
    }

    fun refreshGeminiQuota() {
        val model = _selectedGeminiModel.value
        com.example.data.GeminiRateLimiter.refreshUsageState(getApplication(), model)
    }

    private val _geminiModels = MutableStateFlow<List<com.example.data.GeminiModel>>(
        com.example.data.GeminiModelManager.getCachedModels(application)
    )
    val geminiModels: StateFlow<List<com.example.data.GeminiModel>> = _geminiModels.asStateFlow()

    private val _isGeminiLoadingModels = MutableStateFlow(false)
    val isGeminiLoadingModels: StateFlow<Boolean> = _isGeminiLoadingModels.asStateFlow()

    fun fetchGeminiLiveModels(onResult: ((Boolean, String) -> Unit)? = null) {
        val app = getApplication<android.app.Application>()
        val key = com.example.data.GeminiChatAssistant.getApiKey(app)
        viewModelScope.launch {
            _isGeminiLoadingModels.value = true
            val res = com.example.data.GeminiModelManager.fetchLiveModels(app, key)
            _isGeminiLoadingModels.value = false
            res.fold(
                onSuccess = { models ->
                    if (models.isNotEmpty()) {
                        _geminiModels.value = models
                    }
                    onResult?.invoke(true, "✅ Synced ${models.size} live Gemini models from Google!")
                },
                onFailure = { err ->
                    val fallback = com.example.data.GeminiModelManager.popularGeminiModels
                    _geminiModels.value = fallback
                    val msg = if (key.isBlank()) {
                        "⚠️ Please enter your Gemini API Key in Settings first."
                    } else {
                        "Sync error: ${err.message}. Showing ${fallback.size} default models."
                    }
                    onResult?.invoke(false, msg)
                }
            )
        }
    }

    // In-App PDF Reader state
    private val _currentPdfReaderUri = kotlinx.coroutines.flow.MutableStateFlow<Uri?>(null)
    val currentPdfReaderUri: StateFlow<Uri?> = _currentPdfReaderUri

    private val _currentPdfReaderTitle = kotlinx.coroutines.flow.MutableStateFlow<String?>("PDF Document")
    val currentPdfReaderTitle: StateFlow<String?> = _currentPdfReaderTitle

    private val _currentPdfReaderSubject = kotlinx.coroutines.flow.MutableStateFlow<String?>("Physics")
    val currentPdfReaderSubject: StateFlow<String?> = _currentPdfReaderSubject

    private val _pdfOpenTrigger = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
    val pdfOpenTrigger: StateFlow<Long?> = _pdfOpenTrigger

    fun detectSubjectFromPdf(title: String?): String {
        val available = com.example.data.ExamSyllabusDatabase.getSubjectsFor(activeExamGoal.value).ifEmpty {
            listOf("Physics", "Chemistry", "Biology")
        }
        if (title.isNullOrBlank()) return available.firstOrNull() ?: "Physics"
        val lower = title.lowercase()

        // 1. Direct match with syllabus subjects
        for (subj in available) {
            if (lower.contains(subj.lowercase())) return subj
        }

        // 2. Keyword detection
        val isPhysics = listOf("physic", "kinematic", "motion", "mechanic", "gravitat", "thermo", "electro", "magnet", "optic", "ray", "wave", "fluid", "current", "capacit", "oscillation", "semiconductor", "nucleus", "atom", "rotat", "work", "power", "energy", "eduniti", "hc verma", "d c pandey", "dc pandey", "irodov", "sl arora").any { lower.contains(it) }
        if (isPhysics) {
            val matched = available.firstOrNull { it.contains("Physics", ignoreCase = true) }
            if (matched != null) return matched
        }

        val isChemistry = listOf("chem", "organic", "inorganic", "mole", "atomic", "bonding", "equilibrium", "redox", "coordination", "p-block", "d-block", "hydrocarbon", "aldehyde", "ketone", "alcohol", "ether", "amine", "biomolecule", "polymer", "solid state", "solution", "electrochem", "kinetic", "surface", "metallurgy", "periodic", "ncert chem", "ms chouhan", "himanshu pandey").any { lower.contains(it) }
        if (isChemistry) {
            val matched = available.firstOrNull { it.contains("Chemistry", ignoreCase = true) }
            if (matched != null) return matched
        }

        val isBiology = listOf("bio", "botany", "zoology", "cell", "genetics", "evolution", "morphology", "anatomy", "plant", "animal", "human physio", "reproduction", "ecology", "biotech", "microbe", "living world", "biological", "photosynthesis", "respiration", "endocrine", "neural", "circulation", "digestion", "ncert bio", "dr ali", "fingertips").any { lower.contains(it) }
        if (isBiology) {
            if (lower.contains("botan") && available.any { it.contains("Botany", ignoreCase = true) }) {
                return available.first { it.contains("Botany", ignoreCase = true) }
            }
            if (lower.contains("zool") && available.any { it.contains("Zoology", ignoreCase = true) }) {
                return available.first { it.contains("Zoology", ignoreCase = true) }
            }
            val bioSubj = available.firstOrNull { it.contains("Bio", ignoreCase = true) }
            if (bioSubj != null) return bioSubj
        }

        val isMath = listOf("math", "calculus", "algebra", "trig", "coordinate", "matrix", "determinant", "integral", "derivative", "vector", "probability", "permutation", "combination", "differential", "cengage", "rd sharma", "arihant math").any { lower.contains(it) }
        if (isMath) {
            val matched = available.firstOrNull { it.contains("Math", ignoreCase = true) }
            if (matched != null) return matched
        }

        return available.firstOrNull() ?: "Physics"
    }

    fun openPdfInReader(uri: Uri, title: String? = null, subject: String? = null) {
        _currentPdfReaderUri.value = uri
        val resolvedTitle = title ?: uri.lastPathSegment ?: "PDF Document"
        _currentPdfReaderTitle.value = resolvedTitle
        _currentPdfReaderSubject.value = subject ?: detectSubjectFromPdf(resolvedTitle)
        _pdfOpenTrigger.value = System.currentTimeMillis()
    }

    fun setPdfReaderSubject(subject: String) {
        _currentPdfReaderSubject.value = subject
    }

    fun clearPdfReaderUri() {
        _currentPdfReaderUri.value = null
        _currentPdfReaderTitle.value = null
        _currentPdfReaderSubject.value = null
    }

    fun setSelectedGeminiModel(model: String, switchProvider: Boolean = false) {
        val trimmed = model.trim()
        if (trimmed.isNotBlank()) {
            prefs.edit().putString("selected_gemini_model", trimmed).apply()
            com.example.data.GeminiModelManager.setSelectedModel(getApplication(), trimmed)
            _selectedGeminiModel.value = trimmed
            if (switchProvider) {
                setAiProvider(AiProvider.NATIVE_GEMINI)
            }
            com.example.data.GeminiRateLimiter.refreshUsageState(getApplication(), trimmed)
        }
    }

    fun saveYoutubeApiKey(key: String) {
        prefs.edit().putString("youtube_api_key", key.trim()).apply()
        _youtubeApiKey.value = key.trim()
    }

    fun saveCustomApiKeys(key1: String, key2: String, key3: String) {
        prefs.edit()
            .putString("gemini_api_key_1", key1.trim())
            .putString("gemini_api_key_2", key2.trim())
            .putString("gemini_api_key_3", key3.trim())
            .apply()
        _geminiApiKey1.value = key1.trim()
        _geminiApiKey2.value = key2.trim()
        _geminiApiKey3.value = key3.trim()
    }



    // Weekly Question Targets State
    private val _weeklyPhysicsTarget = kotlinx.coroutines.flow.MutableStateFlow(prefs.getInt("weekly_p_target", 300))
    val weeklyPhysicsTarget: StateFlow<Int> = _weeklyPhysicsTarget

    private val _weeklyChemTarget = kotlinx.coroutines.flow.MutableStateFlow(prefs.getInt("weekly_c_target", 300))
    val weeklyChemTarget: StateFlow<Int> = _weeklyChemTarget

    private val _weeklyBioTarget = kotlinx.coroutines.flow.MutableStateFlow(prefs.getInt("weekly_b_target", 600))
    val weeklyBioTarget: StateFlow<Int> = _weeklyBioTarget

    private val _weeklyPhysicsSolved = kotlinx.coroutines.flow.MutableStateFlow(prefs.getInt("weekly_p_solved", 0))
    val weeklyPhysicsSolved: StateFlow<Int> = _weeklyPhysicsSolved

    private val _weeklyChemSolved = kotlinx.coroutines.flow.MutableStateFlow(prefs.getInt("weekly_c_solved", 0))
    val weeklyChemSolved: StateFlow<Int> = _weeklyChemSolved

    private val _weeklyBioSolved = kotlinx.coroutines.flow.MutableStateFlow(prefs.getInt("weekly_b_solved", 0))
    val weeklyBioSolved: StateFlow<Int> = _weeklyBioSolved

    private val _syllabusAnalysisLoading = kotlinx.coroutines.flow.MutableStateFlow(false)
    val syllabusAnalysisLoading: StateFlow<Boolean> = _syllabusAnalysisLoading

    private val _syllabusAnalysisResult = kotlinx.coroutines.flow.MutableStateFlow("")
    val syllabusAnalysisResult: StateFlow<String> = _syllabusAnalysisResult
    
    private val _syllabusAnalyzerText = kotlinx.coroutines.flow.MutableStateFlow("")
    val syllabusAnalyzerText: StateFlow<String> = _syllabusAnalyzerText

    fun updateSyllabusAnalyzerText(text: String) {
        _syllabusAnalyzerText.value = text
    }

    private val _syllabusAnalyzerUri = kotlinx.coroutines.flow.MutableStateFlow<Uri?>(null)
    val syllabusAnalyzerUri: StateFlow<Uri?> = _syllabusAnalyzerUri

    fun updateSyllabusAnalyzerUri(uri: Uri?) {
        _syllabusAnalyzerUri.value = uri
    }
    
    private val _mockTestSelectedTab = kotlinx.coroutines.flow.MutableStateFlow(0)
    val mockTestSelectedTab: StateFlow<Int> = _mockTestSelectedTab
    
    fun updateMockTestSelectedTab(tab: Int) {
        _mockTestSelectedTab.value = tab
    }

    fun analyzeSyllabus(context: Context, syllabusText: String, fileUriStr: String?, fileType: String?) {
        if (syllabusText.isBlank() && fileUriStr.isNullOrBlank()) return
        viewModelScope.launch {
            _syllabusAnalysisLoading.value = true
            _syllabusAnalysisResult.value = ""
            val result = com.example.data.GeminiChatAssistant.analyzeSyllabus(
                context, syllabusText, fileUriStr, fileType
            )
            _syllabusAnalysisResult.value = result.getOrNull() ?: ("Error: " + (result.exceptionOrNull()?.localizedMessage ?: "Failed to analyze syllabus"))
            _syllabusAnalysisLoading.value = false
        }
    }

    fun clearSyllabusAnalysis() {
        _syllabusAnalysisResult.value = ""
    }

    fun saveWeeklyPractice(pT: Int, cT: Int, bT: Int, pS: Int, cS: Int, bS: Int) {
        prefs.edit()
            .putInt("weekly_p_target", pT)
            .putInt("weekly_c_target", cT)
            .putInt("weekly_b_target", bT)
            .putInt("weekly_p_solved", pS)
            .putInt("weekly_c_solved", cS)
            .putInt("weekly_b_solved", bS)
            .apply()
        _weeklyPhysicsTarget.value = pT
        _weeklyChemTarget.value = cT
        _weeklyBioTarget.value = bT
        _weeklyPhysicsSolved.value = pS
        _weeklyChemSolved.value = cS
        _weeklyBioSolved.value = bS
    }

    private val dao = AppDatabase.getDatabase(application).appDao()

    val studyLogs: StateFlow<List<StudyLog>> = dao.getAllStudyLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<Goal>> = dao.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    
    val notes: StateFlow<List<com.example.data.Note>> = dao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addNote(title: String, content: String, color: Long) {
        viewModelScope.launch {
            dao.insertNote(com.example.data.Note(title = title, content = content, color = color))
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun updateNote(note: com.example.data.Note) {
        viewModelScope.launch {
            dao.insertNote(note)
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            dao.deleteNote(id)
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }
    
    // Password / App Lock Logic & Security Questions
    val defaultSecurityQuestions = listOf(
        "What is your favourite food?",
        "What was the name of your first school?",
        "What is your pet's name?",
        "What is your birth city?",
        "What is your favourite subject?",
        "What is your dream medical college?"
    )

    private val _appPassword = MutableStateFlow<String?>(prefs.getString("app_lock_password", null))
    val appPassword: StateFlow<String?> = _appPassword.asStateFlow()

    private val _habitPassword = MutableStateFlow<String?>(prefs.getString("habit_lock_password", null))
    val habitPassword: StateFlow<String?> = _habitPassword.asStateFlow()

    fun setHabitPassword(password: String?) {
        if (password.isNullOrBlank()) {
            prefs.edit().remove("habit_lock_password").apply()
            _habitPassword.value = null
        } else {
            prefs.edit().putString("habit_lock_password", password.trim()).apply()
            _habitPassword.value = password.trim()
        }
    }

    private val _securityQuestion = MutableStateFlow<String>(
        prefs.getString("app_lock_security_question", defaultSecurityQuestions[0]) ?: defaultSecurityQuestions[0]
    )
    val securityQuestion: StateFlow<String> = _securityQuestion.asStateFlow()

    private val _securityAnswer = MutableStateFlow<String?>(prefs.getString("app_lock_security_answer", null))
    val securityAnswer: StateFlow<String?> = _securityAnswer.asStateFlow()

    fun setAppPassword(password: String?) {
        if (password.isNullOrBlank()) {
            removeAppPassword()
        } else {
            prefs.edit().putString("app_lock_password", password.trim()).apply()
            _appPassword.value = password.trim()
        }
    }

    fun setSecurityLock(password: String?, question: String?, answer: String?) {
        val editor = prefs.edit()
        if (password.isNullOrBlank()) {
            editor.remove("app_lock_password")
            .remove("habit_lock_password")
            editor.remove("app_lock_security_question")
            editor.remove("app_lock_security_answer")
            _appPassword.value = null
            _habitPassword.value = null
            _securityQuestion.value = defaultSecurityQuestions[0]
            _securityAnswer.value = null
        } else {
            editor.putString("app_lock_password", password.trim())
            _appPassword.value = password.trim()
            if (!question.isNullOrBlank()) {
                editor.putString("app_lock_security_question", question.trim())
                _securityQuestion.value = question.trim()
            }
            if (!answer.isNullOrBlank()) {
                editor.putString("app_lock_security_answer", answer.trim())
                _securityAnswer.value = answer.trim()
            }
        }
        editor.apply()
    }

    fun removeAppPassword() {
        prefs.edit()
            .remove("app_lock_password")
            .remove("habit_lock_password")
            .remove("app_lock_security_question")
            .remove("app_lock_security_answer")
            .apply()
        _appPassword.value = null
            _habitPassword.value = null
        _securityAnswer.value = null
    }

    fun verifySecurityAnswer(input: String): Boolean {
        val currentAnswer = _securityAnswer.value?.trim() ?: return false
        return input.trim().equals(currentAnswer, ignoreCase = true)
    }

    val habits: StateFlow<List<Habit>> = dao.getAllHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleHabitDay(habit: Habit, dateStr: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dates = habit.completedDates.split(",").filter { it.isNotBlank() }.toMutableSet()
            if (dates.contains(dateStr)) {
                dates.remove(dateStr)
            } else {
                dates.add(dateStr)
            }
            dao.insertHabit(habit.copy(completedDates = dates.joinToString(",")))
        }
    }

    private val _detoxMotivationLoading = MutableStateFlow(false)
    val detoxMotivationLoading: StateFlow<Boolean> = _detoxMotivationLoading
    private val _detoxMotivationResult = MutableStateFlow<String?>(null)
    val detoxMotivationResult: StateFlow<String?> = _detoxMotivationResult

    fun getDetoxMotivation(context: android.content.Context, habitName: String, userFeeling: String? = null) {
        viewModelScope.launch {
            _detoxMotivationLoading.value = true
            _detoxMotivationResult.value = null
            val result = GeminiChatAssistant.generateDetoxMotivation(context, habitName, userFeeling)
            if (result.isSuccess) {
                _detoxMotivationResult.value = result.getOrNull()
            } else {
                _detoxMotivationResult.value = "Failed to load motivation. Stay strong!"
            }
            _detoxMotivationLoading.value = false
        }
    }
    
    fun clearDetoxMotivation() {
        _detoxMotivationResult.value = null
    }

    fun addHabit(name: String, iconEmoji: String, isGoodHabit: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertHabit(Habit(name = name, iconEmoji = iconEmoji, isGoodHabit = isGoodHabit))
        }
    }

    fun deleteHabit(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.deleteHabit(id)
        }
    }

    val dppItems: StateFlow<List<DppItem>> = dao.getAllDppItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mockTests: StateFlow<List<MockTest>> = dao.getAllMockTests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduledMockTests: StateFlow<List<ScheduledMockTest>> = dao.getAllScheduledMockTests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookProgressions: StateFlow<List<BookProgression>> = dao.getAllBookProgressions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dynamicChecklistTopics: kotlinx.coroutines.flow.StateFlow<List<com.example.data.DynamicChecklistTopic>> = dao.getAllDynamicChecklistTopics()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveDynamicChecklistTopic(topic: com.example.data.DynamicChecklistTopic) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            dao.insertDynamicChecklistTopic(topic)
        }
    }

    fun deleteDynamicChecklistTopic(id: Long) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            dao.deleteDynamicChecklistTopic(id)
        }
    }

    fun deleteDynamicChecklistPhase(phaseTitle: String, subject: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            dao.deleteDynamicChecklistPhase(phaseTitle, subject)
        }
    }

    val edunitiTargets: StateFlow<List<EdunitiTarget>> = dao.getAllEdunitiTargets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashcardDecks: StateFlow<List<FlashcardDeck>> = dao.getAllFlashcardDecks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashcardItems: StateFlow<List<FlashcardItem>> = dao.getAllFlashcardItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun recordSolvedQuestions(subject: String, count: Int, date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val currentList = dailyPractices.value
            val existing = currentList.find { it.date == date } ?: DailyPractice(date = date)
            val cleanSubject = subject.trim().lowercase()
            val updated = when {
                cleanSubject.contains("phy") -> existing.copy(physicsSolved = (existing.physicsSolved + count).coerceAtLeast(0))
                cleanSubject.contains("chem") -> existing.copy(chemistrySolved = (existing.chemistrySolved + count).coerceAtLeast(0))
                cleanSubject.contains("bio") || cleanSubject.contains("botan") || cleanSubject.contains("zool") -> existing.copy(biologySolved = (existing.biologySolved + count).coerceAtLeast(0))
                else -> existing.copy(physicsSolved = (existing.physicsSolved + count).coerceAtLeast(0))
            }
            dao.insertDailyPractice(updated)
        }
    }

    fun importAllNeetChecklist(clearExisting: Boolean = false) {
        importAllChecklistForExam(ExamCategory.NEET, clearExisting)
    }

    fun importAllChecklistForExam(examGoalStr: String, clearExisting: Boolean = false) {
        val category = com.example.data.ExamSyllabusDatabase.parseExamCategory(examGoalStr)
        importAllChecklistForExam(category, clearExisting)
    }

    fun importAllChecklistForExam(exam: ExamCategory = ExamCategory.NEET, clearExisting: Boolean = false) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (clearExisting) {
                val existing = dao.getAllDynamicChecklistTopics().firstOrNull() ?: emptyList()
                existing.forEach { dao.deleteDynamicChecklistTopic(it.id) }
            }
            val defaultList = com.example.data.ExamSyllabusDatabase.getFullNeetChecklist()
            val current = if (clearExisting) emptyList() else (dao.getAllDynamicChecklistTopics().firstOrNull() ?: emptyList())
            val existingKeys = current.map { "${it.subject}-${it.topicName}" }.toSet()
            defaultList.forEach { topic ->
                if (!existingKeys.contains("${topic.subject}-${topic.topicName}")) {
                    dao.insertDynamicChecklistTopic(topic)
                }
            }
        }
    }

    private suspend fun seedDefaultFlashcards() {
        importAllPreMadeDecksInternal()
    }

    fun importAllPreMadeDecks() {
        viewModelScope.launch {
            importAllPreMadeDecksInternal()
        }
    }

    private suspend fun importAllPreMadeDecksInternal() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val existingDecks = dao.getAllFlashcardDecks().firstOrNull() ?: emptyList()

        for (seed in com.example.data.DefaultFlashcards.preMadeDecks) {
            val matchingDeck = existingDecks.find {
                it.name.equals(seed.name, ignoreCase = true) ||
                (seed.category.equals("Physics", ignoreCase = true) && it.name.contains("Physics", ignoreCase = true)) ||
                (seed.category.equals("Chemistry", ignoreCase = true) && it.name.contains("Chemistry", ignoreCase = true)) ||
                (seed.category.equals("Biology", ignoreCase = true) && (it.name.contains("Biology", ignoreCase = true) || it.name.contains("NCERT", ignoreCase = true)))
            }
            if (matchingDeck == null) {
                val deckId = dao.insertFlashcardDeck(
                    FlashcardDeck(name = seed.name, icon = seed.icon, category = seed.category)
                )
                for ((front, back) in seed.cards) {
                    dao.insertFlashcardItem(
                        FlashcardItem(
                            deckId = deckId,
                            front = front,
                            back = back,
                            due = todayStr
                        )
                    )
                }
            } else if (matchingDeck.category == "Other" || matchingDeck.category.isBlank()) {
                // Update existing deck category so subject filters work
                dao.insertFlashcardDeck(matchingDeck.copy(category = seed.category))
            }
        }
    }

    fun saveBookProgression(progression: BookProgression) {
        viewModelScope.launch {
            dao.insertBookProgression(progression)
        }
    }

    fun saveEdunitiTarget(target: EdunitiTarget) {
        viewModelScope.launch {
            dao.insertEdunitiTarget(target)
        }
    }

    fun deleteEdunitiTarget(id: Long) {
        viewModelScope.launch {
            dao.deleteEdunitiTarget(id)
        }
    }

    fun addScheduledMockTest(title: String, date: String, phySyllabus: String = "", chemSyllabus: String = "", bioSyllabus: String = "", notes: String = "", isPinned: Boolean = false) {
        viewModelScope.launch {
            dao.insertScheduledMockTest(ScheduledMockTest(
                title = title,
                scheduledDate = date,
                physicsSyllabus = phySyllabus,
                chemistrySyllabus = chemSyllabus,
                biologySyllabus = bioSyllabus,
                syllabusNotes = notes,
                isPinned = isPinned
            ))
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun updateScheduledMockTest(id: Int, title: String, date: String, phySyllabus: String = "", chemSyllabus: String = "", bioSyllabus: String = "", notes: String = "", isCompleted: Boolean = false, isPinned: Boolean = false) {
        viewModelScope.launch {
            dao.insertScheduledMockTest(ScheduledMockTest(
                id = id,
                title = title,
                scheduledDate = date,
                physicsSyllabus = phySyllabus,
                chemistrySyllabus = chemSyllabus,
                biologySyllabus = bioSyllabus,
                syllabusNotes = notes,
                isCompleted = isCompleted,
                isPinned = isPinned
            ))
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun togglePinScheduledMockTest(test: ScheduledMockTest) {
        viewModelScope.launch {
            dao.insertScheduledMockTest(test.copy(isPinned = !test.isPinned))
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun deleteScheduledMockTest(id: Int) {
        viewModelScope.launch {
            dao.deleteScheduledMockTest(id)
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating

    fun getChatMessages(subject: String, chatType: String): Flow<List<ChatMessageEntity>> {
        return dao.getChatMessages(subject, chatType)
    }

    fun sendChatMessage(
        context: android.content.Context? = null,
        subject: String,
        chatType: String,
        text: String,
        mediaUri: String? = null,
        mediaType: String? = null,
        mediaName: String? = null,
        sender: String = "user"
    ) {
        viewModelScope.launch {
            val timeStr = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            dao.insertChatMessage(
                com.example.data.ChatMessageEntity(
                    subject = subject,
                    chatType = chatType,
                    text = text,
                    mediaUri = mediaUri,
                    mediaType = mediaType,
                    mediaName = mediaName,
                    sender = sender,
                    time = timeStr
                )
            )

            if (sender == "user" && context != null && !_isAiGenerating.value) {
                _isAiGenerating.value = true
                val recentMessages = dao.getChatMessages(subject, chatType).firstOrNull()?.takeLast(10) ?: emptyList()
                val historyText = recentMessages.joinToString("\n") { 
                    val role = if (it.sender == "user") "Student" else "AI"
                    "[${role}]: ${it.text}"
                }

                val currentProvider = _aiProvider.value
                val activeModelName = when (currentProvider) {
                    com.example.data.AiProvider.NATIVE_GEMINI -> com.example.data.GeminiModelManager.getSelectedModel(context)
                    com.example.data.AiProvider.GROQ -> _groqSelectedModel.value
                    com.example.data.AiProvider.OPENROUTER -> _openrouterSelectedModel.value
                    com.example.data.AiProvider.CLOUDFLARE -> _cloudflareSelectedModel.value
                }
                val providerLabel = when (currentProvider) {
                    com.example.data.AiProvider.NATIVE_GEMINI -> "Google Gemini"
                    com.example.data.AiProvider.GROQ -> "Groq LPU Engine"
                    com.example.data.AiProvider.OPENROUTER -> "OpenRouter AI Hub"
                    com.example.data.AiProvider.CLOUDFLARE -> "Cloudflare Workers AI"
                }

                val prompt = """
                    You are Lakshya AI Guardian, an elite master tutor and doubt solver for NEET/JEE.
                    The student is studying subject: ${subject}. (Chat context: ${chatType}).
                    
                    CRITICAL SYSTEM ENGINE & ACTIVE MODEL IDENTITY:
                    - Active AI Provider: $providerLabel
                    - Active Model Running Right Now: "$activeModelName"
                    - IF THE STUDENT ASKS: "kaun sa model hai", "kon sa model", "which model", "what model are you", "model name", "gemini ka kaun sa model hai", "aap kaun sa model use kar rahe ho", or asks about your version/model in any language (Hindi, Hinglish, English):
                      YOU MUST DIRECTLY, PROUDLY AND EXPLICITLY TELL THEM:
                      "Main abhi **$activeModelName** (${if (currentProvider == com.example.data.AiProvider.NATIVE_GEMINI) "Google Gemini" else providerLabel}) model par run kar raha hoon."
                      Mention that this model is currently active in their Lakshya AI setup and they can switch to any other model (like Gemini 3.8 Flash, 3.5 Flash, 3.1 Pro, Flash Lite) anytime using the top model badge.
                    
                    CONCEPTUAL EXPLANATION & SCIENTIFIC RIGOR:
                    - Explain scientific and numerical problems with 100% academic depth, NCERT alignment, and precision.
                    - When explaining structures or circuits, break them down clearly into labeled components, functions, formulas, and tabular summaries.

                    Here is the recent conversation history:
                    ${historyText}
                    
                    Respond concisely to the student's latest message, providing academic guidance, clearing doubts, and acting as their elite tutor.
                    Formatting guidelines:
                    - When displaying tabular comparisons or rules, use standard Markdown table format (| Header 1 | Header 2 | with divider |:---|:---|).
                    - For formulas and scientific notation, use clean LaTeX ($...$ or $$...$$) or clear Unicode symbols (e.g., →, ⇌, Δ, Ω, μ, θ, ², ³, ₁, ₂, v_rms, B_ind).
                """.trimIndent()
                
                val result = com.example.data.GeminiChatAssistant.executeTestingPrompt(
                    context = context,
                    prompt = prompt,
                    mediaUriStr = mediaUri,
                    mediaType = mediaType,
                    targetModel = if (currentProvider == com.example.data.AiProvider.NATIVE_GEMINI) activeModelName else null
                )
                handleAiResponse(result, subject, chatType, text)
            }
        }
    }

    fun updateChatMessage(
        id: Long,
        text: String,
        mediaUri: String? = null,
        mediaType: String? = null,
        mediaName: String? = null
    ) {
        viewModelScope.launch {
            dao.updateChatMessage(id, text, mediaUri, mediaType, mediaName)
        }
    }

    fun deleteChatMessage(id: Long) {
        viewModelScope.launch {
            dao.deleteChatMessage(id)
        }
    }

    fun clearAllChatMessages() {
        viewModelScope.launch {
            dao.deleteAllChatMessages()
        }
    }

    fun analyzeErrorWithGemini(
        context: Context,
        subject: String,
        chatType: String,
        noteText: String,
        mediaUri: String? = null,
        mediaType: String? = null
    ) {
        if (_isAiGenerating.value) return
        viewModelScope.launch {
            _isAiGenerating.value = true
            val activeModel = if (_aiProvider.value == com.example.data.AiProvider.NATIVE_GEMINI) _selectedGeminiModel.value else null
            val result = com.example.data.GeminiChatAssistant.generateErrorAnalysis(
                context = context,
                subject = subject,
                noteText = noteText,
                mediaUriStr = mediaUri,
                mediaType = mediaType,
                targetModel = activeModel
            )
            handleAiResponse(result, subject, chatType, noteText)
        }
    }

    fun solveDoubtWithGemini(
        context: Context,
        subject: String,
        chatType: String,
        doubtText: String,
        mediaUri: String? = null,
        mediaType: String? = null
    ) {
        if (_isAiGenerating.value) return
        viewModelScope.launch {
            _isAiGenerating.value = true
            val activeModel = if (_aiProvider.value == com.example.data.AiProvider.NATIVE_GEMINI) _selectedGeminiModel.value else null
            val result = com.example.data.GeminiChatAssistant.solveDoubt(
                context = context,
                subject = subject,
                doubtText = doubtText,
                mediaUriStr = mediaUri,
                mediaType = mediaType,
                targetModel = activeModel
            )
            handleAiResponse(result, subject, chatType, doubtText)
        }
    }

    fun explainConceptWithGemini(
        context: Context,
        subject: String,
        chatType: String,
        conceptText: String
    ) {
        if (_isAiGenerating.value) return
        viewModelScope.launch {
            _isAiGenerating.value = true
            val activeModel = if (_aiProvider.value == com.example.data.AiProvider.NATIVE_GEMINI) _selectedGeminiModel.value else null
            val result = com.example.data.GeminiChatAssistant.explainConcept(
                context = context,
                subject = subject,
                conceptText = conceptText,
                targetModel = activeModel
            )
            handleAiResponse(result, subject, chatType, conceptText)
        }
    }

    private fun handleAiResponse(result: Result<String>, subject: String, chatType: String, userQuery: String? = null) {
        _isAiGenerating.value = false
        val timeStr = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())
        result.onSuccess { reply ->
            viewModelScope.launch {
                dao.insertChatMessage(
                    com.example.data.ChatMessageEntity(
                        subject = subject,
                        chatType = chatType,
                        text = reply,
                        sender = "ai",
                        time = timeStr
                    )
                )
            }
        }.onFailure { err ->
            viewModelScope.launch {
                val app = getApplication<Application>()
                val currentProvider = _aiProvider.value
                val activeModelName = when (currentProvider) {
                    com.example.data.AiProvider.NATIVE_GEMINI -> com.example.data.GeminiModelManager.getSelectedModel(app)
                    com.example.data.AiProvider.GROQ -> _groqSelectedModel.value
                    com.example.data.AiProvider.OPENROUTER -> _openrouterSelectedModel.value
                    com.example.data.AiProvider.CLOUDFLARE -> _cloudflareSelectedModel.value
                }
                val providerLabel = when (currentProvider) {
                    com.example.data.AiProvider.NATIVE_GEMINI -> "Google Gemini"
                    com.example.data.AiProvider.GROQ -> "Groq LPU Engine"
                    com.example.data.AiProvider.OPENROUTER -> "OpenRouter AI Hub"
                    com.example.data.AiProvider.CLOUDFLARE -> "Cloudflare Workers AI"
                }

                val lowerQuery = userQuery?.trim()?.lowercase() ?: ""
                val isModelQuery = lowerQuery.contains("model") || lowerQuery.contains("gemini") || lowerQuery.contains("geemini")

                val fallbackText = if (isModelQuery) {
                    "Main abhi **$activeModelName** (${if (currentProvider == com.example.data.AiProvider.NATIVE_GEMINI) "Google Gemini" else providerLabel}) model par configured hoon.\n\n*(Note: AI Server response note: ${err.message}. Agar API key missing hai toh Settings me jaakar Gemini API key configure karein.)*"
                } else {
                    "⚠️ **LAKSHYA AI Error:** ${err.message}"
                }

                dao.insertChatMessage(
                    com.example.data.ChatMessageEntity(
                        subject = subject,
                        chatType = chatType,
                        text = fallbackText,
                        sender = "ai",
                        time = timeStr
                    )
                )
            }
        }
    }

    private fun saveGuardianMessages(messages: List<GuardianMessage>) {
        val arr = org.json.JSONArray()
        messages.forEach { msg ->
            val obj = org.json.JSONObject().apply {
                put("id", msg.id)
                put("sender", msg.sender)
                put("text", msg.text)
                put("timestamp", msg.timestamp)
            }
            arr.put(obj)
        }
        prefs.edit().putString("ai_guardian_messages_v2", arr.toString()).apply()
    }

    private fun loadGuardianMessages(): List<GuardianMessage> {
        val jsonStr = prefs.getString("ai_guardian_messages_v2", null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                val list = mutableListOf<GuardianMessage>()
                val arr = org.json.JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        GuardianMessage(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            sender = obj.optString("sender", "ai"),
                            text = obj.optString("text", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val oldReport = prefs.getString("ai_advisor_report", null)
        if (!oldReport.isNullOrBlank() && !oldReport.contains("actively monitoring")) {
            return listOf(GuardianMessage(sender = "ai", text = oldReport))
        }
        return emptyList()
    }

    private val _isAiAdvisorAnalyzing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isAiAdvisorAnalyzing: StateFlow<Boolean> = _isAiAdvisorAnalyzing

    private val _aiAdvisorMessages = kotlinx.coroutines.flow.MutableStateFlow<List<GuardianMessage>>(loadGuardianMessages())
    val aiAdvisorMessages: StateFlow<List<GuardianMessage>> = _aiAdvisorMessages

    val aiAdvisorReport: StateFlow<String> = _aiAdvisorMessages.map { list ->
        list.lastOrNull { it.sender == "ai" }?.text ?: "Your LAKSHYA AI Guardian is actively monitoring your performance graph... Click the button below to request full diagnostic report! 🕵️‍♂️🔮"
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "Your LAKSHYA AI Guardian is actively monitoring your performance graph... Click the button below to request full diagnostic report! 🕵️‍♂️🔮"
    )

    private val _isAiAdvisorReplying = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isAiAdvisorReplying: StateFlow<Boolean> = _isAiAdvisorReplying

    fun clearAiAdvisorMessages() {
        _aiAdvisorMessages.value = emptyList()
        prefs.edit().remove("ai_guardian_messages_v2").remove("ai_advisor_report").apply()
    }

    fun runAiAdvisorAnalysis(context: Context) {
        if (_isAiAdvisorAnalyzing.value) return
        viewModelScope.launch {
            _isAiAdvisorAnalyzing.value = true
            val mTests = mockTests.value
            val dpPractices = dailyPractices.value
            val sLogs = studyLogs.value
            val gList = goals.value
            val cTopics = dynamicChecklistTopics.value
            val cState = edunitiChecklistState.value
            val dList = dppItems.value
            val sMockTests = scheduledMockTests.value
            
            val stTodayHrs = _studyTubeTodaySeconds.value / 3600f
            val stTotalHrs = _studyTubeTotalSeconds.value / 3600f
            val stLastWatched = _studyTubeLastWatchedInfo.value
            
            val result = com.example.data.GeminiChatAssistant.analyzePerformanceAndDownfall(
                context = context,
                mockTests = mTests,
                practices = dpPractices,
                logs = sLogs,
                goals = gList,
                checklistTopics = cTopics,
                checklistState = cState,
                dppItems = dList,
                scheduledMockTests = sMockTests,
                studyTubeTodayHours = stTodayHrs,
                studyTubeTotalHours = stTotalHrs,
                studyTubeLastWatched = stLastWatched
            )
            
            _isAiAdvisorAnalyzing.value = false
            result.onSuccess { report ->
                val newMsg = GuardianMessage(sender = "ai", text = report)
                val updatedList = listOf(newMsg)
                _aiAdvisorMessages.value = updatedList
                saveGuardianMessages(updatedList)
            }.onFailure { err ->
                val errMsg = GuardianMessage(
                    sender = "ai", 
                    text = "⚠️ **Analysis Failed:** ${err.message}\n\nPlease verify you have set up your custom Lakshya AI key or have active internet connection, and try again!"
                )
                val updatedList = _aiAdvisorMessages.value + errMsg
                _aiAdvisorMessages.value = updatedList
                saveGuardianMessages(updatedList)
            }
        }
    }

    fun submitFeedbackToAdvisor(context: Context, userFeedback: String) {
        if (_isAiAdvisorReplying.value || userFeedback.isBlank()) return
        val userMsg = GuardianMessage(sender = "user", text = userFeedback)
        val currentList = _aiAdvisorMessages.value + userMsg
        _aiAdvisorMessages.value = currentList
        saveGuardianMessages(currentList)

        // Autonomous proactive timer execution if user asks to set/start timer
        val lowerFeedback = userFeedback.lowercase(Locale.ROOT)
        val isTimerRequest = lowerFeedback.contains("timer") && (
            lowerFeedback.contains("laga") || lowerFeedback.contains("start") || 
            lowerFeedback.contains("chala") || lowerFeedback.contains("set") || 
            lowerFeedback.contains("shuru") || lowerFeedback.contains("on")
        )
        if (isTimerRequest) {
            val minutes = Regex("(\\d+)\\s*(min|minute|m)?").find(lowerFeedback)?.groupValues?.get(1)?.toIntOrNull() ?: 45
            val subject = when {
                lowerFeedback.contains("physic") -> "Physics"
                lowerFeedback.contains("chem") -> "Chemistry"
                lowerFeedback.contains("bio") -> "Biology"
                else -> _timerSubject.value
            }
            guardianStartTimer(subject = subject, chapter = "Guardian Sprint", durationMinutes = minutes)
        }

        viewModelScope.launch {
            _isAiAdvisorReplying.value = true
            val historyText = currentList.takeLast(6).joinToString("\n\n") { msg ->
                if (msg.sender == "user") "Student: ${msg.text}" else "Advisor: ${msg.text}"
            }
            val stTodayHrs = String.format(Locale.US, "%.1f", _studyTubeTodaySeconds.value / 3600f)
            val stTotalHrs = String.format(Locale.US, "%.1f", _studyTubeTotalSeconds.value / 3600f)
            val lastVid = _studyTubeLastWatchedInfo.value
            val mTests = mockTests.value.takeLast(3).joinToString("; ") { "${it.testName}: ${it.score}/${it.getMaxScore()} (Neg: ${it.negative})" }
            val practices = dailyPractices.value.takeLast(3).joinToString("; ") { "P:${it.physicsSolved}/${it.physicsTarget}, C:${it.chemistrySolved}/${it.chemistryTarget}, B:${it.biologySolved}/${it.biologyTarget}" }

            val upcomingScheduled = scheduledMockTests.value.filter { !it.isCompleted }
            val upcomingStr = if (upcomingScheduled.isEmpty()) "No upcoming tests scheduled" else upcomingScheduled.joinToString("; ") {
                "${it.title} on ${it.scheduledDate} (Physics: [${it.physicsSyllabus.ifBlank { "Full" }}], Chem: [${it.chemistrySyllabus.ifBlank { "Full" }}], Bio: [${it.biologySyllabus.ifBlank { "Full" }}], Notes: ${it.syllabusNotes})"
            }
            val totalDpps = dppItems.value.size
            val completedDpps = dppItems.value.count { it.isCompleted }
            val pendingDpps = totalDpps - completedDpps
            val timerStatusStr = if (_timerIsRunning.value) "RUNNING for ${_timerSubject.value} - ${_timerChapter.value}" else "STOPPED"

            val prompt = """
                You are "LAKSHYA AI Guardian", the student's personal Big Brother, IIT/NEET Topper Coach, and proactive academic mentor with direct control over the app.
                
                [STUDENT'S REAL-TIME ACADEMIC RADAR]
                - StudyTube Video Lecture Time Today: $stTodayHrs hours (Total watched: $stTotalHrs hours)
                - Recently Watched Lecture: "$lastVid"
                - Recent Question Practice Solved: $practices
                - Recent Mock Test Performance: ${if (mTests.isNotBlank()) mTests else "No tests recently"}
                - Upcoming Scheduled Tests & Syllabus: $upcomingStr
                - DPP Quests: $completedDpps completed, $pendingDpps pending out of $totalDpps total
                - Study Timer Status: $timerStatusStr ${if (isTimerRequest) "(AUTOMATICALLY STARTED BY YOU ON STUDENT REQUEST)" else ""}
                
                [GUARDIAN PERSONA & INTERACTION RULES]
                1. Speak in warm, energetic, and sharp Hinglish (Hindi + English) as a caring elder brother/mentor who has cracked JEE/NEET.
                2. If the student asked kaisa chal raha hai / test ki taiyari kaisa chal raha hai, give a comprehensive analysis covering:
                   - Upcoming test proximity and whether their Physics/Chemistry/Biology syllabus is on track.
                   - DPP completion status.
                   - Today's study velocity and question solving.
                3. NEVER guilt-trip or demotivate. Always give an immediate, tactical recovery plan!
                4. ZOMBIE LEARNING ALERT: If video watch time is high (>1.5h) but question solving is low, actively call it out: "Bhai, sirf screen dekhne se rank nahi aayegi! Ye 'Zombie Learning' trap hai. Turant pen uthao aur 20 questions practice karo."
                5. APP REMOTE CONTROL & TIMER POWERS:
                   - You have direct power over the app! Include actionable command tags in your response when recommending actions:
                     - `[ACTION:TIMER:<minutes>:<subject>:<chapter>]` (e.g. `[ACTION:TIMER:45:Physics:Kinematics]`)
                     - `[ACTION:NAV:mocks]` (to check upcoming tests & syllabus)
                     - `[ACTION:NAV:targets]` (for DPPs & daily targets)
                     - `[ACTION:NAV:timer]` (to open study timer)
                     - `[ACTION:NAV:studytube]` (for StudyTube lectures)
                     - `[ACTION:NAV:mistakes]` (for error notebook)
                   - If the student asked to set/start a timer, acknowledge it enthusiastically: "Bhai maine automatic timer laga diya hai! Ab screen off karke pen-paper pe lag jao."
                6. 39-Year Multi-Exam Advantage: Remind them that solving 39-Year JEE Main PYQs gives an unbeatable calculation edge in NEET Physics & Chemistry.
                
                [CONVERSATION HISTORY]
                $historyText
                
                Reply with brief, highly personalized, empathetic, and actionable guidance under 250 words using clean Markdown and LaTeX $...$ for any formulas.
            """.trimIndent()
            
            val result = com.example.data.GeminiChatAssistant.executeTestingPrompt(context, prompt)
            _isAiAdvisorReplying.value = false
            result.onSuccess { response ->
                val aiMsg = GuardianMessage(sender = "ai", text = response)
                val updatedList = _aiAdvisorMessages.value + aiMsg
                _aiAdvisorMessages.value = updatedList
                saveGuardianMessages(updatedList)

                // If response contains timer action and user had requested timer but not running
                if (response.contains("[ACTION:TIMER:") && !_timerIsRunning.value && isTimerRequest) {
                    executeGuardianActionTag(
                        Regex("\\[ACTION:TIMER:[^\\]]+\\]").find(response)?.value ?: ""
                    )
                }
            }.onFailure { err ->
                val errMsg = GuardianMessage(sender = "ai", text = "❌ **Failed to send response:** ${err.message}")
                val updatedList = _aiAdvisorMessages.value + errMsg
                _aiAdvisorMessages.value = updatedList
                saveGuardianMessages(updatedList)
            }
        }
    }

    val dailyPractices: StateFlow<List<DailyPractice>> = dao.getAllDailyPractices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveDailyPractice(
        date: String,
        pT: Int,
        cT: Int,
        bT: Int,
        pS: Int,
        cS: Int,
        bS: Int,
        pDiff: String = "Medium",
        cDiff: String = "Medium",
        bDiff: String = "Medium"
    ) {
        viewModelScope.launch {
            dao.insertDailyPractice(DailyPractice(date, pT, cT, bT, pS, cS, bS, pDiff, cDiff, bDiff))
        }
    }

    fun deleteDailyPractice(date: String) {
        viewModelScope.launch {
            dao.deleteDailyPractice(date)
        }
    }

    val guardianPulse: StateFlow<GuardianPulseState> = kotlinx.coroutines.flow.combine(
        mockTests,
        dailyPractices,
        studyLogs,
        scheduledMockTests,
        dppItems
    ) { mTests, dPractices, sLogs, sMockTests, dpps ->
        computeGuardianPulse(
            mTests = mTests,
            dPractices = dPractices,
            sLogs = sLogs,
            sMockTests = sMockTests,
            dpps = dpps,
            stTodaySecs = _studyTubeTodaySeconds.value,
            targetMins = _dailyStudyTargetMinutes.value
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        GuardianPulseState()
    )

    private fun computeGuardianPulse(
        mTests: List<MockTest>,
        dPractices: List<DailyPractice>,
        sLogs: List<StudyLog>,
        sMockTests: List<ScheduledMockTest>,
        dpps: List<DppItem>,
        stTodaySecs: Long,
        targetMins: Int
    ): GuardianPulseState {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        val completedTodaySeconds = sLogs.filter { it.timestamp >= todayStart }.sumOf { it.durationSeconds }
        val activeSeconds = _timerSecondsElapsed.value
        val totalStudyTodayHours = (completedTodaySeconds + activeSeconds) / 3600f
        val targetStudyHours = (targetMins / 60f).coerceAtLeast(1f)

        val dppCompleted = dpps.count { it.isCompleted }
        val dppTotal = dpps.size
        val dppPending = dppTotal - dppCompleted

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val upcomingTestsList = sMockTests
            .filter { !it.isCompleted }
            .map { st ->
                val daysRemaining = try {
                    val targetDate = sdf.parse(st.scheduledDate)
                    if (targetDate != null) {
                        val diffMillis = targetDate.time - todayStart
                        (diffMillis / (1000 * 60 * 60 * 24)).toInt()
                    } else 999
                } catch (e: Exception) {
                    999
                }
                GuardianUpcomingTestInfo(
                    id = st.id,
                    title = st.title,
                    scheduledDate = st.scheduledDate,
                    daysRemaining = daysRemaining,
                    physicsSyllabus = st.physicsSyllabus,
                    chemistrySyllabus = st.chemistrySyllabus,
                    biologySyllabus = st.biologySyllabus,
                    syllabusNotes = st.syllabusNotes,
                    isCompleted = st.isCompleted,
                    isPinned = st.isPinned
                )
            }
            .sortedWith(compareByDescending<GuardianUpcomingTestInfo> { it.isPinned }.thenBy { it.daysRemaining })

        val nextTest = upcomingTestsList.firstOrNull()

        val avgScore = if (mTests.isNotEmpty()) mTests.map { it.score }.average().toInt() else 0
        val latestScore = mTests.maxByOrNull { it.timestamp }?.score

        val weakestSubject = if (mTests.isNotEmpty()) {
            val pAvg = mTests.map { it.physics }.average()
            val cAvg = mTests.map { it.chemistry }.average()
            val bAvg = mTests.map { it.biology }.average()
            when {
                pAvg <= cAvg && pAvg <= bAvg -> "Physics"
                cAvg <= pAvg && cAvg <= bAvg -> "Chemistry"
                else -> "Biology"
            }
        } else "Physics"

        val stTodayHrs = stTodaySecs / 3600f
        val todayPractices = dPractices.filter { 
            try {
                val pDate = sdf.parse(it.date)
                pDate != null && pDate.time >= todayStart
            } catch (e: Exception) { false }
        }
        val solvedToday = todayPractices.sumOf { it.physicsSolved + it.chemistrySolved + it.biologySolved }
        val isZombie = stTodayHrs >= 1.5f && solvedToday < 15

        var scoreAcc = 0
        scoreAcc += ((totalStudyTodayHours / targetStudyHours).coerceIn(0f, 1f) * 35).toInt()
        scoreAcc += if (dppTotal > 0) ((dppCompleted.toFloat() / dppTotal) * 30).toInt() else 20
        scoreAcc += if (avgScore > 0) ((avgScore / 720f).coerceIn(0f, 1f) * 35).toInt() else 20
        if (isZombie) {
            scoreAcc = (scoreAcc - 15).coerceAtLeast(10)
        }
        val readinessScore = scoreAcc.coerceIn(15, 100)

        val (status, color) = when {
            readinessScore >= 80 -> "Topper Momentum 🚀" to 0xFF10B981
            readinessScore >= 65 -> "Battle Ready ⚡" to 0xFF6366F1
            readinessScore >= 45 -> "Steady Revision 📈" to 0xFFF59E0B
            else -> "Needs Sprint Push 🚨" to 0xFFEF4444
        }

        val diagnosis = when {
            nextTest != null && nextTest.daysRemaining in 0..3 -> {
                val dayStr = if (nextTest.daysRemaining == 0) "Today!" else "in ${nextTest.daysRemaining} days"
                "🚨 Test Alert: '${nextTest.title}' is $dayStr! Pending DPPs: $dppPending. Focus on: Physics [${nextTest.physicsSyllabus.ifBlank { "Core Topics" }}], Chem [${nextTest.chemistrySyllabus.ifBlank { "Core Topics" }}]."
            }
            isZombie -> {
                "⚠️ Zombie Learning Alert: You have watched ${String.format(Locale.US, "%.1f", stTodayHrs)}h of video lectures today with low question solving. Pause video now and solve 20 numericals!"
            }
            dppPending > 6 -> {
                "📝 DPP Backlog: You have $dppPending pending DPPs. Guardian recommends clearing at least 2 DPPs today."
            }
            totalStudyTodayHours >= targetStudyHours -> {
                "🔥 Target Crushed: ${String.format(Locale.US, "%.1f", totalStudyTodayHours)}h completed today! Keep maintaining this intensity for rank boost."
            }
            else -> {
                "🛡️ Study Velocity: ${String.format(Locale.US, "%.1f", totalStudyTodayHours)}h logged today (Goal: ${String.format(Locale.US, "%.1f", targetStudyHours)}h). Next test: ${nextTest?.title ?: "No test scheduled"}."
            }
        }

        val advice = when {
            isZombie -> "Solve pending DPP questions to convert video knowledge into test marks."
            nextTest != null && nextTest.daysRemaining in 0..3 -> "Revise mock test syllabus formulas and solve 30 high-yield PYQs."
            dppPending > 0 -> "Solve pending DPP questions for $weakestSubject."
            else -> "Revise high-weightage chapters and practice test problems."
        }

        return GuardianPulseState(
            readinessScore = readinessScore,
            readinessStatus = status,
            readinessColorHex = color,
            studyHoursToday = totalStudyTodayHours,
            studyTargetHours = targetStudyHours,
            dppCompletedCount = dppCompleted,
            dppTotalCount = dppTotal,
            dppPendingCount = dppPending,
            upcomingTests = upcomingTestsList,
            nextUpcomingTest = nextTest,
            averageMockScore = avgScore,
            latestMockScore = latestScore,
            weakestSubject = weakestSubject,
            isZombieRisk = isZombie,
            proactiveDiagnosis = diagnosis,
            actionableGuidance = advice
        )
    }

    fun guardianStartTimer(
        subject: String = "Physics",
        chapter: String = "Revision Sprint",
        durationMinutes: Int = 45,
        isPomodoro: Boolean = false
    ) {
        if (_timerIsRunning.value) {
            stopAndSaveTimer()
        }
        _timerSubject.value = subject
        _timerChapter.value = chapter
        prefs.edit()
            .putString("timer_subject", subject)
            .putString("timer_chapter", chapter)
            .apply()

        if (isPomodoro) {
            _isPomodoroMode.value = true
            _isCountdownMode.value = true
            _dailyStudyTargetMinutes.value = durationMinutes
        } else {
            _isPomodoroMode.value = false
            _isCountdownMode.value = durationMinutes > 0
            if (durationMinutes > 0) {
                _dailyStudyTargetMinutes.value = durationMinutes
            }
        }

        accumulatedBaseSeconds = 0
        _timerSecondsElapsed.value = 0
        val now = android.os.SystemClock.elapsedRealtime()
        val nowEpoch = System.currentTimeMillis()
        val todayKey = getTodayDateKey()
        timerStartRealtime = now
        timerSessionStartEpoch = nowEpoch
        timerSessionDate = todayKey
        _timerIsRunning.value = true

        prefs.edit()
            .putBoolean("timer_is_running", true)
            .putLong("timer_start_realtime", now)
            .putLong("timer_session_start_epoch", nowEpoch)
            .putString("timer_session_date", todayKey)
            .putInt("timer_accumulated_seconds", 0)
            .apply()

        val targetSecs = if (_isCountdownMode.value) _dailyStudyTargetMinutes.value * 60 else 0
        com.example.service.StudyTimerService.startTimer(
            context = getApplication(),
            subject = subject,
            chapter = chapter,
            startRealtime = now,
            baseSeconds = 0,
            isCountdown = _isCountdownMode.value,
            targetSeconds = targetSecs
        )
        startTimerLoop()
    }

    fun guardianStopTimer() {
        stopAndSaveTimer()
    }

    fun guardianToggleTimer() {
        toggleTimer()
    }

    fun executeGuardianActionTag(actionTag: String, onNavigate: ((String) -> Unit)? = null) {
        try {
            val clean = actionTag.trim().removeSurrounding("[", "]").removePrefix("ACTION:")
            val parts = clean.split(":")
            when (parts.getOrNull(0)?.uppercase(Locale.ROOT)) {
                "TIMER", "START_TIMER" -> {
                    val mins = parts.getOrNull(1)?.toIntOrNull() ?: 45
                    val sub = parts.getOrNull(2) ?: "Physics"
                    val chap = parts.getOrNull(3) ?: "Revision Sprint"
                    guardianStartTimer(sub, chap, mins)
                    onNavigate?.invoke("timer")
                }
                "NAV" -> {
                    val route = parts.getOrNull(1) ?: "dashboard"
                    onNavigate?.invoke(route)
                }
                "DPP_TIMER" -> {
                    val mins = parts.getOrNull(1)?.toIntOrNull() ?: 45
                    val sub = parts.getOrNull(2) ?: "Physics"
                    val chap = parts.getOrNull(3) ?: "DPP Practice"
                    guardianStartTimer(sub, chap, mins)
                    onNavigate?.invoke("timer")
                }
                "TEST_TIMER" -> {
                    val mins = parts.getOrNull(1)?.toIntOrNull() ?: 60
                    val testName = parts.getOrNull(2) ?: "Mock Test Prep"
                    guardianStartTimer("Revision", testName, mins)
                    onNavigate?.invoke("timer")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    
    val completedTopics: StateFlow<List<CompletedTopic>> = dao.getAllCompletedTopics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeTargetTab = MutableStateFlow("Targets")
    val activeTargetTab: StateFlow<String> = _activeTargetTab.asStateFlow()

    fun setActiveTargetTab(tab: String) {
        _activeTargetTab.value = tab
    }

    private val _activeTargetSubject = MutableStateFlow("Physics")
    val activeTargetSubject: StateFlow<String> = _activeTargetSubject.asStateFlow()

    fun setActiveTargetSubject(subject: String) {
        _activeTargetSubject.value = subject
    }

    fun toggleTopicCompletion(topicId: String, isCompleted: Boolean, customDateMillis: Long? = null) {
        viewModelScope.launch {
            if (isCompleted) {
                dao.insertCompletedTopic(CompletedTopic(topicId = topicId, customRevisionDate = customDateMillis))
            } else {
                dao.deleteCompletedTopic(topicId)
            }
        }
    }

    fun markTopicRevised(topicId: String) {
        viewModelScope.launch {
            dao.insertCompletedTopic(CompletedTopic(topicId = topicId, timestamp = System.currentTimeMillis(), customRevisionDate = null))
        }
    }

    fun markTopicPending(topicId: String) {
        viewModelScope.launch {
            // Set timestamp to 5 days ago and custom date to null so it is due
            dao.insertCompletedTopic(CompletedTopic(topicId = topicId, timestamp = System.currentTimeMillis() - 5 * 86400000L, customRevisionDate = null))
        }
    }

    fun toggleTopicRevisedStatus(topicId: String, isCurrentlyDone: Boolean) {
        if (isCurrentlyDone) {
            markTopicPending(topicId)
        } else {
            markTopicRevised(topicId)
        }
    }

    fun toggleChapterRevisionDone(chapterName: String, isCurrentlyDone: Boolean) {
        viewModelScope.launch {
            val list = dao.getAllCompletedTopics().firstOrNull() ?: emptyList()
            val matching = list.filter { it.topicId.startsWith("$chapterName-") }
            val now = System.currentTimeMillis()
            if (matching.isNotEmpty()) {
                matching.forEach { topic ->
                    if (isCurrentlyDone) {
                        dao.insertCompletedTopic(CompletedTopic(topicId = topic.topicId, timestamp = now - 5 * 86400000L, customRevisionDate = null))
                    } else {
                        dao.insertCompletedTopic(CompletedTopic(topicId = topic.topicId, timestamp = now, customRevisionDate = null))
                    }
                }
            } else {
                val subtopics = com.example.data.ExamSyllabusDatabase.getSubtopicsForChapter(chapterName)
                if (subtopics.isNotEmpty()) {
                    subtopics.forEach { subtopic ->
                        val targetTime = if (isCurrentlyDone) (now - 5 * 86400000L) else now
                        dao.insertCompletedTopic(CompletedTopic(topicId = "$chapterName-$subtopic", timestamp = targetTime, customRevisionDate = null))
                    }
                } else {
                    val targetTime = if (isCurrentlyDone) (now - 5 * 86400000L) else now
                    dao.insertCompletedTopic(CompletedTopic(topicId = "$chapterName-Revision", timestamp = targetTime, customRevisionDate = null))
                }
            }
        }
    }

    fun applyAiMindBatchSchedule(scheduleMap: Map<String, Long>) {
        viewModelScope.launch {
            val list = dao.getAllCompletedTopics().firstOrNull() ?: emptyList()
            scheduleMap.forEach { (chapterName, targetDateMillis) ->
                val matching = list.filter { it.topicId.startsWith("$chapterName-") }
                if (matching.isNotEmpty()) {
                    matching.forEach { topic ->
                        dao.insertCompletedTopic(CompletedTopic(topicId = topic.topicId, timestamp = topic.timestamp, customRevisionDate = targetDateMillis))
                    }
                } else {
                    val subtopics = com.example.data.ExamSyllabusDatabase.getSubtopicsForChapter(chapterName)
                    if (subtopics.isNotEmpty()) {
                        subtopics.forEach { subtopic ->
                            dao.insertCompletedTopic(CompletedTopic(topicId = "$chapterName-$subtopic", timestamp = System.currentTimeMillis(), customRevisionDate = targetDateMillis))
                        }
                    } else {
                        dao.insertCompletedTopic(CompletedTopic(topicId = "$chapterName-Revision", timestamp = System.currentTimeMillis(), customRevisionDate = targetDateMillis))
                    }
                }
            }
        }
    }

    fun setTopicCustomRevisionDate(topicId: String, targetDateMillis: Long?) {
        viewModelScope.launch {
            dao.insertCompletedTopic(CompletedTopic(topicId = topicId, timestamp = System.currentTimeMillis(), customRevisionDate = targetDateMillis))
        }
    }

    fun setChapterCustomRevisionDate(chapterName: String, targetDateMillis: Long?) {
        viewModelScope.launch {
            val list = dao.getAllCompletedTopics().firstOrNull() ?: emptyList()
            val matching = list.filter { it.topicId.startsWith("$chapterName-") }
            if (matching.isNotEmpty()) {
                matching.forEach { topic ->
                    dao.insertCompletedTopic(CompletedTopic(topicId = topic.topicId, timestamp = topic.timestamp, customRevisionDate = targetDateMillis))
                }
            } else {
                val subtopics = com.example.data.ExamSyllabusDatabase.getSubtopicsForChapter(chapterName)
                if (subtopics.isNotEmpty()) {
                    subtopics.forEach { subtopic ->
                        dao.insertCompletedTopic(CompletedTopic(topicId = "$chapterName-$subtopic", timestamp = System.currentTimeMillis(), customRevisionDate = targetDateMillis))
                    }
                } else {
                    dao.insertCompletedTopic(CompletedTopic(topicId = "$chapterName-Revision", timestamp = System.currentTimeMillis(), customRevisionDate = targetDateMillis))
                }
            }
        }
    }

    fun addChapterToRevision(chapterName: String, subtopics: List<String>, customDateMillis: Long? = null) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (subtopics.isNotEmpty()) {
                subtopics.forEach { subtopic ->
                    val topicId = "$chapterName-$subtopic"
                    dao.insertCompletedTopic(CompletedTopic(topicId = topicId, timestamp = now, customRevisionDate = customDateMillis))
                }
            } else {
                dao.insertCompletedTopic(CompletedTopic(topicId = "$chapterName-Chapter Revision", timestamp = now, customRevisionDate = customDateMillis))
            }
        }
    }

    fun autoSyncChecklistChapterToRevision(chapterName: String, milestone: String? = null, customDateMillis: Long? = null) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val subtopics = com.example.data.ExamSyllabusDatabase.getSubtopicsForChapter(chapterName)
            if (subtopics.isNotEmpty()) {
                subtopics.forEach { subtopic ->
                    val topicId = "$chapterName-$subtopic"
                    dao.insertCompletedTopic(CompletedTopic(topicId = topicId, timestamp = now, customRevisionDate = customDateMillis))
                }
            } else {
                val topicId = if (milestone != null) "$chapterName-PYQ $milestone" else "$chapterName-PYQ Checklist"
                dao.insertCompletedTopic(CompletedTopic(topicId = topicId, timestamp = now, customRevisionDate = customDateMillis))
            }
        }
    }

    
    fun resetAllData() {
        viewModelScope.launch {
            dao.clearAll()
            prefs.edit().clear().apply()
            try {
                val persistentFile = java.io.File(getApplication<Application>().filesDir, "user_avatar_persistent.jpg")
                if (persistentFile.exists()) {
                    persistentFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _userName.value = "NEET Aspirant"
            _userClass.value = "Class 12 / Dropper"
            _userAvatarUri.value = null
            _geminiApiKey1.value = ""
            _geminiApiKey2.value = ""
            _geminiApiKey3.value = ""
        }
    }

    fun addStudyLog(subject: String, chapter: String, durationSeconds: Int, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            dao.insertStudyLog(StudyLog(subject = subject, chapter = chapter, durationSeconds = durationSeconds, timestamp = timestamp))
        }
    }

    fun deleteStudyLog(id: Int) {
        viewModelScope.launch {
            dao.deleteStudyLog(id)
        }
    }

    fun addGoal(
        subject: String,
        text: String,
        targetType: String = "today",
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) {
        viewModelScope.launch {
            dao.insertGoal(
                Goal(
                    subject = subject,
                    text = text,
                    status = GoalStatus.TODO.value,
                    targetType = targetType,
                    date = date
                )
            )
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun updateGoal(goal: Goal) {
        viewModelScope.launch {
            dao.insertGoal(goal)
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun addDppItem(
        subject: String,
        chapter: String,
        dppName: String,
        pdfUri: String? = null,
        pdfFileName: String? = null,
        isCompleted: Boolean = false,
        questionsCount: Int = 0,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) {
        viewModelScope.launch {
            dao.insertDppItem(
                DppItem(
                    subject = subject,
                    chapter = chapter,
                    dppName = dppName,
                    isCompleted = isCompleted,
                    pdfUri = pdfUri,
                    pdfFileName = pdfFileName,
                    questionsCount = questionsCount,
                    date = date
                )
            )
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun updateDppItem(item: DppItem) {
        viewModelScope.launch {
            dao.insertDppItem(item)
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun deleteDppItem(id: Long) {
        viewModelScope.launch {
            dao.deleteDppItem(id)
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun toggleDppItemCompletion(item: DppItem) {
        val completedDate = if (!item.isCompleted) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        } else null
        viewModelScope.launch {
            dao.insertDppItem(
                item.copy(
                    isCompleted = !item.isCompleted,
                    completedDate = completedDate
                )
            )
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun updateGoalStatus(id: Int, status: GoalStatus) {
        updateGoalStatus(id, status.value)
    }

    fun updateGoalStatus(id: Int, status: String) {
        val completedDate = if (status == GoalStatus.COMPLETED.value) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
        } else null
        viewModelScope.launch {
            dao.updateGoalStatus(id, status, completedDate)
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    fun deleteGoal(id: Int) {
        viewModelScope.launch {
            dao.deleteGoal(id)
            com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
        }
    }

    private val _analyzingMockTestId = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null)
    val analyzingMockTestId: StateFlow<Int?> = _analyzingMockTestId.asStateFlow()

    fun addMockTest(
        name: String,
        score: Int,
        physics: Int = 0,
        chemistry: Int = 0,
        biology: Int = 0,
        negative: Int = 0,
        pdfUri: String? = null,
        pdfFileName: String? = null,
        geminiAnalysis: String? = null
    ) {
        viewModelScope.launch {
            dao.insertMockTest(
                MockTest(
                    testName = name,
                    score = score,
                    physics = physics,
                    chemistry = chemistry,
                    biology = biology,
                    negative = negative,
                    pdfUri = pdfUri,
                    pdfFileName = pdfFileName,
                    geminiAnalysis = geminiAnalysis
                )
            )
        }
    }

    fun updateMockTest(
        id: Int,
        name: String,
        score: Int,
        physics: Int = 0,
        chemistry: Int = 0,
        biology: Int = 0,
        negative: Int = 0,
        timestamp: Long,
        pdfUri: String? = null,
        pdfFileName: String? = null,
        geminiAnalysis: String? = null
    ) {
        viewModelScope.launch {
            dao.insertMockTest(
                MockTest(
                    id = id,
                    testName = name,
                    score = score,
                    physics = physics,
                    chemistry = chemistry,
                    biology = biology,
                    negative = negative,
                    timestamp = timestamp,
                    pdfUri = pdfUri,
                    pdfFileName = pdfFileName,
                    geminiAnalysis = geminiAnalysis
                )
            )
        }
    }

    fun analyzeMockTestPdf(context: android.content.Context, test: MockTest, pdfUri: android.net.Uri, fileName: String) {
        viewModelScope.launch {
            _analyzingMockTestId.value = test.id
            val isJee = test.testName.contains("JEE", ignoreCase = true) || test.testName.contains("IIT", ignoreCase = true)
            val examTag = if (isJee) "JEE" else "NEET"
            android.widget.Toast.makeText(context, "Analyzing '${test.testName}' PDF against $examTag level...", android.widget.Toast.LENGTH_SHORT).show()
            val res = com.example.data.GeminiChatAssistant.analyzeMockTestPdfWithGemini(
                context = context,
                pdfUriStr = pdfUri.toString(),
                testName = test.testName,
                userScore = test.score
            )
            if (res.isSuccess) {
                val analysis = res.getOrNull() ?: ""
                dao.insertMockTest(
                    test.copy(
                        pdfUri = pdfUri.toString(),
                        pdfFileName = fileName,
                        geminiAnalysis = analysis
                    )
                )
                android.widget.Toast.makeText(context, "Lakshya AI $examTag Level Analysis complete!", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "Analysis Failed: ${res.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_LONG).show()
            }
            _analyzingMockTestId.value = null
        }
    }

    fun deleteMockTest(id: Int) {
        viewModelScope.launch {
            dao.deleteMockTest(id)
        }
    }

    private val _lastSyncedTestbookMockId = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null)
    val lastSyncedTestbookMockId: StateFlow<Int?> = _lastSyncedTestbookMockId.asStateFlow()

    fun syncTestbookScorecardToApp(
        result: com.example.data.TestbookScorecardResult,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            // 1. Insert Mock Test with dynamic Max Marks formatted in title if not full 720
            val formattedTitle = if (result.maxScore != 720 && !result.testTitle.contains("${result.maxScore}")) {
                "${result.testTitle} (${result.maxScore}M)"
            } else {
                result.testTitle
            }

            val testAnalysis = buildString {
                append("📊 Total Marks: ${result.totalScore} / ${result.maxScore}\n")
                if (result.physicsMax > 0 || result.chemistryMax > 0 || result.biologyMax > 0) {
                    append("📚 Sectional Breakdown: Phy: ${result.physicsScore}/${result.physicsMax}, Chem: ${result.chemistryScore}/${result.chemistryMax}, Bio: ${result.biologyScore}/${result.biologyMax}\n")
                }
                if (result.rankOrPercentile.isNotBlank()) append("🏆 ${result.rankOrPercentile}\n")
                if (result.accuracyPercentage > 0f) append("🎯 Accuracy: ${String.format(Locale.getDefault(), "%.1f", result.accuracyPercentage)}%\n")
                if (result.weakTopics.isNotEmpty()) append("⚠️ Weak Topics: ${result.weakTopics.joinToString(", ")}\n\n")
                if (result.rawAnalysisMarkdown.isNotBlank()) append(result.rawAnalysisMarkdown)
            }

            val mockTest = MockTest(
                testName = formattedTitle,
                score = result.totalScore,
                physics = result.physicsScore,
                chemistry = result.chemistryScore,
                biology = result.biologyScore,
                negative = result.negativeMarks,
                timestamp = System.currentTimeMillis(),
                geminiAnalysis = testAnalysis
            )
            dao.insertMockTest(mockTest)

            // 2. Log identified mistakes to chat_messages error notebook
            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            result.mistakesIdentified.forEachIndexed { idx, mistake ->
                val subj = when {
                    mistake.contains("physics", true) || idx % 3 == 0 -> "physics"
                    mistake.contains("chem", true) || idx % 3 == 1 -> "chemistry"
                    else -> "biology"
                }
                dao.insertChatMessage(
                    com.example.data.ChatMessageEntity(
                        subject = subj,
                        chatType = "error",
                        text = "📝 **Testbook Mistake Logged**: $mistake",
                        sender = "system",
                        time = timeStr
                    )
                )
            }
            // 3. Increment solved questions for Physics, Chemistry, Biology
            val phyQCount = if (result.physicsMax > 0) (result.physicsMax / 4).coerceAtLeast(1) else 0
            val chemQCount = if (result.chemistryMax > 0) (result.chemistryMax / 4).coerceAtLeast(1) else 0
            val bioQCount = if (result.biologyMax > 0) (result.biologyMax / 4).coerceAtLeast(1) else 0
            if (phyQCount > 0) recordSolvedQuestions("Physics", phyQCount)
            if (chemQCount > 0) recordSolvedQuestions("Chemistry", chemQCount)
            if (bioQCount > 0) recordSolvedQuestions("Biology", bioQCount)

            // 4. Trigger Guardian refresh
            runAiAdvisorAnalysis(getApplication<Application>())

            onSuccess("Scorecard synced to Mock Tests, Analytics, Error Notebook & AI Guardian!")
        }
    }

    private val _isDarkMode = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    fun toggleDarkMode() {
        val newMode = !_isDarkMode.value
        prefs.edit().putBoolean("is_dark_mode", newMode).apply()
        _isDarkMode.value = newMode
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_dark_mode", enabled).apply()
        _isDarkMode.value = enabled
    }

    private val _isNavMenuHidden = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isNavMenuHidden: StateFlow<Boolean> = _isNavMenuHidden

    fun toggleNavMenuHidden() {
        _isNavMenuHidden.value = !_isNavMenuHidden.value
    }

    private val _timerIsRunning = kotlinx.coroutines.flow.MutableStateFlow(false)
    val timerIsRunning: StateFlow<Boolean> = _timerIsRunning

    private val _isFullScreenTimer = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isFullScreenTimer: StateFlow<Boolean> = _isFullScreenTimer

    fun setIsFullScreenTimer(isFull: Boolean) {
        _isFullScreenTimer.value = isFull
    }

    private val _isFullScreenBrowser = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isFullScreenBrowser: StateFlow<Boolean> = _isFullScreenBrowser

    fun setIsFullScreenBrowser(isFull: Boolean) {
        _isFullScreenBrowser.value = isFull
    }

    // STUDY TARGET NOTIFICATION STATE
    private val _isStudyTargetAlertEnabled = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("is_study_target_alert_enabled", true))
    val isStudyTargetAlertEnabled: StateFlow<Boolean> = _isStudyTargetAlertEnabled

    private val _dailyStudyTargetMinutes = kotlinx.coroutines.flow.MutableStateFlow(prefs.getInt("daily_study_target_minutes", 180)) // default 3 hours = 180 mins
    val dailyStudyTargetMinutes: StateFlow<Int> = _dailyStudyTargetMinutes

    private val _showStudyTargetSubtleAlert = kotlinx.coroutines.flow.MutableStateFlow(false)
    val showStudyTargetSubtleAlert: StateFlow<Boolean> = _showStudyTargetSubtleAlert

    fun setStudyTargetAlertEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_study_target_alert_enabled", enabled).apply()
        _isStudyTargetAlertEnabled.value = enabled
        if (!enabled) {
            _showStudyTargetSubtleAlert.value = false
        }
    }

    fun setDailyStudyTargetMinutes(minutes: Int) {
        prefs.edit().putInt("daily_study_target_minutes", minutes).apply()
        _dailyStudyTargetMinutes.value = minutes
        checkStudyTargetAlert()
    }

    fun dismissStudyTargetAlert() {
        _showStudyTargetSubtleAlert.value = false
    }

    fun checkStudyTargetAlert() {
        if (!_isStudyTargetAlertEnabled.value) {
            _showStudyTargetSubtleAlert.value = false
            return
        }

        // Get start of today
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        val completedTodaySeconds = studyLogs.value.filter { it.timestamp >= todayStart }.sumOf { it.durationSeconds }
        val activeSeconds = _timerSecondsElapsed.value
        val totalSecondsToday = completedTodaySeconds + activeSeconds

        val targetSeconds = _dailyStudyTargetMinutes.value * 60
        val diffSeconds = targetSeconds - totalSecondsToday

        // Subtle alert when less than 30 minutes away from target (30 minutes = 1800 seconds)
        _showStudyTargetSubtleAlert.value = diffSeconds in 1..1800
    }

    private val _isCountdownMode = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("is_countdown_mode", false))
    val isCountdownMode: StateFlow<Boolean> = _isCountdownMode

    fun setIsCountdownMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_countdown_mode", enabled).apply()
        _isCountdownMode.value = enabled
    }

    private val _isStrictFocusMode = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("is_strict_focus_mode", false))
    val isStrictFocusMode: StateFlow<Boolean> = _isStrictFocusMode

    fun setIsStrictFocusMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_strict_focus_mode", enabled).apply()
        _isStrictFocusMode.value = enabled
    }

    private val _isPomodoroMode = kotlinx.coroutines.flow.MutableStateFlow(prefs.getBoolean("is_pomodoro_mode", false))
    val isPomodoroMode: kotlinx.coroutines.flow.StateFlow<Boolean> = _isPomodoroMode

    fun setIsPomodoroMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_pomodoro_mode", enabled).apply()
        _isPomodoroMode.value = enabled
    }

    private val _timerSecondsElapsed = kotlinx.coroutines.flow.MutableStateFlow(0)
    val timerSecondsElapsed: StateFlow<Int> = _timerSecondsElapsed

    private val _timerSubject = kotlinx.coroutines.flow.MutableStateFlow("Physics")
    val timerSubject: StateFlow<String> = _timerSubject

    private val _timerChapter = kotlinx.coroutines.flow.MutableStateFlow("Units and Measurements")
    val timerChapter: StateFlow<String> = _timerChapter
    
    private var timerJob: kotlinx.coroutines.Job? = null
    private var timerStartRealtime: Long = 0L
    private var accumulatedBaseSeconds: Int = 0
    private var timerSessionStartEpoch: Long = 0L
    private var timerSessionDate: String = ""

    private fun getTodayDateKey(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    private fun getTodayMidnightMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun checkAndHandleMidnightRollover() {
        if (!_timerIsRunning.value) return
        val currentTodayKey = getTodayDateKey()
        if (timerSessionDate.isNotBlank() && timerSessionDate != currentTodayKey) {
            val todayMidnight = getTodayMidnightMillis()
            val currentEpoch = System.currentTimeMillis()

            // 1. Calculate how many seconds belonged to the previous day (up to midnight)
            val prevDaySeconds = if (timerSessionStartEpoch in 1 until todayMidnight) {
                val deltaBeforeMidnight = ((todayMidnight - timerSessionStartEpoch) / 1000L).toInt().coerceAtLeast(0)
                accumulatedBaseSeconds + deltaBeforeMidnight
            } else {
                _timerSecondsElapsed.value.coerceAtLeast(1)
            }

            val sub = _timerSubject.value
            val chap = _timerChapter.value.ifBlank { "General Study" }

            if (prevDaySeconds > 0) {
                // Save previous day's study log with 11:59:59 PM timestamp of previous day
                addStudyLog(sub, chap, prevDaySeconds, timestamp = todayMidnight - 1000L)
            }

            // 2. Automatically roll over and continue timer starting from 00:00:00 for the new day with SAME subject and chapter!
            val nowRealtime = android.os.SystemClock.elapsedRealtime()
            val deltaSinceMidnight = ((currentEpoch - todayMidnight) / 1000L).toInt().coerceAtLeast(0)

            accumulatedBaseSeconds = 0
            timerStartRealtime = nowRealtime - (deltaSinceMidnight * 1000L)
            timerSessionStartEpoch = todayMidnight
            timerSessionDate = currentTodayKey
            _timerSecondsElapsed.value = deltaSinceMidnight

            prefs.edit()
                .putBoolean("timer_is_running", true)
                .putLong("timer_start_realtime", timerStartRealtime)
                .putLong("timer_session_start_epoch", timerSessionStartEpoch)
                .putString("timer_session_date", currentTodayKey)
                .putInt("timer_accumulated_seconds", 0)
                .putString("timer_subject", sub)
                .putString("timer_chapter", chap)
                .putInt("timer_last_known_seconds", deltaSinceMidnight)
                .apply()

            val targetSecs = if (_isCountdownMode.value) _dailyStudyTargetMinutes.value * 60 else 0
            com.example.service.StudyTimerService.startTimer(
                context = getApplication(),
                subject = sub,
                chapter = chap,
                startRealtime = timerStartRealtime,
                baseSeconds = 0,
                isCountdown = _isCountdownMode.value,
                targetSeconds = targetSecs
            )
        }
    }

    fun restoreTimerState() {
        val isRunning = prefs.getBoolean("timer_is_running", false)
        accumulatedBaseSeconds = prefs.getInt("timer_accumulated_seconds", 0)
        _timerSubject.value = prefs.getString("timer_subject", "Physics") ?: "Physics"
        _timerChapter.value = prefs.getString("timer_chapter", "Units and Measurements") ?: "Units and Measurements"
        timerSessionDate = prefs.getString("timer_session_date", "") ?: ""
        timerSessionStartEpoch = prefs.getLong("timer_session_start_epoch", 0L)

        if (isRunning) {
            val currentTodayKey = getTodayDateKey()
            if (timerSessionDate.isNotBlank() && timerSessionDate != currentTodayKey) {
                // Rollover occurred while app was suspended / offline
                val todayMidnight = getTodayMidnightMillis()
                val currentEpoch = System.currentTimeMillis()
                val prevDaySeconds = if (timerSessionStartEpoch in 1 until todayMidnight) {
                    val deltaBeforeMidnight = ((todayMidnight - timerSessionStartEpoch) / 1000L).toInt().coerceAtLeast(0)
                    accumulatedBaseSeconds + deltaBeforeMidnight
                } else {
                    prefs.getInt("timer_last_known_seconds", 0).coerceAtLeast(1)
                }

                val sub = _timerSubject.value
                val chap = _timerChapter.value.ifBlank { "General Study" }
                if (prevDaySeconds > 0) {
                    addStudyLog(sub, chap, prevDaySeconds, timestamp = todayMidnight - 1000L)
                }

                val nowRealtime = android.os.SystemClock.elapsedRealtime()
                val deltaSinceMidnight = ((currentEpoch - todayMidnight) / 1000L).toInt().coerceAtLeast(0)

                accumulatedBaseSeconds = 0
                timerStartRealtime = nowRealtime - (deltaSinceMidnight * 1000L)
                timerSessionStartEpoch = todayMidnight
                timerSessionDate = currentTodayKey
                _timerSecondsElapsed.value = deltaSinceMidnight
                _timerIsRunning.value = true

                prefs.edit()
                    .putBoolean("timer_is_running", true)
                    .putLong("timer_start_realtime", timerStartRealtime)
                    .putLong("timer_session_start_epoch", timerSessionStartEpoch)
                    .putString("timer_session_date", currentTodayKey)
                    .putInt("timer_accumulated_seconds", 0)
                    .apply()
            } else {
                timerStartRealtime = prefs.getLong("timer_start_realtime", android.os.SystemClock.elapsedRealtime())
                if (timerSessionStartEpoch <= 0L) {
                    timerSessionStartEpoch = System.currentTimeMillis()
                    timerSessionDate = currentTodayKey
                }
                val delta = ((android.os.SystemClock.elapsedRealtime() - timerStartRealtime) / 1000L).toInt().coerceAtLeast(0)
                _timerSecondsElapsed.value = accumulatedBaseSeconds + delta
                _timerIsRunning.value = true
            }

            val targetSecs = if (_isCountdownMode.value) _dailyStudyTargetMinutes.value * 60 else 0
            com.example.service.StudyTimerService.startTimer(
                context = getApplication(),
                subject = _timerSubject.value,
                chapter = _timerChapter.value,
                startRealtime = timerStartRealtime,
                baseSeconds = accumulatedBaseSeconds,
                isCountdown = _isCountdownMode.value,
                targetSeconds = targetSecs
            )
            startTimerLoop()
        } else {
            _timerSecondsElapsed.value = accumulatedBaseSeconds
            _timerIsRunning.value = false
        }
    }

    fun syncTimerFromClock() {
        val isRunning = prefs.getBoolean("timer_is_running", _timerIsRunning.value)
        if (isRunning) {
            timerSessionDate = prefs.getString("timer_session_date", timerSessionDate) ?: timerSessionDate
            timerSessionStartEpoch = prefs.getLong("timer_session_start_epoch", timerSessionStartEpoch)
            checkAndHandleMidnightRollover()

            val startRealtime = prefs.getLong("timer_start_realtime", timerStartRealtime)
            val baseSecs = prefs.getInt("timer_accumulated_seconds", accumulatedBaseSeconds)
            if (startRealtime > 0L) {
                val delta = ((android.os.SystemClock.elapsedRealtime() - startRealtime) / 1000L).toInt().coerceAtLeast(0)
                _timerSecondsElapsed.value = baseSecs + delta
            }
            _timerIsRunning.value = true
        } else {
            val baseSecs = prefs.getInt("timer_accumulated_seconds", accumulatedBaseSeconds)
            _timerSecondsElapsed.value = baseSecs
            _timerIsRunning.value = false
        }
        checkStudyTargetAlert()
    }
    
    fun toggleTimer() {
        if (_timerIsRunning.value) {
            // Pause
            val now = android.os.SystemClock.elapsedRealtime()
            if (timerStartRealtime > 0L) {
                accumulatedBaseSeconds += ((now - timerStartRealtime) / 1000L).toInt().coerceAtLeast(0)
            }
            timerStartRealtime = 0L
            _timerSecondsElapsed.value = accumulatedBaseSeconds
            _timerIsRunning.value = false
            timerJob?.cancel()

            prefs.edit()
                .putBoolean("timer_is_running", false)
                .putLong("timer_start_realtime", 0L)
                .putInt("timer_accumulated_seconds", accumulatedBaseSeconds)
                .putString("timer_subject", _timerSubject.value)
                .putString("timer_chapter", _timerChapter.value)
                .apply()

            com.example.service.StudyTimerService.pauseTimer(getApplication())
        } else {
            // Start
            val now = android.os.SystemClock.elapsedRealtime()
            val nowEpoch = System.currentTimeMillis()
            val todayKey = getTodayDateKey()
            timerStartRealtime = now
            timerSessionStartEpoch = nowEpoch
            timerSessionDate = todayKey
            _timerIsRunning.value = true

            prefs.edit()
                .putBoolean("timer_is_running", true)
                .putLong("timer_start_realtime", now)
                .putLong("timer_session_start_epoch", nowEpoch)
                .putString("timer_session_date", todayKey)
                .putInt("timer_accumulated_seconds", accumulatedBaseSeconds)
                .putString("timer_subject", _timerSubject.value)
                .putString("timer_chapter", _timerChapter.value)
                .apply()

            val targetSecs = if (_isCountdownMode.value) _dailyStudyTargetMinutes.value * 60 else 0
            com.example.service.StudyTimerService.startTimer(
                context = getApplication(),
                subject = _timerSubject.value,
                chapter = _timerChapter.value,
                startRealtime = now,
                baseSeconds = accumulatedBaseSeconds,
                isCountdown = _isCountdownMode.value,
                targetSeconds = targetSecs
            )

            startTimerLoop()
        }
    }

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerIsRunning.value) {
                kotlinx.coroutines.delay(500)
                checkAndHandleMidnightRollover()
                if (timerStartRealtime > 0L) {
                    val delta = ((android.os.SystemClock.elapsedRealtime() - timerStartRealtime) / 1000L).toInt().coerceAtLeast(0)
                    _timerSecondsElapsed.value = accumulatedBaseSeconds + delta
                }
                
                if (_isCountdownMode.value) {
                    val target = _dailyStudyTargetMinutes.value * 60
                    if (_timerSecondsElapsed.value >= target) {
                        stopAndSaveTimer()
                        break
                    }
                }
                
                if (_isPomodoroMode.value) {
                    // 50 minutes = 3000 seconds
                    if (_timerSecondsElapsed.value > 0 && _timerSecondsElapsed.value % 3000 == 0) {
                        _showStudyTargetSubtleAlert.value = true
                    }
                }
                
                checkStudyTargetAlert()
            }
        }
    }
    
    fun stopAndSaveTimer() {
        val now = android.os.SystemClock.elapsedRealtime()
        val totalSecs = if (_timerIsRunning.value && timerStartRealtime > 0L) {
            accumulatedBaseSeconds + ((now - timerStartRealtime) / 1000L).toInt().coerceAtLeast(0)
        } else {
            accumulatedBaseSeconds.coerceAtLeast(_timerSecondsElapsed.value)
        }

        val sub = _timerSubject.value
        var chap = _timerChapter.value
        
        if (chap.isBlank()) {
            val defaultChapters = mapOf(
                "Physics" to "Units and Measurements",
                "Chemistry" to "Some Basic Concepts of Chemistry",
                "Biology" to "The Living World",
                "Mock Test" to "Full Syllabus NEET"
            )
            chap = defaultChapters[sub] ?: "General Study"
        }
        
        if (totalSecs > 0) {
            addStudyLog(sub, chap, totalSecs)
        }
        
        _timerIsRunning.value = false
        timerJob?.cancel()
        timerStartRealtime = 0L
        timerSessionStartEpoch = 0L
        timerSessionDate = ""
        accumulatedBaseSeconds = 0
        _timerSecondsElapsed.value = 0
        _timerChapter.value = chap

        prefs.edit()
            .putBoolean("timer_is_running", false)
            .putLong("timer_start_realtime", 0L)
            .putLong("timer_session_start_epoch", 0L)
            .putString("timer_session_date", "")
            .putInt("timer_accumulated_seconds", 0)
            .putString("timer_subject", sub)
            .putString("timer_chapter", chap)
            .putInt("timer_last_known_seconds", 0)
            .apply()

        com.example.service.StudyTimerService.stopTimer(getApplication())
    }
    
    fun setTimerSubject(sub: String) {
        _timerSubject.value = sub
        prefs.edit().putString("timer_subject", sub).apply()
    }

    fun setTimerChapter(chap: String) {
        _timerChapter.value = chap
        prefs.edit().putString("timer_chapter", chap).apply()
    }

    fun addFlashcardDeck(name: String, icon: String = "📖", category: String = "Other") {
        viewModelScope.launch {
            dao.insertFlashcardDeck(FlashcardDeck(name = name, icon = icon, category = category))
        }
    }

    fun updateFlashcardDeck(id: Long, name: String, icon: String, category: String = "Other") {
        viewModelScope.launch {
            dao.insertFlashcardDeck(FlashcardDeck(id = id, name = name, icon = icon, category = category))
        }
    }

    fun createDeckWithGeneratedCards(
        name: String,
        category: String,
        icon: String,
        cards: List<com.example.data.GeneratedCard>
    ) {
        viewModelScope.launch {
            val newDeckId = dao.insertFlashcardDeck(FlashcardDeck(name = name, icon = icon, category = category))
            cards.forEach { card ->
                dao.insertFlashcardItem(FlashcardItem(deckId = newDeckId, front = card.front, back = card.back))
            }
        }
    }

    fun addGeneratedCardsToDeck(
        deckId: Long,
        cards: List<com.example.data.GeneratedCard>
    ) {
        viewModelScope.launch {
            cards.forEach { card ->
                dao.insertFlashcardItem(FlashcardItem(deckId = deckId, front = card.front, back = card.back))
            }
        }
    }

    fun deleteFlashcardDeck(id: Long) {
        viewModelScope.launch {
            dao.deleteFlashcardDeck(id)
            dao.deleteFlashcardItemsForDeck(id)
        }
    }

    fun addFlashcardItem(deckId: Long, front: String, back: String) {
        viewModelScope.launch {
            dao.insertFlashcardItem(FlashcardItem(deckId = deckId, front = front, back = back))
        }
    }

    fun updateFlashcardItem(item: FlashcardItem) {
        viewModelScope.launch {
            dao.insertFlashcardItem(item)
        }
    }

    fun deleteFlashcardItem(id: Long) {
        viewModelScope.launch {
            dao.deleteFlashcardItem(id)
        }
    }

    val allMistakeLogs: StateFlow<List<com.example.data.MistakeLog>> = dao.getAllMistakeLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMistakeLog(
        subject: String,
        question: String,
        mistakeType: String,
        chapter: String = "",
        pdfUri: String? = null,
        pdfName: String? = null,
        imageUri: String? = null
    ) {
        viewModelScope.launch {
            dao.insertMistakeLog(
                com.example.data.MistakeLog(
                    subject = subject,
                    question = question,
                    mistakeType = mistakeType,
                    chapter = chapter,
                    pdfUri = pdfUri,
                    pdfName = pdfName,
                    imageUri = imageUri
                )
            )
            recordSolvedQuestions(subject, 1)
        }
    }

    fun addBatchMistakeLogs(logs: List<com.example.data.MistakeLog>) {
        viewModelScope.launch {
            for (log in logs) {
                dao.insertMistakeLog(log)
            }
            logs.groupBy { it.subject.trim().lowercase() }.forEach { (subj, list) ->
                val cleanSubj = when {
                    subj.contains("phy") -> "Physics"
                    subj.contains("chem") -> "Chemistry"
                    subj.contains("bio") || subj.contains("botan") || subj.contains("zool") -> "Biology"
                    else -> "Biology"
                }
                recordSolvedQuestions(cleanSubj, list.size)
            }
        }
    }

    fun updateMistakeLog(log: com.example.data.MistakeLog) {
        viewModelScope.launch {
            dao.insertMistakeLog(log)
        }
    }

    fun advanceMistakeReview(id: Int, isHard: Boolean = false) {
        viewModelScope.launch {
            val list = dao.getAllMistakeLogs().firstOrNull() ?: return@launch
            val item = list.find { it.id == id } ?: return@launch
            val now = System.currentTimeMillis()
            val currentStage = item.reviewStage
            val newStage = if (isHard) currentStage else (currentStage + 1)
            val isNowMastered = newStage >= 3
            val nextIntervalDays = when (newStage) {
                0 -> 1L   // Day 1 (24h)
                1 -> 3L   // Day 3 (72h)
                2 -> 7L   // Day 7 (1 Week)
                3 -> 14L  // Day 14 (2 Weeks)
                else -> 30L // Mastered / Maintenance
            }
            val nextTime = now + (nextIntervalDays * 86400_000L)
            val updated = item.copy(
                reviewStage = newStage,
                reviewCount = item.reviewCount + 1,
                lastReviewedTime = now,
                nextReviewTime = nextTime,
                isMastered = isNowMastered
            )
            dao.insertMistakeLog(updated)
        }
    }

    fun resetMistakeReview(id: Int) {
        viewModelScope.launch {
            val list = dao.getAllMistakeLogs().firstOrNull() ?: return@launch
            val item = list.find { it.id == id } ?: return@launch
            val now = System.currentTimeMillis()
            val updated = item.copy(
                reviewStage = 0,
                lastReviewedTime = now,
                nextReviewTime = now + (1L * 86400_000L), // Repeat on Day 1
                isMastered = false
            )
            dao.insertMistakeLog(updated)
        }
    }

    fun markMistakeMastered(id: Int, mastered: Boolean = true) {
        viewModelScope.launch {
            val list = dao.getAllMistakeLogs().firstOrNull() ?: return@launch
            val item = list.find { it.id == id } ?: return@launch
            val now = System.currentTimeMillis()
            val updated = item.copy(
                isMastered = mastered,
                reviewStage = if (mastered) 4 else 0,
                lastReviewedTime = now,
                nextReviewTime = if (mastered) now + (60L * 86400_000L) else now
            )
            dao.insertMistakeLog(updated)
        }
    }

    fun setMistakeCustomReviewTime(id: Int, nextReviewTimestamp: Long) {
        viewModelScope.launch {
            val list = dao.getAllMistakeLogs().firstOrNull() ?: return@launch
            val item = list.find { it.id == id } ?: return@launch
            val updated = item.copy(
                nextReviewTime = nextReviewTimestamp,
                isMastered = false
            )
            dao.insertMistakeLog(updated)
        }
    }

    fun deleteMistakeLog(id: Int) {
        viewModelScope.launch {
            dao.deleteMistakeLog(id)
        }
    }

    // ==========================================
    // 🧠 LAKSHYA AI TEST ENGINE (NEET / JEE MAIN / JEE ADVANCED)
    // ==========================================

    val aiSavedTests: StateFlow<List<AiSavedTest>> = dao.getAllAiSavedTests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeExamGoal = MutableStateFlow(
        try {
            val saved = prefs.getString("active_exam_goal", ExamCategory.NEET.name) ?: ExamCategory.NEET.name
            ExamCategory.valueOf(saved)
        } catch (e: Exception) {
            ExamCategory.NEET
        }
    )
    val activeExamGoal: StateFlow<ExamCategory> = _activeExamGoal.asStateFlow()

    fun setActiveExamGoal(exam: ExamCategory) {
        prefs.edit().putString("active_exam_goal", exam.name).apply()
        _activeExamGoal.value = exam
        _activeTestExam.value = exam
    }

    private val _activeTestExam = MutableStateFlow(
        try {
            val saved = prefs.getString("active_exam_goal", ExamCategory.NEET.name) ?: ExamCategory.NEET.name
            ExamCategory.valueOf(saved)
        } catch (e: Exception) {
            ExamCategory.NEET
        }
    )
    val activeTestExam: StateFlow<ExamCategory> = _activeTestExam.asStateFlow()

    private val _activeTestType = MutableStateFlow(TestType.FULL_LENGTH)
    val activeTestType: StateFlow<TestType> = _activeTestType.asStateFlow()

    private val _activeTestTitle = MutableStateFlow("")
    val activeTestTitle: StateFlow<String> = _activeTestTitle.asStateFlow()

    private val _activeSavedTestId = MutableStateFlow<Long?>(null)
    val activeSavedTestId: StateFlow<Long?> = _activeSavedTestId.asStateFlow()

    private val _activeChapterName = MutableStateFlow<String?>(null)
    val activeChapterName: StateFlow<String?> = _activeChapterName.asStateFlow()

    private val _activeTestQuestions = MutableStateFlow<List<AiTestQuestion>>(emptyList())
    val activeTestQuestions: StateFlow<List<AiTestQuestion>> = _activeTestQuestions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _currentQuestionTimeSpentSeconds = MutableStateFlow(0)
    val currentQuestionTimeSpentSeconds: StateFlow<Int> = _currentQuestionTimeSpentSeconds.asStateFlow()

    private val _isAiTestGenerating = MutableStateFlow(false)
    val isAiTestGenerating: StateFlow<Boolean> = _isAiTestGenerating.asStateFlow()

    private val _aiTestGenerationError = MutableStateFlow<String?>(null)
    val aiTestGenerationError: StateFlow<String?> = _aiTestGenerationError.asStateFlow()

    private val _isTestActive = MutableStateFlow(false)
    val isTestActive: StateFlow<Boolean> = _isTestActive.asStateFlow()

    private val _isTestSubmitted = MutableStateFlow(false)
    val isTestSubmitted: StateFlow<Boolean> = _isTestSubmitted.asStateFlow()

    private val _testTimeRemainingSeconds = MutableStateFlow(0)
    val testTimeRemainingSeconds: StateFlow<Int> = _testTimeRemainingSeconds.asStateFlow()

    private val _testInitialDurationSeconds = MutableStateFlow(0)
    val testInitialDurationSeconds: StateFlow<Int> = _testInitialDurationSeconds.asStateFlow()

    private val _testScoreSummary = MutableStateFlow<TestScoreSummary?>(null)
    val testScoreSummary: StateFlow<TestScoreSummary?> = _testScoreSummary.asStateFlow()

    private val _activeTestInstitute = MutableStateFlow("Self/General")
    val activeTestInstitute: StateFlow<String> = _activeTestInstitute.asStateFlow()

    private val _cbtUnmasteredMistakesCount = MutableStateFlow(0)
    val cbtUnmasteredMistakesCount: StateFlow<Int> = _cbtUnmasteredMistakesCount.asStateFlow()

    private val _cbtUnmasteredSkippedCount = MutableStateFlow(0)
    val cbtUnmasteredSkippedCount: StateFlow<Int> = _cbtUnmasteredSkippedCount.asStateFlow()

    private val _cbtMasteredCount = MutableStateFlow(0)
    val cbtMasteredCount: StateFlow<Int> = _cbtMasteredCount.asStateFlow()

    private val _smartOcrProgress = MutableStateFlow<SmartOcrMultiPageEngine.ExtractionProgress?>(null)
    val smartOcrProgress: StateFlow<SmartOcrMultiPageEngine.ExtractionProgress?> = _smartOcrProgress.asStateFlow()

    private var testTimerJob: kotlinx.coroutines.Job? = null
    private var ocrExtractionJob: kotlinx.coroutines.Job? = null

    fun setExamCategory(category: ExamCategory) {
        _activeTestExam.value = category
    }

    fun setCurrentQuestionIndex(index: Int) {
        val total = _activeTestQuestions.value.size
        if (index in 0 until total && index != _currentQuestionIndex.value) {
            val oldIdx = _currentQuestionIndex.value
            val currentList = _activeTestQuestions.value
            if (oldIdx in 0 until currentList.size) {
                currentList[oldIdx].timeSpentSeconds = _currentQuestionTimeSpentSeconds.value
            }
            _currentQuestionIndex.value = index
            val newTime = if (index in 0 until currentList.size) currentList[index].timeSpentSeconds else 0
            _currentQuestionTimeSpentSeconds.value = newTime
        }
    }

    /**
     * Gathers all unmastered incorrect and skipped questions from previous tests (AiSavedTest) and MistakeLog.
     * Guarantees that any question already answered correctly in any test (tracked in MasteredMistakeTracker)
     * is STRICTLY excluded and will never appear again.
     */
    suspend fun getUnmasteredMistakesAndSkipped(
        context: Context,
        exam: ExamCategory,
        subjectFilter: String = "All",
        includeSkipped: Boolean = true,
        onlySkipped: Boolean = false
    ): List<AiTestQuestion> {
        val collected = mutableListOf<AiTestQuestion>()
        val savedList = dao.getAllAiSavedTests().firstOrNull() ?: emptyList()
        for (saved in savedList) {
            if (saved.examCategory.equals(exam.name, ignoreCase = true)) {
                try {
                    val arr = JSONArray(saved.questionsJson)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val sel = obj.optString("selectedOption", "")
                        val corr = obj.optString("correctOption", "")
                        val subj = obj.optString("subject", "")
                        val qText = obj.optString("questionText", "").trim()
                        if (qText.isBlank()) continue

                        // STRICT EXCLUSION: If already answered correctly in ANY test, NEVER repeat!
                        if (com.example.data.MasteredMistakeTracker.isMastered(context, qText)) {
                            continue
                        }

                        val isWrong = sel.isNotBlank() && !sel.equals(corr, ignoreCase = true)
                        val isSkipped = sel.isBlank() || sel.equals("Unattempted", true) || sel.equals("null", true)

                        val matchesFilter = when {
                            onlySkipped -> isSkipped
                            !includeSkipped -> isWrong
                            else -> isWrong || isSkipped
                        }

                        if (matchesFilter) {
                            if (subjectFilter == "All" || subj.equals(subjectFilter, ignoreCase = true)) {
                                if (collected.none { it.questionText == qText }) {
                                    collected.add(
                                        AiTestQuestion(
                                            id = collected.size + 1,
                                            subject = if (subj.isNotBlank()) subj else "General",
                                            chapter = obj.optString("chapter", "Revision"),
                                            pyqYear = if (isWrong) "Past Mistake (Revision)" else "Skipped Question (Revision)",
                                            questionText = qText,
                                            optionA = obj.optString("optionA", ""),
                                            optionB = obj.optString("optionB", ""),
                                            optionC = obj.optString("optionC", ""),
                                            optionD = obj.optString("optionD", ""),
                                            correctOption = corr,
                                            selectedOption = null,
                                            explanation = obj.optString("explanation", ""),
                                            hasImage = obj.optBoolean("hasImage", false),
                                            imageUrl = if (obj.has("imageUrl")) obj.optString("imageUrl") else null,
                                            diagramLabel = if (obj.has("diagramLabel")) obj.optString("diagramLabel") else null,
                                            diagramSvg = if (obj.has("diagramSvg")) obj.optString("diagramSvg") else null,
                                            diagramType = if (obj.has("diagramType")) obj.optString("diagramType") else null
                                        )
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Also check MistakeLog table for unmastered mistakes
        if (!onlySkipped && collected.size < 60) {
            val mistakeLogs = dao.getAllMistakeLogs().firstOrNull() ?: emptyList()
            for (m in mistakeLogs) {
                if (m.isMastered) continue
                val parsed = com.example.ui.screens.parseMistakeToAiQuestion(m)
                val text = parsed.questionText.trim()
                if (text.isBlank()) continue
                if (com.example.data.MasteredMistakeTracker.isMastered(context, text)) {
                    continue
                }
                if (subjectFilter == "All" || m.subject.equals(subjectFilter, ignoreCase = true)) {
                    if (collected.none { it.questionText == text }) {
                        collected.add(
                            parsed.copy(
                                id = collected.size + 1,
                                selectedOption = null,
                                pyqYear = "Mistake Notebook (Revision)"
                            )
                        )
                    }
                }
            }
        }
        return collected
    }

    fun refreshCbtMistakeStats(context: Context, exam: ExamCategory) {
        viewModelScope.launch {
            val list = getUnmasteredMistakesAndSkipped(context, exam, "All", includeSkipped = true, onlySkipped = false)
            val wrong = list.count { it.pyqYear.contains("Mistake", ignoreCase = true) }
            val skipped = list.count { it.pyqYear.contains("Skipped", ignoreCase = true) }
            val mastered = com.example.data.MasteredMistakeTracker.getMasteredCount(context)
            _cbtUnmasteredMistakesCount.value = wrong
            _cbtUnmasteredSkippedCount.value = skipped
            _cbtMasteredCount.value = mastered
        }
    }

    fun startFullLengthMockTest(
        context: Context,
        exam: ExamCategory,
        questionCount: Int = 30,
        customApiKey: String? = null,
        includeMistakesAndSkipped: Boolean = false,
        websiteSource: String? = null
    ) {
        if (_isAiTestGenerating.value) return
        _isAiTestGenerating.value = true
        _aiTestGenerationError.value = null
        _activeTestExam.value = exam
        _activeTestType.value = TestType.FULL_LENGTH
        _activeChapterName.value = null
        _activeSavedTestId.value = null
        val sourceLabel = if (!websiteSource.isNullOrBlank()) " [via ${websiteSource.take(20)}]" else ""
        _activeTestTitle.value = "${exam.displayName} 39Y PYQ Full Mock (${questionCount}Q)$sourceLabel"
        _activeTestInstitute.value = if (!websiteSource.isNullOrBlank()) websiteSource else "Lakshya NTA Mock Series"

        viewModelScope.launch {
            val collectedPast = if (includeMistakesAndSkipped) {
                getUnmasteredMistakesAndSkipped(
                    context = context,
                    exam = exam,
                    subjectFilter = "All",
                    includeSkipped = true,
                    onlySkipped = false
                )
            } else emptyList()

            val revisionPortion = if (collectedPast.isNotEmpty()) {
                val maxPast = (questionCount * 0.45f).toInt().coerceAtLeast(1).coerceAtMost(collectedPast.size)
                collectedPast.shuffled().take(maxPast)
            } else emptyList()

            val freshNeeded = (questionCount - revisionPortion.size).coerceAtLeast(0)

            val freshQuestions = if (freshNeeded > 0) {
                val result = GeminiAiTestGenerator.generateFullLengthMockTest(
                    context = context,
                    exam = exam,
                    questionCount = freshNeeded,
                    customApiKey = customApiKey,
                    websiteSource = websiteSource
                )
                result.getOrElse { emptyList() }
            } else emptyList()

            val combined = (revisionPortion + freshQuestions).mapIndexed { idx, q ->
                q.copy(id = idx + 1)
            }

            _isAiTestGenerating.value = false
            if (combined.isNotEmpty()) {
                _activeTestQuestions.value = combined
                _currentQuestionIndex.value = 0
                val totalCount = combined.size
                val durationMins = when {
                    totalCount <= 15 -> 20
                    totalCount <= 30 -> 45
                    totalCount <= 45 -> 60
                    totalCount <= 90 -> 100
                    totalCount == 180 -> 200 // NTA NEET standard 180Q = 200 mins (3h 20m)
                    else -> (totalCount * 1.15f).toInt().coerceAtLeast(20)
                }
                val totalSecs = durationMins * 60
                _testInitialDurationSeconds.value = totalSecs
                _testTimeRemainingSeconds.value = totalSecs
                _isTestActive.value = true
                _isTestSubmitted.value = false
                _testScoreSummary.value = null
                if (revisionPortion.isNotEmpty()) {
                    _activeTestTitle.value = "${exam.displayName} Full Mock (${totalCount}Q • Incl. ${revisionPortion.size} Revision Mistakes & Skipped)$sourceLabel"
                }
                startTestTimer()
            } else {
                _aiTestGenerationError.value = "Could not generate questions. Please verify your connection or AI settings."
            }
        }
    }

    fun startChapterWiseAiTest(
        context: Context,
        exam: ExamCategory = _activeTestExam.value,
        subject: String,
        chapter: String,
        questionCount: Int = 25,
        difficulty: String = "NEET Standard",
        customCommand: String? = null,
        websiteSource: String? = null,
        avoidRepeats: Boolean = true
    ) {
        if (_isAiTestGenerating.value) return
        _isAiTestGenerating.value = true
        _aiTestGenerationError.value = null
        _activeChapterName.value = chapter
        _activeSavedTestId.value = null

        val siteSuffix = if (!websiteSource.isNullOrBlank()) " [via ${websiteSource.take(20)}]" else ""
        _activeTestTitle.value = "$subject: $chapter (${questionCount}Q Chapter CBT)$siteSuffix"
        _activeTestInstitute.value = if (!websiteSource.isNullOrBlank()) websiteSource else "Lakshya Chapter CBT Series"
        _activeTestExam.value = exam
        _activeTestType.value = TestType.CHAPTER_WISE

        viewModelScope.launch {
            val result = GeminiAiTestGenerator.generateChapterWiseTest(
                context = context,
                exam = exam,
                subject = subject,
                chapter = chapter,
                topic = "All Topics",
                questionCount = questionCount.coerceIn(5, 200),
                difficulty = difficulty,
                customCommand = customCommand?.ifBlank { null },
                websiteSource = websiteSource?.ifBlank { null },
                avoidRepeats = avoidRepeats
            )

            _isAiTestGenerating.value = false
            result.onSuccess { questions ->
                if (questions.isNotEmpty()) {
                    val sanitized = QuestionImageFilter.sanitizeTestQuestions(questions)
                    val finalQuestions = sanitized.mapIndexed { idx, q -> q.copy(id = idx + 1) }
                    testTimerJob?.cancel()
                    _activeTestQuestions.value = finalQuestions
                    _currentQuestionIndex.value = 0

                    val totalCount = finalQuestions.size
                    val durationMins = when {
                        totalCount <= 15 -> 20
                        totalCount <= 25 -> 35
                        totalCount <= 45 -> 60
                        totalCount <= 90 -> 100
                        totalCount >= 180 -> 200 // NTA NEET standard 180Q = 200 mins
                        else -> (totalCount * 1.3f).toInt().coerceAtLeast(20)
                    }
                    val totalSecs = durationMins * 60
                    _testInitialDurationSeconds.value = totalSecs
                    _testTimeRemainingSeconds.value = totalSecs
                    _isTestActive.value = true
                    _isTestSubmitted.value = false
                    _testScoreSummary.value = null
                    startTestTimer()
                } else {
                    _aiTestGenerationError.value = "Could not generate questions for $chapter. Please verify your connection or AI settings."
                }
            }.onFailure { err ->
                _aiTestGenerationError.value = err.message ?: "Failed to generate Chapter CBT test"
            }
        }
    }

    fun extractTestFromPdf(
        context: Context,
        exam: ExamCategory,
        pdfUri: Uri,
        fileName: String,
        institute: String = "Self/General",
        startPage: Int = 1,
        endPage: Int? = null,
        autoCropDiagrams: Boolean = true,
        customApiKey: String? = null
    ) {
        extractTestFromDocumentOrImage(
            context = context,
            exam = exam,
            uri = pdfUri,
            fileName = fileName,
            institute = institute,
            startPage = startPage,
            endPage = endPage,
            autoCropDiagrams = autoCropDiagrams,
            customApiKey = customApiKey
        )
    }

    fun extractTestFromDocumentOrImage(
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
        customDurationMinutes: Int? = null,
        answerKeyUri: Uri? = null,
        answerKeyText: String? = null
    ) {
        if (_isAiTestGenerating.value) return
        _isAiTestGenerating.value = true
        _aiTestGenerationError.value = null
        _activeTestExam.value = exam
        _activeTestType.value = TestType.PDF_EXTRACTED
        _activeChapterName.value = null
        _activeTestInstitute.value = institute.ifBlank { "Self/General" }
        _smartOcrProgress.value = null

        val isImg = fileName.endsWith(".png", ignoreCase = true) || fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) || fileName.endsWith(".webp", ignoreCase = true)
        val prefix = if (isImg) "Abhi Magic 🪄" else "${institute.ifBlank { "Test" }} Paper"
        val langSuffix = if (scanLanguage == "Hindi") " (हिंदी)" else if (scanLanguage == "English") " (Eng)" else ""
        _activeTestTitle.value = "$prefix: ${fileName.take(20)}$langSuffix"

        ocrExtractionJob?.cancel()
        ocrExtractionJob = viewModelScope.launch {
            val result = SmartOcrMultiPageEngine.extractTest(
                context = context,
                exam = exam,
                uri = uri,
                fileName = fileName,
                institute = institute.ifBlank { "Self/General" },
                startPage = startPage,
                endPage = endPage,
                scanLanguage = scanLanguage,
                autoCropDiagrams = autoCropDiagrams,
                customApiKey = customApiKey,
                answerKeyUri = answerKeyUri,
                answerKeyText = answerKeyText,
                aiProvider = _aiProvider.value,
                openRouterModel = _openrouterSelectedModel.value,
                openRouterApiKey = _openrouterApiKey.value,
                onProgress = { prog ->
                    _smartOcrProgress.value = prog
                }
            )
            _isAiTestGenerating.value = false
            result.onSuccess { questions ->
                if (questions.isNotEmpty()) {
                    _activeTestQuestions.value = questions
                    _currentQuestionIndex.value = 0
                    val durationMins = if (customDurationMinutes != null && customDurationMinutes > 0) {
                        customDurationMinutes
                    } else {
                        when {
                            questions.size <= 15 -> 20
                            questions.size <= 45 -> 60
                            questions.size <= 90 -> 120
                            questions.size == 180 -> 200
                            else -> (questions.size * 1.5f).toInt().coerceAtLeast(15)
                        }
                    }
                    val totalSecs = durationMins * 60
                    _testInitialDurationSeconds.value = totalSecs
                    _testTimeRemainingSeconds.value = totalSecs
                    _isTestActive.value = true
                    _isTestSubmitted.value = false
                    _testScoreSummary.value = null
                    startTestTimer()

                    // Automatically save test to Room DB so it goes to "Custom Test" archive immediately
                    try {
                        val questionsJsonArray = JSONArray()
                        questions.forEach { q ->
                            val obj = JSONObject().apply {
                                put("id", q.id)
                                put("subject", q.subject)
                                put("chapter", q.chapter)
                                put("pyqYear", q.pyqYear)
                                put("questionText", q.questionText)
                                put("optionA", q.optionA)
                                put("optionB", q.optionB)
                                put("optionC", q.optionC)
                                put("optionD", q.optionD)
                                put("correctOption", q.correctOption)
                                put("selectedOption", q.selectedOption ?: "")
                                put("explanation", q.explanation)
                                put("isMarkedForReview", q.isMarkedForReview)
                                put("hasImage", q.hasImage)
                                put("difficulty", q.difficulty)
                                put("institute", q.institute)
                                put("isNumerical", q.isNumerical)
                                put("timeSpentSeconds", q.timeSpentSeconds)
                                if (q.imageUrl != null) put("imageUrl", q.imageUrl)
                                if (q.diagramLabel != null) put("diagramLabel", q.diagramLabel)
                                if (q.diagramSvg != null) put("diagramSvg", q.diagramSvg)
                                if (q.diagramType != null) put("diagramType", q.diagramType)
                                if (q.paperQNo != null) put("paperQNo", q.paperQNo)
                            }
                            questionsJsonArray.put(obj)
                        }
                        val insertedId = dao.insertAiSavedTest(
                            AiSavedTest(
                                title = _activeTestTitle.value,
                                examCategory = exam.name,
                                testType = "OCR_CUSTOM",
                                chapterName = null,
                                totalQuestions = questions.size,
                                score = 0,
                                maxScore = questions.size * 4,
                                accuracyPct = 0f,
                                timeTakenSeconds = 0,
                                questionsJson = questionsJsonArray.toString(),
                                institute = institute.ifBlank { "General" }
                            )
                        )
                        _activeSavedTestId.value = insertedId
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    _aiTestGenerationError.value = "Could not extract MCQs from this file. Please check if the document/photo contains readable questions."
                }
            }.onFailure { err ->
                _aiTestGenerationError.value = err.message ?: "OCR extraction failed"
            }
        }
    }

    fun stopExtractionAndLaunchActiveTest() {
        ocrExtractionJob?.cancel()
        _isAiTestGenerating.value = false
        val questions = _activeTestQuestions.value
        if (questions.isNotEmpty()) {
            _currentQuestionIndex.value = 0
            val totalSecs = questions.size * 120
            _testInitialDurationSeconds.value = totalSecs
            _testTimeRemainingSeconds.value = totalSecs
            _isTestActive.value = true
            _isTestSubmitted.value = false
            _testScoreSummary.value = null
            startTestTimer()
        } else {
            _aiTestGenerationError.value = "Extraction stopped. No questions were extracted yet."
        }
    }

    private fun startTestTimer() {
        testTimerJob?.cancel()
        val curIdx = _currentQuestionIndex.value
        val currentList = _activeTestQuestions.value
        if (curIdx in 0 until currentList.size) {
            _currentQuestionTimeSpentSeconds.value = currentList[curIdx].timeSpentSeconds
        } else {
            _currentQuestionTimeSpentSeconds.value = 0
        }
        testTimerJob = viewModelScope.launch {
            while (_testTimeRemainingSeconds.value > 0 && _isTestActive.value && !_isTestSubmitted.value) {
                kotlinx.coroutines.delay(1000)
                _testTimeRemainingSeconds.value -= 1
                _currentQuestionTimeSpentSeconds.value += 1

                // Sync live time spent with underlying question object
                val activeIdx = _currentQuestionIndex.value
                val qList = _activeTestQuestions.value
                if (activeIdx in 0 until qList.size) {
                    qList[activeIdx].timeSpentSeconds = _currentQuestionTimeSpentSeconds.value
                }
            }
            if (_testTimeRemainingSeconds.value <= 0 && _isTestActive.value && !_isTestSubmitted.value) {
                // Auto submit on time elapsed
                submitAiTestInternal()
            }
        }
    }

    fun selectQuestionOption(questionIndex: Int, option: String) {
        val currentList = _activeTestQuestions.value.map { it.copy() }.toMutableList()
        if (questionIndex in 0 until currentList.size) {
            val q = currentList[questionIndex]
            val optNorm = option.trim().uppercase()
            // In CBT examination, selecting an option selects it. Clicking Clear Response clears it.
            currentList[questionIndex] = q.copy(selectedOption = optNorm)
            _activeTestQuestions.value = currentList
        }
    }

    fun setNumericalResponse(questionIndex: Int, answer: String) {
        val currentList = _activeTestQuestions.value.map { it.copy() }.toMutableList()
        if (questionIndex in 0 until currentList.size) {
            val q = currentList[questionIndex]
            val cleaned = answer.trim()
            currentList[questionIndex] = q.copy(selectedOption = if (cleaned.isEmpty()) null else cleaned)
            _activeTestQuestions.value = currentList
        }
    }

    fun clearQuestionResponse(questionIndex: Int) {
        val currentList = _activeTestQuestions.value.toMutableList()
        if (questionIndex in 0 until currentList.size) {
            currentList[questionIndex] = currentList[questionIndex].copy(selectedOption = null)
            _activeTestQuestions.value = currentList
        }
    }

    fun toggleQuestionReview(questionIndex: Int) {
        val currentList = _activeTestQuestions.value.toMutableList()
        if (questionIndex in 0 until currentList.size) {
            val current = currentList[questionIndex].isMarkedForReview
            currentList[questionIndex] = currentList[questionIndex].copy(isMarkedForReview = !current)
            _activeTestQuestions.value = currentList
        }
    }

    fun submitAiTest() {
        submitAiTestInternal()
    }

    private fun submitAiTestInternal(isReviewOnly: Boolean = false) {
        testTimerJob?.cancel()
        _isTestActive.value = false
        _isTestSubmitted.value = true

        val questions = _activeTestQuestions.value
        val exam = _activeTestExam.value
        val correctMark = 4
        val negativeMark = when (exam) {
            ExamCategory.NEET -> 1
            ExamCategory.JEE_MAIN -> 1
            ExamCategory.JEE_ADVANCED -> 2
        }

        var totalCorrect = 0
        var totalIncorrect = 0
        var totalUnattempted = 0
        var totalScore = 0
        val maxScore = questions.size * correctMark

        val subjectMap = mutableMapOf<String, MutableList<AiTestQuestion>>()
        questions.forEach { q ->
            val subj = if (q.subject.isBlank()) "General" else q.subject
            subjectMap.getOrPut(subj) { mutableListOf() }.add(q)
            if (q.selectedOption == null) {
                totalUnattempted++
            } else if (q.isUserAnswerCorrect()) {
                totalCorrect++
                totalScore += correctMark
            } else {
                totalIncorrect++
                totalScore -= negativeMark
            }
        }

        val subjectBreakdown = mutableMapOf<String, SubjectScore>()
        subjectMap.forEach { (subj, qList) ->
            var scCorrect = 0
            var scIncorrect = 0
            var scUnattempted = 0
            var scMarks = 0
            qList.forEach { q ->
                if (q.selectedOption == null) {
                    scUnattempted++
                } else if (q.isUserAnswerCorrect()) {
                    scCorrect++
                    scMarks += correctMark
                } else {
                    scIncorrect++
                    scMarks -= negativeMark
                }
            }
            val posMarks = scCorrect * correctMark
            val negMarks = scIncorrect * negativeMark
            val acc = if (scCorrect + scIncorrect > 0) (scCorrect.toFloat() / (scCorrect + scIncorrect)) * 100f else 0f
            subjectBreakdown[subj] = SubjectScore(
                correct = scCorrect,
                incorrect = scIncorrect,
                unattempted = scUnattempted,
                marks = scMarks,
                maxMarks = qList.size * correctMark,
                positiveMarks = posMarks,
                negativeMarks = negMarks,
                accuracy = acc
            )
        }

        val attempted = totalCorrect + totalIncorrect
        val accuracy = if (attempted > 0) (totalCorrect.toFloat() / attempted) * 100f else 0f
        val timeSpent = (_testInitialDurationSeconds.value - _testTimeRemainingSeconds.value).coerceAtLeast(0)

        val rankTier = when (exam) {
            ExamCategory.NEET -> when {
                totalScore >= maxScore * 0.85 -> "🏆 Top 1% (AIIMS / Top MAMC Tier)"
                totalScore >= maxScore * 0.70 -> "🌟 Top 5% (Top GMC / Govt Medical Tier)"
                totalScore >= maxScore * 0.50 -> "🎯 Top 15% (State GMC Tier)"
                else -> "📈 Needs Focused NCERT Revision"
            }
            ExamCategory.JEE_MAIN -> when {
                totalScore >= maxScore * 0.85 -> "🏆 Top 1% (Top NIT & IIIT CSE Tier)"
                totalScore >= maxScore * 0.70 -> "🌟 Top 5% (Top NIT Tier)"
                totalScore >= maxScore * 0.50 -> "🎯 Top 15% (State Govt Engg / NIT Tier)"
                else -> "📈 Needs Focused Concept & PYQ Revision"
            }
            ExamCategory.JEE_ADVANCED -> when {
                totalScore >= maxScore * 0.85 -> "🏆 Top 1% (Top IIT Tier)"
                totalScore >= maxScore * 0.70 -> "🌟 Top 5% (Top IIT Core Tier)"
                totalScore >= maxScore * 0.50 -> "🎯 Top 15% (IIT Qualified Tier)"
                else -> "📈 Needs Advanced Concept Practice"
            }
        }

        val summary = TestScoreSummary(
            totalScore = totalScore,
            maxScore = maxScore,
            correctCount = totalCorrect,
            incorrectCount = totalIncorrect,
            unattemptedCount = totalUnattempted,
            subjectBreakdown = subjectBreakdown,
            accuracyPercentage = accuracy,
            rankTier = rankTier,
            timeTakenSeconds = timeSpent
        )
        _testScoreSummary.value = summary

        // Automatically sync to Room DB (MockTest, DailyPractice, and AiSavedTest)
        if (!isReviewOnly) {
            syncTestToAppDatabase(summary)
        }
    }

    private fun syncTestToAppDatabase(summary: TestScoreSummary) {
        viewModelScope.launch {
            val questions = _activeTestQuestions.value
            val exam = _activeTestExam.value
            val title = _activeTestTitle.value

            // 1. Save to AiSavedTest
            val questionsJsonArray = JSONArray()
            questions.forEach { q ->
                val obj = JSONObject().apply {
                    put("id", q.id)
                    put("subject", q.subject)
                    put("chapter", q.chapter)
                    put("pyqYear", q.pyqYear)
                    put("questionText", q.questionText)
                    put("optionA", q.optionA)
                    put("optionB", q.optionB)
                    put("optionC", q.optionC)
                    put("optionD", q.optionD)
                    put("correctOption", q.correctOption)
                    put("selectedOption", q.selectedOption ?: "")
                    put("explanation", q.explanation)
                    put("isMarkedForReview", q.isMarkedForReview)
                    put("hasImage", q.hasImage)
                    put("difficulty", q.difficulty)
                    put("institute", q.institute)
                    if (q.imageUrl != null) put("imageUrl", q.imageUrl)
                    if (q.diagramLabel != null) put("diagramLabel", q.diagramLabel)
                    if (q.diagramSvg != null) put("diagramSvg", q.diagramSvg)
                    if (q.diagramType != null) put("diagramType", q.diagramType)
                    if (q.paperQNo != null) put("paperQNo", q.paperQNo)
                }
                questionsJsonArray.put(obj)
            }

            var easyCount = 0
            var medCount = 0
            var hardCount = 0
            questions.forEach { q ->
                when (q.difficulty.lowercase()) {
                    "easy" -> easyCount++
                    "hard" -> hardCount++
                    else -> medCount++
                }
            }
            val diffDistribution = "Easy: $easyCount • Medium: $medCount • Hard: $hardCount"

            val currentSavedId = _activeSavedTestId.value ?: 0L
            val savedId = dao.insertAiSavedTest(
                AiSavedTest(
                    id = currentSavedId,
                    title = title,
                    examCategory = exam.name,
                    testType = _activeTestType.value.name,
                    chapterName = _activeChapterName.value,
                    institute = _activeTestInstitute.value,
                    difficultyDistribution = diffDistribution,
                    totalQuestions = questions.size,
                    score = summary.totalScore,
                    maxScore = summary.maxScore,
                    accuracyPct = summary.accuracyPercentage,
                    timeTakenSeconds = summary.timeTakenSeconds,
                    questionsJson = questionsJsonArray.toString()
                )
            )
            _activeSavedTestId.value = savedId

            // 2. Also register in MockTest table so it seamlessly appears on Dashboard & PDF Scorecards!
            val phyMarks = summary.subjectBreakdown["Physics"]?.marks ?: 0
            val chemMarks = summary.subjectBreakdown["Chemistry"]?.marks ?: 0
            val botZooMarks = (summary.subjectBreakdown["Botany"]?.marks ?: 0) + (summary.subjectBreakdown["Zoology"]?.marks ?: 0)
            val mathMarks = (summary.subjectBreakdown["Mathematics"]?.marks ?: 0) + (summary.subjectBreakdown["Maths"]?.marks ?: 0)
            val bioMarks = summary.subjectBreakdown["Biology"]?.marks ?: if (botZooMarks != 0) botZooMarks else mathMarks
            val totalNegMarks = summary.incorrectCount * 1

            // Detect if this test is exclusively for a single subject (e.g. Physics chapterwise test)
            val activeSubjects = summary.subjectBreakdown.filter { it.value.maxMarks > 0 }.keys
            val singleSubjectTag = when {
                activeSubjects.size == 1 && activeSubjects.contains("Physics") -> "Single Subject (Physics)\n"
                activeSubjects.size == 1 && activeSubjects.contains("Chemistry") -> "Single Subject (Chemistry)\n"
                activeSubjects.size == 1 && (activeSubjects.contains("Biology") || activeSubjects.contains("Botany") || activeSubjects.contains("Zoology") || activeSubjects.contains("Mathematics") || activeSubjects.contains("Maths")) -> "Single Subject (Biology)\n"
                else -> ""
            }

            dao.insertMockTest(
                MockTest(
                    testName = title,
                    score = summary.totalScore,
                    physics = phyMarks,
                    chemistry = chemMarks,
                    biology = bioMarks,
                    negative = totalNegMarks,
                    timestamp = System.currentTimeMillis(),
                    geminiAnalysis = "${singleSubjectTag}Max Marks: ${summary.maxScore}\nAI ${exam.displayName} Test. Score: ${summary.totalScore} / ${summary.maxScore}. Accuracy: ${String.format(Locale.getDefault(), "%.1f", summary.accuracyPercentage)}%. Tier: ${summary.rankTier}."
                )
            )

            // 3. Update today's DailyPractice solved counts
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val existingDaily = dao.getAllDailyPractices().firstOrNull()?.firstOrNull { it.date == todayStr }
            val phySolved = summary.subjectBreakdown["Physics"]?.let { it.correct + it.incorrect } ?: 0
            val chemSolved = summary.subjectBreakdown["Chemistry"]?.let { it.correct + it.incorrect } ?: 0
            val botZooSolved = (summary.subjectBreakdown["Botany"]?.let { it.correct + it.incorrect } ?: 0) + (summary.subjectBreakdown["Zoology"]?.let { it.correct + it.incorrect } ?: 0)
            val mathSolved = (summary.subjectBreakdown["Mathematics"]?.let { it.correct + it.incorrect } ?: 0) + (summary.subjectBreakdown["Maths"]?.let { it.correct + it.incorrect } ?: 0)
            val directBioSolved = summary.subjectBreakdown["Biology"]?.let { it.correct + it.incorrect } ?: 0
            var bioSolved = if (directBioSolved > 0) directBioSolved else if (botZooSolved > 0) botZooSolved else mathSolved

            // If questions were under General or other names, allocate remainder so total practice count is always 100% accurate
            val totalAttempted = summary.correctCount + summary.incorrectCount
            if ((phySolved + chemSolved + bioSolved) < totalAttempted) {
                val remainder = totalAttempted - (phySolved + chemSolved + bioSolved)
                bioSolved += remainder
            }

            val updatedDaily = existingDaily?.copy(
                physicsSolved = existingDaily.physicsSolved + phySolved,
                chemistrySolved = existingDaily.chemistrySolved + chemSolved,
                biologySolved = existingDaily.biologySolved + bioSolved
            ) ?: DailyPractice(
                date = todayStr,
                physicsSolved = phySolved,
                chemistrySolved = chemSolved,
                biologySolved = bioSolved
            )
            dao.insertDailyPractice(updatedDaily)

            // 4. CRITICAL: Add test duration to StudyLog so it immediately reflects in Dashboard Study Hours & 6h Goal!
            val actualSeconds = if (summary.timeTakenSeconds > 0) summary.timeTakenSeconds else 60
            val primarySubject = when {
                summary.subjectBreakdown.size == 1 -> summary.subjectBreakdown.keys.first()
                questions.map { it.subject }.distinct().size == 1 -> questions.first().subject
                exam == ExamCategory.NEET -> "Biology"
                else -> "Physics"
            }
            dao.insertStudyLog(
                StudyLog(
                    subject = primarySubject,
                    chapter = "$title (Test)",
                    durationSeconds = actualSeconds,
                    timestamp = System.currentTimeMillis()
                )
            )

            // 5. CRITICAL: Automatically track mastered questions vs mistakes!
            val correctOnes = questions.filter { it.selectedOption != null && it.selectedOption.equals(it.correctOption, ignoreCase = true) }
            com.example.data.MasteredMistakeTracker.markMastered(getApplication(), correctOnes)

            // Update matching MistakeLog entries in Room DB to isMastered = true & reviewStage = 4 (Mastered)
            if (correctOnes.isNotEmpty()) {
                val allMistakes = dao.getAllMistakeLogs().firstOrNull() ?: emptyList()
                correctOnes.forEach { corrQ ->
                    val corrFp = com.example.data.MasteredMistakeTracker.extractFingerprint(corrQ.questionText)
                    val matching = allMistakes.firstOrNull { m ->
                        val mFp = com.example.data.MasteredMistakeTracker.extractFingerprint(m.question)
                        mFp.contains(corrFp.take(30)) || corrFp.contains(mFp.take(30))
                    }
                    if (matching != null && !matching.isMastered) {
                        dao.insertMistakeLog(matching.copy(isMastered = true, reviewStage = 4))
                    }
                }
            }

            val incorrectOnes = questions.filter { it.selectedOption != null && !it.selectedOption.equals(it.correctOption, ignoreCase = true) }
            incorrectOnes.forEach { q ->
                com.example.data.MasteredMistakeTracker.unmarkMastered(getApplication(), q.questionText)
                dao.insertMistakeLog(
                    com.example.data.MistakeLog(
                        subject = if (q.subject.isNotBlank()) q.subject else primarySubject,
                        question = buildMistakeFormattedText(q),
                        mistakeType = "${exam.displayName} 39Y PYQ Mistake (${q.chapter.ifBlank { "General" }})",
                        chapter = q.chapter
                    )
                )
            }
        }
    }

    private fun buildMistakeFormattedText(q: AiTestQuestion): String {
        val sb = StringBuilder()
        sb.append(q.questionText.trim())
        if (q.optionA.isNotBlank()) sb.append("\n(A) ").append(q.optionA.trim())
        if (q.optionB.isNotBlank()) sb.append("\n(B) ").append(q.optionB.trim())
        if (q.optionC.isNotBlank()) sb.append("\n(C) ").append(q.optionC.trim())
        if (q.optionD.isNotBlank()) sb.append("\n(D) ").append(q.optionD.trim())
        val userAns = q.selectedOption ?: "Unattempted"
        sb.append("\n[Your Answer: Option ").append(userAns).append(" | Correct: Option ").append(q.correctOption).append("]")
        if (q.explanation.isNotBlank()) {
            sb.append("\n💡 Solution: ").append(q.explanation.trim())
        }
        return sb.toString()
    }

    fun addAllIncorrectToMistakesNotebook() {
        viewModelScope.launch {
            val questions = _activeTestQuestions.value
            val incorrectOnes = questions.filter { it.selectedOption != null && !it.selectedOption.equals(it.correctOption, ignoreCase = true) }
            incorrectOnes.forEach { q ->
                dao.insertMistakeLog(
                    com.example.data.MistakeLog(
                        subject = q.subject,
                        question = buildMistakeFormattedText(q),
                        mistakeType = "${_activeTestExam.value.displayName} PYQ Mistake",
                        chapter = q.chapter
                    )
                )
            }
        }
    }

    fun addSingleQuestionToMistakes(q: AiTestQuestion) {
        viewModelScope.launch {
            dao.insertMistakeLog(
                com.example.data.MistakeLog(
                    subject = q.subject,
                    question = buildMistakeFormattedText(q),
                    mistakeType = "${_activeTestExam.value.displayName} PYQ Mistake",
                    chapter = q.chapter
                )
            )
        }
    }

    /**
     * Instantly launches a live CBT Revision Retest containing strictly the questions
     * the user answered incorrectly or skipped in the current test.
     */
    fun startImmediateWrongQuestionsRetest(includeSkipped: Boolean = true) {
        val questions = _activeTestQuestions.value
        val candidateQuestions = questions.filter { q ->
            val isWrong = q.selectedOption != null && !q.selectedOption.equals(q.correctOption, ignoreCase = true)
            val isSkipped = q.selectedOption == null || q.selectedOption.isNullOrBlank()
            isWrong || (includeSkipped && isSkipped)
        }
        if (candidateQuestions.isEmpty()) return

        val retestList = candidateQuestions.mapIndexed { idx, q ->
            q.copy(
                id = idx + 1,
                selectedOption = null,
                isMarkedForReview = false
            )
        }
        testTimerJob?.cancel()
        _activeTestQuestions.value = retestList
        _activeTestTitle.value = "📕 Revision Retest: ${retestList.size} Wrong & Skipped Questions"
        _currentQuestionIndex.value = 0
        val totalSecs = retestList.size * 120
        _testInitialDurationSeconds.value = totalSecs
        _testTimeRemainingSeconds.value = totalSecs
        _isTestActive.value = true
        _isTestSubmitted.value = false
        _testScoreSummary.value = null
        startTestTimer()
    }

    /**
     * Gathers all stored incorrect and skipped questions from past saved AI tests or mistake logs
     * to launch a dedicated CBT Revision Test for mastering weak areas.
     * Questions answered correctly in any test are marked as Mastered and will NEVER appear again!
     */
    fun startMistakesRevisionTestFromSaved(
        context: Context,
        exam: ExamCategory,
        subjectFilter: String = "All",
        customApiKey: String? = null,
        includeSkipped: Boolean = true,
        onlySkipped: Boolean = false,
        websiteSource: String? = null
    ) {
        if (_isAiTestGenerating.value) return
        _isAiTestGenerating.value = true
        _aiTestGenerationError.value = null
        _activeTestExam.value = exam
        _activeTestType.value = TestType.FULL_LENGTH
        _activeChapterName.value = "Mistakes Revision"
        val siteTag = if (!websiteSource.isNullOrBlank()) " [via ${websiteSource.take(20)}]" else ""
        _activeTestTitle.value = "📕 ${exam.displayName} Mistakes & Weak Area Revision (${subjectFilter})$siteTag"

        viewModelScope.launch {
            val collectedMistakes = mutableListOf<AiTestQuestion>()
            val savedList = dao.getAllAiSavedTests().firstOrNull() ?: emptyList()
            for (saved in savedList) {
                if (saved.examCategory.equals(exam.name, ignoreCase = true)) {
                    try {
                        val arr = JSONArray(saved.questionsJson)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            val sel = obj.optString("selectedOption", "")
                            val corr = obj.optString("correctOption", "")
                            val subj = obj.optString("subject", "")
                            val qText = obj.optString("questionText", "").trim()
                            if (qText.isBlank()) continue

                            // CRITICAL: If the student has already answered this question correctly, EXCLUDE IT!
                            if (com.example.data.MasteredMistakeTracker.isMastered(context, qText)) {
                                continue
                            }

                            val isWrong = sel.isNotBlank() && !sel.equals(corr, ignoreCase = true)
                            val isSkipped = sel.isBlank() || sel.equals("Unattempted", true) || sel.equals("null", true)

                            val matchesFilter = when {
                                onlySkipped -> isSkipped
                                !includeSkipped -> isWrong
                                else -> isWrong || isSkipped
                            }

                            if (matchesFilter) {
                                if (subjectFilter == "All" || subj.equals(subjectFilter, ignoreCase = true)) {
                                    if (collectedMistakes.none { it.questionText == qText }) {
                                        collectedMistakes.add(
                                            AiTestQuestion(
                                                id = collectedMistakes.size + 1,
                                                subject = subj,
                                                chapter = obj.optString("chapter", "Revision"),
                                                pyqYear = if (isWrong) "Past Mistake" else "Skipped Question",
                                                questionText = qText,
                                                optionA = obj.optString("optionA", ""),
                                                optionB = obj.optString("optionB", ""),
                                                optionC = obj.optString("optionC", ""),
                                                optionD = obj.optString("optionD", ""),
                                                correctOption = corr,
                                                selectedOption = null,
                                                explanation = obj.optString("explanation", ""),
                                                hasImage = obj.optBoolean("hasImage", false),
                                                imageUrl = if (obj.has("imageUrl")) obj.optString("imageUrl") else null,
                                                diagramLabel = if (obj.has("diagramLabel")) obj.optString("diagramLabel") else null,
                                                diagramSvg = if (obj.has("diagramSvg")) obj.optString("diagramSvg") else null,
                                                diagramType = if (obj.has("diagramType")) obj.optString("diagramType") else null
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // Also search MistakeLog for unmastered mistakes
            if (!onlySkipped && collectedMistakes.size < 30) {
                val mistakeLogs = dao.getAllMistakeLogs().firstOrNull() ?: emptyList()
                for (m in mistakeLogs) {
                    if (collectedMistakes.size >= 30) break
                    val parsed = com.example.ui.screens.parseMistakeToAiQuestion(m)
                    val text = parsed.questionText.trim()
                    if (text.isBlank()) continue
                    if (com.example.data.MasteredMistakeTracker.isMastered(context, text)) {
                        continue
                    }
                    if (subjectFilter == "All" || m.subject.equals(subjectFilter, ignoreCase = true)) {
                        if (collectedMistakes.none { it.questionText == text }) {
                            collectedMistakes.add(
                                parsed.copy(
                                    id = collectedMistakes.size + 1,
                                    selectedOption = null,
                                    pyqYear = "Mistake Notebook"
                                )
                            )
                        }
                    }
                }
            }

            if (collectedMistakes.isNotEmpty()) {
                _isAiTestGenerating.value = false
                val finalList = collectedMistakes.take(30)
                _activeTestQuestions.value = finalList
                _currentQuestionIndex.value = 0
                val totalSecs = finalList.size * 120
                _testInitialDurationSeconds.value = totalSecs
                _testTimeRemainingSeconds.value = totalSecs
                _isTestActive.value = true
                _isTestSubmitted.value = false
                _testScoreSummary.value = null
                startTestTimer()
            } else {
                // If all previous mistakes and skipped questions have been mastered!
                val fallbackResult = GeminiAiTestGenerator.generateFullLengthMockTest(
                    context = context,
                    exam = exam,
                    questionCount = 15,
                    customApiKey = customApiKey,
                    websiteSource = websiteSource
                )
                _isAiTestGenerating.value = false
                fallbackResult.onSuccess { questions ->
                    val filtered = if (subjectFilter == "All") questions else questions.filter { it.subject.equals(subjectFilter, ignoreCase = true) }
                    val finalQ = if (filtered.isNotEmpty()) filtered else questions
                    _activeTestQuestions.value = finalQ.mapIndexed { idx, q -> q.copy(id = idx + 1) }
                    _activeTestTitle.value = "🏆 Mastered All Previous Mistakes! Fresh Revision (${subjectFilter})"
                    _currentQuestionIndex.value = 0
                    val totalSecs = finalQ.size * 120
                    _testInitialDurationSeconds.value = totalSecs
                    _testTimeRemainingSeconds.value = totalSecs
                    _isTestActive.value = true
                    _isTestSubmitted.value = false
                    _testScoreSummary.value = null
                    startTestTimer()
                }.onFailure { err ->
                    _aiTestGenerationError.value = err.message ?: "Failed to generate revision test"
                }
            }
        }
    }

    fun loadSavedAiTest(savedTest: AiSavedTest) {
        try {
            val list = mutableListOf<AiTestQuestion>()
            val arr = JSONArray(savedTest.questionsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val hasImg = obj.optBoolean("hasImage", false) || obj.has("diagramLabel") || obj.has("diagramSvg") || obj.has("imageUrl")
                val imgUrl = if (obj.has("imageUrl") && obj.optString("imageUrl").isNotBlank()) obj.optString("imageUrl") else null
                val diagLabel = if (obj.has("diagramLabel") && obj.optString("diagramLabel").isNotBlank()) obj.optString("diagramLabel") else null
                val diagSvg = if (obj.has("diagramSvg") && obj.optString("diagramSvg").isNotBlank()) obj.optString("diagramSvg") else null
                val diagType = if (obj.has("diagramType") && obj.optString("diagramType").isNotBlank()) obj.optString("diagramType") else null

                val rawDiff = obj.optString("difficulty", "Medium")
                val inst = obj.optString("institute", savedTest.institute)
                val paperQNo = obj.optString("paperQNo", "").ifBlank { null }

                list.add(
                    AiTestQuestion(
                        id = obj.optInt("id", i + 1),
                        subject = obj.optString("subject", "Physics"),
                        chapter = obj.optString("chapter", ""),
                        pyqYear = obj.optString("pyqYear", "PYQ"),
                        questionText = obj.optString("questionText", ""),
                        optionA = obj.optString("optionA", ""),
                        optionB = obj.optString("optionB", ""),
                        optionC = obj.optString("optionC", ""),
                        optionD = obj.optString("optionD", ""),
                        correctOption = obj.optString("correctOption", "A"),
                        selectedOption = obj.optString("selectedOption", "").ifBlank { null },
                        explanation = obj.optString("explanation", ""),
                        isMarkedForReview = obj.optBoolean("isMarkedForReview", false),
                        hasImage = hasImg,
                        imageUrl = imgUrl,
                        diagramLabel = diagLabel,
                        diagramSvg = diagSvg,
                        diagramType = diagType,
                        difficulty = rawDiff,
                        institute = inst,
                        paperQNo = paperQNo
                    )
                )
            }
            if (list.isNotEmpty()) {
                val sanitizedList = QuestionImageFilter.sanitizeTestQuestions(list)
                _activeSavedTestId.value = savedTest.id
                _activeTestQuestions.value = sanitizedList
                _activeTestTitle.value = savedTest.title
                _activeTestInstitute.value = savedTest.institute
                _activeTestExam.value = try { ExamCategory.valueOf(savedTest.examCategory) } catch (e: Exception) { ExamCategory.NEET }
                _activeTestType.value = try { TestType.valueOf(savedTest.testType) } catch (e: Exception) { TestType.FULL_LENGTH }
                _activeChapterName.value = savedTest.chapterName
                _currentQuestionIndex.value = 0
                _isTestActive.value = false
                _isTestSubmitted.value = true

                // Calculate summary for review without re-syncing duplicate records
                submitAiTestInternal(isReviewOnly = true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Launch a fresh re-attempt of any previously saved or completed test.
     * Clears previous student answers and sets a fresh CBT timer!
     */
    fun reattemptSavedAiTest(savedTest: AiSavedTest) {
        try {
            val list = mutableListOf<AiTestQuestion>()
            val arr = JSONArray(savedTest.questionsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val hasImg = obj.optBoolean("hasImage", false) || obj.has("diagramLabel") || obj.has("diagramSvg") || obj.has("imageUrl")
                val imgUrl = if (obj.has("imageUrl") && obj.optString("imageUrl").isNotBlank()) obj.optString("imageUrl") else null
                val diagLabel = if (obj.has("diagramLabel") && obj.optString("diagramLabel").isNotBlank()) obj.optString("diagramLabel") else null
                val diagSvg = if (obj.has("diagramSvg") && obj.optString("diagramSvg").isNotBlank()) obj.optString("diagramSvg") else null
                val diagType = if (obj.has("diagramType") && obj.optString("diagramType").isNotBlank()) obj.optString("diagramType") else null
                val paperQNo = obj.optString("paperQNo", "").ifBlank { null }

                val rawDiff = obj.optString("difficulty", "Medium")
                val inst = obj.optString("institute", savedTest.institute)

                list.add(
                    AiTestQuestion(
                        id = obj.optInt("id", i + 1),
                        subject = obj.optString("subject", "Physics"),
                        chapter = obj.optString("chapter", ""),
                        pyqYear = obj.optString("pyqYear", "PYQ"),
                        questionText = obj.optString("questionText", ""),
                        optionA = obj.optString("optionA", ""),
                        optionB = obj.optString("optionB", ""),
                        optionC = obj.optString("optionC", ""),
                        optionD = obj.optString("optionD", ""),
                        correctOption = obj.optString("correctOption", "A"),
                        selectedOption = null, // FRESH BLANK ANSWER
                        explanation = obj.optString("explanation", ""),
                        isMarkedForReview = false, // FRESH
                        hasImage = hasImg,
                        imageUrl = imgUrl,
                        diagramLabel = diagLabel,
                        diagramSvg = diagSvg,
                        diagramType = diagType,
                        difficulty = rawDiff,
                        institute = inst,
                        paperQNo = paperQNo
                    )
                )
            }
            if (list.isNotEmpty()) {
                val sanitizedList = QuestionImageFilter.sanitizeTestQuestions(list)
                testTimerJob?.cancel()
                _activeSavedTestId.value = null
                _activeTestQuestions.value = sanitizedList
                _activeTestTitle.value = if (savedTest.title.contains("Re-attempt")) savedTest.title else "${savedTest.title} (Re-attempt)"
                _activeTestInstitute.value = savedTest.institute
                _activeTestExam.value = try { ExamCategory.valueOf(savedTest.examCategory) } catch (e: Exception) { ExamCategory.NEET }
                _activeTestType.value = try { TestType.valueOf(savedTest.testType) } catch (e: Exception) { TestType.FULL_LENGTH }
                _activeChapterName.value = savedTest.chapterName
                _currentQuestionIndex.value = 0

                val totalSecs = (list.size * 60).coerceAtLeast(300)
                _testInitialDurationSeconds.value = totalSecs
                _testTimeRemainingSeconds.value = totalSecs
                _isTestActive.value = true
                _isTestSubmitted.value = false
                _testScoreSummary.value = null
                startTestTimer()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Re-attempt the currently loaded test immediately from the solutions/scorecard screen.
     */
    fun reattemptCurrentTest() {
        val questions = _activeTestQuestions.value
        if (questions.isEmpty()) return
        testTimerJob?.cancel()
        val cleanQuestions = questions.map { q ->
            q.copy(selectedOption = null, isMarkedForReview = false, timeSpentSeconds = 0)
        }
        _activeTestQuestions.value = cleanQuestions
        _currentQuestionIndex.value = 0
        val totalSecs = (cleanQuestions.size * 60).coerceAtLeast(300)
        _testInitialDurationSeconds.value = totalSecs
        _testTimeRemainingSeconds.value = totalSecs
        _isTestActive.value = true
        _isTestSubmitted.value = false
        _testScoreSummary.value = null
        startTestTimer()
    }

    fun deleteSavedAiTest(id: Long) {
        viewModelScope.launch {
            dao.deleteAiSavedTest(id)
        }
    }

    fun updateAiSavedTest(savedTest: AiSavedTest, newTitle: String, newCategory: String) {
        viewModelScope.launch {
            dao.insertAiSavedTest(
                savedTest.copy(
                    title = newTitle.trim(),
                    institute = newCategory.trim()
                )
            )
        }
    }

    fun setCustomTestDuration(minutes: Int) {
        if (minutes > 0) {
            val totalSecs = minutes * 60
            _testInitialDurationSeconds.value = totalSecs
            _testTimeRemainingSeconds.value = totalSecs
        }
    }

    fun resetAiTestState() {
        testTimerJob?.cancel()
        ocrExtractionJob?.cancel()
        _smartOcrProgress.value = null
        _isAiTestGenerating.value = false
        _isTestActive.value = false
        _isTestSubmitted.value = false
        _activeSavedTestId.value = null
        _activeTestQuestions.value = emptyList()
        _currentQuestionIndex.value = 0
        _currentQuestionTimeSpentSeconds.value = 0
        _testScoreSummary.value = null
        _aiTestGenerationError.value = null
    }

    val weakTopics: StateFlow<List<WeakTopic>> = dao.getAllWeakTopics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getWeakTopicsBySubject(subject: String): Flow<List<WeakTopic>> {
        return dao.getWeakTopicsBySubject(subject)
    }

    fun addWeakTopic(
        subject: String,
        chapter: String,
        topicName: String,
        mistakesCount: Int = 1,
        avgTimeSpentSeconds: Int = 90,
        severityLevel: String = "HIGH",
        aiGuidanceNotes: String = "",
        lastTestDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) {
        viewModelScope.launch {
            dao.insertWeakTopic(
                WeakTopic(
                    subject = subject,
                    chapter = chapter,
                    topicName = topicName,
                    mistakesCount = mistakesCount,
                    avgTimeSpentSeconds = avgTimeSpentSeconds,
                    severityLevel = severityLevel,
                    aiGuidanceNotes = aiGuidanceNotes,
                    lastTestDate = lastTestDate
                )
            )
        }
    }

    fun deleteWeakTopic(id: Long) {
        viewModelScope.launch {
            dao.deleteWeakTopic(id)
        }
    }

    fun seedDefaultWeakTopicsIfEmpty() {
        viewModelScope.launch {
            val existing = dao.getAllWeakTopicsDirect()
            if (existing.isEmpty()) {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val defaults = listOf(
                    WeakTopic(subject = "Physics", chapter = "Ray Optics & Optical Instruments", topicName = "Total Internal Reflection & Prism Minimum Deviation", mistakesCount = 4, avgTimeSpentSeconds = 145, severityLevel = "CRITICAL", aiGuidanceNotes = "Key Formulas: \$\$\\mu = \\frac{\\sin\\left(\\frac{A + D_{min}}{2}\\right)}{\\sin\\left(\\frac{A}{2}\\right)}\$\$ and \$\$D = (\\mu - 1)A\$\$. Critical angle: \$\\sin\\theta_c = \\frac{1}{\\mu}\$. Always verify ray travels from denser to rarer medium with angle \$\\theta > \\theta_c\$.", lastTestDate = todayStr),
                    WeakTopic(subject = "Physics", chapter = "Current Electricity", topicName = "Meter Bridge & Potentiometer Sensitivity", mistakesCount = 3, avgTimeSpentSeconds = 125, severityLevel = "HIGH", aiGuidanceNotes = "Unknown resistance: \$\$R_x = R \\cdot \\frac{l}{100 - l}\$\$. Remember end-resistance corrections \$\\alpha\$ and \$\\beta\$. Sensitivity increases when potential gradient \$k = \\frac{V}{L}\$ decreases.", lastTestDate = todayStr),
                    WeakTopic(subject = "Physics", chapter = "Thermodynamics", topicName = "Carnot Efficiency & Adiabatic Process", mistakesCount = 3, avgTimeSpentSeconds = 110, severityLevel = "MODERATE", aiGuidanceNotes = "Carnot efficiency: \$\$\\eta = 1 - \\frac{T_2}{T_1} = \\frac{W}{Q_1}\$\$. Temperatures must strictly be in Kelvin! For adiabatic: \$\$P V^\\gamma = \\text{const}, \\quad T V^{\\gamma - 1} = \\text{const}\$\$.", lastTestDate = todayStr),

                    WeakTopic(subject = "Chemistry", chapter = "Aldehydes, Ketones & Carboxylic Acids", topicName = "Aldol Condensation vs Cannizzaro Reaction", mistakesCount = 5, avgTimeSpentSeconds = 135, severityLevel = "CRITICAL", aiGuidanceNotes = "Aldol requires \$\\alpha-\\text{H}\$: \$\$2\\text{CH}_3\\text{CHO} \\xrightarrow{\\text{dil. NaOH}} \\text{CH}_3\\text{CH(OH)CH}_2\\text{CHO} \\xrightarrow{\\Delta} \\text{CH}_3\\text{CH=CHCHO}\$\$. Cannizzaro (no \$\\alpha-\\text{H}\$): \$\$2\\text{HCHO} \\xrightarrow{\\text{conc. KOH}} \\text{CH}_3\\text{OH} + \\text{HCOOK}\$\$.", lastTestDate = todayStr),
                    WeakTopic(subject = "Chemistry", chapter = "Chemical Equilibrium", topicName = "Le Chatelier's Principle & Equilibrium Constant", mistakesCount = 4, avgTimeSpentSeconds = 100, severityLevel = "HIGH", aiGuidanceNotes = "Van 't Hoff relation: \$\$\\ln\\left(\\frac{K_2}{K_1}\\right) = \\frac{\\Delta H^\\circ}{R}\\left(\\frac{1}{T_1} - \\frac{1}{T_2}\\right)\$\$. Inert gas at constant volume has \$\\Delta n_g = 0\$ effect! At constant pressure, shifts towards side with more gas moles.", lastTestDate = todayStr),
                    WeakTopic(subject = "Chemistry", chapter = "Coordination Compounds", topicName = "Spectrochemical Series & Crystal Field Splitting (CFSE)", mistakesCount = 3, avgTimeSpentSeconds = 95, severityLevel = "MODERATE", aiGuidanceNotes = "Spectrochemical series: \$\\text{I}^- < \\text{Br}^- < \\text{SCN}^- < \\text{Cl}^- < \\text{F}^- < \\text{OH}^- < \\text{H}_2\\text{O} < \\text{NH}_3 < \\text{en} < \\text{CN}^- < \\text{CO}\$. CFSE for octahedral: \$\\Delta_o\$, tetrahedral: \$\\Delta_t = \\frac{4}{9}\\Delta_o\$.", lastTestDate = todayStr),

                    WeakTopic(subject = "Biology", chapter = "Principles of Inheritance & Variation", topicName = "Pedigree Analysis (Autosomal vs Sex-Linked)", mistakesCount = 6, avgTimeSpentSeconds = 155, severityLevel = "CRITICAL", aiGuidanceNotes = "1) Check if generation is skipped: Yes \$\\rightarrow\$ Recessive. 2) Check parent-offspring: Unaffected parents with affected female child \$\\rightarrow\$ Strictly Autosomal Recessive (cannot be X-linked recessive).", lastTestDate = todayStr),
                    WeakTopic(subject = "Biology", chapter = "Photosynthesis in Higher Plants", topicName = "Z-Scheme & Cyclic Photophosphorylation", mistakesCount = 4, avgTimeSpentSeconds = 85, severityLevel = "HIGH", aiGuidanceNotes = "Non-cyclic involves PS-II (\$P_{680}\$) and PS-I (\$P_{700}\$), produces ATP + \$\\text{NADPH} + \\text{H}^+\$ with photolysis of \$\\text{H}_2\\text{O}\$. Cyclic involves only PS-I, produces ONLY ATP, no \$\\text{O}_2\$ evolution.", lastTestDate = todayStr),
                    WeakTopic(subject = "Biology", chapter = "Excretory Products & Their Elimination", topicName = "Counter-Current Multiplier & Osmolarity Gradient", mistakesCount = 3, avgTimeSpentSeconds = 90, severityLevel = "MODERATE", aiGuidanceNotes = "Medullary interstitium gradient: \$300\\text{ mOsmol/L}\$ in cortex to \$1200\\text{ mOsmol/L}\$ in inner medulla maintained by \$\\text{NaCl}\$ and Urea recycling.", lastTestDate = todayStr)
                )
                defaults.forEach { dao.insertWeakTopic(it) }
            }
        }
    }

    fun generateAndLaunchWeakTopicTest(
        context: Context,
        exam: ExamCategory = ExamCategory.NEET,
        subject: String,
        chapter: String,
        topic: String,
        questionCount: Int,
        difficulty: String = "NEET Standard",
        customPrompt: String? = null,
        websiteSource: String? = null,
        avoidRepeats: Boolean = true,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isAiTestGenerating.value = true
            _aiTestGenerationError.value = null

            val result = GeminiAiTestGenerator.generateChapterWiseTest(
                context = context,
                exam = exam,
                subject = subject,
                chapter = chapter,
                topic = topic + if (!customPrompt.isNullOrBlank()) " ($customPrompt)" else "",
                questionCount = questionCount.coerceIn(5, 200),
                difficulty = difficulty,
                websiteSource = websiteSource,
                avoidRepeats = avoidRepeats
            )

            _isAiTestGenerating.value = false
            result.onSuccess { questions ->
                if (questions.isNotEmpty()) {
                    val sanitized = QuestionImageFilter.sanitizeTestQuestions(questions)
                    testTimerJob?.cancel()
                    _activeSavedTestId.value = null
                    _activeTestQuestions.value = sanitized.mapIndexed { idx, q -> q.copy(id = idx + 1) }
                    val siteSuffix = if (!websiteSource.isNullOrBlank()) " [via ${websiteSource.take(24)}]" else ""
                    _activeTestTitle.value = "Targeted Weak Topic: $topic ($subject)$siteSuffix"
                    _activeTestInstitute.value = if (!websiteSource.isNullOrBlank()) websiteSource else "AI Diagnostic Series"
                    _activeTestExam.value = exam
                    _activeTestType.value = TestType.FULL_LENGTH
                    _activeChapterName.value = chapter
                    _currentQuestionIndex.value = 0

                    val totalSecs = (questions.size * 90).coerceAtLeast(300)
                    _testInitialDurationSeconds.value = totalSecs
                    _testTimeRemainingSeconds.value = totalSecs
                    _isTestActive.value = true
                    _isTestSubmitted.value = false
                    _testScoreSummary.value = null
                    startTestTimer()
                    onSuccess()
                } else {
                    onError("No questions could be generated. Please try again.")
                }
            }.onFailure { err ->
                _aiTestGenerationError.value = err.message
                onError(err.message ?: "Failed to generate test")
            }
        }
    }

    fun analyzeTestAndExtractWeakTopics(
        context: Context,
        testTitle: String,
        exam: ExamCategory,
        questions: List<AiTestQuestion>,
        customUserPrompt: String? = null,
        onResult: (report: String, candidates: List<CandidateWeakTopic>) -> Unit
    ) {
        viewModelScope.launch {
            val totalQ = questions.size
            val wrongQuestions = questions.filter { !it.isUserAnswerCorrect() && !it.selectedOption.isNullOrBlank() }
            
            // Group mistakes by chapter & topic
            val chapterMistakes = wrongQuestions.groupBy { if (it.chapter.isNotBlank()) it.chapter else it.subject }
            
            // Build candidate weak topics for explicit user approval (Do NOT automatically insert into DB)
            val candidateList = chapterMistakes.map { (chapterName, list) ->
                val sample = list.firstOrNull()
                val subj = sample?.subject ?: "Physics"
                val subtopic = sample?.subtopic?.ifBlank { "$chapterName Key Concepts" } ?: "$chapterName Key Concepts"
                val avgTime = if (list.isNotEmpty()) list.map { it.timeSpentSeconds }.average().toInt() else 90
                val severity = if (list.size >= 3 || avgTime > 120) "CRITICAL" else "HIGH"
                
                CandidateWeakTopic(
                    subject = subj,
                    chapter = chapterName,
                    topicName = subtopic,
                    mistakesCount = list.size,
                    avgTimeSpentSeconds = avgTime,
                    severityLevel = severity,
                    aiGuidanceNotes = "Identified from test '$testTitle'. ${list.size} mistakes recorded with ${avgTime}s average response time.",
                    testTitle = testTitle
                )
            }

            // Call AI model through GeminiChatAssistant (which respects user's selected provider from profile: Gemini, OpenRouter, Groq)
            val scoreSummary = _testScoreSummary.value
            val weakSummaryList = chapterMistakes.map { (ch, qList) -> "$ch (${qList.size} mistakes, avg ${qList.map { it.timeSpentSeconds }.average().toInt()}s/Q)" }
            val strongSummaryList = questions.filter { it.isUserAnswerCorrect() }.groupBy { it.chapter }.filter { it.value.size >= 2 }.map { "${it.key} (${it.value.size} correct)" }

            val responseResult = GeminiChatAssistant.generatePostTestAiStrategy(
                context = context,
                exam = exam.displayName,
                testTitle = testTitle,
                score = scoreSummary?.totalScore ?: 0,
                maxScore = scoreSummary?.maxScore ?: (totalQ * 4),
                accuracy = scoreSummary?.accuracyPercentage ?: 0f,
                timeTakenSeconds = scoreSummary?.timeTakenSeconds ?: (questions.sumOf { it.timeSpentSeconds }),
                weakTopicsWithMarks = weakSummaryList,
                strongTopics = strongSummaryList,
                customUserPrompt = customUserPrompt ?: "Perform a detailed speed vs accuracy diagnosis and list high-priority weak chapters."
            )

            responseResult.onSuccess { text ->
                onResult(text, candidateList)
            }.onFailure { err ->
                onResult("⚠️ AI Diagnosis Error: ${err.message ?: "Unable to complete analysis. Please verify your API key in Profile."}", candidateList)
            }
        }
    }

    init {
        loadEdunitiChecklist()
        loadStudyPortals()
        restoreTimerState()
        refreshGeminiQuota()
        // Seed default high-yield NEET flashcard decks & Master NEET checklist if empty
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            val currentDecks = dao.getAllFlashcardDecks().firstOrNull() ?: emptyList()
            if (currentDecks.isEmpty()) {
                seedDefaultFlashcards()
            }
            val currentChecklist = dao.getAllDynamicChecklistTopics().firstOrNull() ?: emptyList()
            if (currentChecklist.isEmpty()) {
                importAllNeetChecklist(clearExisting = false)
            }

            // One-time safe reconciliation of questions from existing mistake logs & AI tests
            val prefs = getApplication<Application>().getSharedPreferences("lakshya_prefs", Context.MODE_PRIVATE)
            if (!prefs.getBoolean("question_accumulator_reconciled_v1", false)) {
                val practices = dao.getAllDailyPractices().firstOrNull() ?: emptyList()
                val totalExistingPractice = practices.sumOf { it.physicsSolved + it.chemistrySolved + it.biologySolved }
                val allMistakes = dao.getAllMistakeLogs().firstOrNull() ?: emptyList()
                val allAiTests = dao.getAllAiSavedTests().firstOrNull() ?: emptyList()
                val totalAiQuestions = allAiTests.sumOf { it.totalQuestions }
                val totalExpected = allMistakes.size + totalAiQuestions
                if (totalExistingPractice < totalExpected && totalExpected > 0) {
                    val missingPhy = allMistakes.count { it.subject.contains("phy", ignoreCase = true) }
                    val missingChem = allMistakes.count { it.subject.contains("chem", ignoreCase = true) }
                    val missingBio = allMistakes.count { 
                        it.subject.contains("bio", ignoreCase = true) || 
                        it.subject.contains("botan", ignoreCase = true) || 
                        it.subject.contains("zool", ignoreCase = true) 
                    }
                    val otherMistakes = allMistakes.size - (missingPhy + missingChem + missingBio)
                    
                    var aiPhy = 0
                    var aiChem = 0
                    var aiBio = 0
                    allAiTests.forEach { test ->
                        val totalQ = test.totalQuestions
                        if (test.examCategory.contains("JEE", ignoreCase = true)) {
                            aiPhy += totalQ / 3
                            aiChem += totalQ / 3
                            aiBio += totalQ - (2 * (totalQ / 3))
                        } else {
                            aiPhy += totalQ / 4
                            aiChem += totalQ / 4
                            aiBio += totalQ - (2 * (totalQ / 4))
                        }
                    }
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val currentToday = practices.find { it.date == todayStr } ?: DailyPractice(date = todayStr)
                    dao.insertDailyPractice(
                        currentToday.copy(
                            physicsSolved = currentToday.physicsSolved + missingPhy + aiPhy,
                            chemistrySolved = currentToday.chemistrySolved + missingChem + aiChem,
                            biologySolved = currentToday.biologySolved + missingBio + otherMistakes + aiBio
                        )
                    )
                }
                prefs.edit().putBoolean("question_accumulator_reconciled_v1", true).apply()
            }
        }

        // Synchronize study streak across app and home screen widgets
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(studyLogs, dailyPractices) { logs, practices ->
                calculateStudyStreak(logs, practices)
            }.collect { streak ->
                val userPrefs = getApplication<Application>().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                userPrefs.edit().putInt("study_streak_days", streak).apply()
                try {
                    com.example.widget.WidgetUpdateHelper.updateAllWidgets(getApplication())
                } catch (_: Exception) {}
            }
        }
    }

    fun calculateStudyStreak(logs: List<StudyLog>, practices: List<DailyPractice>): Int {
        val activeDates = mutableSetOf<String>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        logs.forEach { 
            if (it.durationSeconds > 0 && it.timestamp > 0) {
                activeDates.add(sdf.format(Date(it.timestamp)))
            }
        }
        practices.forEach { 
            if (it.physicsSolved > 0 || it.chemistrySolved > 0 || it.biologySolved > 0) {
                activeDates.add(it.date)
            }
        }
        
        val cal = Calendar.getInstance()
        val todayStr = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)
        
        val isTodayActive = activeDates.contains(todayStr)
        val isYesterdayActive = activeDates.contains(yesterdayStr)
        
        if (!isTodayActive && !isYesterdayActive) return 0
        
        var streak = 0
        val checkCal = Calendar.getInstance()
        if (!isTodayActive) {
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        }
        while (true) {
            val dStr = sdf.format(checkCal.time)
            if (activeDates.contains(dStr)) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }
}
