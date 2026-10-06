package com.example.util

object MathFormatter {

    // Pre-compiled list of LaTeX replacements sorted by command length descending
    // so longer commands like \rightarrow are matched before \right or \to
    private val LATEX_REPLACEMENTS = listOf(
        // Arrows and Reactions (Chemistry & Physics)
        "\\Longleftrightarrow" to " ⟺ ",
        "\\longleftrightarrow" to " ↔ ",
        "\\Longrightarrow" to " ⟹ ",
        "\\longrightarrow" to " ⟶ ",
        "\\Longleftarrow" to " ⟸ ",
        "\\longleftarrow" to " ⟵ ",
        "\\rightleftharpoons" to " ⇌ ",
        "\\leftrightharpoons" to " ⇌ ",
        "\\rightleftarrows" to " ⇄ ",
        "\\leftrightarrows" to " ⇆ ",
        "\\Leftrightarrow" to " ⇔ ",
        "\\leftrightarrow" to " ↔ ",
        "\\Rightarrow" to " ⇒ ",
        "\\rightarrow" to " → ",
        "\\leftarrow" to " ← ",
        "\\uparrow" to " ↑ ",
        "\\downarrow" to " ↓ ",
        "\\hookleftarrow" to " ↩ ",
        "\\hookrightarrow" to " ↪ ",
        "\\mapsto" to " ↦ ",
        "\\longmapsto" to " ⟼ ",
        "\\leadsto" to " ⇝ ",
        "\\iff" to " ⟺ ",
        "\\implies" to " ⟹ ",
        "\\impliedby" to " ⟸ ",
        "\\nearrow" to " ↗ ",
        "\\searrow" to " ↘ ",
        "\\swarrow" to " ↙ ",
        "\\nwarrow" to " ↖ ",
        "\\updownarrow" to " ↕ ",
        "\\Updownarrow" to " ⇕ ",
        "\\to" to " → ",

        // Physics Electromagnetism & Fields
        "\\Phi_{B}" to "Φ_B",
        "\\Phi_{E}" to "Φ_E",
        "\\Phi_B" to "Φ_B",
        "\\Phi_E" to "Φ_E",
        "\\mu_{0}" to "μ₀",
        "\\mu_0" to "μ₀",
        "\\epsilon_{0}" to "ε₀",
        "\\epsilon_0" to "ε₀",
        "\\varepsilon_{0}" to "ε₀",
        "\\varepsilon_0" to "ε₀",
        "\\mathcal{E}" to "ℰ",
        "\\emf" to "ℰ",

        // Common Physics Vectors & Unit Vectors
        "\\hat{i}" to "î",
        "\\hat{j}" to "ĵ",
        "\\hat{k}" to "k̂",
        "\\hat{n}" to "n̂",
        "\\hat{r}" to "r̂",
        "\\hat{u}" to "û",
        "\\hat{v}" to "v̂",
        "\\hat{x}" to "x̂",
        "\\hat{y}" to "ŷ",
        "\\hat{z}" to "ẑ",
        "\\hat{i}" to "î",
        "\\hat{j}" to "ĵ",
        "\\hat{k}" to "k̂",
        "\\hat{e}" to "ê",
        "\\hat{\\theta}" to "θ̂",
        "\\hat{\\phi}" to "ϕ̂",
        "\\vec{B}" to "B⃗",
        "\\vec{E}" to "E⃗",
        "\\vec{F}" to "F⃗",
        "\\vec{v}" to "v⃗",
        "\\vec{a}" to "a⃗",
        "\\vec{p}" to "p⃗",
        "\\vec{r}" to "r⃗",
        "\\vec{L}" to "L⃗",
        "\\vec{A}" to "A⃗",
        "\\vec{I}" to "I⃗",
        "\\vec{J}" to "J⃗",
        "\\vec{M}" to "M⃗",
        "\\vec{k}" to "k⃗",
        "\\vec{s}" to "s⃗",
        "\\vec{d}" to "d⃗",
        "\\vec{\\tau}" to "τ⃗",
        "\\vec{\\omega}" to "ω⃗",
        "\\vec{\\alpha}" to "α⃗",
        "\\vec{\\mu}" to "μ⃗",

        // Common Physics & Chemistry Subscripts
        "_{\\text{ind}}" to "_ind",
        "_{\\text{net}}" to "_net",
        "_{\\text{ext}}" to "_ext",
        "_{\\text{rms}}" to "ᵣₘₛ",
        "_{\\text{avg}}" to "ₐᵥ₉",
        "_{\\text{max}}" to "ₘₐₓ",
        "_{\\text{min}}" to "ₘᵢₙ",
        "_{\\text{eq}}" to "ₑᵩ",
        "_{\\text{eff}}" to "ₑff",
        "_{\\text{initial}}" to "_initial",
        "_{\\text{final}}" to "_final",
        "_{\\text{in}}" to "ᵢₙ",
        "_{\\text{out}}" to "ₒᵤₜ",
        "_{ind}" to "_ind",
        "_{net}" to "_net",
        "_{ext}" to "_ext",
        "_{rms}" to "ᵣₘₛ",
        "_{avg}" to "ₐᵥ₉",
        "_{max}" to "ₘₐₓ",
        "_{min}" to "ₘᵢₙ",
        "_{eq}" to "ₑᵩ",
        "_{eff}" to "ₑff",
        "_{initial}" to "_initial",
        "_{final}" to "_final",
        "_{in}" to "ᵢₙ",
        "_{out}" to "ₒᵤₜ",

        // Electrochemistry, Thermochemistry & General Chemistry
        "E^\\circ_{\\text{cell}}" to "E°_cell",
        "E^\\circ_{\\text{red}}" to "E°_red",
        "E^\\circ_{\\text{ox}}" to "E°_ox",
        "E^\\circ_{\\text{cathode}}" to "E°_cathode",
        "E^\\circ_{\\text{anode}}" to "E°_anode",
        "E^\\circ_{\\text{SHE}}" to "E°_SHE",
        "E^\\circ_{\\text{NHE}}" to "E°_NHE",
        "E^{\\circ}_{\\text{cell}}" to "E°_cell",
        "E^{\\circ}_{\\text{red}}" to "E°_red",
        "E^{\\circ}_{\\text{ox}}" to "E°_ox",
        "E^{\\circ}_{\\text{cathode}}" to "E°_cathode",
        "E^{\\circ}_{\\text{anode}}" to "E°_anode",
        "E^\\circ_{cell}" to "E°_cell",
        "E^\\circ_{red}" to "E°_red",
        "E^\\circ_{ox}" to "E°_ox",
        "E^\\circ_{cathode}" to "E°_cathode",
        "E^\\circ_{anode}" to "E°_anode",
        "E^{\\circ}_{cell}" to "E°_cell",
        "E^{\\circ}_{red}" to "E°_red",
        "E^{\\circ}_{ox}" to "E°_ox",
        "E^{\\circ}_{cathode}" to "E°_cathode",
        "E^{\\circ}_{anode}" to "E°_anode",
        "E_{\\text{cell}}" to "E_cell",
        "E_{\\text{red}}" to "E_red",
        "E_{\\text{ox}}" to "E_ox",
        "E_{\\text{cathode}}" to "E_cathode",
        "E_{\\text{anode}}" to "E_anode",
        "E_{cell}" to "E_cell",
        "E_{red}" to "E_red",
        "E_{ox}" to "E_ox",
        "E_{cathode}" to "E_cathode",
        "E_{anode}" to "E_anode",
        "E^{\\circ}" to "E°",
        "E^\\circ" to "E°",
        "E^{\\ominus}" to "E⦵",
        "E^\\ominus" to "E⦵",
        "\\Lambda_m^\\circ" to "Λ°ₘ",
        "\\Lambda_m^{\\circ}" to "Λ°ₘ",
        "\\Lambda_m^\\infty" to "Λ^∞ₘ",
        "\\Lambda_m^{\\infty}" to "Λ^∞ₘ",
        "\\Lambda^\\circ_m" to "Λ°ₘ",
        "\\Lambda^\\infty_m" to "Λ^∞ₘ",
        "\\Lambda_m" to "Λₘ",
        "\\Lambda_{m}" to "Λₘ",
        "\\Lambda_{eq}^\\circ" to "Λ°ₑᵩ",
        "\\Lambda_{eq}^{\\circ}" to "Λ°ₑᵩ",
        "\\Lambda_{eq}" to "Λₑᵩ",
        "\\lambda_m^\\circ" to "λ°ₘ",
        "\\lambda_m^{\\circ}" to "λ°ₘ",
        "\\lambda_m" to "λₘ",
        "\\lambda_{m}" to "λₘ",
        "\\lambda_{eq}" to "λₑᵩ",
        "\\Delta_{lattice}H" to "Δ_lattice H",
        "\\Delta_{lattice} H" to "Δ_lattice H",
        "\\Delta_{atom}H" to "Δ_atom H",
        "\\Delta_{atom} H" to "Δ_atom H",
        "\\Delta_{comb}H" to "Δ_comb H",
        "\\Delta_{comb} H" to "Δ_comb H",
        "\\Delta_{hyd}H" to "Δ_hyd H",
        "\\Delta_{hyd} H" to "Δ_hyd H",
        "\\Delta_{sol}H" to "Δ_sol H",
        "\\Delta_{sol} H" to "Δ_sol H",
        "\\Delta_{vap}H" to "Δ_vap H",
        "\\Delta_{vap} H" to "Δ_vap H",
        "\\Delta_{fus}H" to "Δ_fus H",
        "\\Delta_{fus} H" to "Δ_fus H",
        "\\Delta_{sub}H" to "Δ_sub H",
        "\\Delta_{sub} H" to "Δ_sub H",
        "\\Delta_{eg}H" to "Δₑ_g H",
        "\\Delta_{eg} H" to "Δₑ_g H",
        "\\Delta_{ion}H" to "Δ_ion H",
        "\\Delta_{ion} H" to "Δ_ion H",
        "\\Delta_r H^\\circ" to "ΔᵣH°",
        "\\Delta_r H^{\\circ}" to "ΔᵣH°",
        "\\Delta_r H" to "ΔᵣH",
        "\\Delta_f H^\\circ" to "Δ_f H°",
        "\\Delta_f H^{\\circ}" to "Δ_f H°",
        "\\Delta_f H" to "Δ_f H",
        "\\Delta_c H^\\circ" to "Δ_c H°",
        "\\Delta_c H^{\\circ}" to "Δ_c H°",
        "\\Delta_c H" to "Δ_c H",
        "\\Delta G^\\circ" to "ΔG°",
        "\\Delta G^{\\circ}" to "ΔG°",
        "\\Delta G^\\ominus" to "ΔG⦵",
        "\\Delta G^{\\ominus}" to "ΔG⦵",
        "\\Delta H^\\circ" to "ΔH°",
        "\\Delta H^{\\circ}" to "ΔH°",
        "\\Delta H^\\ominus" to "ΔH⦵",
        "\\Delta H^{\\ominus}" to "ΔH⦵",
        "\\Delta S^\\circ" to "ΔS°",
        "\\Delta S^{\\circ}" to "ΔS°",
        "\\Delta U^\\circ" to "ΔU°",
        "\\Delta U^{\\circ}" to "ΔU°",
        "\\Delta n_g" to "Δn_g",
        "\\Delta n_{g}" to "Δn_g",
        "\\Delta_o" to "Δₒ",
        "\\Delta_{o}" to "Δₒ",
        "\\Delta_t" to "Δₜ",
        "\\Delta_{t}" to "Δₜ",
        "\\mu_s" to "μₛ",
        "\\mu_{s}" to "μₛ",
        "\\mu_B" to "μ_B",
        "\\mu_{B}" to "μ_B",
        "\\mu_{eff}" to "μ_eff",
        "\\mu_eff" to "μ_eff",
        "K_{sp}" to "Kₛₚ",
        "K_{eq}" to "Kₑᵩ",
        "K_a" to "Kₐ",
        "K_b" to "K_b",
        "K_w" to "K_w",
        "K_h" to "Kₕ",
        "K_p" to "Kₚ",
        "K_c" to "K꜀",
        "pK_a" to "pKₐ",
        "pK_b" to "pK_b",
        "pK_w" to "pK_w",
        "t_{1/2}" to "t_½",
        "t_{half}" to "t_½",
        "E_a" to "Eₐ",
        "E_{a}" to "Eₐ",
        "\\ominus" to "⦵",
        "\\ddagger" to "‡",

        // Biology & Genetics
        "\\male" to "♂",
        "\\female" to "♀",
        "\\mars" to "♂",
        "\\venus" to "♀",
        "\\hermaphrodite" to "☿",
        "\\bisexual" to "⚥",

        // Blackboard Bold Number Sets
        "\\mathbb{R}" to "ℝ",
        "\\mathbb{C}" to "ℂ",
        "\\mathbb{Z}" to "ℤ",
        "\\mathbb{N}" to "ℕ",
        "\\mathbb{Q}" to "ℚ",
        "\\mathbb{P}" to "ℙ",
        "\\mathbb{F}" to "𝔽",

        // Dirac & Quantum Bra-Ket Notation (Physics)
        "\\langle" to "⟨",
        "\\rangle" to "⟩",
        "\\lvert" to "|",
        "\\rvert" to "|",
        "\\lVert" to "‖",
        "\\rVert" to "‖",

        // Operators & Calculus
        "\\triangleq" to "≜",
        "\\coloneqq" to "≔",
        "\\eqqcolon" to "≕",
        "\\approxeq" to "≊",
        "\\approx" to "≈",
        "\\equiv" to "≡",
        "\\cong" to "≅",
        "\\asymp" to "≍",
        "\\doteq" to "≐",
        "\\neq" to "≠",
        "\\ne" to "≠",
        "\\leqslant" to "≤",
        "\\geqslant" to "≥",
        "\\leq" to "≤",
        "\\le" to "≤",
        "\\geq" to "≥",
        "\\ge" to "≥",
        "\\lll" to "⋘",
        "\\ggg" to "⋙",
        "\\ll" to "≪",
        "\\gg" to "≫",
        "\\pm" to "±",
        "\\mp" to "∓",
        "\\times" to "×",
        "\\div" to "÷",
        "\\cdot" to "·",
        "\\bullet" to "·",
        "\\infty" to "∞",
        "\\propto" to "∝",
        "\\sim" to "∼",
        "\\partial" to "∂",
        "\\nabla" to "∇",
        "\\hbar" to "ħ",
        "\\hslash" to "ℏ",
        "\\degree" to "°",
        "\\circ" to "°",
        "\\angle" to "∠",
        "\\measuredangle" to "∠",
        "\\parallel" to "∥",
        "\\perp" to "⟂",
        "\\perpendicular" to "⟂",
        "\\triangle" to "△",
        "\\odot" to "⊙",
        "\\otimes" to "⊗",
        "\\oplus" to "⊕",
        "\\ominus" to "⊖",
        "\\oslash" to "⊘",
        "\\angstrom" to "Å",
        "\\AA" to "Å",
        "\\micro" to "µ",
        "\\ohm" to "Ω",
        "\\mho" to "℧",
        "\\oiiint" to "∰",
        "\\oiint" to "∯",
        "\\iiint" to "∭",
        "\\iint" to "∬",
        "\\oint" to "∮",
        "\\int" to "∫",
        "\\sum" to "∑",
        "\\prod" to "∏",
        "\\wedge" to "∧",
        "\\vee" to "∨",
        "\\neg" to "¬",
        "\\top" to "⊤",
        "\\bot" to "⊥",
        "\\ell" to "ℓ",
        "\\lambdabar" to "ƛ",
        "\\aleph_0" to "ℵ₀",
        "\\aleph_1" to "ℵ₁",
        "\\aleph" to "ℵ",
        "\\beth" to "ℶ",
        "\\gimel" to "ℷ",
        "\\daleth" to "ℸ",
        "\\wp" to "℘",
        "\\Re" to "ℜ",
        "\\Im" to "ℑ",
        "\\dagger" to "†",
        "\\ddagger" to "‡",
        "\\spadesuit" to "♠",
        "\\heartsuit" to "♥",
        "\\diamondsuit" to "♦",
        "\\clubsuit" to "♣",

        // Logic & Sets
        "\\therefore" to " ∴ ",
        "\\because" to " ∵ ",
        "\\forall" to " ∀ ",
        "\\exists" to " ∃ ",
        "\\nexists" to " ∄ ",
        "\\varnothing" to " ∅ ",
        "\\emptyset" to " ∅ ",
        "\\notin" to " ∉ ",
        "\\in" to " ∈ ",
        "\\subseteq" to " ⊆ ",
        "\\subset" to " ⊂ ",
        "\\supseteq" to " ⊇ ",
        "\\supset" to " ⊃ ",
        "\\nparallel" to " ∦ ",
        "\\coprod" to " ∐ ",
        "\\models" to " ⊨ ",
        "\\vdash" to " ⊢ ",
        "\\dashv" to " ⊣ ",
        "\\iff" to " ⟺ ",
        "\\implies" to " ⟹ ",
        "\\impliedby" to " ⟸ ",
        "\\setminus" to " \\ ",
        "\\cap" to " ∩ ",
        "\\cup" to " ∪ ",

        // Greek Letters (Uppercase)
        "\\Gamma" to "Γ",
        "\\Delta" to "Δ",
        "\\Theta" to "Θ",
        "\\Lambda" to "Λ",
        "\\Xi" to "Ξ",
        "\\Pi" to "Π",
        "\\Sigma" to "Σ",
        "\\Upsilon" to "Υ",
        "\\Phi" to "Φ",
        "\\Psi" to "Ψ",
        "\\Omega" to "Ω",

        // Greek Letters (Lowercase)
        "\\alpha" to "α",
        "\\beta" to "β",
        "\\gamma" to "γ",
        "\\delta" to "δ",
        "\\varepsilon" to "ε",
        "\\epsilon" to "ε",
        "\\zeta" to "ζ",
        "\\eta" to "η",
        "\\theta" to "θ",
        "\\vartheta" to "θ",
        "\\iota" to "ι",
        "\\kappa" to "κ",
        "\\lambda" to "λ",
        "\\mu" to "μ",
        "\\nu" to "ν",
        "\\xi" to "ξ",
        "\\pi" to "π",
        "\\varpi" to "ϖ",
        "\\rho" to "ρ",
        "\\varrho" to "ϱ",
        "\\sigma" to "σ",
        "\\varsigma" to "ς",
        "\\tau" to "τ",
        "\\upsilon" to "υ",
        "\\varphi" to "ϕ",
        "\\phi" to "φ",
        "\\chi" to "χ",
        "\\psi" to "ψ",
        "\\omega" to "ω",

        // Standard Functions
        "\\sin" to "sin",
        "\\cos" to "cos",
        "\\tan" to "tan",
        "\\cot" to "cot",
        "\\sec" to "sec",
        "\\csc" to "csc",
        "\\arcsin" to "arcsin",
        "\\arccos" to "arccos",
        "\\arctan" to "arctan",
        "\\sinh" to "sinh",
        "\\cosh" to "cosh",
        "\\tanh" to "tanh",
        "\\ln" to "ln",
        "\\log" to "log",
        "\\exp" to "exp",
        "\\lim" to "lim",
        "\\det" to "det",
        "\\max" to "max",
        "\\min" to "min",

        // Spaces and formatting
        "\\qquad" to "   ",
        "\\quad" to "  ",
        "\\," to " ",
        "\\:" to " ",
        "\\;" to " ",
        "\\!" to ""
    )

