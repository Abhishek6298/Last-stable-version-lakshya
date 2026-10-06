package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "study_logs")
data class StudyLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val chapter: String,
    val durationSeconds: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val text: String,
    val status: String, // "todo", "in_progress", "completed"
    val targetType: String = "today", // "today" or "weekly"
    val date: String = "", // YYYY-MM-DD
    val completedDate: String? = null // Timestamp or date string when completed
)

@Entity(tableName = "dpp_items")
data class DppItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val chapter: String,
    val dppName: String = "DPP 01",
    val isCompleted: Boolean = false,
    val pdfUri: String? = null,
    val pdfFileName: String? = null,
    val questionsCount: Int = 0,
    val date: String = "",
    val completedDate: String? = null
)

@Entity(tableName = "mock_tests")
data class MockTest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val testName: String,
    val score: Int,
    val physics: Int = 0,
    val chemistry: Int = 0,
    val biology: Int = 0,
    val negative: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val pdfUri: String? = null,
    val pdfFileName: String? = null,
    val geminiAnalysis: String? = null
)


@Entity(tableName = "daily_practice")
data class DailyPractice(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val physicsTarget: Int = 45,
    val chemistryTarget: Int = 45,
    val biologyTarget: Int = 90,
    val physicsSolved: Int = 0,
    val chemistrySolved: Int = 0,
    val biologySolved: Int = 0,
    val physicsDifficulty: String = "Medium",
    val chemistryDifficulty: String = "Medium",
    val biologyDifficulty: String = "Medium"
)


@Entity(tableName = "completed_topics")
data class CompletedTopic(
    @PrimaryKey val topicId: String, // format: "chapterName-topicName"
    val timestamp: Long = System.currentTimeMillis(),
    val customRevisionDate: Long? = null
)

@Entity(tableName = "scheduled_mock_tests")
data class ScheduledMockTest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val scheduledDate: String, // YYYY-MM-DD
    val physicsSyllabus: String = "",
    val chemistrySyllabus: String = "",
    val biologySyllabus: String = "",
    val syllabusNotes: String = "",
    val isCompleted: Boolean = false,
    val isPinned: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String, // "physics", "chemistry", "biology"
    val chatType: String, // "error", "doubt"
    val text: String,
    val mediaUri: String? = null,
    val mediaType: String? = null, // "image", "pdf", "table"
    val mediaName: String? = null,
    val sender: String = "user",
    val time: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "book_progressions")
data class BookProgression(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String, // "Physics", "Chemistry", "Biology"
    val chapter: String, // Chapter Name
    val bookType: String, // "NEET" or "JEE"
    val bookName: String = "", // e.g. "HC Verma"
    val progressPercent: Int = 0, // 0 to 100
    val status: String = "Not Started", // "Not Started", "In Progress", "Completed"
    val subtopicsProgress: String = "" // "Subtopic=Status|..."
)


@Entity(tableName = "dynamic_checklist_topics")

data class DynamicChecklistTopic(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val phaseTitle: String,
    val topicName: String,
    val days: Int,
    val columns: String,
    val timestamp: Long = System.currentTimeMillis(),
    val customPlaylistUrl: String? = null
)

@Entity(tableName = "eduniti_targets")
data class EdunitiTarget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val totalQuestions: Int,
    val totalDays: Int,
    val questionsSolved: Int = 0,
    val pdfName: String? = null,
    val pdfUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "flashcard_decks")
data class FlashcardDeck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String = "📖",
    val category: String = "Other",
    val created: Long = System.currentTimeMillis()
)

@Entity(tableName = "mistake_logs")
data class MistakeLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val question: String,
    val mistakeType: String, // e.g. "Silly Mistake", "Conceptual Error"
    val chapter: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val pdfUri: String? = null,
    val pdfName: String? = null,
    val imageUri: String? = null,
    val reviewStage: Int = 0, // 0 = Day 1 (24h), 1 = Day 3 (72h), 2 = Day 7 (Week), 3 = Day 14+, 4 = Mastered
    val nextReviewTime: Long = 0L, // timestamp when next review is due
    val reviewCount: Int = 0, // total reviews completed
    val lastReviewedTime: Long = 0L, // timestamp of last review
    val isMastered: Boolean = false // marked as mastered
)

