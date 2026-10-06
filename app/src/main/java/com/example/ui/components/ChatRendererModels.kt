package com.example.ui.components

import androidx.compose.runtime.Immutable

/**
 * Typed AST Content Blocks for Modern ChatGPT-style Chat Rendering Pipeline.
 *
 * Pipeline:
 * AI Raw Response -> Streaming Buffer -> ChatMarkdownParser -> List<AiBlock> -> Specialized Compose Renderers
 */
@Immutable
enum class CalloutType {
    NOTE,
    TIP,
    IMPORTANT,
    WARNING,
    CAUTION,
    CONCEPT,
    FORMULA,
    KEY_TAKEAWAY,
    COMMON_TRAP,
    STEP
}

@Immutable
sealed class AiBlock {
    data class Paragraph(val inlines: List<InlineNode>) : AiBlock()
    data class Heading(val level: Int, val inlines: List<InlineNode>) : AiBlock()
    data class Code(val language: String, val code: String, val isComplete: Boolean = true) : AiBlock()
    data class MathDisplay(val latex: String, val isComplete: Boolean = true) : AiBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>, val isComplete: Boolean = true) : AiBlock()
    data class ListBlock(val items: List<ListItem>, val isOrdered: Boolean, val startNumber: Int = 1) : AiBlock()
    data class BlockQuote(val blocks: List<AiBlock>) : AiBlock()
    data class Callout(val type: CalloutType, val title: String?, val blocks: List<AiBlock>) : AiBlock()
    data class StepCard(val stepNumber: Int, val title: String, val blocks: List<AiBlock>) : AiBlock()
    data class Thinking(val thought: String, val isComplete: Boolean = true) : AiBlock()
    data class Image(val url: String, val alt: String, val caption: String? = null) : AiBlock()
    object Divider : AiBlock()
}

@Immutable
data class ListItem(val inlines: List<InlineNode>, val level: Int = 0)

@Immutable
sealed class InlineNode {
    data class Text(
        val text: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val strike: Boolean = false
    ) : InlineNode()

    data class Code(val code: String) : InlineNode()

    data class Math(val latex: String) : InlineNode()

    data class Link(val text: String, val url: String) : InlineNode()
}