    /**
     * Extracts balanced curly brace content starting at [startIndex].
     * Returns a Pair of (content inside braces, index immediately following the closing brace),
     * or null if unbalanced or if [startIndex] does not point to '{'.
     */
    fun extractBalancedBrace(str: String, startIndex: Int): Pair<String, Int>? {
        if (startIndex >= str.length || str[startIndex] != '{') return null
        var depth = 0
        var i = startIndex
        val startContent = startIndex + 1
        while (i < str.length) {
            val c = str[i]
            if (c == '{') {
                depth++
            } else if (c == '}') {
                depth--
                if (depth == 0) {
                    return Pair(str.substring(startContent, i), i + 1)
                }
            }
            i++
        }
        return null
    }

    /**
     * Dedicated Math formula renderer for remark-math tokens ($...$, $$...$$, \(...\), \[...\]).
     * Cleans up equation wrappers, aligned environments, LaTeX linebreaks, and formats symbols,
     * superscripts, subscripts, fractions, and square roots into pure Unicode mathematics.
     */
    private val formulaCache = SimpleLruCache<String, String>(1000)

    fun formatFormula(input: String): String {
        if (input.isBlank()) return input
        val cached = formulaCache.get(input)
        if (cached != null) return cached
        val result = doFormatFormula(input)
        formulaCache.put(input, result)
        return result
    }

    private fun doFormatFormula(input: String): String {
        if (input.isBlank()) return input
        var formula = input.trim()

        // Strip LaTeX math delimiters
        if (formula.startsWith("$$") && formula.endsWith("$$") && formula.length >= 4) {
            formula = formula.substring(2, formula.length - 2).trim()
        }
        if (formula.startsWith("\\[") && formula.endsWith("\\]") && formula.length >= 4) {
            formula = formula.substring(2, formula.length - 2).trim()
        }
        if (formula.startsWith("\\(") && formula.endsWith("\\)") && formula.length >= 4) {
            formula = formula.substring(2, formula.length - 2).trim()
        }
        if (formula.startsWith("$") && formula.endsWith("$") && formula.length >= 2) {
            formula = formula.substring(1, formula.length - 1).trim()
        }

        // Clean LaTeX environment wrappers
        val envWrappers = listOf("aligned", "align", "equation", "equation*", "gather", "cases", "matrix", "pmatrix")
        for (env in envWrappers) {
            formula = formula.replace("\\begin{$env}", "").replace("\\end{$env}", "")
        }

        // Convert LaTeX equation newlines (\\) to clean newlines
        formula = formula.replace(Regex("""\\\\(?!\w)"""), "\n")
        // Remove LaTeX alignment tab symbol &
        formula = formula.replace(Regex("""(?<!\\)&"""), " ")

        return formatMathAndLatex(formula)
    }