enum class MistakePhotoCategory(val label: String, val code: String, val emoji: String) {
    QUESTION("Questions", "Q", "❓"),
    ANSWER("Answer", "A", "💡"),
    BOTH("Q & A in Same Photo", "BOTH", "📑");

    companion object {
        fun fromCode(code: String): MistakePhotoCategory {
            return when (code.trim().uppercase()) {
                "Q", "QUESTION", "QUESTIONS" -> QUESTION
                "A", "ANSWER", "ANS", "SOLUTION", "SOL" -> ANSWER
                "BOTH", "QA", "QNA", "COMBINED" -> BOTH
                else -> BOTH
            }
        }
    }
}

data class MistakePhoto(
    val uri: String,
    val category: MistakePhotoCategory = MistakePhotoCategory.BOTH
) {
    fun toSerialized(): String = "${category.code}::$uri"

    companion object {
        fun fromSerialized(raw: String): MistakePhoto {
            val trimmed = raw.trim()
            return if (trimmed.contains("::")) {
                val prefix = trimmed.substringBefore("::")
                val path = trimmed.substringAfter("::")
                MistakePhoto(uri = path, category = MistakePhotoCategory.fromCode(prefix))
            } else {
                MistakePhoto(uri = trimmed, category = MistakePhotoCategory.BOTH)
            }
        }
    }
}

val MistakeLog.photosList: List<MistakePhoto>
    get() = if (imageUri.isNullOrBlank()) {
        emptyList()
    } else {
        imageUri.split("||")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { MistakePhoto.fromSerialized(it) }
    }

val MistakeLog.imageUriList: List<String>
    get() = photosList.map { it.uri }

val MistakeLog.isDueForReview: Boolean
    get() {
        if (isMastered) return false
        val now = System.currentTimeMillis()
        if (nextReviewTime <= 0L) {
            // If never scheduled or legacy item: due on Day 1 (immediate active recall)
            return true
        }
        return nextReviewTime <= now
    }

val MistakeLog.effectiveStage: Int
    get() = if (isMastered) 4 else reviewStage.coerceIn(0, 4)

val MistakeLog.stageLabel: String
    get() = when (effectiveStage) {
        0 -> "Day 1 (24h)"
        1 -> "Day 3 (72h)"
        2 -> "Day 7 (Week)"
        3 -> "Day 14+ (Deep)"
        else -> "Mastered"
    }

val MistakeLog.stageEmoji: String
    get() = when (effectiveStage) {
        0 -> "🌱"
        1 -> "🌿"
        2 -> "🌳"
        3 -> "💎"
        else -> "🏆"
    }


