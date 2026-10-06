package com.example.ui.components

import com.example.util.MathFormatter
import com.example.util.SimpleLruCache

/**
 * Streaming-resilient AST Parser for Modern ChatGPT-style Chat Renderer.
 *
 * Converts incoming streaming or final AI response strings into typed content blocks:
 * - Paragraphs (with rich InlineNodes: Bold, Italic, Strikethrough, Inline Code, Inline Math, Links)
 * - Code Blocks (with language identification, syntax parsing, incomplete fence resilience)
 * - Display Math ($$...$$ and \[...\] with incomplete delimiter resilience)
 * - Tables (Markdown tables with column detection, zebra striping, partial row resilience)
 * - Lists (ordered and unordered with nesting levels)
 * - Blockquotes (single or multi-line with nested block parsing)
 * - Headings (H1 through H6)
 * - Horizontal Dividers (---, ***, ___)
 * - Images (![alt](url))
 */
object ChatMarkdownParser {

    private val HEADING_REGEX = Regex("""^(#{1,6})\s+(.*)$""")
    private val IMAGE_REGEX = Regex("""^!\[(.*?)\]\((https?://[^\s)]+|\S+)\)$""")
    private val TABLE_SEPARATOR_REGEX = Regex("""^\|?[\s:\-]+(\|[\s:\-]+)+\|?$""")
    private val UNORDERED_LIST_REGEX = Regex("""^(\s*)([*+-])\s+(.*)$""")
    private val ORDERED_LIST_REGEX = Regex("""^(\s*)(\d+)[.)]\s+(.*)$""")

    // Inline regex matching: LaTeX display, LaTeX inline, Chemistry mhchem, Bare Fractions/Roots, Links, Images, Code, Bold-Italic, Bold, Strike, Italic
    private val INLINE_REGEX = Regex(
        "(" +
        """(\\[\[\(][\s\S]*?\\[\]\)])""" +              // Group 1: \[...\] or \(...\)
        "|" + """(\$\$[\s\S]*?\$\$)""" +                // Group 2: $$...$$
        "|" + """(\$[^$\n]+\$)""" +                     // Group 3: $...$
        "|" + """(\\ce\{[^\}\n]+\})""" +                // Group 3b: \ce{...}
        "|" + """(\\(?:frac|dfrac|tfrac)\{[^\}\n]+\}\{[^\}\n]+\})""" + // Group 3c: bare \frac{num}{den}
        "|" + """(\\sqrt(?:\[[^\]\n]+\])?\{[^\}\n]+\})""" +             // Group 3d: bare \sqrt{x}
        "|" + """(!?\[[^\]\n]+\]\([^\)\s]+\))""" +      // Group 4: [text](url) or ![alt](url)
        "|" + """(`[^`\n]+`)""" +                       // Group 5: `code`
        "|" + """(\*\*\*[^\*\n]+\*\*\*)""" +            // Group 6: ***bold italic***
        "|" + """((?<!\w)\*\*[^\*\n]+?\*\*(?!\w))""" +  // Group 7: **bold**
        "|" + """(\b__[^\n_]+?__\b)""" +                // Group 8: __bold__
        "|" + """(~~[^~\n]+?~~)""" +                    // Group 9: ~~strike~~
        "|" + """((?<!\w)\*[^\*\n]+?\*(?!\w))""" +      // Group 10: *italic*
        "|" + """(\b_[^\n_]+?_\b)""" +                  // Group 11: _italic_
        ")"
    )

    private val blockCache = SimpleLruCache<String, List<AiBlock>>(300)

    /**
     * Parses the incoming raw AI text into structured AST blocks.
     * Guaranteed never to throw or crash on incomplete streaming buffers or malformed markdown/LaTeX.
     */
    fun parseBlocks(text: String, isStreaming: Boolean = false): List<AiBlock> {
        if (text.isBlank()) return emptyList()
        if (!isStreaming) {
            val cached = blockCache.get(text)
            if (cached != null) return cached
        }
        val blocks = doParseBlocks(text, isStreaming)
        if (!isStreaming) {
            blockCache.put(text, blocks)
        }
        return blocks
    }