    /**
     * Pre-processes and formats inline scientific, mathematical, physical, and chemical text.
     * Converts raw text, unformatted formulas, arrows, chemical compounds, equilibrium terms,
     * arithmetic multiplication, superscripts, and subscripts into elegant Unicode typography.
     */
    fun formatScienceAndMathInText(input: String): String {
        if (input.isBlank()) return input
        var text = input

        // 1. Math and Reaction Arrows
        text = text.replace("<==>", " ⇌ ")
        text = text.replace("<=>", " ⇌ ")
        text = text.replace("<->", " ↔ ")
        text = text.replace("==>", " ⇒ ")
        text = text.replace("=>", " ⇒ ")
        text = text.replace(Regex("""(?<=\s|^)(?:->|-->)(?=\s|$)"""), " → ")
        text = text.replace(Regex("""(?<=\s|^)(?:<-|<--)(?=\s|$)"""), " ← ")

        // 2. Arithmetic multiplication (*) - replace with × when between numbers, expressions or variables
        // This ensures * in math (e.g. 6*100, (9+x)*0.082*610, 3/12 * 6) is never eaten by markdown italic
        text = text.replace(Regex("""(?<=[\d\)\]\}\w°ΔΩπθ])\s*\*\s*(?=[\d\(\[\{\w√πθΔ])"""), " × ")
        text = text.replace(Regex("""(?<=[\d\)\]\}])\s+\*\s+(?=[\d\(\[\{])"""), " × ")

        // 3. Mathematical Comparisons & Operators
        text = text.replace("!=", " ≠ ")
        text = text.replace("<=", " ≤ ")
        text = text.replace(">=", " ≥ ")
        text = text.replace("+-", " ± ")
        text = text.replace("-+", " ∓ ")
        text = text.replace(Regex("""\b(?:\~=|\~\~)\b"""), " ≈ ")
        text = text.replace(Regex("""\bproportional to\b""", RegexOption.IGNORE_CASE), "∝")

        // 4. Degrees and Temperature
        text = text.replace(Regex("""\b(?:deg\s*C|°\s*C)\b""", RegexOption.IGNORE_CASE), "°C")
        text = text.replace(Regex("""\b(?:deg\s*K|°\s*K)\b""", RegexOption.IGNORE_CASE), "K")
        text = text.replace(Regex("""\b(?:deg\s*F|°\s*F)\b""", RegexOption.IGNORE_CASE), "°F")
        text = text.replace(Regex("""\bdegree\b""", RegexOption.IGNORE_CASE), "°")

        // Biology Directionality: 5' and 3' ends
        text = text.replace(Regex("""\b5['’]\s*(?:to|->|→)\s*3['’]\b"""), "5′ → 3′")
        text = text.replace(Regex("""\b3['’]\s*(?:to|->|→)\s*5['’]\b"""), "3′ → 5′")
        text = text.replace(Regex("""\b5['’]\b"""), "5′")
        text = text.replace(Regex("""\b3['’]\b"""), "3′")

        // Genetics & Cell Biology Phases
        text = text.replace(Regex("""\bG0\s+phase\b""", RegexOption.IGNORE_CASE), "G₀ phase")
        text = text.replace(Regex("""\bG1\s+phase\b""", RegexOption.IGNORE_CASE), "G₁ phase")
        text = text.replace(Regex("""\bG2\s+phase\b""", RegexOption.IGNORE_CASE), "G₂ phase")
        text = text.replace(Regex("""\bF1\s+generation\b""", RegexOption.IGNORE_CASE), "F₁ generation")
        text = text.replace(Regex("""\bF2\s+generation\b""", RegexOption.IGNORE_CASE), "F₂ generation")
        text = text.replace(Regex("""\bP1\s+generation\b""", RegexOption.IGNORE_CASE), "P₁ generation")
        text = text.replace(Regex("""\bATP\s*(?:->|→)\s*ADP\s*\+\s*Pi\b""", RegexOption.IGNORE_CASE), "ATP → ADP + Pᵢ")
        text = text.replace(Regex("""\bPPi\b"""), "PPᵢ")

        // Chemistry Hybridization
        text = text.replace(Regex("""\bsp3d2\b""", RegexOption.IGNORE_CASE), "sp³d²")
        text = text.replace(Regex("""\bd2sp3\b""", RegexOption.IGNORE_CASE), "d²sp³")
        text = text.replace(Regex("""\bsp3d\b""", RegexOption.IGNORE_CASE), "sp³d")
        text = text.replace(Regex("""\bdsp2\b""", RegexOption.IGNORE_CASE), "dsp²")
        text = text.replace(Regex("""\bsp3\b""", RegexOption.IGNORE_CASE), "sp³")
        text = text.replace(Regex("""\bsp2\b""", RegexOption.IGNORE_CASE), "sp²")

        // Physics & Chemistry Constants and Thermodynamic Variables
        text = text.replace(Regex("""\bmu_0\b"""), "μ₀")
        text = text.replace(Regex("""\bepsilon_0\b"""), "ε₀")
        text = text.replace(Regex("""\bomega_0\b"""), "ω₀")
        text = text.replace(Regex("""\btheta_0\b"""), "θ₀")
        text = text.replace(Regex("""\bv_rms\b"""), "vᵣₘₛ")
        text = text.replace(Regex("""\bv_avg\b"""), "vₐᵥᵍ")
        text = text.replace(Regex("""\bv_mp\b"""), "vₘₚ")

        // Electrochemistry Variables & Cell Potentials
        text = text.replace(Regex("""\bE0_cell\b"""), "E°_cell")
        text = text.replace(Regex("""\bE0_red\b"""), "E°_red")
        text = text.replace(Regex("""\bE0_ox\b"""), "E°_ox")
        text = text.replace(Regex("""\bE0_cathode\b"""), "E°_cathode")
        text = text.replace(Regex("""\bE0_anode\b"""), "E°_anode")
        text = text.replace(Regex("""\bE0_SHE\b"""), "E°_SHE")
        text = text.replace(Regex("""\bE0_NHE\b"""), "E°_NHE")
        text = text.replace(Regex("""\bE0\b"""), "E°")
        text = text.replace(Regex("""\bE\^o\b"""), "E°")
        text = text.replace(Regex("""\bE\^\{\\circ\}\b"""), "E°")
        text = text.replace(Regex("""\bE\^\\circ\b"""), "E°")
        text = text.replace(Regex("""\bE_\{?cell\}?\b"""), "E_cell")
        text = text.replace(Regex("""\bE_\{?red\}?\b"""), "E_red")
        text = text.replace(Regex("""\bE_\{?ox\}?\b"""), "E_ox")
        text = text.replace(Regex("""\bE_\{?cathode\}?\b"""), "E_cathode")
        text = text.replace(Regex("""\bE_\{?anode\}?\b"""), "E_anode")
        text = text.replace(Regex("""\bE°_\{?cell\}?\b"""), "E°_cell")
        text = text.replace(Regex("""\bE°_\{?red\}?\b"""), "E°_red")
        text = text.replace(Regex("""\bE°_\{?ox\}?\b"""), "E°_ox")
        text = text.replace(Regex("""\bE°_\{?cathode\}?\b"""), "E°_cathode")
        text = text.replace(Regex("""\bE°_\{?anode\}?\b"""), "E°_anode")

        // Standard reduction and redox couples: E°(Zn2+/Zn), E°(Cu2+/Cu), E(Fe3+/Fe2+)
        text = text.replace(Regex("""E[°0]\s*\(\s*([A-Za-z0-9\+\-]+)\s*/\s*([A-Za-z0-9\+\-]+)\s*\)""")) { match ->
            "E°(${formatChemistry(match.groupValues[1])}/${formatChemistry(match.groupValues[2])})"
        }
        text = text.replace(Regex("""\bE\s*\(\s*([A-Za-z0-9\+\-]+)\s*/\s*([A-Za-z0-9\+\-]+)\s*\)""")) { match ->
            "E(${formatChemistry(match.groupValues[1])}/${formatChemistry(match.groupValues[2])})"
        }

        // Conductance, Kohlrausch Law & Overpotential
        text = text.replace(Regex("""\b(?:Lambda_m\^0|Lambda_m\^o|Lambda\^0_m|Lambda°_m|Lambda0_m)\b"""), "Λ°ₘ")
        text = text.replace(Regex("""\b(?:Lambda_m\^inf|Lambda\^inf_m|Lambda_m\^\\infty)\b"""), "Λ^∞ₘ")
        text = text.replace(Regex("""\b(?:Lambda_m|\\Lambda_m|Lambda_\{m\})\b"""), "Λₘ")
        text = text.replace(Regex("""\b(?:Lambda_eq\^0|Lambda_eq\^o|Lambda°_eq|Lambda0_eq)\b"""), "Λ°ₑᵩ")
        text = text.replace(Regex("""\b(?:Lambda_eq|\\Lambda_eq|Lambda_\{eq\})\b"""), "Λₑᵩ")
        text = text.replace(Regex("""\b(?:lambda_m\^0|lambda°_m)\b"""), "λ°ₘ")
        text = text.replace(Regex("""\b(?:lambda_m|\\lambda_m)\b"""), "λₘ")
        text = text.replace(Regex("""\b(?:lambda_eq|\\lambda_eq)\b"""), "λₑᵩ")
        text = text.replace(Regex("""\b(?:kappa|\\kappa)\b"""), "κ")
        text = text.replace(Regex("""\b(?:eta|\\eta)\b"""), "η")

        // Chemical Equilibrium, Acids & Bases, Kinetics
        text = text.replace(Regex("""\bKp\b"""), "Kₚ")
        text = text.replace(Regex("""\bKc\b"""), "K꜀")
        text = text.replace(Regex("""\bKa\b"""), "Kₐ")
        text = text.replace(Regex("""\bKb\b"""), "K_b")
        text = text.replace(Regex("""\bKw\b"""), "K_w")
        text = text.replace(Regex("""\bKh\b"""), "Kₕ")
        text = text.replace(Regex("""\bKsp\b"""), "Kₛₚ")
        text = text.replace(Regex("""\bQc\b"""), "Q꜀")
        text = text.replace(Regex("""\bQp\b"""), "Qₚ")
        text = text.replace(Regex("""\bpKa\b"""), "pKₐ")
        text = text.replace(Regex("""\bpKb\b"""), "pK_b")
        text = text.replace(Regex("""\bpKw\b"""), "pK_w")
        text = text.replace(Regex("""\b(?:t_\{?1/2\}?|t_half)\b"""), "t_½")
        text = text.replace(Regex("""\bE_\{?a\}?\b"""), "Eₐ")

        // Delta & Enthalpy/Gibbs Variables
        text = text.replace(Regex("""\b(?:delta|\\Delta)\s*G\s*(?:0|°|\^0|\^\\circ|\^\\ominus)\b""", RegexOption.IGNORE_CASE), "ΔG°")
        text = text.replace(Regex("""\b(?:delta|\\Delta)\s*H\s*(?:0|°|\^0|\^\\circ|\^\\ominus)\b""", RegexOption.IGNORE_CASE), "ΔH°")
        text = text.replace(Regex("""\b(?:delta|\\Delta)\s*S\s*(?:0|°|\^0|\^\\circ|\^\\ominus)\b""", RegexOption.IGNORE_CASE), "ΔS°")
        text = text.replace(Regex("""\b(?:delta|\\Delta)\s*U\s*(?:0|°|\^0|\^\\circ|\^\\ominus)\b""", RegexOption.IGNORE_CASE), "ΔU°")
        text = text.replace(Regex("""\b(?:delta|\\Delta)\s*n_?g\b""", RegexOption.IGNORE_CASE), "Δn_g")
        text = text.replace(Regex("""\b(?:delta|\\Delta)_?r\s*H\b""", RegexOption.IGNORE_CASE), "ΔᵣH")
        text = text.replace(Regex("""\b(?:delta|\\Delta)_?f\s*H\b""", RegexOption.IGNORE_CASE), "Δ_f H")
        text = text.replace(Regex("""\b(?:delta|\\Delta)_?c\s*H\b""", RegexOption.IGNORE_CASE), "Δ_c H")
        text = text.replace(Regex("""\b(?:delta|\\Delta)_?lattice\s*H\b""", RegexOption.IGNORE_CASE), "Δ_lattice H")
        text = text.replace(Regex("""\b(?:delta|\\Delta)_?atom\s*H\b""", RegexOption.IGNORE_CASE), "Δ_atom H")
        text = text.replace(Regex("""\b(?:delta|\\Delta)_?hyd\s*H\b""", RegexOption.IGNORE_CASE), "Δ_hyd H")
        text = text.replace(Regex("""\b(?:delta|\\Delta)_?sol\s*H\b""", RegexOption.IGNORE_CASE), "Δ_sol H")
        text = text.replace(Regex("""\b(?:delta|\\Delta)_?vap\s*H\b""", RegexOption.IGNORE_CASE), "Δ_vap H")
        text = text.replace(Regex("""\b(?:delta|\\Delta)_?fus\s*H\b""", RegexOption.IGNORE_CASE), "Δ_fus H")
        text = text.replace(Regex("""\b(?:delta|\\Delta)_?sub\s*H\b""", RegexOption.IGNORE_CASE), "Δ_sub H")
        text = text.replace(Regex("""\b(?:delta|\\Delta)_?eg\s*H\b""", RegexOption.IGNORE_CASE), "Δₑ_g H")
        text = text.replace(Regex("""\b(?:delta|\\Delta)_?ion\s*H\b""", RegexOption.IGNORE_CASE), "Δ_ion H")
        text = text.replace(Regex("""\b(?:delta|\\Delta)\s*H\b""", RegexOption.IGNORE_CASE), "ΔH")
        text = text.replace(Regex("""\b(?:delta|\\Delta)\s*G\b""", RegexOption.IGNORE_CASE), "ΔG")
        text = text.replace(Regex("""\b(?:delta|\\Delta)\s*S\b""", RegexOption.IGNORE_CASE), "ΔS")
        text = text.replace(Regex("""\b(?:delta|\\Delta)\s*U\b""", RegexOption.IGNORE_CASE), "ΔU")
        text = text.replace(Regex("""\b(?:delta|\\Delta)\s*T\b""", RegexOption.IGNORE_CASE), "ΔT")
        text = text.replace(Regex("""\b(?:delta|\\Delta)\s*n\b""", RegexOption.IGNORE_CASE), "Δn")
        text = text.replace(Regex("""\b(?:delta|\\Delta)\s*V\b""", RegexOption.IGNORE_CASE), "ΔV")
        text = text.replace(Regex("""\b(?:delta|\\Delta)\s*P\b""", RegexOption.IGNORE_CASE), "ΔP")

        // Coordination Chemistry & Crystal Field Splitting
        text = text.replace(Regex("""\b(?:Delta_o|\\Delta_o|Delta_\{o\})\b"""), "Δₒ")
        text = text.replace(Regex("""\b(?:Delta_t|\\Delta_t|Delta_\{t\})\b"""), "Δₜ")
        text = text.replace(Regex("""\bt2g\b"""), "t₂_g")
        text = text.replace(Regex("""\beg\b"""), "e_g")
        text = text.replace(Regex("""\b(?:mu_s|\\mu_s|mu_\{s\})\b"""), "μₛ")
        text = text.replace(Regex("""\b(?:mu_eff|\\mu_eff|mu_\{eff\})\b"""), "μ_eff")
        text = text.replace(Regex("""\b(?:mu_B|\\mu_B|mu_\{B\})\b"""), "μ_B")

        // Salt bridges and phase boundaries in galvanic cells
        text = text.replace(Regex("""(?<=\w|\))\s*(?:\|\||//)\s*(?=\w|\[)"""), " ‖ ")

        // Single and transfer electrons in reactions
        text = text.replace(Regex("""(?<=\s|^|\+)(\d*)\s*e\s*[\^]?\s*-(?=\s|$|[,\.\+\)]|->|→)""")) { match ->
            val coeff = match.groupValues[1]
            "${coeff}e⁻"
        }
        text = text.replace(Regex("""\be\s*-\b"""), "e⁻")

        // Organic Chemistry Radicals, Partial Charges & Transition State
        text = text.replace(Regex("""\bdelta\+"""), "δ⁺")
        text = text.replace(Regex("""\bdelta\-"""), "δ⁻")
        text = text.replace(Regex("""\[TS\][#‡]"""), "[TS]‡")

        // Common Chemistry Ions in Solutions & Reactions
        val chemIons = listOf(
            "Zn2+" to "Zn²⁺", "Cu2+" to "Cu²⁺", "Cu+" to "Cu⁺", "Fe2+" to "Fe²⁺", "Fe3+" to "Fe³⁺",
            "Ag+" to "Ag⁺", "Al3+" to "Al³⁺", "Mg2+" to "Mg²⁺", "Ca2+" to "Ca²⁺", "Ba2+" to "Ba²⁺",
            "Sr2+" to "Sr²⁺", "Na+" to "Na⁺", "K+" to "K⁺", "Li+" to "Li⁺", "Pb2+" to "Pb²⁺",
            "Sn2+" to "Sn²⁺", "Sn4+" to "Sn⁴⁺", "Ni2+" to "Ni²⁺", "Co2+" to "Co²⁺", "Co3+" to "Co³⁺",
            "Cr3+" to "Cr³⁺", "Cr6+" to "Cr⁶⁺", "Mn2+" to "Mn²⁺", "Mn7+" to "Mn⁷⁺", "Hg2+" to "Hg²⁺",
            "Hg2^2+" to "Hg₂²⁺", "Pt2+" to "Pt²⁺", "Au3+" to "Au³⁺", "NH4+" to "NH₄⁺", "H3O+" to "H₃O⁺",
            "Cl-" to "Cl⁻", "Br-" to "Br⁻", "I-" to "I⁻", "F-" to "F⁻", "OH-" to "OH⁻",
            "CN-" to "CN⁻", "SCN-" to "SCN⁻", "NO3-" to "NO₃⁻", "NO2-" to "NO₂⁻",
            "SO4^2-" to "SO₄²⁻", "SO42-" to "SO₄²⁻", "SO3^2-" to "SO₃²⁻", "SO32-" to "SO₃²⁻",
            "CO3^2-" to "CO₃²⁻", "CO32-" to "CO₃²⁻", "HCO3-" to "HCO₃⁻", "HSO4-" to "HSO₄⁻",
            "PO4^3-" to "PO₄³⁻", "PO43-" to "PO₄³⁻", "MnO4-" to "MnO₄⁻", "MnO4^2-" to "MnO₄²⁻",
            "CrO4^2-" to "CrO₄²⁻", "CrO42-" to "CrO₄²⁻", "Cr2O7^2-" to "Cr₂O₇²⁻", "Cr2O72-" to "Cr₂O₇²⁻",
            "C2O4^2-" to "C₂O₄²⁻", "C2O42-" to "C₂O₄²⁻", "CH3COO-" to "CH₃COO⁻",
            "ClO-" to "ClO⁻", "ClO2-" to "ClO₂⁻", "ClO3-" to "ClO₃⁻", "ClO4-" to "ClO₄⁻",
            "O2-" to "O²⁻", "S2-" to "S²⁻", "N3-" to "N³⁻",
            "[Fe(CN)6]4-" to "[Fe(CN)₆]⁴⁻", "[Fe(CN)6]3-" to "[Fe(CN)₆]³⁻",
            "[Co(NH3)6]3+" to "[Co(NH₃)₆]³⁺", "[Cu(NH3)4]2+" to "[Cu(NH₃)₄]²⁺",
            "[Ni(CN)4]2-" to "[Ni(CN)₄]²⁻", "[Ni(CO)4]" to "[Ni(CO)₄]",
            "[Pt(NH3)2Cl2]" to "[Pt(NH₃)₂Cl₂]", "[Ag(NH3)2]+" to "[Ag(NH₃)₂]⁺",
            "[Fe(H2O)6]2+" to "[Fe(H₂O)₆]²⁺", "[Fe(H2O)6]3+" to "[Fe(H₂O)₆]³⁺",
            "[Al(OH)4]-" to "[Al(OH)₄]⁻", "[Zn(OH)4]2-" to "[Zn(OH)₄]²⁻",
            "[Cr(H2O)6]3+" to "[Cr(H₂O)₆]³⁺", "[Co(en)3]3+" to "[Co(en)₃]³⁺",
            "[Fe(C2O4)3]3-" to "[Fe(C₂O₄)₃]³⁻"
        )
        for ((raw, formatted) in chemIons) {
            text = text.replace(Regex("""(?<![a-zA-Z0-9])${Regex.escape(raw)}(?![a-zA-Z0-9])"""), formatted)
        }

        // 6. Partial Pressures & Ion Concentrations: p(PCl3) -> p(PCl₃), [H+] -> [H⁺]
        text = text.replace(Regex("""\b([pP])\(([A-Z][a-zA-Z0-9]*)\)""")) { match ->
            val p = match.groupValues[1]
            val inside = formatChemistry(match.groupValues[2])
            "$p($inside)"
        }
        text = text.replace(Regex("""\[\s*H\+\s*\]"""), "[H⁺]")
        text = text.replace(Regex("""\[\s*OH-\s*\]"""), "[OH⁻]")
        text = text.replace(Regex("""\[\s*H3O\+\s*\]"""), "[H₃O⁺]")

        // 7. Common High-Frequency Chemical Compounds with Subscripts
        val chemCompounds = listOf(
            "PCl5" to "PCl₅", "PCl3" to "PCl₃", "Cl2" to "Cl₂", "H2SO4" to "H₂SO₄",
            "H2O" to "H₂O", "CO2" to "CO₂", "NH3" to "NH₃", "CH4" to "CH₄",
            "KMnO4" to "KMnO₄", "K2Cr2O7" to "K₂Cr₂O₇", "CaCO3" to "CaCO₃", "CaO" to "CaO",
            "NO2" to "NO₂", "N2O4" to "N₂O₄", "N2O5" to "N₂O₅", "N2O" to "N₂O",
            "SO2" to "SO₂", "SO3" to "SO₃", "SO4" to "SO₄", "PO4" to "PO₄",
            "NO3" to "NO₃", "O2" to "O₂", "N2" to "N₂", "H2" to "H₂",
            "F2" to "F₂", "Br2" to "Br₂", "I2" to "I₂", "O3" to "O₃",
            "C2H5OH" to "C₂H₅OH", "C6H12O6" to "C₆H₁₂O₆", "CH3COOH" to "CH₃COOH",
            "Al2O3" to "Al₂O₃", "Fe2O3" to "Fe₂O₃", "Fe3O4" to "Fe₃O₄",
            "CuSO4" to "CuSO₄", "BaSO4" to "BaSO₄", "FeSO4" to "FeSO₄",
            "Ca(OH)2" to "Ca(OH)₂", "Mg(OH)2" to "Mg(OH)₂", "Al(OH)3" to "Al(OH)₃",
            "(NH4)2SO4" to "(NH₄)₂SO₄", "SF6" to "SF₆", "XeF4" to "XeF₄",
            "XeF6" to "XeF₆", "XeO3" to "XeO₃", "IF7" to "IF₇",
            "P4O10" to "P₄O₁₀", "P2O5" to "P₂O₅", "H3PO4" to "H₃PO₄",
            "H3PO3" to "H₃PO₃", "H3PO2" to "H₃PO₂", "HNO3" to "HNO₃",
            "HNO2" to "HNO₂", "HCl" to "HCl", "NaOH" to "NaOH", "KOH" to "KOH",
            "NaCl" to "NaCl", "KCl" to "KCl", "MgCl2" to "MgCl₂", "CaCl2" to "CaCl₂"
        )
        for ((raw, formatted) in chemCompounds) {
            text = text.replace(Regex("""\b${Regex.escape(raw)}\b"""), formatted)
        }

        // Generic chemical formulas: e.g. XY2 -> XY₂, Fe2O3 -> Fe₂O₃
        val chemFormulaRegex = Regex("""\b([A-Z][a-z]?(?:[A-Z][a-z]?)+)(\d+)\b""")
        text = text.replace(chemFormulaRegex) { match ->
            val formula = match.groupValues[1]
            val num = match.groupValues[2]
            "$formula${toSubscript(num)}"
        }

        // 8. Scientific Powers & Superscripts: 10^5 -> 10⁵, 10^-3 -> 10⁻³, m/s^2 -> m/s², cm^3 -> cm³
        text = text.replace(Regex("""\b10\^([\-+]?\d+)\b""")) { match ->
            "10${toSuperscript(match.groupValues[1])}"
        }
        text = text.replace(Regex("""\b([a-zA-Z\)])\^([\-+]?[0-9a-zA-Z]+)\b""")) { match ->
            "${match.groupValues[1]}${toSuperscript(match.groupValues[2])}"
        }
        text = text.replace(Regex("""\^\{\s*([^}]+)\s*\}""")) { match ->
            toSuperscript(match.groupValues[1])
        }

        // 9. Units Formatting
        text = text.replace(Regex("""\bm/s\^2\b"""), "m/s²")
        text = text.replace(Regex("""\bcm\^3\b"""), "cm³")
        text = text.replace(Regex("""\bm\^3\b"""), "m³")
        text = text.replace(Regex("""\bs\^\-1\b"""), "s⁻¹")
        text = text.replace(Regex("""\bmol\s*L\^\-1\b"""), "mol·L⁻¹")
        text = text.replace(Regex("""\bmol\s*L\^\-1\s*s\^\-1\b"""), "mol·L⁻¹·s⁻¹")
        text = text.replace(Regex("""\bJ\s*mol\^\-1\s*K\^\-1\b"""), "J·mol⁻¹·K⁻¹")
        text = text.replace(Regex("""\b(?:ohm|\\Omega)\b"""), "Ω")
        text = text.replace(Regex("""\b(?:micro|\\mu)\b"""), "µ")
        text = text.replace(Regex("""\b(?:angstrom|\\AA|\\angstrom)\b"""), "Å")

        // 10. Subscripts for Physics Variables: v_rms -> vᵣₘₛ, v_avg -> vₐᵥᵍ, x_1 -> x₁, T_1 -> T₁
        text = text.replace(Regex("""\bv_\{?rms\}?\b"""), "vᵣₘₛ")
        text = text.replace(Regex("""\bv_\{?avg\}?\b"""), "vₐᵥᵍ")
        text = text.replace(Regex("""\bv_\{?mp\}?\b"""), "vₘₚ")
        text = text.replace(Regex("""\b([a-zA-Z])_([0-9])\b""")) { match ->
            "${match.groupValues[1]}${toSubscript(match.groupValues[2])}"
        }

        // 11. Process any LaTeX styling, fractions, roots, vectors, and symbol tokens in text
        if (text.contains("\\")) {
            // Convert bare LaTeX fractions \frac{num}{den}, \dfrac{num}{den}, \tfrac{num}{den}
            val fracKeywords = listOf("\\frac", "\\dfrac", "\\tfrac")
            for (kw in fracKeywords) {
                var safety = 0
                while (text.contains(kw) && safety < 10) {
                    val idx = text.indexOf(kw)
                    var pos = idx + kw.length
                    while (pos < text.length && text[pos].isWhitespace()) pos++
                    val numRes = extractBalancedBrace(text, pos)
                    if (numRes != null) {
                        var denPos = numRes.second
                        while (denPos < text.length && text[denPos].isWhitespace()) denPos++
                        val denRes = extractBalancedBrace(text, denPos)
                        if (denRes != null) {
                            val num = formatScienceAndMathInText(numRes.first.trim())
                            val den = formatScienceAndMathInText(denRes.first.trim())
                            val rep = if (den.length <= 2 && num.length <= 2 && !den.contains(" ") && !num.contains(" ")) {
                                "$num/$den"
                            } else {
                                "($num / $den)"
                            }
                            text = text.substring(0, idx) + rep + text.substring(denRes.second)
                        } else break
                    } else break
                    safety++
                }
            }

            // Convert bare LaTeX square roots \sqrt{x}
            var sqrtSafety = 0
            while (text.contains("\\sqrt") && sqrtSafety < 10) {
                val idx = text.indexOf("\\sqrt")
                var pos = idx + 5
                while (pos < text.length && text[pos].isWhitespace()) pos++
                val res = extractBalancedBrace(text, pos)
                if (res != null) {
                    val inner = formatScienceAndMathInText(res.first.trim())
                    text = text.substring(0, idx) + "√($inner)" + text.substring(res.second)
                } else break
                sqrtSafety++
            }

            // Boxed notation: \boxed{768} -> 768
            while (text.contains("\\boxed{")) {
                val idx = text.indexOf("\\boxed{")
                val res = extractBalancedBrace(text, idx + 6)
                if (res != null) {
                    val inner = formatScienceAndMathInText(res.first.trim())
                    text = text.substring(0, idx) + " $inner " + text.substring(res.second)
                } else {
                    text = text.replace("\\boxed{", "").replace("}", "")
                    break
                }
            }

            // LaTeX spaces and multiplication dots
            text = text.replace("\\qquad", "   ")
            text = text.replace("\\quad", "  ")
            text = text.replace("\\cdot", " · ")

            // Style wrappers: \mathbf{B} -> B, \text{ind} -> ind, \mathrm{X} -> X
            val wrappers = listOf("mathbf", "boldsymbol", "bm", "text", "mathrm", "textbf", "textit", "mathit")
            for (w in wrappers) {
                text = text.replace(Regex("""\\$w\{([^{}]+)\}""")) { it.groupValues[1] }
            }
            // Vector notation: \vec{B} -> B⃗
            text = text.replace(Regex("""\\vec\{([a-zA-Z])\}""")) { "${it.groupValues[1]}⃗" }
            // Apply symbol replacements
            for ((cmd, replacement) in LATEX_REPLACEMENTS) {
                if (text.contains(cmd)) {
                    val escapedCmd = Regex.escape(cmd)
                    text = text.replace(Regex("""$escapedCmd(?![a-zA-Z])"""), replacement)
                }
            }
        }

        // 12. Generic curly subscripts and superscripts in plain text
        text = text.replace(Regex("""_\{([0-9a-zA-Z\+\-]+)\}""")) { matchResult ->
            toSubscript(matchResult.groupValues[1])
        }
        text = text.replace(Regex("""\^\{([0-9a-zA-Z\+\-]+)\}""")) { matchResult ->
            toSuperscript(matchResult.groupValues[1])
        }

        // 13. Biology Genetics & DNA notations
        text = text.replace(Regex("""\b5'(\s*(?:[-–—>→]|to)\s*)3'(?!\w)"""), "5′ → 3′")
        text = text.replace(Regex("""\b3'(\s*(?:[-–—>→]|to)\s*)5'(?!\w)"""), "3′ → 5′")
        text = text.replace(Regex("""\b5'(?!\w)"""), "5′")
        text = text.replace(Regex("""\b3'(?!\w)"""), "3′")
        text = text.replace(Regex("""\bF1\s+generation\b""", RegexOption.IGNORE_CASE), "F₁ generation")
        text = text.replace(Regex("""\bF2\s+generation\b""", RegexOption.IGNORE_CASE), "F₂ generation")
        text = text.replace(Regex("""\bP1\s+generation\b""", RegexOption.IGNORE_CASE), "P₁ generation")

        return text
    }

