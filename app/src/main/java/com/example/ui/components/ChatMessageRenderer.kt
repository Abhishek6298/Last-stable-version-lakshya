package com.example.ui.components

import com.example.util.SimpleLruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.util.MathFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modern ChatGPT-style Chat Message Renderer for Jetpack Compose.
 *
 * Pipeline:
 * AI text / streaming chunk
 * -> Streaming Buffer (ChatMarkdownParser)
 * -> Typed AST Content Blocks (AiBlock)
 * -> Specialized Compose Renderers:
 *    1. Normal text & Inline Markdown (Native Text / AnnotatedString)
 *    2. Headings (H1 to H6)
 *    3. Display LaTeX ($$...$$ and \[...\])
 *    4. Inline LaTeX ($...$ and \(...\))
 *    5. Code Blocks (with language label, copy button, syntax highlight, horizontal scroll)
 *    6. Chemistry & Physics (subscripts, reaction arrows, thermodynamic deltas, electromagnetic symbols)
 *    7. Tables (responsive horizontal scrolling, dynamic column widths, zebra striping)
 *    8. Lists (ordered & unordered with indentation)
 *    9. Blockquotes & Callouts (Tips, Warnings, Formulas, Traps, Concepts)
 *    10. Thinking / Reasoning collapsible accordion (<think>...</think>)
 *    11. Numbered Step-by-Step cards
 *    12. Images (![alt](url))
 *    13. Streaming cursor (when isStreaming = true)
 */
@Composable
fun ChatMessageRenderer(
    text: String,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme(),
    isStreaming: Boolean = false,
    textColor: Color? = null,
    fontSize: TextUnit = 14.sp
) {
    if (text.isEmpty()) return

    val blocks = remember(text, isStreaming) {
        ChatMarkdownParser.parseBlocks(text, isStreaming)
    }

    val resolvedTextColor = textColor ?: if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEachIndexed { index, block ->
            val isLastBlock = index == blocks.lastIndex
            RenderBlock(
                block = block,
                isDark = isDark,
                textColor = resolvedTextColor,
                fontSize = fontSize,
                showCursor = isStreaming && isLastBlock
            )
        }

        if (blocks.isEmpty() && isStreaming) {
            StreamingCursor(fontSize = fontSize)
        }
    }
}

@Composable
private fun RenderBlock(
    block: AiBlock,
    isDark: Boolean,
    textColor: Color,
    fontSize: TextUnit,
    showCursor: Boolean = false
) {
    when (block) {
        is AiBlock.Paragraph -> {
            RenderParagraphBlock(
                inlines = block.inlines,
                isDark = isDark,
                textColor = textColor,
                fontSize = fontSize,
                showCursor = showCursor
            )
        }

        is AiBlock.Heading -> {
            RenderHeadingBlock(
                level = block.level,
                inlines = block.inlines,
                isDark = isDark,
                textColor = textColor,
                showCursor = showCursor
            )
        }

        is AiBlock.Code -> {
            RenderCodeBlock(
                code = block.code,
                language = block.language,
                isComplete = block.isComplete,
                isDark = isDark
            )
        }

        is AiBlock.MathDisplay -> {
            RenderMathBlock(
                latex = block.latex,
                isComplete = block.isComplete,
                isDark = isDark
            )
        }

        is AiBlock.Table -> {
            RenderTableBlock(
                headers = block.headers,
                rows = block.rows,
                isDark = isDark,
                textColor = textColor
            )
        }

        is AiBlock.ListBlock -> {
            RenderListBlock(
                items = block.items,
                isOrdered = block.isOrdered,
                startNumber = block.startNumber,
                isDark = isDark,
                textColor = textColor,
                fontSize = fontSize,
                showCursor = showCursor
            )
        }

        is AiBlock.BlockQuote -> {
            RenderBlockQuote(
                blocks = block.blocks,
                isDark = isDark,
                textColor = textColor,
                fontSize = fontSize
            )
        }

        is AiBlock.Callout -> {
            RenderCalloutBlock(
                callout = block,
                isDark = isDark,
                textColor = textColor,
                fontSize = fontSize
            )
        }

        is AiBlock.StepCard -> {
            RenderStepCard(
                step = block,
                isDark = isDark,
                textColor = textColor,
                fontSize = fontSize
            )
        }

        is AiBlock.Thinking -> {
            RenderThinkingBlock(
                thought = block.thought,
                isComplete = block.isComplete,
                isDark = isDark,
                fontSize = fontSize
            )
        }

        is AiBlock.Image -> {
            RenderImageBlock(
                url = block.url,
                alt = block.alt,
                caption = block.caption,
                isDark = isDark
            )
        }

        is AiBlock.Divider -> {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                color = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000),
                thickness = 1.dp
            )
        }
    }
}

