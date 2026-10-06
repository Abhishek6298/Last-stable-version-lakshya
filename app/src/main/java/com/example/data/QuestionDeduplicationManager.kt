package com.example.data

import android.content.Context
import java.util.Locale

/**
 * Manages question fingerprints and deduplication history across test sessions.
 * Guarantees that when a student asks for questions for a weak topic or from a specific website/URL,
 * previously seen questions are strictly prevented from repeating.
 */
object QuestionDeduplicationManager {

    private const val PREFS_NAME = "cbt_question_dedup_store"
    private const val MAX_SAVED_STEMS_PER_TOPIC = 400

    private fun normalizeKey(topic: String, chapter: String, source: String? = null): String {
        val raw = "${chapter}_${topic}_${source ?: ""}".lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9_]"), "_")
        return raw.take(64)
    }

    /**
     * Extracts a normalized fingerprint of the question stem for similarity comparison.
     */
    fun extractFingerprint(questionText: String): String {
        return questionText.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]"), "")
            .take(80)
    }

    /**
     * Extracts key keywords from the question to detect semantic duplication.
     */
    fun extractKeywords(questionText: String): Set<String> {
        return questionText.lowercase(Locale.ROOT)
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 && it !in STOP_WORDS }
            .toSet()
    }

    private val STOP_WORDS = setOf(
        "the", "which", "what", "following", "statement", "statements", "correct", "incorrect",
        "with", "from", "that", "this", "these", "those", "have", "been", "when", "where",
        "given", "find", "calculate", "value", "ratio", "select", "option", "consider", "below"
    )

    /**
     * Retrieves all previously tested question fingerprints for the given topic & chapter.
     */
    fun getRecordedFingerprints(context: Context, topic: String, chapter: String, source: String? = null): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "stems_" + normalizeKey(topic, chapter, source)
        return prefs.getStringSet(key, emptySet())?.toSet() ?: emptySet()
    }

    /**
     * Retrieves readable samples of past question stems to feed into the AI negative prompt.
     */
    fun getRecordedSamplePrompts(context: Context, topic: String, chapter: String, source: String? = null, maxSamples: Int = 12): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "samples_" + normalizeKey(topic, chapter, source)
        val rawSet = prefs.getStringSet(key, emptySet()) ?: emptySet()
        return rawSet.take(maxSamples).toList()
    }

    /**
     * Records new questions into the history store so future tests will not repeat them.
     */
    fun recordQuestions(
        context: Context,
        topic: String,
        chapter: String,
        source: String? = null,
        questions: List<AiTestQuestion>
    ) {
        if (questions.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stemKey = "stems_" + normalizeKey(topic, chapter, source)
        val sampleKey = "samples_" + normalizeKey(topic, chapter, source)

        val existingStems = (prefs.getStringSet(stemKey, emptySet()) ?: emptySet()).toMutableSet()
        val existingSamples = (prefs.getStringSet(sampleKey, emptySet()) ?: emptySet()).toMutableSet()

        for (q in questions) {
            val fp = extractFingerprint(q.questionText)
            if (fp.length >= 15) {
                existingStems.add(fp)
            }
            // Save clean readable summary (first 70 chars) for prompt exclusion
            val sample = q.questionText.trim().replace("\n", " ").take(75)
            if (sample.length >= 15) {
                existingSamples.add(sample)
            }
        }

        // Limit size to prevent unbounded growth
        val trimmedStems = if (existingStems.size > MAX_SAVED_STEMS_PER_TOPIC) {
            existingStems.toList().takeLast(MAX_SAVED_STEMS_PER_TOPIC).toSet()
        } else existingStems

        val trimmedSamples = if (existingSamples.size > 50) {
            existingSamples.toList().takeLast(50).toSet()
        } else existingSamples

        prefs.edit()
            .putStringSet(stemKey, trimmedStems)
            .putStringSet(sampleKey, trimmedSamples)
            .apply()
    }

    /**
     * Checks if a new question is a duplicate of any previously recorded question.
     */
    fun isDuplicate(questionText: String, recordedFingerprints: Set<String>): Boolean {
        if (questionText.isBlank()) return true
        val fp = extractFingerprint(questionText)
        if (fp in recordedFingerprints) return true

        val newWords = extractKeywords(questionText)
        if (newWords.size >= 5) {
            // Also check partial stem overlap
            val prefix = fp.take(35)
            if (prefix.length >= 25 && recordedFingerprints.any { it.startsWith(prefix) }) {
                return true
            }
        }
        return false
    }

    /**
     * Filters out duplicate questions from the new candidate list,
     * comparing against both recorded past questions and intra-batch duplicates.
     */
    fun filterDuplicates(
        candidates: List<AiTestQuestion>,
        recordedFingerprints: Set<String>
    ): List<AiTestQuestion> {
        val unique = mutableListOf<AiTestQuestion>()
        for (q in candidates) {
            if (q.questionText.isBlank()) continue
            if (isDuplicate(q.questionText, recordedFingerprints)) continue

            // Intra-batch check
            val isDuplicateInBatch = unique.any { existing ->
                GeminiAiTestGenerator.isSimilarQuestion(existing.questionText, q.questionText)
            }
            if (!isDuplicateInBatch) {
                unique.add(q)
            }
        }
        return unique
    }

    /**
     * Builds a negative prompt clause to instruct the AI not to repeat past questions.
     */
    fun buildNegativePromptClause(pastSamples: List<String>): String {
        if (pastSamples.isEmpty()) return ""
        val bulletPoints = pastSamples.joinToString("\n") { "  - \"$it...\"" }
        return """
            CRITICAL REPETITION PREVENTION (STRICT MANDATE):
            The student has ALREADY solved the following questions on this topic.
            Under NO circumstances repeat, rephrase, or change numbers for any of these questions:
            $bulletPoints
            
            Every question generated MUST be completely distinct, testing a fresh angle, reaction, formula, or concept.
        """.trimIndent()
    }

    /**
     * Clears history for a specific topic if user requests a full reset.
     */
    fun clearTopicHistory(context: Context, topic: String, chapter: String, source: String? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stemKey = "stems_" + normalizeKey(topic, chapter, source)
        val sampleKey = "samples_" + normalizeKey(topic, chapter, source)
        prefs.edit().remove(stemKey).remove(sampleKey).apply()
    }
}
