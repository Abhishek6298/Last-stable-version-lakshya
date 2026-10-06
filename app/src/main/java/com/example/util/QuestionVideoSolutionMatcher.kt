package com.example.util

import com.example.ui.screens.YouTubeVideo

/**
 * Intelligent Question-to-Video Matcher (Doubtnut / Lens AI Engine)
 *
 * Extracts clean question stems, eliminates OCR noise (Q numbers, options, watermarks),
 * generates targeted YouTube search queries, and performs n-gram + numerical
 * semantic re-ranking to surface exact same question video solutions at position #1.
 */
object QuestionVideoSolutionMatcher {

    data class CleanQuestionInfo(
        val rawText: String,
        val cleanStatement: String,
        val searchStem: String,
        val secondaryStem: String,
        val numericalTokens: List<String>,
        val keyWords: List<String>,
        val options: List<String> = emptyList()
    )

    private val STOP_WORDS = setOf(
        "the", "is", "at", "which", "on", "a", "an", "and", "or", "in", "to", "of", "for",
        "with", "by", "from", "as", "into", "like", "through", "after", "over", "between",
        "out", "against", "during", "without", "before", "under", "around", "among",
        "what", "where", "when", "why", "how", "find", "calculate", "determine", "evaluate",
        "given", "shown", "figure", "following", "statement", "correct", "incorrect",
        "option", "options", "choose", "value", "ratio", "magnitude", "respect", "total"
    )