// ============================================================================
// 1. PARAGRAPH RENDERER (Inline Markdown, Math, Chemistry, Code Chips, Links)
// ============================================================================
@Composable
private fun RenderParagraphBlock(
    inlines: List<InlineNode>,
    isDark: Boolean,
    textColor: Color,
    fontSize: TextUnit,
    showCursor: Boolean = false
) {
    val annotatedString = remember(inlines, isDark, textColor) {
        buildInlineString(inlines, isDark, textColor)
    }
    val uriHandler = LocalUriHandler.current

    val hasLinks = remember(annotatedString) {
        annotatedString.getStringAnnotations(tag = "URL", start = 0, end = annotatedString.length).isNotEmpty()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        if (hasLinks) {
            ClickableText(
                text = annotatedString,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = fontSize,
                    lineHeight = (fontSize.value * 1.55f).sp,
                    color = textColor
                ),
                modifier = Modifier.weight(1f, fill = false),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            try {
                                uriHandler.openUri(annotation.item)
                            } catch (_: Exception) {}
                        }
                }
            )
        } else {
            Text(
                text = annotatedString,
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = fontSize,
                    lineHeight = (fontSize.value * 1.55f).sp,
                    color = textColor
                ),
                modifier = Modifier.weight(1f, fill = false)
            )
        }

        if (showCursor) {
            StreamingCursor(fontSize = fontSize)
        }
    }
}

// ============================================================================
// 2. HEADING RENDERER (H1 to H6 with distinct styling)
// ============================================================================
@Composable
private fun RenderHeadingBlock(
    level: Int,
    inlines: List<InlineNode>,
    isDark: Boolean,
    textColor: Color,
    showCursor: Boolean = false
) {
    val (headingSize, headingWeight, topPad, bottomPad, headingColor) = when (level) {
        1 -> HeadingStyle(20.sp, FontWeight.ExtraBold, 10.dp, 6.dp, if (isDark) Color(0xFF67E8F9) else Color(0xFF0284C7))
        2 -> HeadingStyle(17.sp, FontWeight.Bold, 8.dp, 4.dp, if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5))
        3 -> HeadingStyle(15.sp, FontWeight.SemiBold, 6.dp, 3.dp, textColor)
        else -> HeadingStyle(14.sp, FontWeight.SemiBold, 4.dp, 2.dp, textColor)
    }

    val annotatedString = remember(inlines, isDark, headingColor) {
        buildInlineString(inlines, isDark, headingColor)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPad, bottom = bottomPad)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = annotatedString,
                fontSize = headingSize,
                fontWeight = headingWeight,
                lineHeight = (headingSize.value * 1.35f).sp,
                color = headingColor,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (showCursor) {
                StreamingCursor(fontSize = headingSize)
            }
        }
        if (level <= 2) {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                thickness = 1.dp,
                color = headingColor.copy(alpha = 0.25f)
            )
        }
    }
}

private data class HeadingStyle(
    val size: TextUnit,
    val weight: FontWeight,
    val topPad: Dp,
    val bottomPad: Dp,
    val color: Color
)

// ============================================================================
// 3. CODE BLOCK RENDERER (ChatGPT-style code card with copy & syntax highlight)
// ============================================================================
@Composable
private fun RenderCodeBlock(
    code: String,
    language: String,
    isComplete: Boolean,
    isDark: Boolean
) {
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    val displayLang = remember(language) {
        if (language.isBlank()) "CODE" else language.uppercase()
    }

    val highlightedCode = remember(code, language) {
        CodeHighlighter.highlight(code, language)
    }

    val cardBg = Color(0xFF0F172A) // Sleek dark slate
    val headerBg = Color(0xFF1E293B) // Dark header bar
    val borderColor = Color(0x33FFFFFF)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column {
            // Header Bar with Language label and Copy button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = displayLang,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isCopied) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Transparent,
                    border = BorderStroke(1.dp, if (isCopied) Color(0xFF10B981) else Color(0x22FFFFFF)),
                    modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(code))
                        isCopied = true
                        coroutineScope.launch {
                            delay(2000)
                            isCopied = false
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = if (isCopied) "Copied" else "Copy Code",
                            tint = if (isCopied) Color(0xFF10B981) else Color(0xFF94A3B8),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isCopied) "Copied!" else "Copy",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isCopied) Color(0xFF10B981) else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Code Content with Horizontal Scroll and Monospace Typography
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = highlightedCode,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFFF8FAFC)
                )
            }
        }
    }
}

