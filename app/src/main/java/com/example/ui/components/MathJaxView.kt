package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.util.MathFormatter

/**
 * Backward compatibility facade for MathJaxView / AiMessageRenderer.
 * Delegates directly to the Modern ChatGPT-style ChatMessageRenderer pipeline.
 */

fun parseBlocks(text: String, isStreaming: Boolean = false): List<AiBlock> {
    return ChatMarkdownParser.parseBlocks(text, isStreaming)
}

fun parseInlineRobust(text: String): List<InlineNode> {
    return ChatMarkdownParser.parseInlineRobust(text)
}

@Composable
fun AiMessageRenderer(
    text: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    textColor: Color? = null,
    fontSize: TextUnit = 13.5.sp,
    isStreaming: Boolean = false
) {
    ChatMessageRenderer(
        text = text,
        modifier = modifier,
        isDark = isDark,
        isStreaming = isStreaming,
        textColor = textColor,
        fontSize = fontSize
    )
}

@Composable
fun MathJaxView(
    text: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 13.5.sp,
    textColor: Color? = null,
    isStreaming: Boolean = false
) {
    val clean = text.trim()
    if (clean.isBlank()) return

    val resolvedTextColor = textColor ?: if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
    val isEnglish = remember(clean) { MathFormatter.hasEnglishNarrative(clean) }
    val isComplex = remember(clean) { MathFormatter.isComplexMathOrChemistry(clean) }

    // Hybrid dispatch: standalone complex formula (e.g. \sqrt, \frac, \ce, \vec) without English narrative uses MathJax 3
    if (!isEnglish && isComplex && !clean.contains("\n\n") && clean.length < 250 && !isStreaming) {
        MathJax3OptionText(
            text = clean,
            isDark = isDark,
            modifier = modifier,
            fontSize = fontSize,
            textColor = resolvedTextColor
        )
    } else {
        ChatMessageRenderer(
            text = clean,
            modifier = modifier,
            isDark = isDark,
            isStreaming = isStreaming,
            textColor = resolvedTextColor,
            fontSize = fontSize
        )
    }
}

@Composable
fun NativeMarkdownText(
    text: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 13.5.sp,
    textColor: Color? = null,
    isStreaming: Boolean = false
) {
    ChatMessageRenderer(
        text = text,
        modifier = modifier,
        isDark = isDark,
        isStreaming = isStreaming,
        textColor = textColor,
        fontSize = fontSize
    )
}

fun needsMathJax(text: String): Boolean {
    return MathFormatter.hasLatexMath(text)
}

fun parseMarkdown(
    text: String,
    isDark: Boolean,
    baseFontSize: TextUnit = 13.5.sp,
    customTextColor: Color? = null
): AnnotatedString {
    return buildInlineString(
        parseInlineRobust(text),
        isDark,
        customTextColor ?: if (isDark) Color.White else Color.Black
    )
}