    /**
     * Convenience alias for formatMathAndLatex.
     */
    fun formatAll(input: String): String = formatMathAndLatex(input)

    /**
     * Formats mathematical expressions, formulas, multiplication/division symbols,
     * LaTeX expressions, powers, superscripts, subscripts, Greek symbols and arrows
     * into clean, crisp Unicode typography for the Android UI.
     */
    fun formatMathAndLatex(input: String): String = formatMathAndLatexInternal(input, depth = 0)

    private fun formatMathAndLatexInternal(input: String, depth: Int): String {
        if (input.isBlank() || depth > 5) return input

        var text = input

        // 1. Normalize escaped backslashes (JSON unescaped representations like \\frac -> \frac)
        text = text.replace("\\\\", "\\").replace("\\\\", "\\")

        // 2. Remove LaTeX math wrappers & display blocks
        text = text.replace("\\[", "\n").replace("\\]", "\n")
        text = text.replace("\\(", "").replace("\\)", "")
        text = text.replace("$$", "\n")
        
        // Strip standalone $ math fences while preserving text
        text = text.replace(Regex("""(?<!\\)\$"""), "")

        // 3. Clean delimiters: \left(, \right), \left[, \right], etc.
        text = text.replace("\\left(", "(").replace("\\right)", ")")
        text = text.replace("\\left[", "[").replace("\\right]", "]")
        text = text.replace("\\left\\{", "{").replace("\\right\\}", "}")
        text = text.replace("\\left|", "|").replace("\\right|", "|")
        text = text.replace("\\left.", "").replace("\\right.", "")

        // Degree symbols with exponents or LaTeX tags: 90^\circ, 45^{\circ}, \degree, ^°
        text = text.replace(Regex("""\^\{\s*\\circ\s*\}|\^\\circ\b"""), "°")
        text = text.replace(Regex("""\^\{\s*°\s*\}|\^°"""), "°")
        text = text.replace("\\degree", "°")

        // 3a. Handle Chemistry mhchem \ce{...} and \pu{...}
        var ceIdx = text.indexOf("\\ce")
        var ceSafety = 0
        while (ceIdx != -1 && ceSafety < 25) {
            var pos = ceIdx + 3
            while (pos < text.length && text[pos].isWhitespace()) pos++
            val braceResult = extractBalancedBrace(text, pos)
            if (braceResult != null) {
                val inner = braceResult.first
                val formattedCe = formatChemistry(inner)
                text = text.substring(0, ceIdx) + formattedCe + text.substring(braceResult.second)
                ceIdx = text.indexOf("\\ce")
            } else {
                ceIdx = text.indexOf("\\ce", ceIdx + 3)
            }
            ceSafety++
        }

        var puIdx = text.indexOf("\\pu")
        var puSafety = 0
        while (puIdx != -1 && puSafety < 20) {
            var pos = puIdx + 3
            while (pos < text.length && text[pos].isWhitespace()) pos++
            val braceResult = extractBalancedBrace(text, pos)
            if (braceResult != null) {
                val inner = braceResult.first
                text = text.substring(0, puIdx) + inner + text.substring(braceResult.second)
                puIdx = text.indexOf("\\pu")
            } else {
                puIdx = text.indexOf("\\pu", puIdx + 3)
            }
            puSafety++
        }

        // 3b. Chemistry reaction arrows with conditions: \xrightarrow[below]{above}
        var arrowSafety = 0
        while (text.contains("\\xrightarrow") && arrowSafety < 10) {
            val idx = text.indexOf("\\xrightarrow")
            var pos = idx + "\\xrightarrow".length
            while (pos < text.length && text[pos].isWhitespace()) pos++
            var belowText = ""
            if (pos < text.length && text[pos] == '[') {
                val closeBracket = text.indexOf(']', pos)
                if (closeBracket != -1) {
                    belowText = text.substring(pos + 1, closeBracket).trim()
                    pos = closeBracket + 1
                }
            }
            while (pos < text.length && text[pos].isWhitespace()) pos++
            val braceResult = extractBalancedBrace(text, pos)
            if (braceResult != null) {
                val aboveText = braceResult.first.trim()
                val condition = when {
                    aboveText.isNotEmpty() && belowText.isNotEmpty() -> "$aboveText, $belowText"
                    aboveText.isNotEmpty() -> aboveText
                    else -> belowText
                }
                val formattedCond = if (condition.isNotEmpty()) " ──($condition)──> " else " → "
                text = text.substring(0, idx) + formattedCond + text.substring(braceResult.second)
            } else {
                text = text.replaceFirst("\\xrightarrow", " → ")
            }
            arrowSafety++
        }

        // 4. Unwrap LaTeX text styling wrappers: \text{...}, \mathrm{...}, \mathbf{...}, etc.
        val styleWrappers = listOf("text", "mathrm", "mathbf", "textbf", "textit", "mathit", "operatorname", "underline", "overline", "boldsymbol", "chemfig", "mathsf", "mathtt", "textsf", "texttt")
        for (wrapper in styleWrappers) {
            val kw = "\\$wrapper"
            var idx = text.indexOf(kw)
            var safetyCount = 0
            while (idx != -1 && safetyCount < 15) {
                var pos = idx + kw.length
                while (pos < text.length && text[pos].isWhitespace()) pos++
                val braceResult = extractBalancedBrace(text, pos)
                if (braceResult != null) {
                    val inner = braceResult.first
                    text = text.substring(0, idx) + inner + text.substring(braceResult.second)
                    idx = text.indexOf(kw)
                } else {
                    idx = text.indexOf(kw, idx + kw.length)
                }
                safetyCount++
            }
        }


        // 4a. Mathcal script letters
        var mathcalSafety = 0
        while (text.contains("\\mathcal") && mathcalSafety < 15) {
            val idx = text.indexOf("\\mathcal")
            var pos = idx + 8
            while (pos < text.length && text[pos].isWhitespace()) pos++
            val braceResult = extractBalancedBrace(text, pos)
            if (braceResult != null) {
                val inner = braceResult.first.trim()
                val scriptChar = when(inner) {
                    "A" -> "𝒜"; "B" -> "ℬ"; "C" -> "𝒞"; "D" -> "𝒟"; "E" -> "ℰ"; "F" -> "ℱ"; "G" -> "𝒢"
                    "H" -> "ℋ"; "I" -> "ℐ"; "J" -> "𝒥"; "K" -> "𝒦"; "L" -> "ℒ"; "M" -> "ℳ"; "N" -> "𝒩"
                    "O" -> "𝒪"; "P" -> "𝒫"; "Q" -> "𝒬"; "R" -> "ℛ"; "S" -> "𝒮"; "T" -> "𝒯"; "U" -> "𝒰"
                    "V" -> "𝒱"; "W" -> "𝒲"; "X" -> "𝒳"; "Y" -> "𝒴"; "Z" -> "𝒵"
                    "a" -> "𝒶"; "b" -> "𝒷"; "c" -> "𝒸"; "d" -> "𝒹"; "e" -> "ℯ"; "f" -> "𝒻"; "g" -> "ℊ"
                    "h" -> "𝒽"; "i" -> "𝒾"; "j" -> "𝒿"; "k" -> "𝓀"; "l" -> "𝓁"; "m" -> "𝓂"; "n" -> "𝓃"
                    "o" -> "ℴ"; "p" -> "𝓅"; "q" -> "𝓆"; "r" -> "𝓇"; "s" -> "𝓈"; "t" -> "𝓉"; "u" -> "𝓊"
                    "v" -> "𝓋"; "w" -> "𝓌"; "x" -> "𝓍"; "y" -> "𝓎"; "z" -> "𝓏"
                    else -> inner
                }
                text = text.substring(0, idx) + scriptChar + text.substring(braceResult.second)
            } else if (pos < text.length && text[pos].isLetter()) {
                val inner = text[pos].toString()
                val scriptChar = when(inner) {
                    "A" -> "𝒜"; "B" -> "ℬ"; "C" -> "𝒞"; "D" -> "𝒟"; "E" -> "ℰ"; "F" -> "ℱ"; "G" -> "𝒢"
                    "H" -> "ℋ"; "I" -> "ℐ"; "J" -> "𝒥"; "K" -> "𝒦"; "L" -> "ℒ"; "M" -> "ℳ"; "N" -> "𝒩"
                    "O" -> "𝒪"; "P" -> "𝒫"; "Q" -> "𝒬"; "R" -> "ℛ"; "S" -> "𝒮"; "T" -> "𝒯"; "U" -> "𝒰"
                    "V" -> "𝒱"; "W" -> "𝒲"; "X" -> "𝒳"; "Y" -> "𝒴"; "Z" -> "𝒵"
                    else -> inner
                }
                text = text.substring(0, idx) + scriptChar + text.substring(pos + 1)
            } else {
                break
            }
            mathcalSafety++
        }
        
        var mathbbSafety = 0
        while (text.contains("\\mathbb") && mathbbSafety < 15) {
            val idx = text.indexOf("\\mathbb")
            var pos = idx + 7
            while (pos < text.length && text[pos].isWhitespace()) pos++
            val braceResult = extractBalancedBrace(text, pos)
            if (braceResult != null) {
                val inner = braceResult.first.trim()
                val doubleChar = when(inner) {
                    "A" -> "𝔸"; "B" -> "𝔹"; "C" -> "ℂ"; "D" -> "𝔻"; "E" -> "𝔼"; "F" -> "𝔽"; "G" -> "𝔾"
                    "H" -> "ℍ"; "I" -> "𝕀"; "J" -> "𝕁"; "K" -> "𝕂"; "L" -> "𝕃"; "M" -> "𝕄"; "N" -> "ℕ"
                    "O" -> "𝕆"; "P" -> "ℙ"; "Q" -> "ℚ"; "R" -> "ℝ"; "S" -> "𝕊"; "T" -> "𝕋"; "U" -> "𝕌"
                    "V" -> "𝕍"; "W" -> "𝕎"; "X" -> "𝕏"; "Y" -> "𝕐"; "Z" -> "ℤ"
                    else -> inner
                }
                text = text.substring(0, idx) + doubleChar + text.substring(braceResult.second)
            } else if (pos < text.length && text[pos].isLetter()) {
                val inner = text[pos].toString()
                val doubleChar = when(inner) {
                    "A" -> "𝔸"; "B" -> "𝔹"; "C" -> "ℂ"; "D" -> "𝔻"; "E" -> "𝔼"; "F" -> "𝔽"; "G" -> "𝔾"
                    "H" -> "ℍ"; "I" -> "𝕀"; "J" -> "𝕁"; "K" -> "𝕂"; "L" -> "𝕃"; "M" -> "𝕄"; "N" -> "ℕ"
                    "O" -> "𝕆"; "P" -> "ℙ"; "Q" -> "ℚ"; "R" -> "ℝ"; "S" -> "𝕊"; "T" -> "𝕋"; "U" -> "𝕌"
                    "V" -> "𝕍"; "W" -> "𝕎"; "X" -> "𝕏"; "Y" -> "𝕐"; "Z" -> "ℤ"
                    else -> inner
                }
                text = text.substring(0, idx) + doubleChar + text.substring(pos + 1)
            } else {
                break
            }
            mathbbSafety++
        }

        // 4c. Dirac Quantum Mechanics Bra-Ket notation: \braket{a}{b}, \bra{psi}, \ket{psi}
        var braketSafety = 0
        while (text.contains("\\braket") && braketSafety < 10) {
            val idx = text.indexOf("\\braket")
            var pos = idx + 7
            while (pos < text.length && text[pos].isWhitespace()) pos++
            val b1 = extractBalancedBrace(text, pos)
            if (b1 != null) {
                var p2 = b1.second
                while (p2 < text.length && text[p2].isWhitespace()) p2++
                val b2 = extractBalancedBrace(text, p2)
                if (b2 != null) {
                    val innerA = formatMathAndLatexInternal(b1.first.trim(), depth + 1)
                    val innerB = formatMathAndLatexInternal(b2.first.trim(), depth + 1)
                    text = text.substring(0, idx) + "⟨$innerA | $innerB⟩" + text.substring(b2.second)
                } else {
                    val innerA = formatMathAndLatexInternal(b1.first.trim(), depth + 1)
                    text = text.substring(0, idx) + "⟨$innerA⟩" + text.substring(b1.second)
                }
            } else {
                break
            }
            braketSafety++
        }

        var braSafety = 0
        while (text.contains("\\bra") && braSafety < 10) {
            val idx = text.indexOf("\\bra")
            var pos = idx + 4
            while (pos < text.length && text[pos].isWhitespace()) pos++
            val b = extractBalancedBrace(text, pos)
            if (b != null) {
                val inner = formatMathAndLatexInternal(b.first.trim(), depth + 1)
                text = text.substring(0, idx) + "⟨$inner|" + text.substring(b.second)
            } else {
                break
            }
            braSafety++
        }

        var ketSafety = 0
        while (text.contains("\\ket") && ketSafety < 10) {
            val idx = text.indexOf("\\ket")
            var pos = idx + 4
            while (pos < text.length && text[pos].isWhitespace()) pos++
            val b = extractBalancedBrace(text, pos)
            if (b != null) {
                val inner = formatMathAndLatexInternal(b.first.trim(), depth + 1)
                text = text.substring(0, idx) + "|$inner⟩" + text.substring(b.second)
            } else {
                break
            }
            ketSafety++
        }

        // 5. Common natural language and chemistry Delta shortcuts
        text = text.replace(Regex("""\bproportional to\b""", RegexOption.IGNORE_CASE), "∝")
        text = text.replace(Regex("""\bdelta_([a-zA-Z0-9]+)\b"""), "Δ$1")
        text = text.replace(Regex("""\bdelta([a-zA-Z])\b"""), "Δ$1")
        text = text.replace(Regex("""\\Delta\s*([a-zA-Z0-9])"""), "Δ$1")
        text = text.replace(Regex("""\\Delta(?![a-zA-Z])"""), "Δ")
        text = text.replace(Regex("""\\delta(?![a-zA-Z])"""), "δ")

        // 6. Vector arrows and unit hats: \vec{OA}, \vec{OD}, \vec{v}, \hat{i}, \bar{x}, \dot{x}, \ddot{x}
        text = text.replace(Regex("""\\hat\{i\}|\\hat\s+i\b"""), "î")
        text = text.replace(Regex("""\\hat\{j\}|\\hat\s+j\b"""), "ĵ")
        text = text.replace(Regex("""\\hat\{k\}|\\hat\s+k\b"""), "k̂")
        text = text.replace(Regex("""\\hat\{n\}|\\hat\s+n\b"""), "n̂")
        text = text.replace(Regex("""\\hat\{r\}|\\hat\s+r\b"""), "r̂")

        var vecSafety = 0
        while (text.contains("\\vec") && vecSafety < 15) {
            val vecIdx = text.indexOf("\\vec")
            var pos = vecIdx + 4
            while (pos < text.length && text[pos].isWhitespace()) pos++
            val braceResult = extractBalancedBrace(text, pos)
            if (braceResult != null) {
                val inner = braceResult.first.trim()
                text = text.substring(0, vecIdx) + "${inner}⃗" + text.substring(braceResult.second)
            } else if (pos < text.length && text[pos].isLetterOrDigit()) {
                text = text.substring(0, vecIdx) + "${text[pos]}⃗" + text.substring(pos + 1)
            } else {
                break
            }
            vecSafety++
        }

        var barSafety = 0
        while (text.contains("\\bar") && barSafety < 15) {
            val barIdx = text.indexOf("\\bar")
            var pos = barIdx + 4
            while (pos < text.length && text[pos].isWhitespace()) pos++
            val braceResult = extractBalancedBrace(text, pos)
            if (braceResult != null) {
                val inner = braceResult.first.trim()
                text = text.substring(0, barIdx) + "${inner}̄" + text.substring(braceResult.second)
            } else if (pos < text.length && text[pos].isLetterOrDigit()) {
                text = text.substring(0, barIdx) + "${text[pos]}̄" + text.substring(pos + 1)
            } else {
                break
            }
            barSafety++
        }

        var hatSafety = 0
        while (text.contains("\\hat") && hatSafety < 15) {
            val hatIdx = text.indexOf("\\hat")
            var pos = hatIdx + 4
            while (pos < text.length && text[pos].isWhitespace()) pos++
            val braceResult = extractBalancedBrace(text, pos)
            if (braceResult != null) {
                val inner = braceResult.first.trim()
                text = text.substring(0, hatIdx) + "${inner}̂" + text.substring(braceResult.second)
            } else if (pos < text.length && text[pos].isLetterOrDigit()) {
                text = text.substring(0, hatIdx) + "${text[pos]}̂" + text.substring(pos + 1)
            } else {
                break
            }
            hatSafety++
        }

        text = text.replace(Regex("""\\ddot\{\s*([^{}]+)\s*\}""")) { "${it.groupValues[1]}̈" }
        text = text.replace(Regex("""\\dot\{\s*([^{}]+)\s*\}""")) { "${it.groupValues[1]}̇" }

        // 7. Chemical reaction arrows with conditions: \xrightarrow[below]{above} -> ──(above/below)──>
        val rxArrowRegex = Regex("""\\(?:xrightarrow|xrightleftharpoons|xleftarrow)\[\s*([^\]]*)\s*\]\{\s*([^}]*)\s*\}""")
        text = text.replace(rxArrowRegex) { match ->
            val below = match.groupValues[1].trim()
            val above = match.groupValues[2].trim()
            val condition = if (below.isNotEmpty() && above.isNotEmpty()) "$above / $below" else above.ifEmpty { below }
            val arrow = if (match.value.contains("xrightleftharpoons")) " ──($condition)⇌ " else if (match.value.contains("xleftarrow")) " <──($condition)── " else " ──($condition)──> "
            arrow
        }
        text = text.replace(Regex("""\\xrightarrow\{\s*([^}]+)\s*\}""")) { " ──(${it.groupValues[1]})──> " }
        text = text.replace(Regex("""\\xleftarrow\{\s*([^}]+)\s*\}""")) { " <──(${it.groupValues[1]})── " }
        text = text.replace(Regex("""\\xrightleftharpoons\{\s*([^}]+)\s*\}""")) { " ──(${it.groupValues[1]})⇌ " }
        text = text.replace("<=>", " ⇌ ")
        text = text.replace("<==>", " ⇌ ")
        text = text.replace("=>", " ⇒ ")
        text = text.replace("==>", " ⇒ ")
        text = text.replace("<->", " ↔ ")
        text = text.replace(Regex("""(?<=\s|^)(?:->|-->)(?=\s|$)"""), " → ")

        // 8. Fractions: \frac{num}{den}, \dfrac{num}{den}, \tfrac{num}{den}
        val fracKeywords = listOf("\\frac", "\\dfrac", "\\tfrac")
        var fracSafety = 0
        while (fracKeywords.any { text.contains(it) } && fracSafety < 8) {
            var found = false
            for (kw in fracKeywords) {
                var idx = text.indexOf(kw)
                while (idx != -1) {
                    var pos = idx + kw.length
                    while (pos < text.length && text[pos].isWhitespace()) pos++
                    val numResult = extractBalancedBrace(text, pos)
                    if (numResult != null) {
                        var denPos = numResult.second
                        while (denPos < text.length && text[denPos].isWhitespace()) denPos++
                        val denResult = extractBalancedBrace(text, denPos)
                        if (denResult != null) {
                            val num = formatMathAndLatexInternal(numResult.first.trim(), depth + 1)
                            val den = formatMathAndLatexInternal(denResult.first.trim(), depth + 1)
                            val replacement = if (den.length <= 2 && num.length <= 2 && !den.contains(" ") && !num.contains(" ")) {
                                "$num/$den"
                            } else {
                                "($num ÷ $den)"
                            }
                            text = text.substring(0, idx) + replacement + text.substring(denResult.second)
                            found = true
                            break
                        }
                    }
                    idx = text.indexOf(kw, idx + kw.length)
                }
                if (found) break
            }
            if (!found) break
            fracSafety++
        }
        text = text.replace(Regex("""\\(?:frac|dfrac|tfrac)\s*(\d)\s*(\d)""")) { "(${it.groupValues[1]} ÷ ${it.groupValues[2]})" }

        // 9. Square roots: \sqrt{x}, \sqrt[3]{x}, \sqrt[n]{x}
        val nthRootRegex = Regex("""\\sqrt\[([0-9a-zA-Z]+)\]""")
        var nthMatch = nthRootRegex.find(text)
        var safetyNth = 0
        while (nthMatch != null && safetyNth < 10) {
            val n = nthMatch.groupValues[1]
            val bracePos = nthMatch.range.last + 1
            var pos = bracePos
            while (pos < text.length && text[pos].isWhitespace()) pos++
            val braceResult = extractBalancedBrace(text, pos)
            if (braceResult != null) {
                val inner = formatMathAndLatexInternal(braceResult.first.trim(), depth + 1)
                val prefix = when (n) {
                    "3" -> "∛"
                    "4" -> "∜"
                    else -> "${toSuperscript(n)}√"
                }
                text = text.substring(0, nthMatch.range.first) + "$prefix($inner)" + text.substring(braceResult.second)
                nthMatch = nthRootRegex.find(text)
            } else {
                break
            }
            safetyNth++
        }

        var sqrtSafety = 0
        while (text.contains("\\sqrt{") && sqrtSafety < 10) {
            val sqrtIdx = text.indexOf("\\sqrt{")
            val bracePos = sqrtIdx + 5
            val braceResult = extractBalancedBrace(text, bracePos)
            if (braceResult != null) {
                val inner = formatMathAndLatexInternal(braceResult.first.trim(), depth + 1)
                text = text.substring(0, sqrtIdx) + "√($inner)" + text.substring(braceResult.second)
            } else {
                break
            }
            sqrtSafety++
        }
        text = text.replace(Regex("""\bsqrt\(\s*([^)]+)\s*\)""")) { "√(${formatMathAndLatexInternal(it.groupValues[1].trim(), depth + 1)})" }
        text = text.replace(Regex("""\\sqrt\s*([0-9a-zA-Z])\b""")) { "√(${it.groupValues[1]})" }
        text = text.replace("\\sqrt", "√")

        // 10. Replace all tokenized LaTeX symbols from the master dictionary
        for ((cmd, replacement) in LATEX_REPLACEMENTS) {
            if (text.contains(cmd)) {
                val escapedCmd = Regex.escape(cmd)
                text = text.replace(Regex("""$escapedCmd(?![a-zA-Z])"""), replacement)
            }
        }

        // 11. Convert raw asterisks (*) to multiplication (×) when between numbers or algebraic variables
        text = text.replace(Regex("""(?<=[\d\)\]\}\w°ΔΩπθ])\s*\*\s*(?=[\d\(\[\{\w√πθΔ])"""), " × ")
        text = text.replace(Regex("""(?<=[\d\)\]\}])\s+\*\s+(?=[\d\(\[\{])"""), " × ")

        // 12. Convert slash (/) to division (÷) when between isolated numbers with spaces
        text = text.replace(Regex("""(?<=\b\d{1,4})\s*/\s*(?=\d{1,4}\b)"""), " ÷ ")

        // 13. Convert Superscripts (powers) e.g., ^2 -> ², ^(-1) -> ⁻¹, K^-1 -> K⁻¹, 10^5 -> 10⁵, 10^-34 -> 10⁻³⁴, Fe^{3+} -> Fe³⁺
        text = text.replace(Regex("""\^\(\s*([^)]+)\s*\)""")) { matchResult ->
            toSuperscript(matchResult.groupValues[1])
        }
        text = text.replace(Regex("""\^\{\s*([^}]+)\s*\}""")) { matchResult ->
            toSuperscript(matchResult.groupValues[1])
        }
        text = text.replace(Regex("""\^([\-+]?[0-9a-zA-Z\/\.]+)\b""")) { matchResult ->
            toSuperscript(matchResult.groupValues[1])
        }

        // 14. Convert Subscripts e.g., _1 -> ₁, _{eq} -> ₑᵩ, _0 -> ₀, H_2O -> H₂O, v_{rms} -> vᵣₘₛ, P_{PCl5} -> P_PCl₅, K_p -> Kₚ
        text = text.replace(Regex("""_\{([0-9a-zA-Z\+\-]+)\}""")) { matchResult ->
            toSubscript(matchResult.groupValues[1])
        }
        text = text.replace(Regex("""_([0-9a-zA-Z]+)\b""")) { matchResult ->
            val sub = matchResult.groupValues[1]
            if (sub.length == 1 && (sub[0].isDigit() || sub == "p" || sub == "c" || sub == "a" || sub == "b" || sub == "e" || sub == "i" || sub == "f" || sub == "t" || sub == "n" || sub == "m" || sub == "x" || sub == "y" || sub == "z" || sub == "o")) {
                toSubscript(sub)
            } else {
                "_${sub}"
            }
        }

        // 15. Apply Scientific and Chemical Formulas Auto-Subscripting
        text = formatScienceAndMathInText(text)

        // 16. Clean up multiple spaces and empty brackets
        text = text.replace(Regex("""[ \t]{2,}"""), " ")
        text = text.replace("{}", "")

        return text
    }