// ============================================================================
// 4. DISPLAY LATEX RENDERER (ChatGPT-style mathematical display card)
// ============================================================================
@Composable
private fun RenderMathBlock(
    latex: String,
    isComplete: Boolean,
    isDark: Boolean
) {
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    // Clean, elegant textbook paper container matching the uploaded screenshot
    val cardBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val cardBorder = if (isDark) Color(0x33818CF8) else Color(0xFFE2E8F0)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = if (isDark) 0.dp else 0.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable {
                clipboardManager.setText(AnnotatedString(latex))
                isCopied = true
                coroutineScope.launch {
                    delay(2000)
                    isCopied = false
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            MathJax3MathBlock(
                latex = latex,
                isDark = isDark,
                modifier = Modifier
            )

            if (isCopied) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF10B981),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = "Copied!",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// 5. TABLE RENDERER (Responsive horizontal scroll, dynamic column width, zebra)
// ============================================================================
@Composable
private fun RenderTableBlock(
    headers: List<String>,
    rows: List<List<String>>,
    isDark: Boolean,
    textColor: Color
) {
    if (headers.isEmpty() && rows.isEmpty()) return

    val colCount = remember(headers, rows) {
        maxOf(headers.size, rows.maxOfOrNull { it.size } ?: 0)
    }

    val colWidths: List<Dp> = remember(headers, rows, colCount) {
        (0 until colCount).map { colIdx ->
            val headerLen = headers.getOrNull(colIdx)?.length ?: 0
            val maxRowLen = rows.maxOfOrNull { it.getOrNull(colIdx)?.length ?: 0 } ?: 0
            val maxChars = maxOf(headerLen, maxRowLen)
            (maxChars * 10).coerceIn(80, 240).dp
        }
    }

    val headerBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
    val tableBorder = if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, tableBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Column {
                // Header Row
                if (headers.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .background(headerBg)
                            .padding(vertical = 8.dp)
                    ) {
                        for (c in 0 until colCount) {
                            val headerText = headers.getOrNull(c) ?: ""
                            val inlineNodes = remember(headerText) {
                                ChatMarkdownParser.parseInlineRobust(headerText)
                            }
                            val annotated = remember(inlineNodes, isDark, textColor) {
                                buildInlineString(inlineNodes, isDark, textColor)
                            }
                            Box(
                                modifier = Modifier
                                    .width(colWidths[c])
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = annotated,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = tableBorder,
                        thickness = 1.dp
                    )
                }

                // Data Rows
                rows.forEachIndexed { rowIndex, row ->
                    val rowBg = if (rowIndex % 2 == 0) {
                        Color.Transparent
                    } else {
                        if (isDark) Color(0x0AFFFFFF) else Color(0x05000000)
                    }

                    Row(
                        modifier = Modifier
                            .background(rowBg)
                            .padding(vertical = 7.dp)
                    ) {
                        for (c in 0 until colCount) {
                            val cellText = row.getOrNull(c) ?: ""
                            val inlineNodes = remember(cellText) {
                                ChatMarkdownParser.parseInlineRobust(cellText)
                            }
                            val annotated = remember(inlineNodes, isDark, textColor) {
                                buildInlineString(inlineNodes, isDark, textColor)
                            }
                            Box(
                                modifier = Modifier
                                    .width(colWidths[c])
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = annotated,
                                    fontSize = 12.sp,
                                    color = textColor
                                )
                            }
                        }
                    }

                    if (rowIndex < rows.lastIndex) {
                        HorizontalDivider(
                            color = tableBorder.copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 6. LIST RENDERER (Ordered & Unordered with indentation)
// ============================================================================
@Composable
private fun RenderListBlock(
    items: List<ListItem>,
    isOrdered: Boolean,
    startNumber: Int,
    isDark: Boolean,
    textColor: Color,
    fontSize: TextUnit,
    showCursor: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, item ->
            val isLast = index == items.lastIndex
            val indentDp = (item.level * 16).dp
            val annotated = remember(item.inlines, isDark, textColor) {
                buildInlineString(item.inlines, isDark, textColor)
            }
            val uriHandler = LocalUriHandler.current

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = indentDp),
                verticalAlignment = Alignment.Top
            ) {
                if (isOrdered) {
                    val num = startNumber + index
                    Text(
                        text = "$num.",
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                        modifier = Modifier.widthIn(min = 22.dp)
                    )
                } else {
                    Text(
                        text = "•",
                        fontSize = (fontSize.value * 1.2f).sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF67E8F9) else Color(0xFF0284C7),
                        modifier = Modifier.width(16.dp)
                    )
                }

                val hasLinks = remember(annotated) {
                    annotated.getStringAnnotations(tag = "URL", start = 0, end = annotated.length).isNotEmpty()
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (hasLinks) {
                        ClickableText(
                            text = annotated,
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = fontSize,
                                lineHeight = (fontSize.value * 1.5f).sp,
                                color = textColor
                            ),
                            modifier = Modifier.weight(1f, fill = false),
                            onClick = { offset ->
                                annotated.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                    .firstOrNull()?.let { annotation ->
                                        try {
                                            uriHandler.openUri(annotation.item)
                                        } catch (_: Exception) {}
                                    }
                            }
                        )
                    } else {
                        Text(
                            text = annotated,
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = fontSize,
                                lineHeight = (fontSize.value * 1.5f).sp,
                                color = textColor
                            ),
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    if (isLast && showCursor) {
                        StreamingCursor(fontSize = fontSize)
                    }
                }
            }
        }
    }
}

// ============================================================================
// 7. BLOCKQUOTE RENDERER (Accent bar + translucent card)
// ============================================================================
@Composable
private fun RenderBlockQuote(
    blocks: List<AiBlock>,
    isDark: Boolean,
    textColor: Color,
    fontSize: TextUnit
) {
    val barColor = if (isDark) Color(0xFF818CF8) else Color(0xFF6366F1)
    val quoteBg = if (isDark) Color(0xFF6366F1).copy(alpha = 0.12f) else Color(0xFF6366F1).copy(alpha = 0.06f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .fillMaxHeight()
                .background(barColor, RoundedCornerShape(2.dp))
        )
        Surface(
            shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
            color = quoteBg,
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                blocks.forEach { block ->
                    RenderBlock(
                        block = block,
                        isDark = isDark,
                        textColor = textColor.copy(alpha = 0.9f),
                        fontSize = fontSize
                    )
                }
            }
        }
    }
}