    private fun doParseBlocks(text: String, isStreaming: Boolean = false): List<AiBlock> {
        if (text.isBlank()) return emptyList()

        val blocks = mutableListOf<AiBlock>()
        val lines = text.lines()
        val paragraphBuffer = mutableListOf<String>()

        fun flushParagraph() {
            if (paragraphBuffer.isNotEmpty()) {
                val content = paragraphBuffer.joinToString("\n").trim()
                if (content.isNotEmpty()) {
                    blocks.add(AiBlock.Paragraph(parseInlineRobust(content)))
                }
                paragraphBuffer.clear()
            }
        }

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()

            // 0. Thinking / Reasoning Process (<think> ... </think>)
            if (trimmed.startsWith("<think>") || (trimmed.startsWith("<thought>") || trimmed.startsWith("*Thinking Process:*"))) {
                flushParagraph()
                val isThinkTag = trimmed.startsWith("<think>") || trimmed.startsWith("<thought>")
                val endTag = if (trimmed.startsWith("<thought>")) "</thought>" else "</think>"
                val thinkLines = mutableListOf<String>()
                val firstContent = if (isThinkTag) trimmed.substringAfter(">").removeSuffix(endTag).trim() else trimmed.removePrefix("*Thinking Process:*").trim()
                if (firstContent.isNotEmpty()) thinkLines.add(firstContent)
                
                var closed = isThinkTag && trimmed.contains(endTag)
                i++
                while (i < lines.size && !closed) {
                    val tLine = lines[i]
                    if (isThinkTag && tLine.contains(endTag)) {
                        val finalPart = tLine.substringBefore(endTag).trim()
                        if (finalPart.isNotEmpty()) thinkLines.add(finalPart)
                        closed = true
                        i++
                        break
                    }
                    if (!isThinkTag && (tLine.trim().startsWith("#") || tLine.trim().startsWith("**Step") || tLine.trim().startsWith("```"))) {
                        break
                    }
                    thinkLines.add(tLine)
                    i++
                }
                blocks.add(AiBlock.Thinking(thinkLines.joinToString("\n").trim(), isComplete = closed || !isStreaming))
                continue
            }

            // 1. Code Block Fence (```)
            if (trimmed.startsWith("```")) {
                flushParagraph()
                val lang = trimmed.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                var closed = false
                while (i < lines.size) {
                    if (lines[i].trim().startsWith("```")) {
                        closed = true
                        i++
                        break
                    }
                    codeLines.add(lines[i])
                    i++
                }
                blocks.add(AiBlock.Code(lang, codeLines.joinToString("\n"), isComplete = closed || !isStreaming))
                continue
            }

            // 2. Display Math: $$ ... $$ or \[ ... \]
            if (trimmed.startsWith("$$") || trimmed.startsWith("\\[")) {
                val isDollar = trimmed.startsWith("$$")
                val endToken = if (isDollar) "$$" else "\\]"

                // Single line display math
                if (trimmed.length > 2 && trimmed.endsWith(endToken) && trimmed.length >= 4) {
                    flushParagraph()
                    val latex = trimmed.substring(2, trimmed.length - 2).trim()
                    blocks.add(AiBlock.MathDisplay(latex, isComplete = true))
                    i++
                    continue
                }

                // Multi-line display math
                flushParagraph()
                val mathLines = mutableListOf<String>()
                val firstContent = trimmed.substring(2).trim()
                if (firstContent.isNotEmpty()) mathLines.add(firstContent)
                i++
                var closed = false
                while (i < lines.size) {
                    val mLine = lines[i].trim()
                    if (mLine.endsWith(endToken)) {
                        val finalPart = mLine.removeSuffix(endToken).trim()
                        if (finalPart.isNotEmpty()) mathLines.add(finalPart)
                        closed = true
                        i++
                        break
                    }
                    mathLines.add(lines[i])
                    i++
                }
                blocks.add(AiBlock.MathDisplay(mathLines.joinToString("\n").trim(), isComplete = closed || !isStreaming))
                continue
            }

            // 2b. Chemistry Reaction Display Block: \ce{ ... }
            if (trimmed.startsWith("\\ce{") && trimmed.endsWith("}")) {
                flushParagraph()
                blocks.add(AiBlock.MathDisplay(trimmed, isComplete = true))
                i++
                continue
            }

            // 3. Image block: ![alt](url) (Only for direct valid web/asset images, excluding pollinations)
            val imgFind = Regex("""!\[(.*?)\]\((https?://[^\s)]+|\S+)\)""").find(trimmed)
            if (imgFind != null) {
                val preText = trimmed.substring(0, imgFind.range.first).trim()
                if (preText.isNotEmpty()) paragraphBuffer.add(preText)
                flushParagraph()
                val alt = imgFind.groupValues[1]
                val rawUrl = imgFind.groupValues[2]
                if (!rawUrl.contains("pollinations.ai", ignoreCase = true)) {
                    blocks.add(AiBlock.Image(url = rawUrl, alt = alt))
                }
                val postText = trimmed.substring(imgFind.range.last + 1).trim()
                if (postText.isNotEmpty()) paragraphBuffer.add(postText)
                i++
                continue
            }

            // 4. Markdown Table
            if (trimmed.contains("|") && i + 1 < lines.size && isTableSeparator(lines[i + 1].trim())) {
                flushParagraph()
                val headerLine = trimmed
                val headers = parseTableRow(headerLine)
                i += 2 // Skip header and separator

                val rows = mutableListOf<List<String>>()
                while (i < lines.size) {
                    val rowLine = lines[i].trim()
                    if (rowLine.isEmpty() || !rowLine.contains("|") || isTableSeparator(rowLine)) {
                        break
                    }
                    val row = parseTableRow(rowLine)
                    if (row.isNotEmpty()) {
                        rows.add(row)
                    }
                    i++
                }
                blocks.add(AiBlock.Table(headers, rows, isComplete = !isStreaming || i < lines.size))
                continue
            }

            // 5. Headings: # H1 to ###### H6 (With Step detection)
            val headingMatch = HEADING_REGEX.matchEntire(trimmed)
            if (headingMatch != null) {
                flushParagraph()
                val level = headingMatch.groupValues[1].length
                val content = headingMatch.groupValues[2]
                
                val stepMatch = Regex("""^Step\s+(\d+)[:.-]?\s*(.*)$""", RegexOption.IGNORE_CASE).matchEntire(content.trim())
                if (stepMatch != null) {
                    val stepNum = stepMatch.groupValues[1].toIntOrNull() ?: 1
                    val stepTitle = stepMatch.groupValues[2].ifBlank { "Step $stepNum" }
                    blocks.add(AiBlock.StepCard(stepNum, stepTitle, listOf(AiBlock.Paragraph(parseInlineRobust(stepTitle)))))
                } else {
                    blocks.add(AiBlock.Heading(level, parseInlineRobust(content)))
                }
                i++
                continue
            }

            // 6. Horizontal Divider: ---, ***, ___
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                flushParagraph()
                blocks.add(AiBlock.Divider)
                i++
                continue
            }