    /**
     * Cleans raw OCR / question text, removing question numbering, options, and noise.
     */
    fun extractCleanQuestionInfo(rawText: String): CleanQuestionInfo {
        if (rawText.isBlank()) {
            return CleanQuestionInfo("", "", "", "", emptyList(), emptyList())
        }

        var text = rawText
            .replace("\r\n", "\n")
            .replace("\r", "\n")

        // 1. Remove exam year tags: [NEET 2021], (JEE Main 2023), [AIPMT 2015], (CBSE 2019)
        text = text.replace(Regex("""\[\s*(?:NEET|JEE|AIPMT|AIIMS|CBSE|NCERT|BITSAT)[^\]]*\]""", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("""\(\s*(?:NEET|JEE|AIPMT|AIIMS|CBSE|NCERT|BITSAT)[^\)]*\)""", RegexOption.IGNORE_CASE), "")

        // 2. Remove leading question numbering: "Q.12", "12.", "Q 14:", "Question 5)", etc.
        text = text.replace(Regex("""^\s*(?:Q(?:uestion)?[\.\:\s\-]*\d+|\d+[\.\)\:\-]\s*)+""", RegexOption.IGNORE_CASE), "")

        // 3. Extract multiple choice options: (1), (2), (3), (4) or (A), (B), (C), (D) or A., B., C., D.
        val optionsList = mutableListOf<String>()
        val optionRegex = Regex("""(?:\([a-dA-D1-4]\)|[a-dA-D1-4]\.)\s+([^\(\n\r]+)""")
        for (m in optionRegex.findAll(text)) {
            val optVal = m.groupValues[1].trim()
            if (optVal.isNotBlank() && optVal.length < 100) {
                optionsList.add(optVal)
            }
        }

        // 4. Strip out options from the question stem
        val optionSplitIndex = text.indexOfFirst { c ->
            // Check for pattern like (1) or (a) or (A)
            false
        }
        val firstOptMatch = Regex("""(?:\([a-dA-D1-4]\)|(?:\n\s*[a-dA-D1-4]\.))""").find(text)
        val statementOnly = if (firstOptMatch != null && firstOptMatch.range.first > 15) {
            text.substring(0, firstOptMatch.range.first).trim()
        } else {
            text.lines().firstOrNull { it.isNotBlank() && it.length > 10 } ?: text
        }

        // Clean punctuation and excess whitespace
        val cleanStatement = statementOnly
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""^\s*[\-\–\—\:\.\,]\s*"""), "")
            .trim()

        // 5. Extract numerical tokens with units: e.g. "2 kg", "20 m/s", "30°", "0.25", "10^5", "5 cm"
        val numberRegex = Regex("""\b\d+(?:\.\d+)?(?:\s*(?:kg|g|mg|m/s|km/h|cm/s|m|cm|mm|nm|pm|s|sec|min|hr|eV|keV|MeV|V|mV|kV|A|mA|μA|uA|Ω|ohm|kΩ|Hz|kHz|MHz|GHz|J|kJ|cal|N|kN|rad/s|rad|mol|molar|molarity|mL|L|°|degree|deg|%|pF|μF|uF|nF|F|μC|uC|nC|C|T|G|W|kW|Pa|atm|N/m))\b""", RegexOption.IGNORE_CASE)
        val numMatches = numberRegex.findAll(text).map { it.value.trim() }.toList()

        // Also standalone numbers with >= 2 digits or decimals
        val rawNumRegex = Regex("""\b\d+\.\d+\b|\b\d{2,}\b""")
        val rawNumMatches = rawNumRegex.findAll(text).map { it.value.trim() }.toList()
        val allNumbers = (numMatches + rawNumMatches).distinct().take(8)

        // 6. Extract scientific keywords
        val words = cleanStatement
            .replace(Regex("""[^\w\s\-\.]"""), " ")
            .split(Regex("""\s+"""))
            .map { it.trim().lowercase() }
            .filter { it.length >= 4 && it !in STOP_WORDS && !it.matches(Regex("""^\d+$""")) }
        val keywords = words.distinct().take(10)

        // 7. Generate primary search stem (first 8 to 12 words, capped at ~65 chars)
        val stemWords = cleanStatement
            .replace(Regex("""[^\w\s\-\.\,\/\^\(\)]"""), " ")
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }

        val stem = if (stemWords.size <= 11) {
            stemWords.joinToString(" ")
        } else {
            stemWords.take(11).joinToString(" ")
        }.take(70).trim()

        // Secondary stem (middle clause or next 8 words if available)
        val secondaryStem = if (stemWords.size > 11) {
            stemWords.drop(8).take(10).joinToString(" ").take(65).trim()
        } else {
            ""
        }

        return CleanQuestionInfo(
            rawText = rawText,
            cleanStatement = cleanStatement,
            searchStem = stem,
            secondaryStem = secondaryStem,
            numericalTokens = allNumbers,
            keyWords = keywords,
            options = optionsList
        )
    }

    /**
     * Generates prioritized, ultra-targeted search queries crafted for YouTube
     * to pull exact Doubtnut, Physics Wallah, Unacademy, and educator solutions.
     */
    fun generateHighPrecisionVideoQueries(
        info: CleanQuestionInfo,
        subject: String = "Physics",
        chapter: String = "",
        topic: String = "",
        customQueries: List<String> = emptyList()
    ): List<String> {
        val queries = mutableListOf<String>()
        val stem = info.searchStem

        if (stem.isNotBlank()) {
            // 1. Doubtnut exact question database (Doubtnut titles millions of videos with the question stem)
            queries.add("$stem Doubtnut")

            // 2. Physics Wallah / PW exact question solution
            queries.add("$stem Physics Wallah")

            // 3. Exact stem verbatim across all YouTube creators
            queries.add(stem)

            // 4. Stem + "solution"
            queries.add("$stem solution")

            // 5. Subject-specific top provider
            when (subject.lowercase()) {
                "physics" -> {
                    queries.add("$stem Eduniti Physics")
                    queries.add("Physics Wallah $stem solution")
                }
                "chemistry" -> {
                    queries.add("$stem Chemistry Doubtnut")
                    queries.add("$stem Vedantu Chemistry")
                }
                "mathematics", "maths" -> {
                    queries.add("$stem MathonGo")
                    queries.add("$stem Mohit Tyagi")
                }
                "biology" -> {
                    queries.add("$stem Biology solution NEET")
                    queries.add("$stem Unacademy NEET Biology")
                }
                else -> {
                    queries.add("$stem question solution")
                }
            }

            // 6. Secondary stem or numerical signature if available
            if (info.secondaryStem.isNotBlank()) {
                queries.add("${info.secondaryStem} Doubtnut")
                queries.add("${info.secondaryStem} solution")
            } else if (info.numericalTokens.isNotEmpty() && info.keyWords.isNotEmpty()) {
                val numPart = info.numericalTokens.take(3).joinToString(" ")
                val keyPart = info.keyWords.take(3).joinToString(" ")
                queries.add("$keyPart $numPart question solution")
            }
        }

        // Add custom LLM queries (cleaned)
        for (q in customQueries) {
            val clean = q.replace(Regex("""^\d+[\.\)]\s*"""), "").replace("\"", "").trim()
            if (clean.isNotBlank() && clean !in queries) {
                queries.add(clean)
            }
        }

        // Add chapter/topic fallback if needed
        if (chapter.isNotBlank()) {
            queries.add("Doubtnut $subject $chapter $topic")
            queries.add("Physics Wallah $subject $chapter $topic PYQ")
        }

        return queries.filter { it.isNotBlank() }.distinct().take(8)
    }

    /**
     * Precision Scorer: Evaluates and re-ranks gathered YouTube videos based on
     * n-gram phrase matches, numerical token alignment, keyword recall, and channel reputation.
     */
    fun scoreAndRankVideoSolutions(
        videos: List<YouTubeVideo>,
        info: CleanQuestionInfo,
        subject: String = ""
    ): List<YouTubeVideo> {
        if (videos.isEmpty() || info.cleanStatement.isBlank()) {
            return videos
        }

        val stemLower = info.searchStem.lowercase()
        val stemWords = stemLower.split(Regex("""\s+""")).filter { it.length >= 3 }
        val allKeywords = info.keyWords.map { it.lowercase() }.toSet()
        val allNumbers = info.numericalTokens.map { it.lowercase().replace(" ", "") }

        val scoredVideos = videos.map { video ->
            var score = 0
            val titleLower = video.title.lowercase()
            val descLower = video.description.lowercase()
            val channelLower = video.channelTitle.lowercase()

            // 1. Consecutive N-gram Substring Match (The most reliable indicator of exact question)
            for (windowSize in listOf(6, 5, 4, 3)) {
                if (stemWords.size >= windowSize) {
                    for (i in 0..(stemWords.size - windowSize)) {
                        val subPhrase = stemWords.subList(i, i + windowSize).joinToString(" ")
                        if (titleLower.contains(subPhrase)) {
                            when (windowSize) {
                                6 -> score += 380
                                5 -> score += 260
                                4 -> score += 160
                                3 -> score += 80
                            }
                            break // Only take highest match
                        }
                    }
                }
            }

            // 2. Numerical Token Exact Matching (Checks if numbers in question appear in video title)
            val titleNoSpace = titleLower.replace(" ", "")
            var matchedNumbers = 0
            for (num in allNumbers) {
                if (titleNoSpace.contains(num)) {
                    score += 55
                    matchedNumbers++
                } else if (descLower.contains(num)) {
                    score += 20
                }
            }
            if (matchedNumbers >= 2) {
                score += 80 // Bonus for multi-numerical match
            }

            // 3. Significant Keyword Recall
            var keywordMatchCount = 0
            for (kw in allKeywords) {
                if (titleLower.contains(kw)) {
                    keywordMatchCount++
                    score += 35
                }
            }
            if (allKeywords.isNotEmpty() && keywordMatchCount == allKeywords.size) {
                score += 100 // 100% keyword coverage
            }

            // 4. Channel Reputation & Solution Authority
            when {
                channelLower.contains("doubtnut") || titleLower.contains("doubtnut") -> score += 110
                channelLower.contains("physics wallah") || channelLower.contains("pw") || titleLower.contains("physics wallah") -> score += 85
                channelLower.contains("eduniti") -> score += 80
                channelLower.contains("mathongo") -> score += 80
                channelLower.contains("mohit tyagi") || channelLower.contains("competishun") -> score += 75
                channelLower.contains("vedantu") -> score += 70
                channelLower.contains("unacademy") -> score += 70
                channelLower.contains("allen") || channelLower.contains("aakash") -> score += 65
                channelLower.contains("khan academy") -> score += 60
            }

            // 5. Explicit Solution / Question Indicators
            if (titleLower.contains("solution") || titleLower.contains("solved") ||
                titleLower.contains("pyq") || titleLower.contains("question") ||
                titleLower.contains("numerical") || titleLower.contains("q.")
            ) {
                score += 45
            }

            // 6. Demote generic marathon / full chapter lectures if no strong phrase match
            val isGenericLecture = titleLower.contains("one shot") ||
                                   titleLower.contains("full chapter") ||
                                   titleLower.contains("complete revision") ||
                                   titleLower.contains("marathon") ||
                                   titleLower.contains("strategy") ||
                                   titleLower.contains("roadmap") ||
                                   titleLower.contains("batch launch") ||
                                   titleLower.contains("timetable")

            if (isGenericLecture && score < 200) {
                score -= 160
            }

            val isExact = score >= 160 || (score >= 120 && (channelLower.contains("doubtnut") || channelLower.contains("physics wallah")))

            video.copy(
                matchScore = score,
                isExactMatch = isExact
            )
        }

        // Sort descending by calculated score
        return scoredVideos.sortedByDescending { it.matchScore }
    }
}
