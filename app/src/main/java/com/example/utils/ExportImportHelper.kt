package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.AppDatabase
import com.example.data.StudyLog
import com.example.data.Goal
import com.example.data.MockTest
import com.example.data.DailyPractice
import com.example.data.CompletedTopic
import com.example.data.DppItem
import com.example.data.ChatMessageEntity
import com.example.data.Habit
import com.example.data.ScheduledMockTest
import com.example.data.MistakeLog
import com.example.data.FlashcardDeck
import com.example.data.FlashcardItem
import com.example.data.DynamicChecklistTopic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportImportHelper {

    private const val TAG = "ExportImportHelper"

    suspend fun createBackupJson(context: Context, includeApiKeys: Boolean = true): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val dao = db.appDao()

        val root = JSONObject()
        root.put("version", 1)
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

        // Export Study Logs
        val logs = dao.getAllStudyLogs().firstOrNull() ?: emptyList()
        val logsArray = JSONArray()
        logs.forEach { log ->
            val obj = JSONObject()
            obj.put("subject", log.subject)
            obj.put("chapter", log.chapter)
            obj.put("durationSeconds", log.durationSeconds)
            obj.put("timestamp", log.timestamp)
            logsArray.put(obj)
        }
        root.put("studyLogs", logsArray)

        // Export Goals
        val goals = dao.getAllGoals().firstOrNull() ?: emptyList()
        val goalsArray = JSONArray()
        goals.forEach { g ->
            val obj = JSONObject()
            obj.put("subject", g.subject)
            obj.put("text", g.text)
            obj.put("status", g.status)
            obj.put("targetType", g.targetType)
            obj.put("date", g.date)
            obj.put("completedDate", g.completedDate ?: "")
            goalsArray.put(obj)
        }
        root.put("goals", goalsArray)

        // Export Mock Tests
        val tests = dao.getAllMockTests().firstOrNull() ?: emptyList()
        val testsArray = JSONArray()
        tests.forEach { t ->
            val obj = JSONObject()
            obj.put("testName", t.testName)
            obj.put("score", t.score)
            obj.put("physics", t.physics)
            obj.put("chemistry", t.chemistry)
            obj.put("biology", t.biology)
            obj.put("negative", t.negative)
            obj.put("timestamp", t.timestamp)
            obj.put("geminiAnalysis", t.geminiAnalysis ?: "")
            testsArray.put(obj)
        }
        root.put("mockTests", testsArray)

        // Export DPP Items
        val dpps = dao.getAllDppItems().firstOrNull() ?: emptyList()
        val dppsArray = JSONArray()
        dpps.forEach { d ->
            val obj = JSONObject()
            obj.put("subject", d.subject)
            obj.put("chapter", d.chapter)
            obj.put("dppName", d.dppName)
            obj.put("isCompleted", d.isCompleted)
            obj.put("questionsCount", d.questionsCount)
            obj.put("date", d.date)
            obj.put("completedDate", d.completedDate ?: "")
            dppsArray.put(obj)
        }
        root.put("dppItems", dppsArray)

        // Export Daily Practices
        val practices = dao.getAllDailyPractices().firstOrNull() ?: emptyList()
        val practicesArray = JSONArray()
        practices.forEach { p ->
            val obj = JSONObject()
            obj.put("date", p.date)
            obj.put("physicsTarget", p.physicsTarget)
            obj.put("chemistryTarget", p.chemistryTarget)
            obj.put("biologyTarget", p.biologyTarget)
            obj.put("physicsSolved", p.physicsSolved)
            obj.put("chemistrySolved", p.chemistrySolved)
            obj.put("biologySolved", p.biologySolved)
            obj.put("physicsDifficulty", p.physicsDifficulty)
            obj.put("chemistryDifficulty", p.chemistryDifficulty)
            obj.put("biologyDifficulty", p.biologyDifficulty)
            practicesArray.put(obj)
        }
        root.put("dailyPractices", practicesArray)

        // Export Completed Topics
        val completedTopics = dao.getAllCompletedTopics().firstOrNull() ?: emptyList()
        val completedTopicsArray = JSONArray()
        completedTopics.forEach { ct ->
            val obj = JSONObject()
            obj.put("topicId", ct.topicId)
            obj.put("timestamp", ct.timestamp)
            completedTopicsArray.put(obj)
        }
        root.put("completedTopics", completedTopicsArray)

        // Export Habits
        val habits = dao.getAllHabits().firstOrNull() ?: emptyList()
        val habitsArray = JSONArray()
        habits.forEach { h ->
            val obj = JSONObject()
            obj.put("name", h.name)
            obj.put("iconEmoji", h.iconEmoji)
            obj.put("isGoodHabit", h.isGoodHabit)
            obj.put("completedDates", h.completedDates)
            obj.put("createdAt", h.createdAt)
            habitsArray.put(obj)
        }
        root.put("habits", habitsArray)

        // Export Scheduled Mock Tests
        val scheduledTests = dao.getAllScheduledMockTests().firstOrNull() ?: emptyList()
        val scheduledArray = JSONArray()
        scheduledTests.forEach { st ->
            val obj = JSONObject()
            obj.put("title", st.title)
            obj.put("scheduledDate", st.scheduledDate)
            obj.put("physicsSyllabus", st.physicsSyllabus)
            obj.put("chemistrySyllabus", st.chemistrySyllabus)
            obj.put("biologySyllabus", st.biologySyllabus)
            obj.put("syllabusNotes", st.syllabusNotes)
            obj.put("isCompleted", st.isCompleted)
            scheduledArray.put(obj)
        }
        root.put("scheduledMockTests", scheduledArray)

        // Export Mistake Logs
        val mistakes = dao.getAllMistakeLogs().firstOrNull() ?: emptyList()
        val mistakesArray = JSONArray()
        mistakes.forEach { m ->
            val obj = JSONObject()
            obj.put("subject", m.subject)
            obj.put("question", m.question)
            obj.put("mistakeType", m.mistakeType)
            obj.put("chapter", m.chapter)
            obj.put("timestamp", m.timestamp)
            if (m.pdfUri != null) obj.put("pdfUri", m.pdfUri)
            if (m.pdfName != null) obj.put("pdfName", m.pdfName)
            if (m.imageUri != null) obj.put("imageUri", m.imageUri)
            mistakesArray.put(obj)
        }
        root.put("mistakeLogs", mistakesArray)

        // Export Flashcard Decks & Items
        val decks = dao.getAllFlashcardDecks().firstOrNull() ?: emptyList()
        val decksArray = JSONArray()
        decks.forEach { d ->
            val obj = JSONObject()
            obj.put("id", d.id)
            obj.put("name", d.name)
            obj.put("icon", d.icon)
            obj.put("category", d.category)
            obj.put("created", d.created)
            decksArray.put(obj)
        }
        root.put("flashcardDecks", decksArray)

        val flashcards = dao.getAllFlashcardItems().firstOrNull() ?: emptyList()
        val flashcardsArray = JSONArray()
        flashcards.forEach { fc ->
            val obj = JSONObject()
            obj.put("deckId", fc.deckId)
            obj.put("front", fc.front)
            obj.put("back", fc.back)
            obj.put("interval", fc.interval)
            obj.put("ease", fc.ease.toDouble())
            obj.put("lapses", fc.lapses)
            obj.put("due", fc.due ?: "")
            obj.put("lastStudied", fc.lastStudied ?: "")
            obj.put("reviews", fc.reviews)
            obj.put("created", fc.created)
            flashcardsArray.put(obj)
        }
        root.put("flashcardItems", flashcardsArray)

        // Export Dynamic Checklist Topics
        val checklistTopics = dao.getAllDynamicChecklistTopics().firstOrNull() ?: emptyList()
        val checklistArray = JSONArray()
        checklistTopics.forEach { ct ->
            val obj = JSONObject()
            obj.put("subject", ct.subject)
            obj.put("phaseTitle", ct.phaseTitle)
            obj.put("topicName", ct.topicName)
            obj.put("days", ct.days)
            obj.put("columns", ct.columns)
            obj.put("timestamp", ct.timestamp)
            obj.put("customPlaylistUrl", ct.customPlaylistUrl ?: "")
            checklistArray.put(obj)
        }
        root.put("dynamicChecklistTopics", checklistArray)

        // Export Preferences
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val prefObj = JSONObject()
        prefObj.put("user_name", prefs.getString("user_name", "Future Doctor 👨‍⚕️"))
        prefObj.put("user_class", prefs.getString("user_class", "Dropper"))
        prefObj.put("user_avatar_uri", prefs.getString("user_avatar_uri", ""))

        val k1 = prefs.getString("gemini_api_key_1", "") ?: ""
        val k2 = prefs.getString("gemini_api_key_2", "") ?: ""
        val k3 = prefs.getString("gemini_api_key_3", "") ?: ""
        val workingKey = prefs.getString("currently_working_api_key", "") ?: ""

        if (includeApiKeys) {
            prefObj.put("gemini_api_key_1", k1)
            prefObj.put("gemini_api_key_2", k2)
            prefObj.put("gemini_api_key_3", k3)
            prefObj.put("currently_working_api_key", workingKey)

            val apiKeysInfo = JSONObject().apply {
                put("status", "API Keys Included")
                put("currently_working_key", workingKey.takeIf { it.isNotBlank() } ?: "None")
                put("primary_key_1", k1.takeIf { it.isNotBlank() } ?: "Not Set")
                put("backup_key_2", k2.takeIf { it.isNotBlank() } ?: "Not Set")
                put("backup_key_3", k3.takeIf { it.isNotBlank() } ?: "Not Set")
                put("security_note", "Sensitive API keys are present in this export. Delete these keys before sharing file publicly.")
            }
            root.put("included_api_keys", apiKeysInfo)
        } else {
            prefObj.put("gemini_api_key_1", "")
            prefObj.put("gemini_api_key_2", "")
            prefObj.put("gemini_api_key_3", "")
            prefObj.put("currently_working_api_key", "")

            val apiKeysInfo = JSONObject().apply {
                put("status", "API Keys Excluded (Clean / Safe for Sharing)")
                put("security_note", "API keys were stripped during export by user request.")
            }
            root.put("included_api_keys", apiKeysInfo)
        }

        prefObj.put("selected_gemini_model", prefs.getString("selected_gemini_model", com.example.data.GeminiModelManager.DEFAULT_MODEL))
        prefObj.put("neet_target_date", prefs.getString("neet_target_date", "2027-05-03"))
        root.put("preferences", prefObj)

        root.toString(2)
    }

    suspend fun exportData(context: Context, uri: Uri, includeApiKeys: Boolean = true): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val jsonString = createBackupJson(context, includeApiKeys)
            context.contentResolver.openOutputStream(uri)?.use { os ->
                OutputStreamWriter(os).use { writer ->
                    writer.write(jsonString)
                }
            }
            Pair(true, "Data exported successfully! 📦")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting data", e)
            Pair(false, "Export failed: ${e.localizedMessage}")
        }
    }

    suspend fun importData(context: Context, uri: Uri): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).readText()
            } ?: return@withContext Pair(false, "Failed to read file stream")

            val root = JSONObject(content)
            val db = AppDatabase.getDatabase(context)
            val dao = db.appDao()

            // Restore Study Logs
            if (root.has("studyLogs")) {
                val logsArray = root.getJSONArray("studyLogs")
                val logsList = ArrayList<StudyLog>(logsArray.length())
                for (i in 0 until logsArray.length()) {
                    val obj = logsArray.getJSONObject(i)
                    logsList.add(
                        StudyLog(
                            subject = obj.optString("subject", "General"),
                            chapter = obj.optString("chapter", "General"),
                            durationSeconds = obj.optInt("durationSeconds", 0),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                if (logsList.isNotEmpty()) {
                    dao.insertAllStudyLogs(logsList)
                }
            }

            // Restore Goals
            if (root.has("goals")) {
                val goalsArray = root.getJSONArray("goals")
                for (i in 0 until goalsArray.length()) {
                    val obj = goalsArray.getJSONObject(i)
                    dao.insertGoal(
                        Goal(
                            subject = obj.optString("subject", "Physics"),
                            text = obj.optString("text", ""),
                            status = obj.optString("status", com.example.data.GoalStatus.TODO.value),
                            targetType = obj.optString("targetType", "today"),
                            date = obj.optString("date", ""),
                            completedDate = obj.optString("completedDate", "").ifBlank { null }
                        )
                    )
                }
            }

            // Restore Mock Tests
            if (root.has("mockTests")) {
                val testsArray = root.getJSONArray("mockTests")
                val mockTestsList = ArrayList<MockTest>(testsArray.length())
                for (i in 0 until testsArray.length()) {
                    val obj = testsArray.getJSONObject(i)
                    mockTestsList.add(
                        MockTest(
                            testName = obj.optString("testName", "Mock Test"),
                            score = obj.optInt("score", 0),
                            physics = obj.optInt("physics", 0),
                            chemistry = obj.optInt("chemistry", 0),
                            biology = obj.optInt("biology", 0),
                            negative = obj.optInt("negative", 0),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            geminiAnalysis = obj.optString("geminiAnalysis", "").ifBlank { null }
                        )
                    )
                }
                if (mockTestsList.isNotEmpty()) {
                    dao.insertMockTests(mockTestsList)
                }
            }

            // Restore Daily Practices
            if (root.has("dailyPractices")) {
                val practicesArray = root.getJSONArray("dailyPractices")
                for (i in 0 until practicesArray.length()) {
                    val obj = practicesArray.getJSONObject(i)
                    dao.insertDailyPractice(
                        DailyPractice(
                            date = obj.optString("date", ""),
                            physicsTarget = obj.optInt("physicsTarget", 45),
                            chemistryTarget = obj.optInt("chemistryTarget", 45),
                            biologyTarget = obj.optInt("biologyTarget", 90),
                            physicsSolved = obj.optInt("physicsSolved", 0),
                            chemistrySolved = obj.optInt("chemistrySolved", 0),
                            biologySolved = obj.optInt("biologySolved", 0),
                            physicsDifficulty = obj.optString("physicsDifficulty", "Medium"),
                            chemistryDifficulty = obj.optString("chemistryDifficulty", "Medium"),
                            biologyDifficulty = obj.optString("biologyDifficulty", "Medium")
                        )
                    )
                }
            }

            // Restore Completed Topics
            if (root.has("completedTopics")) {
                val ctArray = root.getJSONArray("completedTopics")
                for (i in 0 until ctArray.length()) {
                    val obj = ctArray.getJSONObject(i)
                    dao.insertCompletedTopic(
                        CompletedTopic(
                            topicId = obj.optString("topicId", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Restore Habits
            if (root.has("habits")) {
                val habitsArray = root.getJSONArray("habits")
                for (i in 0 until habitsArray.length()) {
                    val obj = habitsArray.getJSONObject(i)
                    dao.insertHabit(
                        Habit(
                            name = obj.optString("name", ""),
                            iconEmoji = obj.optString("iconEmoji", "⭐"),
                            isGoodHabit = obj.optBoolean("isGoodHabit", true),
                            completedDates = obj.optString("completedDates", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Restore Scheduled Mock Tests
            if (root.has("scheduledMockTests")) {
                val scheduledArray = root.getJSONArray("scheduledMockTests")
                for (i in 0 until scheduledArray.length()) {
                    val obj = scheduledArray.getJSONObject(i)
                    dao.insertScheduledMockTest(
                        ScheduledMockTest(
                            title = obj.optString("title", ""),
                            scheduledDate = obj.optString("scheduledDate", ""),
                            physicsSyllabus = obj.optString("physicsSyllabus", ""),
                            chemistrySyllabus = obj.optString("chemistrySyllabus", ""),
                            biologySyllabus = obj.optString("biologySyllabus", ""),
                            syllabusNotes = obj.optString("syllabusNotes", ""),
                            isCompleted = obj.optBoolean("isCompleted", false)
                        )
                    )
                }
            }

            // Restore Mistake Logs
            if (root.has("mistakeLogs")) {
                val mistakesArray = root.getJSONArray("mistakeLogs")
                for (i in 0 until mistakesArray.length()) {
                    val obj = mistakesArray.getJSONObject(i)
                    dao.insertMistakeLog(
                        MistakeLog(
                            subject = obj.optString("subject", "Physics"),
                            question = obj.optString("question", ""),
                            mistakeType = obj.optString("mistakeType", "Silly Mistake"),
                            chapter = obj.optString("chapter", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            pdfUri = if (obj.has("pdfUri")) obj.getString("pdfUri") else null,
                            pdfName = if (obj.has("pdfName")) obj.getString("pdfName") else null,
                            imageUri = if (obj.has("imageUri")) obj.getString("imageUri") else null
                        )
                    )
                }
            }

            // Restore Flashcard Decks & Items
            if (root.has("flashcardDecks")) {
                val decksArray = root.getJSONArray("flashcardDecks")
                for (i in 0 until decksArray.length()) {
                    val obj = decksArray.getJSONObject(i)
                    val originalId = obj.optLong("id", 0L)
                    val newId = dao.insertFlashcardDeck(
                        FlashcardDeck(
                            name = obj.optString("name", "Flashcards"),
                            icon = obj.optString("icon", "📖"),
                            category = obj.optString("category", "Other"),
                            created = obj.optLong("created", System.currentTimeMillis())
                        )
                    )
                }
            }

            if (root.has("flashcardItems")) {
                val flashcardsArray = root.getJSONArray("flashcardItems")
                for (i in 0 until flashcardsArray.length()) {
                    val obj = flashcardsArray.getJSONObject(i)
                    dao.insertFlashcardItem(
                        FlashcardItem(
                            deckId = obj.optLong("deckId", 1L),
                            front = obj.optString("front", ""),
                            back = obj.optString("back", ""),
                            interval = obj.optInt("interval", 1),
                            ease = obj.optDouble("ease", 2.5).toFloat(),
                            lapses = obj.optInt("lapses", 0),
                            due = obj.optString("due", "").ifBlank { null },
                            lastStudied = obj.optString("lastStudied", "").ifBlank { null },
                            reviews = obj.optInt("reviews", 0),
                            created = obj.optLong("created", System.currentTimeMillis())
                        )
                    )
                }
            }

            // Restore Dynamic Checklist Topics
            if (root.has("dynamicChecklistTopics")) {
                val checklistArray = root.getJSONArray("dynamicChecklistTopics")
                for (i in 0 until checklistArray.length()) {
                    val obj = checklistArray.getJSONObject(i)
                    dao.insertDynamicChecklistTopic(
                        DynamicChecklistTopic(
                            subject = obj.optString("subject", "Physics"),
                            phaseTitle = obj.optString("phaseTitle", "Phase 1"),
                            topicName = obj.optString("topicName", ""),
                            days = obj.optInt("days", 1),
                            columns = obj.optString("columns", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            customPlaylistUrl = obj.optString("customPlaylistUrl", "").ifBlank { null }
                        )
                    )
                }
            }

            // Restore Preferences
            if (root.has("preferences")) {
                val prefObj = root.getJSONObject("preferences")
                val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                val editor = prefs.edit()
                if (prefObj.has("user_name")) editor.putString("user_name", prefObj.getString("user_name"))
                if (prefObj.has("user_class")) editor.putString("user_class", prefObj.getString("user_class"))
                if (prefObj.has("user_avatar_uri")) editor.putString("user_avatar_uri", prefObj.getString("user_avatar_uri"))
                if (prefObj.has("gemini_api_key_1")) editor.putString("gemini_api_key_1", prefObj.getString("gemini_api_key_1"))
                if (prefObj.has("gemini_api_key_2")) editor.putString("gemini_api_key_2", prefObj.getString("gemini_api_key_2"))
                if (prefObj.has("gemini_api_key_3")) editor.putString("gemini_api_key_3", prefObj.getString("gemini_api_key_3"))
                if (prefObj.has("selected_gemini_model")) editor.putString("selected_gemini_model", prefObj.getString("selected_gemini_model"))
                if (prefObj.has("neet_target_date")) editor.putString("neet_target_date", prefObj.getString("neet_target_date"))
                editor.apply()
            }

            Pair(true, "Data imported successfully! 🎉")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error importing data", e)
            Pair(false, "Import failed: Invalid backup file format (${e.localizedMessage})")
        }
    }

    fun createBackupZipFile(context: Context): File? {
        return try {
            val cacheDir = context.cacheDir
            val backupFile = File(cacheDir, "NEET_Prep_Backup_${System.currentTimeMillis()}.json")
            val jsonString = kotlinx.coroutines.runBlocking { createBackupJson(context) }
            backupFile.writeText(jsonString)
            backupFile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup file", e)
            null
        }
    }

    fun shareBackupToGoogleDriveOrEmail(
        context: Context,
        backupFile: File,
        targetEmail: String = "",
        isEmailMode: Boolean = false
    ): Boolean {
        return try {
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                if (isEmailMode && targetEmail.isNotBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(targetEmail))
                    putExtra(Intent.EXTRA_SUBJECT, "NEET Prep AI Tracker Backup")
                    putExtra(Intent.EXTRA_TEXT, "Here is your latest study database backup file.")
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, if (isEmailMode) "Send Backup via Email" else "Save Backup to Drive")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: android.content.ActivityNotFoundException) {
            Log.e(TAG, "No activity found to handle share backup intent", e)
            false
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid backup file for FileProvider", e)
            false
        }
    }
}