@Entity(tableName = "flashcard_items")
data class FlashcardItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val front: String,
    val back: String,
    val interval: Int = 1,
    val ease: Float = 2.5f,
    val lapses: Int = 0,
    val due: String? = null,
    val lastStudied: String? = null,
    val reviews: Int = 0,
    val created: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val iconEmoji: String,
    val isGoodHabit: Boolean,
    val completedDates: String = "", // Comma separated YYYY-MM-DD
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface AppDao {
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): kotlinx.coroutines.flow.Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY timestamp DESC LIMIT 20")
    suspend fun getRecentNotesDirect(): List<Note>

    @Query("SELECT * FROM goals ORDER BY id DESC LIMIT 20")
    suspend fun getRecentGoalsDirect(): List<Goal>

    @Query("SELECT * FROM dpp_items ORDER BY id DESC LIMIT 20")
    suspend fun getRecentDppItemsDirect(): List<DppItem>

    @Query("SELECT * FROM mistake_logs ORDER BY timestamp DESC LIMIT 20")
    suspend fun getRecentMistakesDirect(): List<MistakeLog>

    @Query("SELECT * FROM scheduled_mock_tests WHERE isCompleted = 0 ORDER BY scheduledDate ASC LIMIT 5")
    suspend fun getUpcomingMockTestsDirect(): List<ScheduledMockTest>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Int)


    @Transaction
    suspend fun clearAll() {
        deleteAllStudyLogs()
        deleteAllGoals()
        deleteAllMockTests()
        deleteAllDailyPractices()
        deleteAllCompletedTopics()
        deleteAllScheduledMockTests()
        deleteAllBookProgressions()
        deleteAllEdunitiTargets()
        deleteAllDppItems()
        deleteAllChatMessages()
        deleteAllFlashcardDecks()
        deleteAllFlashcardItems()
        deleteAllHabits()
    }

    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun getAllHabits(): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Int)

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    @Query("DELETE FROM dpp_items")
    suspend fun deleteAllDppItems()

    @Query("DELETE FROM study_logs")
    suspend fun deleteAllStudyLogs()

    @Query("DELETE FROM goals")
    suspend fun deleteAllGoals()

    @Query("DELETE FROM mock_tests")
    suspend fun deleteAllMockTests()

    @Query("DELETE FROM daily_practice")
    suspend fun deleteAllDailyPractices()

    @Query("DELETE FROM completed_topics")
    suspend fun deleteAllCompletedTopics()

    @Query("DELETE FROM scheduled_mock_tests")
    suspend fun deleteAllScheduledMockTests()


    @Query("SELECT * FROM scheduled_mock_tests ORDER BY scheduledDate ASC")
    fun getAllScheduledMockTests(): Flow<List<ScheduledMockTest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledMockTest(test: ScheduledMockTest)

    @Query("DELETE FROM scheduled_mock_tests WHERE id = :id")
    suspend fun deleteScheduledMockTest(id: Int)


    @Query("SELECT * FROM completed_topics")
    fun getAllCompletedTopics(): Flow<List<CompletedTopic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletedTopic(topic: CompletedTopic)

    @Query("DELETE FROM completed_topics WHERE topicId = :topicId")
    suspend fun deleteCompletedTopic(topicId: String)


    @Query("SELECT * FROM daily_practice ORDER BY date ASC")
    fun getAllDailyPractices(): Flow<List<DailyPractice>>

    @Query("SELECT * FROM daily_practice ORDER BY date ASC")
    suspend fun getAllDailyPracticesDirect(): List<DailyPractice>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyPractice(practice: DailyPractice)

    @Query("DELETE FROM daily_practice WHERE date = :date")
    suspend fun deleteDailyPractice(date: String)

    @Query("SELECT * FROM study_logs ORDER BY timestamp DESC")
    fun getAllStudyLogs(): Flow<List<StudyLog>>

    @Query("SELECT * FROM study_logs ORDER BY timestamp DESC")
    suspend fun getAllStudyLogsDirect(): List<StudyLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyLog(log: StudyLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllStudyLogs(logs: List<StudyLog>)

    @Query("DELETE FROM study_logs WHERE id = :id")
    suspend fun deleteStudyLog(id: Int)

    @Query("SELECT * FROM goals")
    fun getAllGoals(): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)
    
    @Query("UPDATE goals SET status = :status, completedDate = :completedDate WHERE id = :id")
    suspend fun updateGoalStatus(id: Int, status: String, completedDate: String? = null)
    
    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Int)

    @Query("SELECT * FROM mock_tests ORDER BY timestamp ASC")
    fun getAllMockTests(): Flow<List<MockTest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMockTest(test: MockTest)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMockTests(tests: List<MockTest>)

    @Query("DELETE FROM mock_tests WHERE id = :id")
    suspend fun deleteMockTest(id: Int)

    @Query("SELECT * FROM dpp_items")
    fun getAllDppItems(): Flow<List<DppItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDppItem(item: DppItem)

    @Query("DELETE FROM dpp_items WHERE id = :id")
    suspend fun deleteDppItem(id: Long)

    @Query("SELECT * FROM chat_messages WHERE LOWER(subject) = LOWER(:subject) AND LOWER(chatType) = LOWER(:chatType) ORDER BY timestamp ASC")
    fun getChatMessages(subject: String, chatType: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET text = :text, mediaUri = :mediaUri, mediaType = :mediaType, mediaName = :mediaName WHERE id = :id")
    suspend fun updateChatMessage(id: Long, text: String, mediaUri: String?, mediaType: String?, mediaName: String?)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteChatMessage(id: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllChatMessages()

    @Query("SELECT * FROM book_progressions")
    fun getAllBookProgressions(): Flow<List<BookProgression>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookProgression(progression: BookProgression)

    @Query("DELETE FROM book_progressions")
    suspend fun deleteAllBookProgressions()

    @Query("SELECT * FROM eduniti_targets ORDER BY timestamp DESC")
    fun getAllEdunitiTargets(): Flow<List<EdunitiTarget>>

    @Query("SELECT * FROM dynamic_checklist_topics ORDER BY timestamp ASC")
    fun getAllDynamicChecklistTopics(): kotlinx.coroutines.flow.Flow<List<DynamicChecklistTopic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDynamicChecklistTopic(topic: DynamicChecklistTopic)

    @Query("DELETE FROM dynamic_checklist_topics WHERE id = :id")
    suspend fun deleteDynamicChecklistTopic(id: Long)

    @Query("DELETE FROM dynamic_checklist_topics WHERE phaseTitle = :phaseTitle AND subject = :subject")
    suspend fun deleteDynamicChecklistPhase(phaseTitle: String, subject: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdunitiTarget(target: EdunitiTarget)

    @Query("DELETE FROM eduniti_targets WHERE id = :id")
    suspend fun deleteEdunitiTarget(id: Long)

    @Query("DELETE FROM eduniti_targets")
    suspend fun deleteAllEdunitiTargets()

    @Query("SELECT * FROM flashcard_decks ORDER BY created ASC")
    fun getAllFlashcardDecks(): Flow<List<FlashcardDeck>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcardDeck(deck: FlashcardDeck): Long

    @Query("DELETE FROM flashcard_decks WHERE id = :id")
    suspend fun deleteFlashcardDeck(id: Long)

    @Query("SELECT * FROM flashcard_items ORDER BY id DESC")
    fun getAllFlashcardItems(): Flow<List<FlashcardItem>>

    @Query("SELECT * FROM flashcard_items WHERE deckId = :deckId ORDER BY id DESC")
    fun getFlashcardItemsForDeck(deckId: Long): Flow<List<FlashcardItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcardItem(item: FlashcardItem)

    @Query("DELETE FROM flashcard_items WHERE id = :id")
    suspend fun deleteFlashcardItem(id: Long)

    @Query("DELETE FROM flashcard_items WHERE deckId = :deckId")
    suspend fun deleteFlashcardItemsForDeck(deckId: Long)

    @Query("DELETE FROM flashcard_decks")
    suspend fun deleteAllFlashcardDecks()

    @Query("DELETE FROM flashcard_items")
    suspend fun deleteAllFlashcardItems()

    @Query("SELECT * FROM mistake_logs ORDER BY timestamp DESC")
    fun getAllMistakeLogs(): Flow<List<MistakeLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMistakeLog(log: MistakeLog)

    @Query("DELETE FROM mistake_logs WHERE id = :id")
    suspend fun deleteMistakeLog(id: Int)

    @Query("SELECT * FROM ai_saved_tests ORDER BY timestamp DESC")
    fun getAllAiSavedTests(): Flow<List<AiSavedTest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiSavedTest(test: AiSavedTest): Long

    @Query("DELETE FROM ai_saved_tests WHERE id = :id")
    suspend fun deleteAiSavedTest(id: Long)

    @Query("DELETE FROM ai_saved_tests")
    suspend fun deleteAllAiSavedTests()

    @Query("SELECT * FROM weak_topics ORDER BY mistakesCount DESC, timestamp DESC")
    fun getAllWeakTopics(): Flow<List<WeakTopic>>

    @Query("SELECT * FROM weak_topics WHERE subject = :subject ORDER BY mistakesCount DESC, timestamp DESC")
    fun getWeakTopicsBySubject(subject: String): Flow<List<WeakTopic>>

    @Query("SELECT * FROM weak_topics ORDER BY mistakesCount DESC, timestamp DESC LIMIT 30")
    suspend fun getAllWeakTopicsDirect(): List<WeakTopic>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeakTopic(item: WeakTopic): Long

    @Query("DELETE FROM weak_topics WHERE id = :id")
    suspend fun deleteWeakTopic(id: Long)

    @Query("DELETE FROM weak_topics")
    suspend fun deleteAllWeakTopics()

    // ----------------------------------------------------
    // GOAL TRACKER (Physics, Chemistry, Botany, Zoology)
    // ----------------------------------------------------
    @Query("SELECT * FROM chapter_goal_trackers")
    fun getAllChapterGoalProgress(): Flow<List<ChapterGoalTracker>>

    @Query("SELECT * FROM chapter_goal_trackers WHERE subject = :subject")
    fun getChapterGoalProgressBySubject(subject: String): Flow<List<ChapterGoalTracker>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChapterGoalProgress(item: ChapterGoalTracker)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChapterGoalProgressList(items: List<ChapterGoalTracker>)

    @Query("DELETE FROM chapter_goal_trackers WHERE subject = :subject")
    suspend fun resetSubjectGoalProgress(subject: String)

    @Query("SELECT * FROM subject_goal_notes")
    fun getAllSubjectGoalNotes(): Flow<List<SubjectGoalNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSubjectGoalNote(item: SubjectGoalNote)
}

@Entity(tableName = "chapter_goal_trackers")
data class ChapterGoalTracker(
    @PrimaryKey val chapterKey: String, // e.g. "Physics_Unit and Measurements"
    val subject: String, // "Physics", "Chemistry", "Botany", "Zoology"
    val chapterName: String,
    val lectureDone: Boolean = false,
    val ncertUnderstood: Boolean = false,
    val notesMade: Boolean = false,
    val revision1: Boolean = false,
    val revision2: Boolean = false,
    val revision3: Boolean = false,
    val mcqsDone: Boolean = false,
    val pyqsDone: Boolean = false,
    val isCompleted: Boolean = false,
    val customNotes: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "subject_goal_notes")
data class SubjectGoalNote(
    @PrimaryKey val subject: String, // "Physics", "Chemistry", "Botany", "Zoology"
    val notesText: String = "",
    val updatedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val color: Long = 0xFFFFFFFF
)

@Entity(tableName = "weak_topics")
data class WeakTopic(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String, // "Physics", "Chemistry", "Biology"
    val chapter: String,
    val topicName: String,
    val mistakesCount: Int = 1,
    val avgTimeSpentSeconds: Int = 0,
    val severityLevel: String = "HIGH", // "CRITICAL", "HIGH", "MODERATE"
    val aiGuidanceNotes: String = "",
    val lastTestDate: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

val WeakTopic.sourceWebsite: String?
    get() {
        val notes = aiGuidanceNotes
        if (notes.contains("[Source:")) {
            return notes.substringAfter("[Source:").substringBefore("]").trim()
        }
        if (notes.contains("[Website:")) {
            return notes.substringAfter("[Website:").substringBefore("]").trim()
        }
        if (topicName.contains("http://") || topicName.contains("https://") || topicName.contains(".com")) {
            val words = topicName.split(" ", "(", ")", "[", "]")
            return words.firstOrNull { it.contains(".com") || it.contains("http") || it.contains("neetprep") }?.trim()
        }
        return null
    }

fun formatGuidanceWithSource(notes: String, website: String?): String {
    if (website.isNullOrBlank()) return notes
    val cleanNotes = notes.replace(Regex("""\[(Source|Website):[^\]]+\]"""), "").trim()
    return "[Source: ${website.trim()}] $cleanNotes".trim()
}

data class CandidateWeakTopic(
    val id: String = java.util.UUID.randomUUID().toString(),
    val subject: String,
    val chapter: String,
    val topicName: String,
    val mistakesCount: Int,
    val avgTimeSpentSeconds: Int,
    val severityLevel: String,
    val aiGuidanceNotes: String,
    val testTitle: String
)

@Database(entities = [MistakeLog::class, DynamicChecklistTopic::class, StudyLog::class, Goal::class, MockTest::class, DailyPractice::class, CompletedTopic::class, DppItem::class, ScheduledMockTest::class, ChatMessageEntity::class, BookProgression::class, EdunitiTarget::class, FlashcardDeck::class, FlashcardItem::class, AiSavedTest::class, Habit::class, Note::class, WeakTopic::class, ChapterGoalTracker::class, SubjectGoalNote::class], version = 33, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        val MIGRATION_23_24 = object : androidx.room.migration.Migration(23, 24) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `color` INTEGER NOT NULL)", emptyArray())
            }
        }

        val MIGRATION_24_25 = object : androidx.room.migration.Migration(24, 25) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `mistake_logs` ADD COLUMN `chapter` TEXT NOT NULL DEFAULT ''", emptyArray())
            }
        }

        val MIGRATION_25_26 = object : androidx.room.migration.Migration(25, 26) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `mistake_logs` ADD COLUMN `pdfUri` TEXT DEFAULT NULL", emptyArray())
                db.execSQL("ALTER TABLE `mistake_logs` ADD COLUMN `pdfName` TEXT DEFAULT NULL", emptyArray())
            }
        }

        val MIGRATION_26_27 = object : androidx.room.migration.Migration(26, 27) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `daily_practice` ADD COLUMN `physicsDifficulty` TEXT NOT NULL DEFAULT 'Medium'", emptyArray())
                db.execSQL("ALTER TABLE `daily_practice` ADD COLUMN `chemistryDifficulty` TEXT NOT NULL DEFAULT 'Medium'", emptyArray())
                db.execSQL("ALTER TABLE `daily_practice` ADD COLUMN `biologyDifficulty` TEXT NOT NULL DEFAULT 'Medium'", emptyArray())
            }
        }

        val MIGRATION_27_28 = object : androidx.room.migration.Migration(27, 28) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `ai_saved_tests` ADD COLUMN `institute` TEXT NOT NULL DEFAULT 'Self/General'", emptyArray())
                db.execSQL("ALTER TABLE `ai_saved_tests` ADD COLUMN `difficultyDistribution` TEXT NOT NULL DEFAULT ''", emptyArray())
            }
        }

        val MIGRATION_28_29 = object : androidx.room.migration.Migration(28, 29) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `scheduled_mock_tests` ADD COLUMN `isPinned` INTEGER NOT NULL DEFAULT 0", emptyArray())
            }
        }

        val MIGRATION_29_30 = object : androidx.room.migration.Migration(29, 30) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `mistake_logs` ADD COLUMN `imageUri` TEXT DEFAULT NULL", emptyArray())
            }
        }

        val MIGRATION_30_31 = object : androidx.room.migration.Migration(30, 31) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `mistake_logs` ADD COLUMN `reviewStage` INTEGER NOT NULL DEFAULT 0", emptyArray())
                db.execSQL("ALTER TABLE `mistake_logs` ADD COLUMN `nextReviewTime` INTEGER NOT NULL DEFAULT 0", emptyArray())
                db.execSQL("ALTER TABLE `mistake_logs` ADD COLUMN `reviewCount` INTEGER NOT NULL DEFAULT 0", emptyArray())
                db.execSQL("ALTER TABLE `mistake_logs` ADD COLUMN `lastReviewedTime` INTEGER NOT NULL DEFAULT 0", emptyArray())
                db.execSQL("ALTER TABLE `mistake_logs` ADD COLUMN `isMastered` INTEGER NOT NULL DEFAULT 0", emptyArray())
            }
        }

        val MIGRATION_31_32 = object : androidx.room.migration.Migration(31, 32) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `weak_topics` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `subject` TEXT NOT NULL, `chapter` TEXT NOT NULL, `topicName` TEXT NOT NULL, `mistakesCount` INTEGER NOT NULL DEFAULT 1, `avgTimeSpentSeconds` INTEGER NOT NULL DEFAULT 0, `severityLevel` TEXT NOT NULL DEFAULT 'HIGH', `aiGuidanceNotes` TEXT NOT NULL DEFAULT '', `lastTestDate` TEXT NOT NULL DEFAULT '', `timestamp` INTEGER NOT NULL DEFAULT 0)", emptyArray())
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "neet_tracker_db"
                ).addMigrations(MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32)
                    .fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }

        fun closeAndResetDatabase() {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                INSTANCE = null
            }
        }
    }
}