    /**
     * Dedicated Chemistry mhchem formatter that handles formulas, reaction arrows,
     * gas/precipitation markers, charges, bond symbols, and stoichiometric coefficients.
     */
    fun formatChemistry(input: String): String {
        if (input.isBlank()) return input
        var text = input.trim()

        // 0. Unwrap mhchem \ce{...} if nested
        if (text.startsWith("\\ce{") && text.endsWith("}")) {
            text = text.substring(4, text.length - 1).trim()
        }

        // 1. Reaction arrows & Equilibrium
        text = text.replace(Regex("""\\rightleftharpoons|\\leftrightharpoons|<==>|<=>"""), " ⇌ ")
        text = text.replace(Regex("""\\longleftrightarrow|\\leftrightarrow|<->"""), " ↔ ")
        text = text.replace(Regex("""\\longrightarrow|\\rightarrow|-->|->"""), " → ")
        text = text.replace(Regex("""\\longleftarrow|\\leftarrow|<--|<-"""), " ← ")

        // 2. Electrochemistry Galvanic Cell Notations (Salt Bridge)
        text = text.replace(Regex("""(?<=\S)\s*(?:\|\||//|\\parallel)\s*(?=\S)"""), " ‖ ")

        // 3. Reaction conditions in brackets: ->[heat] -> ──(heat)──>
        text = text.replace(Regex("""->\[\s*([^\]]+)\s*\]""")) { " ──(${it.groupValues[1]})──> " }

        // 4. Gas evolution (^) and precipitate (v)
        text = text.replace(Regex("""(?<=\w|\))\s*\^\b"""), " ↑")
        text = text.replace(Regex("""(?<=\w|\))\s*v\b"""), " ↓")

        // 5. Chemical bond notations & radicals
        text = text.replace("#", "≡")
        text = text.replace(Regex("""(?<=\w)\s*[\*•]\b"""), "•")

        // 6. Electrons in half-reactions: 2e-, e-, e^- -> 2e⁻, e⁻
        text = text.replace(Regex("""(?<=\s|^|\+)(\d*)\s*e\s*[\^]?\s*-(?=\s|$|[,\.\+\)]|->|→)""")) { match ->
            val coeff = match.groupValues[1]
            "${coeff}e⁻"
        }
        text = text.replace(Regex("""\be\s*-\b"""), "e⁻")

        // 7. Protect physical states of matter: (s), (l), (g), (aq), (ppt), (dil), (conc)
        val statePlaceholders = mutableMapOf<String, String>()
        var stateIdx = 0
        text = text.replace(Regex("""\((s|l|g|aq|ppt|dil|conc)\)""", RegexOption.IGNORE_CASE)) { match ->
            val placeholder = "§STATE_${stateIdx++}§"
            statePlaceholders[placeholder] = match.value.lowercase()
            placeholder
        }

        // 8. Superscript charges: Fe^{3+} -> Fe³⁺, Fe3+ -> Fe³⁺, SO4^{2-} -> SO₄²⁻, Zn2+ -> Zn²⁺
        text = text.replace(Regex("""\^\{\s*([^}]+)\s*\}""")) { toSuperscript(it.groupValues[1]) }
        text = text.replace(Regex("""\^([0-9\+\-]+)""")) { toSuperscript(it.groupValues[1]) }
        text = text.replace(Regex("""\b([A-Za-z\(\)\[\]]+)(\d*[\+\-])\b""")) { match ->
            val formula = match.groupValues[1]
            val charge = match.groupValues[2]
            if (charge.isNotEmpty() && formula != "pH" && formula != "pOH") {
                "$formula${toSuperscript(charge)}"
            } else {
                match.value
            }
        }

        // 9. Subscripts for atoms and complex brackets: H2SO4 -> H₂SO₄, [Fe(CN)6] -> [Fe(CN)₆]
        text = text.replace(Regex("""([A-Z][a-z]?|\)|\])(\d+)""")) { match ->
            "${match.groupValues[1]}${toSubscript(match.groupValues[2])}"
        }

        // 10. Restore states of matter
        for ((placeholder, original) in statePlaceholders) {
            text = text.replace(placeholder, original)
        }

        return text
    }