            // 7. Blockquote / Callout / Admonitions (> [!TIP], > [!NOTE], > [!IMPORTANT], etc.)
            if (trimmed.startsWith(">")) {
                flushParagraph()
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines.add(lines[i].trim().removePrefix(">").trimStart())
                    i++
                }
                
                val firstQuoteLine = quoteLines.firstOrNull() ?: ""
                val calloutMatch = Regex("""^\[!(NOTE|TIP|IMPORTANT|WARNING|CAUTION|CONCEPT|FORMULA|KEY_TAKEAWAY|TAKEAWAY|TRAP|COMMON_TRAP|STEP)\]\s*(.*)$""", RegexOption.IGNORE_CASE).matchEntire(firstQuoteLine)
                
                if (calloutMatch != null) {
                    val rawType = calloutMatch.groupValues[1].uppercase()
                    val title = calloutMatch.groupValues[2].ifBlank { null }
                    val remainingLines = quoteLines.drop(1).joinToString("\n")
                    val calloutType = when (rawType) {
                        "TIP" -> CalloutType.TIP
                        "IMPORTANT" -> CalloutType.IMPORTANT
                        "WARNING" -> CalloutType.WARNING
                        "CAUTION" -> CalloutType.CAUTION
                        "CONCEPT" -> CalloutType.CONCEPT
                        "FORMULA" -> CalloutType.FORMULA
                        "KEY_TAKEAWAY", "TAKEAWAY" -> CalloutType.KEY_TAKEAWAY
                        "TRAP", "COMMON_TRAP" -> CalloutType.COMMON_TRAP
                        "STEP" -> CalloutType.STEP
                        else -> CalloutType.NOTE
                    }
                    val innerBlocks = parseBlocks(remainingLines, isStreaming)
                    blocks.add(AiBlock.Callout(calloutType, title, innerBlocks))
                } else {
                    val innerBlocks = parseBlocks(quoteLines.joinToString("\n"), isStreaming)
                    blocks.add(AiBlock.BlockQuote(innerBlocks))
                }
                continue
            }

            // 8. Lists: Ordered (1. ) and Unordered (- , * , + )
            val uMatch = UNORDERED_LIST_REGEX.matchEntire(line)
            val oMatch = ORDERED_LIST_REGEX.matchEntire(line)
            if (uMatch != null || oMatch != null) {
                flushParagraph()
                val isOrdered = oMatch != null
                val listItems = mutableListOf<ListItem>()
                val startNum = if (isOrdered) oMatch!!.groupValues[2].toIntOrNull() ?: 1 else 1

                while (i < lines.size) {
                    val curLine = lines[i]
                    val curUMatch = UNORDERED_LIST_REGEX.matchEntire(curLine)
                    val curOMatch = ORDERED_LIST_REGEX.matchEntire(curLine)

                    if (curUMatch != null) {
                        val indent = (curUMatch.groupValues[1].length / 2).coerceAtMost(3)
                        listItems.add(ListItem(parseInlineRobust(curUMatch.groupValues[3]), indent))
                        i++
                    } else if (curOMatch != null) {
                        val indent = (curOMatch.groupValues[1].length / 2).coerceAtMost(3)
                        listItems.add(ListItem(parseInlineRobust(curOMatch.groupValues[3]), indent))
                        i++
                    } else if (curLine.trim().isEmpty()) {
                        if (i + 1 < lines.size && (UNORDERED_LIST_REGEX.matches(lines[i + 1]) || ORDERED_LIST_REGEX.matches(lines[i + 1]))) {
                            i++
                        } else {
                            break
                        }
                    } else if (curLine.trim().startsWith("$$") || curLine.trim().startsWith("\\[")) {
                        break
                    } else if (curLine.startsWith("  ") || curLine.startsWith("\t")) {
                        if (listItems.isNotEmpty()) {
                            val last = listItems.removeAt(listItems.lastIndex)
                            val combined = last.inlines + listOf(InlineNode.Text(" ")) + parseInlineRobust(curLine.trim())
                            listItems.add(last.copy(inlines = combined))
                        }
                        i++
                    } else {
                        break
                    }
                }
                blocks.add(AiBlock.ListBlock(listItems, isOrdered, startNum))
                continue
            }

            // 9. Regular Paragraph line
            if (trimmed.isEmpty()) {
                flushParagraph()
            } else {
                paragraphBuffer.add(line)
            }
            i++
        }

        flushParagraph()
        return blocks
    }

    private fun isTableSeparator(line: String): Boolean {
        if (!line.contains("-")) return false
        return TABLE_SEPARATOR_REGEX.matches(line.trim()) ||
               (line.contains("|") && line.replace(Regex("""[\s|\-:]"""), "").isEmpty())
    }

    private fun parseTableRow(line: String): List<String> {
        var trimmed = line.trim()
        if (trimmed.startsWith("|")) trimmed = trimmed.substring(1)
        if (trimmed.endsWith("|")) trimmed = trimmed.substring(0, trimmed.length - 1)
        return trimmed.split("|").map { it.trim() }
    }

    private val inlineCache = SimpleLruCache<String, List<InlineNode>>(1000)

    /**
     * Parses inline tokens: math ($...$), code (`...`), links ([text](url)), bold/italic, strikethrough,
     * and ensures interstitial text passes through MathFormatter for physics and chemistry Unicode formatting.
     */
    fun parseInlineRobust(text: String): List<InlineNode> {
        if (text.isEmpty()) return emptyList()
        val cached = inlineCache.get(text)
        if (cached != null) return cached
        val result = doParseInlineRobust(text)
        inlineCache.put(text, result)
        return result
    }

    private fun doParseInlineRobust(text: String): List<InlineNode> {
        if (text.isEmpty()) return emptyList()

        val nodes = mutableListOf<InlineNode>()
        var lastIdx = 0

        val matches = try {
            INLINE_REGEX.findAll(text)
        } catch (_: Exception) {
            emptySequence()
        }

        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1

            if (start > lastIdx) {
                val plainText = text.substring(lastIdx, start)
                val formatted = MathFormatter.formatScienceAndMathInText(plainText)
                if (formatted.isNotEmpty()) {
                    nodes.add(InlineNode.Text(formatted))
                }
            }

            val raw = match.value
            when {
                // LaTeX inline or display delimiter
                raw.startsWith("\\[") && raw.endsWith("\\]") -> {
                    nodes.add(InlineNode.Math(raw.substring(2, raw.length - 2).trim()))
                }
                raw.startsWith("\\(") && raw.endsWith("\\)") -> {
                    nodes.add(InlineNode.Math(raw.substring(2, raw.length - 2).trim()))
                }
                raw.startsWith("$$") && raw.endsWith("$$") && raw.length >= 4 -> {
                    nodes.add(InlineNode.Math(raw.substring(2, raw.length - 2).trim()))
                }
                raw.startsWith("$") && raw.endsWith("$") && raw.length >= 2 -> {
                    nodes.add(InlineNode.Math(raw.substring(1, raw.length - 1).trim()))
                }
                raw.startsWith("\\ce{") && raw.endsWith("}") && raw.length >= 5 -> {
                    nodes.add(InlineNode.Math(raw))
                }
                (raw.startsWith("\\frac{") || raw.startsWith("\\dfrac{") || raw.startsWith("\\tfrac{")) -> {
                    nodes.add(InlineNode.Math(raw))
                }
                raw.startsWith("\\sqrt") -> {
                    nodes.add(InlineNode.Math(raw))
                }
                // Markdown link or image
                raw.startsWith("![") -> {
                    val linkMatch = Regex("""!\[(.*?)\]\((.*?)\)""").matchEntire(raw)
                    if (linkMatch != null) {
                        nodes.add(InlineNode.Text("[Image: ${linkMatch.groupValues[1]}]", italic = true))
                    } else {
                        nodes.add(InlineNode.Text(raw))
                    }
                }
                raw.startsWith("[") && raw.contains("](") && raw.endsWith(")") -> {
                    val linkMatch = Regex("""\[(.*?)\]\((.*?)\)""").matchEntire(raw)
                    if (linkMatch != null) {
                        nodes.add(InlineNode.Link(text = linkMatch.groupValues[1], url = linkMatch.groupValues[2]))
                    } else {
                        nodes.add(InlineNode.Text(raw))
                    }
                }
                // Inline code
                raw.startsWith("`") && raw.endsWith("`") && raw.length >= 2 -> {
                    nodes.add(InlineNode.Code(raw.substring(1, raw.length - 1)))
                }
                // Bold & Italic: ***text***
                raw.startsWith("***") && raw.endsWith("***") && raw.length >= 6 -> {
                    nodes.add(InlineNode.Text(raw.substring(3, raw.length - 3), bold = true, italic = true))
                }
                // Bold: **text** or __text__
                (raw.startsWith("**") && raw.endsWith("**") && raw.length >= 4) -> {
                    nodes.add(InlineNode.Text(raw.substring(2, raw.length - 2), bold = true))
                }
                (raw.startsWith("__") && raw.endsWith("__") && raw.length >= 4) -> {
                    nodes.add(InlineNode.Text(raw.substring(2, raw.length - 2), bold = true))
                }
                // Strikethrough: ~~text~~
                raw.startsWith("~~") && raw.endsWith("~~") && raw.length >= 4 -> {
                    nodes.add(InlineNode.Text(raw.substring(2, raw.length - 2), strike = true))
                }
                // Italic: *text* or _text_
                (raw.startsWith("*") && raw.endsWith("*") && raw.length >= 2) -> {
                    nodes.add(InlineNode.Text(raw.substring(1, raw.length - 1), italic = true))
                }
                (raw.startsWith("_") && raw.endsWith("_") && raw.length >= 2) -> {
                    nodes.add(InlineNode.Text(raw.substring(1, raw.length - 1), italic = true))
                }
                else -> {
                    nodes.add(InlineNode.Text(MathFormatter.formatScienceAndMathInText(raw)))
                }
            }
            lastIdx = end
        }

        if (lastIdx < text.length) {
            val remaining = text.substring(lastIdx)
            val formatted = MathFormatter.formatScienceAndMathInText(remaining)
            if (formatted.isNotEmpty()) {
                nodes.add(InlineNode.Text(formatted))
            }
        }

        return nodes
    }
}
