package com.example.data

import java.util.regex.Pattern

enum class TestSubjectCategory(val displayName: String, val maxNeet: Int, val maxJee: Int) {
    FULL_MOCK("Full Mock", 720, 300),
    PHYSICS("Physics", 180, 100),
    CHEMISTRY("Chemistry", 180, 100),
    BIOLOGY("Biology", 360, 100)
}

/**
 * Determines whether a MockTest is a single-subject test (Physics only, Chemistry only, or Biology only)
 * or a full-length comprehensive mock test (720M for NEET / 300M for JEE).
 */
fun MockTest.getSubjectCategory(): TestSubjectCategory {
    val analysis = geminiAnalysis ?: ""
    val nameLower = testName.lowercase()

    // 1. Explicit tags in analysis or test name
    if (analysis.contains("Single Subject (Physics)", ignoreCase = true) ||
        analysis.contains("Subject: Physics", ignoreCase = true) ||
        analysis.contains("SINGLE_SUBJECT_PHYSICS", ignoreCase = true) ||
        nameLower.contains("[physics]") ||
        nameLower.contains("physics only") ||
        nameLower.contains("physics test") ||
        nameLower.contains("physics unit")
    ) {
        return TestSubjectCategory.PHYSICS
    }

    if (analysis.contains("Single Subject (Chemistry)", ignoreCase = true) ||
        analysis.contains("Subject: Chemistry", ignoreCase = true) ||
        analysis.contains("SINGLE_SUBJECT_CHEMISTRY", ignoreCase = true) ||
        nameLower.contains("[chemistry]") ||
        nameLower.contains("chemistry only") ||
        nameLower.contains("chemistry test") ||
        nameLower.contains("chemistry unit")
    ) {
        return TestSubjectCategory.CHEMISTRY
    }

    if (analysis.contains("Single Subject (Biology)", ignoreCase = true) ||
        analysis.contains("Subject: Biology", ignoreCase = true) ||
        analysis.contains("Subject: Botany", ignoreCase = true) ||
        analysis.contains("Subject: Zoology", ignoreCase = true) ||
        analysis.contains("SINGLE_SUBJECT_BIOLOGY", ignoreCase = true) ||
        nameLower.contains("[biology]") ||
        nameLower.contains("biology only") ||
        nameLower.contains("biology test") ||
        nameLower.contains("biology unit") ||
        nameLower.contains("[math") ||
        nameLower.contains("math test")
    ) {
        return TestSubjectCategory.BIOLOGY
    }

    // 2. Marks breakdown heuristic:
    // If only ONE subject has marks and the others are 0, and total score matches single-subject scale (<= 180 for P/C or <= 360 for B)
    val hasPhy = physics > 0
    val hasChem = chemistry > 0
    val hasBio = biology > 0

    if (hasPhy && !hasChem && !hasBio && score <= 180) {
        return TestSubjectCategory.PHYSICS
    }
    if (!hasPhy && hasChem && !hasBio && score <= 180) {
        return TestSubjectCategory.CHEMISTRY
    }
    if (!hasPhy && !hasChem && hasBio && score <= 360) {
        return TestSubjectCategory.BIOLOGY
    }

    // 3. Name mentions single subject exclusively and does not contain "Full" or "720" or "Grand"
    val isExplicitFull = nameLower.contains("full") || nameLower.contains("720") || nameLower.contains("300") || nameLower.contains("grand") || nameLower.contains("all subject")
    if (!isExplicitFull) {
        if ((nameLower.contains("physics") || nameLower.contains("phy")) && !nameLower.contains("chem") && !nameLower.contains("bio")) {
            return TestSubjectCategory.PHYSICS
        }
        if ((nameLower.contains("chemistry") || nameLower.contains("chem")) && !nameLower.contains("physics") && !nameLower.contains("bio")) {
            return TestSubjectCategory.CHEMISTRY
        }
        if ((nameLower.contains("biology") || nameLower.contains("bio") || nameLower.contains("botany") || nameLower.contains("zoology") || nameLower.contains("math")) && !nameLower.contains("physics") && !nameLower.contains("chem")) {
            return TestSubjectCategory.BIOLOGY
        }
    }

    return TestSubjectCategory.FULL_MOCK
}

fun MockTest.isSingleSubjectTest(): Boolean {
    return getSubjectCategory() != TestSubjectCategory.FULL_MOCK
}

/**
 * Returns the effective score for a specific subject from this test.
 * If the test is a single-subject test for that subject, returns `score` (or the subject score).
 * If the test is a full mock, returns the sectional subject score.
 */
fun MockTest.getSubjectScore(category: TestSubjectCategory): Int {
    return when (category) {
        TestSubjectCategory.PHYSICS -> {
            if (getSubjectCategory() == TestSubjectCategory.PHYSICS) {
                if (physics > 0) physics else score
            } else {
                physics
            }
        }
        TestSubjectCategory.CHEMISTRY -> {
            if (getSubjectCategory() == TestSubjectCategory.CHEMISTRY) {
                if (chemistry > 0) chemistry else score
            } else {
                chemistry
            }
        }
        TestSubjectCategory.BIOLOGY -> {
            if (getSubjectCategory() == TestSubjectCategory.BIOLOGY) {
                if (biology > 0) biology else score
            } else {
                biology
            }
        }
        TestSubjectCategory.FULL_MOCK -> score
    }
}

/**
 * Returns the maximum possible score for this test.
 */
fun MockTest.getMaxScore(isJee: Boolean = false): Int {
    // 1. Check in geminiAnalysis first (e.g. "Max Marks: 40", "Total Marks: 35 / 40", "Max: 45")
    val analysis = geminiAnalysis ?: ""
    val analysisMatch = Regex("""(?:Total Marks|Max Marks|Maximum Marks|Max|Total)\s*:\s*(?:\d+\s*/\s*)?(\d+)""", RegexOption.IGNORE_CASE).find(analysis)
    if (analysisMatch != null) {
        val parsed = analysisMatch.groupValues[1].toIntOrNull()
        if (parsed != null && parsed > 0) return maxOf(parsed, score)
    }

    val analysisSlash = Regex("""/\s*(\d+)""").find(analysis)
    if (analysisSlash != null) {
        val parsed = analysisSlash.groupValues[1].toIntOrNull()
        if (parsed != null && parsed > 0) return maxOf(parsed, score)
    }

    // 2. Check if dynamic max is written in testName e.g. "(180M)", "(180 Marks)", "(300M)", "(720M)", "[100M]", "(45M)", "(40M)", "(20M)"
    val match = Regex("""[\(\[]\s*(\d+)\s*(?:M|Marks|marks|pts|Points)?\s*[\)\]]""").find(testName)
    if (match != null) {
        val parsed = match.groupValues[1].toIntOrNull()
        if (parsed != null && parsed > 0) return maxOf(parsed, score)
    }

    // 3. Check if explicit slash format e.g. "Score: 160/180" or "/ 300" in testName
    val slashMatch = Regex("""/\s*(\d+)""").find(testName)
    if (slashMatch != null) {
        val parsed = slashMatch.groupValues[1].toIntOrNull()
        if (parsed != null && parsed > 0) return maxOf(parsed, score)
    }

    // 4. Single subject test category default
    val cat = getSubjectCategory()
    if (cat != TestSubjectCategory.FULL_MOCK) {
        val catMax = if (isJee) cat.maxJee else cat.maxNeet
        return maxOf(catMax, score)
    }

    // 5. If user score itself exceeds standard 720
    if (score > 720) return score

    return if (isJee) 300 else 720
}