    fun toSuperscript(str: String): String {
        return str.map { char ->
            when (char) {
                '0' -> '⁰'
                '1' -> '¹'
                '2' -> '²'
                '3' -> '³'
                '4' -> '⁴'
                '5' -> '⁵'
                '6' -> '⁶'
                '7' -> '⁷'
                '8' -> '⁸'
                '9' -> '⁹'
                '+' -> '⁺'
                '-' -> '⁻'
                '=' -> '⁼'
                '(' -> '⁽'
                ')' -> '⁾'
                '/' -> 'ᐟ'
                '÷' -> 'ᐟ'
                '.' -> '˙'
                'a', 'A' -> 'ᵃ'
                'b', 'B' -> 'ᵇ'
                'c', 'C' -> 'ᶜ'
                'd', 'D' -> 'ᵈ'
                'e', 'E' -> 'ᵉ'
                'f', 'F' -> 'ᶠ'
                'g', 'G' -> 'ᵍ'
                'h', 'H' -> 'ʰ'
                'i', 'I' -> 'ⁱ'
                'j', 'J' -> 'ʲ'
                'k', 'K' -> 'ᵏ'
                'l', 'L' -> 'ˡ'
                'm', 'M' -> 'ᵐ'
                'n', 'N' -> 'ⁿ'
                'o', 'O' -> 'ᵒ'
                'p', 'P' -> 'ᵖ'
                'r', 'R' -> 'ʳ'
                's', 'S' -> 'ˢ'
                't', 'T' -> 'ᵗ'
                'u', 'U' -> 'ᵘ'
                'v', 'V' -> 'ᵛ'
                'w', 'W' -> 'ʷ'
                'x', 'X' -> 'ˣ'
                'y', 'Y' -> 'ʸ'
                'z', 'Z' -> 'ᶻ'
                '°' -> '°'
                '⦵' -> '⦵'
                '‡' -> '‡'
                '†' -> '†'
                '*' -> '﹡'
                '•' -> '•'
                else -> char
            }
        }.joinToString("")
    }

