package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.util.MathFormatter

/**
 * Dedicated Zero-Lag Math & Science Expression Renderer.
 * Completely eliminates Chromium WebViews, eliminating all UI lag, frame drops, and battery drain.
 * Supports LaTeX math, Physics vectors, and Chemistry formulas.
 */
@Composable
fun MathJax3View(
    text: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
    textColor: Color? = null
) {
    if (text.isBlank()) return

    val clean = remember(text) { text.trim() }
    val isEnglish = remember(clean) { MathFormatter.hasEnglishNarrative(clean) }
    val isComplex = remember(clean) { MathFormatter.isComplexMathOrChemistry(clean) }

    // Standalone complex formula without English narrative
    if (!isEnglish && isComplex && !clean.contains("\n\n") && clean.length < 250) {
        ZeroLagOptionText(
            text = clean,
            isDark = isDark,
            modifier = modifier,
            fontSize = fontSize,
            textColor = textColor
        )
    } else {
        ChatMessageRenderer(
            text = clean,
            modifier = modifier,
            isDark = isDark,
            fontSize = fontSize,
            textColor = textColor
        )
    }
}
