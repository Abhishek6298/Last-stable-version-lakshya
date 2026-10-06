package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * High-performance syntax highlighter for Jetpack Compose CodeBlocks.
 * Formats code into syntax-colored AnnotatedString without heavy external parsers.
 */
object CodeHighlighter {

    private val KEYWORDS = setOf(
        "abstract", "actual", "annotation", "as", "async", "await", "break", "byte", "case", "catch",
        "class", "companion", "const", "constructor", "continue", "coroutine", "data", "def", "default",
        "defer", "delegate", "delete", "do", "dynamic", "elif", "else", "enum", "expect", "export",
        "extends", "false", "final", "finally", "fn", "for", "from", "fun", "function", "get", "global",
        "goto", "if", "implements", "import", "in", "!in", "infix", "init", "inline", "inner", "instanceof",
        "interface", "internal", "is", "!is", "it", "lambda", "lateinit", "let", "match", "mut", "native",
        "new", "nil", "none", "not", "null", "object", "open", "operator", "out", "override", "package",
        "pass", "private", "protected", "public", "reified", "return", "sealed", "select", "self", "set",
        "sizeof", "static", "struct", "super", "suspend", "switch", "tailrec", "then", "this", "throw",
        "throws", "trait", "true", "try", "type", "typealias", "typeof", "val", "value", "var", "vararg",
        "void", "when", "where", "while", "with", "yield",
        // SQL keywords
        "SELECT", "FROM", "WHERE", "INSERT", "INTO", "UPDATE", "DELETE", "JOIN", "LEFT", "RIGHT",
        "INNER", "OUTER", "GROUP", "BY", "ORDER", "HAVING", "LIMIT", "OFFSET", "CREATE", "TABLE",
        "ALTER", "DROP", "AND", "OR", "NOT", "NULL", "PRIMARY", "KEY", "FOREIGN", "REFERENCES"
    )

    private val KEYWORD_COLOR = Color(0xFFC084FC) // Soft Purple
    private val STRING_COLOR = Color(0xFF86EFAC)  // Emerald Green
    private val NUMBER_COLOR = Color(0xFFFDBA74)  // Warm Amber
    private val COMMENT_COLOR = Color(0xFF94A3B8) // Muted Slate
    private val TYPE_COLOR = Color(0xFF67E8F9)    // Cyan
    private val FUNCTION_COLOR = Color(0xFF93C5FD)// Sky Blue
    private val DEFAULT_TEXT_COLOR = Color(0xFFF8FAFC) // Crisp White

    private val TOKEN_REGEX = Regex(
        """(//.*?$|#.*?$|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\b\d+(?:\.\d+)?(?:[eE][+-]?\d+)?\b|\b[A-Z][a-zA-Z0-9_]*\b|\b[a-zA-Z_][a-zA-Z0-9_]*(?=\s*\()|\b[a-zA-Z_][a-zA-Z0-9_]*\b)""",
        RegexOption.MULTILINE
    )

    fun highlight(code: String, language: String): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")

        return buildAnnotatedString {
            var lastIdx = 0
            val matches = TOKEN_REGEX.findAll(code)

            for (match in matches) {
                val start = match.range.first
                val end = match.range.last + 1

                if (start > lastIdx) {
                    append(code.substring(lastIdx, start))
                }

                val token = match.value
                when {
                    token.startsWith("//") || token.startsWith("#") || token.startsWith("/*") -> {
                        pushStyle(SpanStyle(color = COMMENT_COLOR, fontStyle = FontStyle.Italic))
                        append(token)
                        pop()
                    }
                    token.startsWith("\"") || token.startsWith("'") -> {
                        pushStyle(SpanStyle(color = STRING_COLOR))
                        append(token)
                        pop()
                    }
                    token.firstOrNull()?.isDigit() == true -> {
                        pushStyle(SpanStyle(color = NUMBER_COLOR))
                        append(token)
                        pop()
                    }
                    KEYWORDS.contains(token) -> {
                        pushStyle(SpanStyle(color = KEYWORD_COLOR, fontWeight = FontWeight.Bold))
                        append(token)
                        pop()
                    }
                    token.firstOrNull()?.isUpperCase() == true -> {
                        pushStyle(SpanStyle(color = TYPE_COLOR))
                        append(token)
                        pop()
                    }
                    // Function calls
                    end < code.length && code[end] == '(' -> {
                        pushStyle(SpanStyle(color = FUNCTION_COLOR))
                        append(token)
                        pop()
                    }
                    else -> {
                        pushStyle(SpanStyle(color = DEFAULT_TEXT_COLOR))
                        append(token)
                        pop()
                    }
                }

                lastIdx = end
            }

            if (lastIdx < code.length) {
                append(code.substring(lastIdx))
            }
        }
    }
}
