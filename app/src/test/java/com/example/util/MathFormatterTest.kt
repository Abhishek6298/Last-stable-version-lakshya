package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MathFormatterTest {

    @Test
    fun toSubscript_convertsDigitsCorrectly() {
        val input = "0123456789"
        val expected = "₀₁₂₃₄₅₆₇₈₉"
        assertEquals(expected, MathFormatter.toSubscript(input))
    }

    @Test
    fun toSubscript_convertsSymbolsAndParenthesesCorrectly() {
        val input = "+-=()"
        val expected = "₊₋₌₍₎"
        assertEquals(expected, MathFormatter.toSubscript(input))
    }

    @Test
    fun toSubscript_convertsSupportedLowercaseLetters() {
        val input = "abceghijklmnoprstuvxq"
        val expected = "ₐᵦ꜀ₑᵧₕᵢⱼₖₗₘₙₒₚᵣₛₜᵤᵥₓᵩ"
        assertEquals(expected, MathFormatter.toSubscript(input))
    }

    @Test
    fun toSubscript_convertsSupportedUppercaseLetters() {
        val input = "ABCEGHIJKLMNOPRSTUVXQ"
        val expected = "ₐᵦ꜀ₑᵧₕᵢⱼₖₗₘₙₒₚᵣₛₜᵤᵥₓᵩ"
        assertEquals(expected, MathFormatter.toSubscript(input))
    }

    @Test
    fun toSubscript_preservesUnmappedCharacters() {
        val input = "dfwyz DFWYZ !@#$%"
        val expected = "dfwyz DFWYZ !@#$%"
        assertEquals(expected, MathFormatter.toSubscript(input))
    }

    @Test
    fun toSubscript_handlesEmptyString() {
        assertEquals("", MathFormatter.toSubscript(""))
    }

    @Test
    fun toSubscript_handlesRealisticExpressions() {
        assertEquals("ₑᵩ", MathFormatter.toSubscript("eq"))
        assertEquals("ₘₐₓ", MathFormatter.toSubscript("max"))
        assertEquals("ₙ₊₁", MathFormatter.toSubscript("n+1"))
        assertEquals("₍ᵢ₋₁₎", MathFormatter.toSubscript("(i-1)"))
    }

    @Test
    fun formatScienceAndMathInText_doesNotThrowOrLoopInfinitely() {
        val input = "Calculate ΔH for PCl5 <=> PCl3 + Cl2 at 298 K with Kp = 10^-3 and 6*100"
        val result = MathFormatter.formatScienceAndMathInText(input)
        assert(result.isNotEmpty())
    }

    @Test
    fun formatMathAndLatex_doesNotThrowOrLoopInfinitely() {
        val input = "\\frac{a + b}{c - d} + \\sqrt{x^2 + y^2} + \\ce{H2SO4 -> 2H+ + SO4^{2-}}"
        val result = MathFormatter.formatMathAndLatex(input)
        assert(result.isNotEmpty())
    }

    @Test
    fun parseBlocks_handlesMarkdownAndLatexWithoutFreeze() {
        val dollar = "$"
        val text = """
            # Physics Doubt
            Here is the formula:
            $$ E = mc^2 $$
            Also in chemistry:
            \ce{2H2 + O2 -> 2H2O}
            - Step 1: Calculate energy
            - Step 2: Use ${dollar}F = ma${dollar}
            ```python
            print("Done")
            ```
        """.trimIndent()
        val blocks = com.example.ui.components.parseBlocks(text)
        assert(blocks.isNotEmpty())
    }

    @Test
    fun parseBlocks_correctlyParsesMarkdownTablesWithVariousSeparators() {
        val markdown = """
            Here is the Lenz's law summary:
            | Case | Change in Flux | Induced Field | Induced Current |
            |:---|:---|:---|:---|
            | N-pole approaching | Increasing ($\rightarrow$) | Points away ($\leftarrow$) | CCW |
            | S-pole retreating | Decreasing ($\leftarrow$) | Points towards ($\rightarrow$) | CW |
            
            Note: Remember Lenz's law opposes flux changes.
        """.trimIndent()

        val blocks = com.example.ui.components.parseBlocks(markdown)
        val tableBlock = blocks.filterIsInstance<com.example.ui.components.AiBlock.Table>().firstOrNull()
        assert(tableBlock != null)
        assertEquals(4, tableBlock!!.headers.size)
        assertEquals(2, tableBlock.rows.size)
        assertEquals("Case", tableBlock.headers[0])
        assertEquals("N-pole approaching", tableBlock.rows[0][0])
        // Verify divider was not included as row
        assert(!tableBlock.rows.any { row -> row.any { it.contains("---") || it.contains(":-") } })
    }

    @Test
    fun formatScienceAndMath_convertsPhysicsElectromagnetismSymbols() {
        val input = "\\Phi_B = \\int \\vec{B} \\cdot d\\vec{A}"
        val result = MathFormatter.formatMathAndLatex(input)
        assert(result.contains("Φ_B"))
        assert(result.contains("∫"))
        assert(result.contains("·"))
    }

    @Test
    fun formatScienceAndMath_convertsCommonSubscriptsAndVectors() {
        val input = "\\mathbf{B}_{\\text{ind}} and \\mu_0 and \\epsilon_0"
        val result = MathFormatter.formatScienceAndMathInText(input)
        assert(result.contains("B_ind"))
        assert(result.contains("μ₀"))
        assert(result.contains("ε₀"))
    }

    @Test
    fun formatScienceAndMath_convertsBiologyAndChemistry() {
        val text = "Replication occurs in 5' to 3' direction with H2O and PCl5 <=> PCl3 + Cl2"
        val formatted = MathFormatter.formatScienceAndMathInText(text)
        assert(formatted.contains("5′ → 3′") || formatted.contains("5′"))
        assert(formatted.contains("H₂O"))
        assert(formatted.contains("⇌"))
    }

    @Test
    fun parseBlocks_handlesStreamingIncompleteCodeFenceWithoutCrash() {
        val streamingText = """
            Here is the streaming code:
            ```kotlin
            fun calculateEnergy(mass: Double, c: Double): Double {
                return mass * c * c
        """.trimIndent()

        val blocks = com.example.ui.components.parseBlocks(streamingText, isStreaming = true)
        val codeBlock = blocks.filterIsInstance<com.example.ui.components.AiBlock.Code>().firstOrNull()
        assert(codeBlock != null)
        assertEquals("kotlin", codeBlock!!.language)
        assert(codeBlock.code.contains("fun calculateEnergy"))
        assertEquals(false, codeBlock.isComplete)
    }

    @Test
    fun parseBlocks_handlesStreamingIncompleteDisplayMathWithoutCrash() {
        val d = "$"
        val streamingText = """
            The formula being derived is:
            ${d}${d} \int_0^\infty e^{-x^2} dx = \frac{\sqrt{\pi}}{2
        """.trimIndent()

        val blocks = com.example.ui.components.parseBlocks(streamingText, isStreaming = true)
        val mathBlock = blocks.filterIsInstance<com.example.ui.components.AiBlock.MathDisplay>().firstOrNull()
        assert(mathBlock != null)
        assert(mathBlock!!.latex.contains("int_0^"))
        assertEquals(false, mathBlock.isComplete)
    }

    @Test
    fun parseBlocks_handlesStreamingIncompleteTableWithoutCrash() {
        val streamingTable = """
            | Element | Symbol | Atomic Number |
            |:---|:---:|---:|
            | Hydrogen | H | 1 |
            | Helium | He |
        """.trimIndent()

        val blocks = com.example.ui.components.parseBlocks(streamingTable, isStreaming = true)
        val tableBlock = blocks.filterIsInstance<com.example.ui.components.AiBlock.Table>().firstOrNull()
        assert(tableBlock != null)
        assertEquals(3, tableBlock!!.headers.size)
        assertEquals(2, tableBlock.rows.size)
        assertEquals("Hydrogen", tableBlock.rows[0][0])
        assertEquals("Helium", tableBlock.rows[1][0])
    }

    @Test
    fun parseBlocks_parsesImagesAndLinksCorrectly() {
        val markdown = """
            ![Diagram](https://example.com/diagram.png)
            
            Check the official documentation at [Kotlin Docs](https://kotlinlang.org).
        """.trimIndent()

        val blocks = com.example.ui.components.parseBlocks(markdown)
        val imgBlock = blocks.filterIsInstance<com.example.ui.components.AiBlock.Image>().firstOrNull()
        assert(imgBlock != null)
        assertEquals("https://example.com/diagram.png", imgBlock!!.url)
        assertEquals("Diagram", imgBlock.alt)

        val pBlock = blocks.filterIsInstance<com.example.ui.components.AiBlock.Paragraph>().firstOrNull()
        assert(pBlock != null)
        val linkNode = pBlock!!.inlines.filterIsInstance<com.example.ui.components.InlineNode.Link>().firstOrNull()
        assert(linkNode != null)
        assertEquals("Kotlin Docs", linkNode!!.text)
        assertEquals("https://kotlinlang.org", linkNode.url)
    }

    @Test
    fun parseBlocks_handlesMixedScientificResponse() {
        val d = "$"
        val mixedContent = """
            # Thermodynamics & Reaction Kinetics
            
            When considering the Haber process:
            ${d}${d} \text{N}_2 + 3\text{H}_2 \rightleftharpoons 2\text{NH}_3 ${d}${d}
            
            - The standard enthalpy change is ${d}\Delta H^\circ = -92.4\text{ kJ/mol}${d}.
            - Equilibrium constant expression: ${d}K_p = \frac{p(\text{NH}_3)^2}{p(\text{N}_2) \cdot p(\text{H}_2)^3}${d}.
            
            ```python
            import numpy as np
            def van_t_hoff(delta_H, T1, T2, K1):
                R = 8.314 # J/(mol*K)
                return K1 * np.exp((-delta_H / R) * (1/T2 - 1/T1))
            ```
            
            > Remember Le Chatelier's principle opposes temperature increases.
        """.trimIndent()

        val blocks = com.example.ui.components.parseBlocks(mixedContent)
        assert(blocks.any { it is com.example.ui.components.AiBlock.Heading })
        assert(blocks.any { it is com.example.ui.components.AiBlock.MathDisplay })
        assert(blocks.any { it is com.example.ui.components.AiBlock.ListBlock })
        assert(blocks.any { it is com.example.ui.components.AiBlock.Code })
        assert(blocks.any { it is com.example.ui.components.AiBlock.BlockQuote })
    }

    @Test
    fun isComplexMathOrChemistry_handlesHybridRulesCorrectly() {
        // English sentences should NOT be classified as complex math (should use native Compose Text)
        assert(!MathFormatter.isComplexMathOrChemistry("Which of the following statements is correct regarding electric flux?"))
        assert(!MathFormatter.isComplexMathOrChemistry("A body of mass 2 kg is moving with velocity v = 10 m/s. Find kinetic energy."))
        assert(!MathFormatter.isComplexMathOrChemistry("Calculate the magnetic field at distance r = 5 cm from the wire."))
        assert(!MathFormatter.isComplexMathOrChemistry("Increases linearly with temperature"))
        assert(!MathFormatter.isComplexMathOrChemistry("Both (A) and (B) are true"))
        assert(!MathFormatter.isComplexMathOrChemistry("10 m/s"))
        assert(!MathFormatter.isComplexMathOrChemistry("None of the above"))

        // Pure standalone complex formulas SHOULD be classified as complex math (uses MathJax 3)
        assert(MathFormatter.isComplexMathOrChemistry("\\frac{\\sqrt{3}}{2} v_0"))
        assert(MathFormatter.isComplexMathOrChemistry("\\vec{A} \\times \\vec{B}"))
        assert(MathFormatter.isComplexMathOrChemistry("\\ce{2H2 + O2 -> 2H2O}"))
        assert(MathFormatter.isComplexMathOrChemistry("\\sqrt{2gR}"))
        assert(MathFormatter.isComplexMathOrChemistry("\\int_0^\\pi \\sin x dx"))
    }

    @Test
    fun hasEnglishNarrative_identifiesEnglishSentences() {
        assert(MathFormatter.hasEnglishNarrative("A particle moves in a circle of radius R with constant speed."))
        assert(MathFormatter.hasEnglishNarrative("Which of the following is the correct relation?"))
        assert(MathFormatter.hasEnglishNarrative("Find the acceleration of the block when force is applied."))
        assert(!MathFormatter.hasEnglishNarrative("\\frac{1}{2} m v^2"))
        assert(!MathFormatter.hasEnglishNarrative("\\sqrt{3gh}"))
    }
}