// ============================================================================
// 7B. CALLOUT / ADMONITION RENDERER (Tips, Warnings, Traps, Formulas, Concepts)
// ============================================================================
@Composable
private fun RenderCalloutBlock(
    callout: AiBlock.Callout,
    isDark: Boolean,
    textColor: Color,
    fontSize: TextUnit
) {
    val (accentColor, bgColor, icon, defaultTitle) = when (callout.type) {
        CalloutType.TIP -> Quad(
            Color(0xFF10B981),
            if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5),
            Icons.Default.Lightbulb,
            "PRO TIP"
        )
        CalloutType.IMPORTANT -> Quad(
            Color(0xFFF59E0B),
            if (isDark) Color(0xFF78350F).copy(alpha = 0.35f) else Color(0xFFFFFBEB),
            Icons.Default.Star,
            "IMPORTANT"
        )
        CalloutType.WARNING -> Quad(
            Color(0xFFEF4444),
            if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.35f) else Color(0xFFFEF2F2),
            Icons.Default.Warning,
            "WARNING"
        )
        CalloutType.CAUTION -> Quad(
            Color(0xFFF43F5E),
            if (isDark) Color(0xFF881337).copy(alpha = 0.35f) else Color(0xFFFFF1F2),
            Icons.Default.Warning,
            "CAUTION"
        )
        CalloutType.CONCEPT -> Quad(
            Color(0xFF6366F1),
            if (isDark) Color(0xFF312E81).copy(alpha = 0.35f) else Color(0xFFEEF2FF),
            Icons.Default.Psychology,
            "CORE CONCEPT"
        )
        CalloutType.FORMULA -> Quad(
            Color(0xFF0284C7),
            if (isDark) Color(0xFF0C4A6E).copy(alpha = 0.35f) else Color(0xFFF0F9FF),
            Icons.Default.Functions,
            "KEY EQUATION"
        )
        CalloutType.KEY_TAKEAWAY -> Quad(
            Color(0xFFD97706),
            if (isDark) Color(0xFF78350F).copy(alpha = 0.35f) else Color(0xFFFFFBEB),
            Icons.Default.AutoAwesome,
            "KEY TAKEAWAY"
        )
        CalloutType.COMMON_TRAP -> Quad(
            Color(0xFFE11D48),
            if (isDark) Color(0xFF4C0519).copy(alpha = 0.4f) else Color(0xFFFFF1F2),
            Icons.Default.Warning,
            "EXAM TRAP / COMMON MISTAKE"
        )
        CalloutType.STEP -> Quad(
            Color(0xFF8B5CF6),
            if (isDark) Color(0xFF4C1D95).copy(alpha = 0.35f) else Color(0xFFF5F3FF),
            Icons.Default.Info,
            "STEP EXPLANATION"
        )
        CalloutType.NOTE -> Quad(
            Color(0xFF3B82F6),
            if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFEFF6FF),
            Icons.Default.Info,
            "NOTE"
        )
    }

    val displayTitle = callout.title?.ifBlank { defaultTitle } ?: defaultTitle

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = displayTitle,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = accentColor,
                    letterSpacing = 0.5.sp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                callout.blocks.forEach { b ->
                    RenderBlock(
                        block = b,
                        isDark = isDark,
                        textColor = textColor,
                        fontSize = fontSize
                    )
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

// ============================================================================
// 7C. STEP-BY-STEP CARD RENDERER (Numbered pills for physics/math solutions)
// ============================================================================
@Composable
private fun RenderStepCard(
    step: AiBlock.StepCard,
    isDark: Boolean,
    textColor: Color,
    fontSize: TextUnit
) {
    val stepAccent = Color(0xFF6366F1)
    val cardBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF8FAFC)
    val borderColor = if (isDark) Color(0xFF475569).copy(alpha = 0.4f) else Color(0xFFE2E8F0)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = stepAccent.copy(alpha = 0.2f),
                    border = BorderStroke(0.8.dp, stepAccent.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "STEP ${step.stepNumber}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = step.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                step.blocks.forEach { b ->
                    RenderBlock(
                        block = b,
                        isDark = isDark,
                        textColor = textColor,
                        fontSize = fontSize
                    )
                }
            }
        }
    }
}

