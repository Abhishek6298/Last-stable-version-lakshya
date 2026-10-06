package com.example.data

import android.content.Context
import java.util.Locale

/**
 * Tracks questions from tests that were previously answered incorrectly or skipped,
 * and marks them as "Mastered" once the student answers them correctly in any subsequent test.
 *
 * Guarantees that mastered questions are excluded from future Mistakes & Weak Area revision CBT tests.
 */
object MasteredMistakeTracker {

    private const val PREFS_NAME = "mastered_mistakes_store"
    private const val KEY_MASTERED_STEMS = "mastered_question_fingerprints"

    fun extractFingerprint(questionText: String): String {
        return questionText.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]"), "")
            .take(90)
    }

    /**
     * Records questions that the user has answered correctly.
     */
    fun markMastered(context: Context, questions: List<AiTestQuestion>) {
        if (questions.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = (prefs.getStringSet(KEY_MASTERED_STEMS, emptySet()) ?: emptySet()).toMutableSet()

        for (q in questions) {
            val fp = extractFingerprint(q.questionText)
            if (fp.length >= 15) {
                existing.add(fp)
            }
        }

        prefs.edit().putStringSet(KEY_MASTERED_STEMS, existing).apply()
    }

    fun markMasteredSingle(context: Context, questionText: String) {
        val fp = extractFingerprint(questionText)
        if (fp.length < 15) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = (prefs.getStringSet(KEY_MASTERED_STEMS, emptySet()) ?: emptySet()).toMutableSet()
        existing.add(fp)
        prefs.edit().putStringSet(KEY_MASTERED_STEMS, existing).apply()
    }

    /**
     * Checks if a question has already been solved correctly in a previous attempt.
     */
    fun isMastered(context: Context, questionText: String): Boolean {
        if (questionText.isBlank()) return false
        val fp = extractFingerprint(questionText)
        if (fp.length < 15) return false

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val masteredSet = prefs.getStringSet(KEY_MASTERED_STEMS, emptySet()) ?: emptySet()
        if (fp in masteredSet) return true

        // Also check prefix overlap for minor formatting/wording variances
        val prefix = fp.take(40)
        if (prefix.length >= 25 && masteredSet.any { mastered ->
            mastered.startsWith(prefix) || (mastered.length >= 25 && prefix.startsWith(mastered.take(25)))
        }) {
            return true
        }
        return false
    }

    /**
     * Removes a question from the mastered list if the student gets it wrong again.
     */
    fun unmarkMastered(context: Context, questionText: String) {
        val fp = extractFingerprint(questionText)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = (prefs.getStringSet(KEY_MASTERED_STEMS, emptySet()) ?: emptySet()).toMutableSet()
        if (existing.remove(fp)) {
            prefs.edit().putStringSet(KEY_MASTERED_STEMS, existing).apply()
        }
    }

    fun getMasteredCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_MASTERED_STEMS, emptySet())?.size ?: 0
    }

    fun clearAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