    fun toSubscript(str: String): String {
        return str.map { char ->
            when (char) {
                '0' -> '₀'
                '1' -> '₁'
                '2' -> '₂'
                '3' -> '₃'
                '4' -> '₄'
                '5' -> '₅'
                '6' -> '₆'
                '7' -> '₇'
                '8' -> '₈'
                '9' -> '₉'
                '+' -> '₊'
                '-' -> '₋'
                '=' -> '₌'
                '(' -> '₍'
                ')' -> '₎'
                'a', 'A' -> 'ₐ'
                'b', 'B' -> 'ᵦ'
                'c', 'C' -> '꜀'
                'e', 'E' -> 'ₑ'
                'g', 'G' -> 'ᵧ'
                'h', 'H' -> 'ₕ'
                'i', 'I' -> 'ᵢ'
                'j', 'J' -> 'ⱼ'
                'k', 'K' -> 'ₖ'
                'l', 'L' -> 'ₗ'
                'm', 'M' -> 'ₘ'
                'n', 'N' -> 'ₙ'
                'o', 'O' -> 'ₒ'
                'p', 'P' -> 'ₚ'
                'r', 'R' -> 'ᵣ'
                's', 'S' -> 'ₛ'
                't', 'T' -> 'ₜ'
                'u', 'U' -> 'ᵤ'
                'v', 'V' -> 'ᵥ'
                'x', 'X' -> 'ₓ'
                'q', 'Q' -> 'ᵩ'
                '°' -> '°'
                '⦵' -> '⦵'
                '½' -> '½'
                else -> char
            }
        }.joinToString("")
    }