// ============================================================================
// 7D. THINKING / REASONING ACCORDION (<think>...</think> collapsible component)
// ============================================================================
@Composable
private fun RenderThinkingBlock(
    thought: String,
    isComplete: Boolean,
    isDark: Boolean,
    fontSize: TextUnit
) {
    var isExpanded by remember(isComplete) { mutableStateOf(!isComplete) }
    val transition = rememberInfiniteTransition(label = "thought_pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    val thinkingBg = if (isDark) Color(0xFF1E1B4B).copy(alpha = 0.3f) else Color(0xFFF5F3FF)
    val thinkingBorder = if (isDark) Color(0xFF6366F1).copy(alpha = 0.3f) else Color(0xFFDDD6FE)
    val accentColor = if (isDark) Color(0xFFA5B4FC) else Color(0xFF6366F1)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = thinkingBg,
        border = BorderStroke(1.dp, thinkingBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "🧠",
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (!isComplete) "Thinking..." else "Reasoning Process",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isComplete) accentColor.copy(alpha = pulseAlpha) else accentColor
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Hide" else "Show",
                        fontSize = 10.5.sp,
                        color = accentColor.copy(alpha = 0.8f)
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                ) {
                    Text(
                        text = com.example.util.MathFormatter.formatScienceAndMathInText(thought),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontStyle = FontStyle.Italic,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

// ============================================================================
// 8. IMAGE RENDERER (Coil AsyncImage with graceful fallback)
// ============================================================================
@Composable
private fun RenderImageBlock(
    url: String,
    alt: String,
    caption: String?,
    isDark: Boolean
) {
    var showFullscreenDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isDark) Color(0x33818CF8) else Color(0xFFE2E8F0)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF6366F1).copy(alpha = 0.15f),
                    border = BorderStroke(0.8.dp, Color(0xFF6366F1).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "🎨 AI VISUAL DIAGRAM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF818CF8)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                    modifier = Modifier.clickable { showFullscreenDialog = true }
                ) {
                    Text(
                        text = "🔍 Zoom",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                    )
                }
            }

            SubcomposeAsyncImage(
                model = url,
                contentDescription = alt.ifBlank { "AI Generated Image" },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showFullscreenDialog = true },
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 2.2.dp,
                                color = Color(0xFF6366F1)
                            )
                            Text(
                                text = "🎨 Generating visual illustration...",
                                fontSize = 11.sp,
                                color = Color(0xFF818CF8)
                            )
                        }
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(if (isDark) Color(0x22FFFFFF) else Color(0x11000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = alt.ifBlank { "Visual diagram preview unavailable" },
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            )

            val displayCaption = caption ?: if (alt.isNotBlank()) alt else null
            if (displayCaption != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = displayCaption,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showFullscreenDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFullscreenDialog = false }
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF0F172A) else Color.White,
                border = BorderStroke(1.dp, if (isDark) Color(0x44818CF8) else Color(0xFFCBD5E1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = alt.ifBlank { "Visual Diagram" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { showFullscreenDialog = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = if (isDark) Color.White else Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SubcomposeAsyncImage(
                        model = url,
                        contentDescription = alt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }
    }
}

// ============================================================================
// 9. STREAMING CURSOR (ChatGPT-style pulsing cursor at active stream boundary)
// ============================================================================
@Composable
private fun StreamingCursor(fontSize: TextUnit) {
    val transition = rememberInfiniteTransition(label = "streaming_cursor")
    val alpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    Text(
        text = "▊",
        color = Color(0xFF6366F1).copy(alpha = alpha),
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 2.dp)
    )
}

private val inlineStringCache = SimpleLruCache<String, AnnotatedString>(500)

// ============================================================================
// INLINE STRING BUILDER (Inline Math, Code chips, Bold, Italic, Strike, Links)
// ============================================================================
fun buildInlineString(
    inlines: List<InlineNode>,
    isDark: Boolean,
    textColor: Color
): AnnotatedString {
    if (inlines.isEmpty()) return AnnotatedString("")
    val key = "${textColor.value}_${isDark}_${inlines.hashCode()}"
    val cached = inlineStringCache.get(key)
    if (cached != null) return cached
    val result = doBuildInlineString(inlines, isDark, textColor)
    inlineStringCache.put(key, result)
    return result
}

private fun doBuildInlineString(
    inlines: List<InlineNode>,
    isDark: Boolean,
    textColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        for (node in inlines) {
            when (node) {
                is InlineNode.Text -> {
                    var style = SpanStyle(color = textColor)
                    if (node.bold) style = style.copy(fontWeight = FontWeight.Bold)
                    if (node.italic) style = style.copy(fontStyle = FontStyle.Italic)
                    if (node.strike) style = style.copy(textDecoration = TextDecoration.LineThrough)
                    pushStyle(style)
                    append(node.text)
                    pop()
                }

                is InlineNode.Code -> {
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.5.sp,
                            color = if (isDark) Color(0xFFF472B6) else Color(0xFFDB2777),
                            background = if (isDark) Color(0x33F472B6) else Color(0x1ADB2777)
                        )
                    )
                    append(" ${node.code} ")
                    pop()
                }

                is InlineNode.Math -> {
                    val formattedMath = try {
                        MathFormatter.formatFormula(node.latex)
                    } catch (_: Exception) {
                        node.latex
                    }
                    val mathColor = if (isDark) Color(0xFFF1F5F9) else Color(0xFF111827)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = mathColor,
                            letterSpacing = 0.3.sp
                        )
                    )
                    append(formattedMath)
                    pop()
                }

                is InlineNode.Link -> {
                    pushStringAnnotation(tag = "URL", annotation = node.url)
                    pushStyle(
                        SpanStyle(
                            color = Color(0xFF38BDF8),
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    append(node.text)
                    pop()
                    pop()
                }
            }
        }
    }
}