    /**
     * Checks if a string contains LaTeX math expressions or MathJax syntax.
     */
    fun hasLatexMath(text: String): Boolean {
        if (text.isBlank()) return false
        val latexPatterns = listOf(
            "\\frac", "\\dfrac", "\\tfrac", "\\cfrac", "\\sqrt", "\\int", "\\iint", "\\iiint", "\\oint",
            "\\sum", "\\prod", "\\lim", "\\alpha", "\\beta", "\\gamma", "\\Gamma", "\\delta", "\\Delta",
            "\\epsilon", "\\varepsilon", "\\zeta", "\\eta", "\\theta", "\\Theta", "\\iota", "\\kappa",
            "\\lambda", "\\Lambda", "\\mu", "\\nu", "\\xi", "\\Xi", "\\pi", "\\Pi", "\\rho", "\\sigma",
            "\\Sigma", "\\tau", "\\upsilon", "\\phi", "\\Phi", "\\chi", "\\psi", "\\Psi", "\\omega",
            "\\Omega", "\\partial", "\\nabla", "\\degree", "\\times", "\\div", "\\cdot", "\\pm", "\\mp",
            "\\approx", "\\neq", "\\leq", "\\geq", "\\le", "\\ge", "\\equiv", "\\propto", "\\infty",
            "\\vec", "\\hat", "\\overrightarrow", "\\to", "\\rightarrow", "\\longrightarrow", "\\Rightarrow",
            "\\rightleftharpoons", "\\ce{", "\\text{", "\\mathrm{", "\\mathbf{", "\\mathit{", "\\mathcal{",
            "$$", "\\[", "\\]", "\\(", "^{", "_{", "\\circ"
        )
        if (latexPatterns.any { pattern -> text.contains(pattern) }) return true
        if (text.contains(Regex("""(?<!\\)\$[^$]+\$"""))) return true
        return false
    }

    /**
     * Checks whether the text contains substantial English narrative/question sentences.
     * When text is an English sentence or question statement, native Compose Text should be used
     * so that the app stays fast, responsive, and uses native Android typography.
     */
    fun hasEnglishNarrative(text: String): Boolean {
        val s = text.trim()
        if (s.isBlank()) return false
        val words = s.split(Regex("""\s+"""))
        if (words.size >= 4) {
            val commonWords = setOf(
                "the", "a", "an", "is", "are", "was", "were", "of", "in", "to", "for", "with",
                "on", "at", "by", "from", "which", "what", "find", "calculate", "determine",
                "given", "following", "statement", "statements", "correct", "incorrect", "true",
                "false", "particle", "electron", "proton", "body", "mass", "velocity", "speed",
                "acceleration", "force", "energy", "power", "work", "current", "voltage",
                "resistance", "field", "radius", "distance", "time", "ratio", "magnitude",
                "value", "when", "where", "if", "then", "consider", "assume", "suppose",
                "according", "between", "increases", "decreases", "remains", "constant", "both",
                "neither", "among", "reaction", "compound", "temperature", "pressure", "mole",
                "moles", "concentration", "equilibrium", "volume", "density", "wavelength",
                "frequency", "intensity", "potential", "charge", "capacitance", "inductance",
                "surface", "motion", "circle", "orbit", "path", "angle", "horizontal", "vertical",
                "plane", "block", "system", "collision", "spring", "rod", "disc", "sphere"
            )
            val matchCount = words.count { w ->
                val cleanWord = w.lowercase().trim('.', ',', '?', ':', ';', '!', '(', ')', '"', '\'')
                commonWords.contains(cleanWord)
            }
            if (matchCount >= 2) return true
        }
        return false
    }

    /**
     * Checks if the text contains complex formulas that benefit from 2D graphical rendering
     * (e.g. square roots with radicands, vertical fractions, physics vector hats/arrows, chemistry mhchem).
     */
    fun containsComplexFormula(text: String): Boolean {
        val s = text.trim()
        if (s.isBlank()) return false

        // 1. Square roots: \sqrt{...}, \sqrt[...]
        if (s.contains("\\sqrt{") || s.contains("\\sqrt[")) return true

        // 2. 2D Vertical fractions: \frac{...}{...}, \dfrac, \cfrac, \tfrac
        if (s.contains("\\frac{") || s.contains("\\dfrac{") || s.contains("\\cfrac{") || s.contains("\\tfrac{")) return true

        // 3. Physics vector arrows, unit hats: \vec{...}, \hat{...}, \overrightarrow{...}
        if (s.contains("\\vec{") || s.contains("\\hat{") || s.contains("\\overrightarrow{") ||
            s.contains("\\vec ") || s.contains("\\hat i") || s.contains("\\hat j") || s.contains("\\hat k")) return true

        // 4. Chemistry reactions / mhchem
        if (s.contains("\\ce{") || s.contains("\\chemfig{") || s.contains("\\rightleftharpoons") || s.contains("\\leftrightharpoons")) return true

        // 5. Matrices, determinants, systems
        if (s.contains("\\begin{matrix}") || s.contains("\\begin{pmatrix}") || s.contains("\\begin{bmatrix}") ||
            s.contains("\\begin{vmatrix}") || s.contains("\\begin{array}") || s.contains("\\begin{aligned}")) return true

        // 6. Calculus with limits / bounds
        if (s.contains("\\int_") || s.contains("\\int^") || s.contains("\\sum_") || s.contains("\\sum^") ||
            s.contains("\\prod_") || s.contains("\\prod^") || s.contains("\\lim_") || s.contains("\\oint")) return true

        // 7. Binomials
        if (s.contains("\\binom{")) return true

        return false
    }

    /**
     * True ONLY for standalone, complex 2D formulas (e.g. MCQ options or single equations)
     * that require full graphical 2D math layout (MathJax 3).
     *
     * Returns FALSE for English sentences, question statements, reading passages,
     * and normal values/units so that they render instantly with 0 lag using native Compose typography.
     */
    fun isComplexMathOrChemistry(text: String): Boolean {
        val s = text.trim()
        if (s.isBlank()) return false

        // CRITICAL HYBRID RULE: If it contains English sentences/narrative, do NOT treat as a standalone complex formula!
        // English sentences must be rendered natively with Compose Text for zero lag and crisp native typography.
        if (hasEnglishNarrative(s)) return false

        // If it's a long text (e.g. > 250 chars or has multiple line breaks), it's not a standalone option/formula
        if (s.length > 250 || s.lines().size > 3) return false

        return containsComplexFormula(s)
    }

    /**
     * Determines if a LaTeX formula should use KaTeX/MathJax rendering.
     */
    fun isComplexLatexFormula(latex: String): Boolean {
        if (latex.isBlank()) return false
        return containsComplexFormula(latex)
    }

    /**
     * Robust JSON sanitizer and repair utility for LLM responses.
     * Fixes invalid escapes, unescaped LaTeX backslashes, unclosed code fences,
     * trailing commas, and truncated brackets.
     */
    fun sanitizeAndRepairJson(raw: String): String {
        var clean = raw.trim()

        // Strip markdown code fences if present
        if (clean.startsWith("```json", ignoreCase = true)) {
            clean = clean.substringAfter("```json").trim()
        } else if (clean.startsWith("```")) {
            clean = clean.substringAfter("```").trim()
        }
        if (clean.endsWith("```")) {
            clean = clean.substringBeforeLast("```").trim()
        }

        // Find outer boundary
        val firstBrace = clean.indexOf('{')
        val firstBracket = clean.indexOf('[')
        val isArray = when {
            firstBracket != -1 && firstBrace != -1 -> firstBracket < firstBrace
            firstBracket != -1 -> true
            else -> false
        }

        val startIdx = if (isArray) firstBracket else firstBrace
        if (startIdx != -1) {
            clean = clean.substring(startIdx)
        }

        // Fix unescaped backslashes inside JSON string values that break org.json parser
        val sb = StringBuilder(clean.length + 64)
        var inString = false
        var i = 0
        while (i < clean.length) {
            val c = clean[i]
            if (c == '"' && (i == 0 || clean[i - 1] != '\\')) {
                inString = !inString
                sb.append(c)
                i++
                continue
            }

            if (inString && c == '\\') {
                val next = if (i + 1 < clean.length) clean[i + 1] else ' '
                if (next == '"' || next == '\\' || next == '/' || next == 'b' || next == 'f' || next == 'n' || next == 'r' || next == 't') {
                    sb.append(c)
                    sb.append(next)
                    i += 2
                    continue
                } else if (next == 'u' && i + 5 < clean.length) {
                    sb.append(c)
                    sb.append(next)
                    i += 2
                    continue
                } else {
                    // Unescaped LaTeX backslash (e.g. \Delta, \alpha, \frac) -> escape it to \\
                    sb.append("\\\\")
                    i++
                    continue
                }
            } else {
                sb.append(c)
                i++
            }
        }
        var processed = sb.toString().trim()

        // Remove trailing commas before closing braces/brackets
        processed = processed.replace(Regex(""",\s*([\]\}])"""), "$1")

        // Auto-close open JSON structure if truncated
        if (isArray) {
            val openCount = processed.count { it == '[' }
            val closeCount = processed.count { it == ']' }
            if (openCount > closeCount) {
                // If it ended abruptly in an object, close the object first
                val openBraces = processed.count { it == '{' }
                val closeBraces = processed.count { it == '}' }
                if (openBraces > closeBraces) {
                    processed += "}".repeat(openBraces - closeBraces)
                }
                processed += "]".repeat(openCount - closeCount)
            }
        } else {
            val openCount = processed.count { it == '{' }
            val closeCount = processed.count { it == '}' }
            if (openCount > closeCount) {
                processed += "}".repeat(openCount - closeCount)
            }
        }

        return processed.trim()
    }
}
