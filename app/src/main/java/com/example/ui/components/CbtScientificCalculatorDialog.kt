package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale
import kotlin.math.*

/**
 * NTA / JEE CBT Virtual Scientific Calculator
 * Specifically tailored for Numerical Value Type (NAT) / Fill-in-the-blank questions
 * with instant "Paste to Answer" support, trigonometric functions, roots, powers, logs, and constants.
 */
@Composable
fun CbtScientificCalculatorDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onUseAnswer: (String) -> Unit,
    isDark: Boolean = true
) {
    if (!isOpen) return

    var expression by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("0") }
    var isDegreeMode by remember { mutableStateOf(true) }
    var lastEvaluated by remember { mutableStateOf(false) }

    fun computeResult(expr: String): String {
        if (expr.isBlank()) return "0"
        return try {
            val eval = MathExpressionParser.eval(expr, isDegreeMode)
            if (eval.isNaN() || eval.isInfinite()) {
                "Error"
            } else {
                // Strip unnecessary trailing decimals (e.g. 25.0 -> 25)
                if (eval % 1.0 == 0.0 && abs(eval) < 1e12) {
                    eval.toLong().toString()
                } else {
                    String.format(Locale.US, "%.6f", eval).trimEnd('0').trimEnd('.')
                }
            }
        } catch (_: Exception) {
            "..."
        }
    }

    fun onKeyClick(key: String) {
        when (key) {
            "AC" -> {
                expression = ""
                resultText = "0"
                lastEvaluated = false
            }
            "⌫" -> {
                if (expression.isNotEmpty()) {
                    expression = expression.dropLast(1)
                    val res = computeResult(expression)
                    if (res != "...") resultText = res
                } else {
                    resultText = "0"
                }
                lastEvaluated = false
            }
            "=" -> {
                if (expression.isNotBlank()) {
                    val evaluated = computeResult(expression)
                    if (evaluated != "..." && evaluated != "Error") {
                        resultText = evaluated
                        expression = evaluated
                        lastEvaluated = true
                    }
                }
            }
            "DEG", "RAD" -> {
                isDegreeMode = !isDegreeMode
                if (expression.isNotBlank()) {
                    val res = computeResult(expression)
                    if (res != "...") resultText = res
                }
            }
            "+/-" -> {
                if (expression.isNotBlank()) {
                    if (expression.startsWith("-")) {
                        expression = expression.removePrefix("-")
                    } else {
                        expression = "-($expression)"
                    }
                    val res = computeResult(expression)
                    if (res != "...") resultText = res
                }
            }
            "√" -> {
                expression = if (lastEvaluated) "sqrt($expression)" else expression + "sqrt("
                lastEvaluated = false
                val res = computeResult(expression + ")")
                if (res != "...") resultText = res
            }
            "x²" -> {
                expression = if (lastEvaluated) "($expression)^2" else expression + "^2"
                lastEvaluated = false
                val res = computeResult(expression)
                if (res != "...") resultText = res
            }
            "x³" -> {
                expression = if (lastEvaluated) "($expression)^3" else expression + "^3"
                lastEvaluated = false
                val res = computeResult(expression)
                if (res != "...") resultText = res
            }
            "1/x" -> {
                expression = if (lastEvaluated) "1/($expression)" else expression + "1/("
                lastEvaluated = false
            }
            "sin", "cos", "tan", "log", "ln" -> {
                expression = expression + "$key("
                lastEvaluated = false
            }
            "π" -> {
                expression = expression + "pi"
                lastEvaluated = false
                val res = computeResult(expression)
                if (res != "...") resultText = res
            }
            "e" -> {
                expression = expression + "e"
                lastEvaluated = false
                val res = computeResult(expression)
                if (res != "...") resultText = res
            }
            "^" -> {
                expression = expression + "^"
                lastEvaluated = false
            }
            "×" -> {
                expression = expression + "*"
                lastEvaluated = false
            }
            "÷" -> {
                expression = expression + "/"
                lastEvaluated = false
            }
            else -> {
                if (lastEvaluated && (key.all { it.isDigit() } || key == ".")) {
                    expression = key
                    lastEvaluated = false
                } else {
                    expression = expression + key
                    lastEvaluated = false
                }
                val res = computeResult(expression)
                if (res != "...") resultText = res
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
            border = BorderStroke(1.5.dp, if (isDark) Color(0xFF6366F1).copy(alpha = 0.5f) else Color(0xFFCBD5E1)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.15f)
                        ) {
                            Text(
                                "🧮 NTA / JEE VIRTUAL CALCULATOR",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF6366F1),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Degree / Radian Toggle
                        Surface(
                            onClick = { onKeyClick(if (isDegreeMode) "DEG" else "RAD") },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDegreeMode) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFF6366F1).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, if (isDegreeMode) Color(0xFF10B981) else Color(0xFF6366F1))
                        ) {
                            Text(
                                if (isDegreeMode) "DEG Mode" else "RAD Mode",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDegreeMode) Color(0xFF10B981) else Color(0xFF6366F1),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDark) Color.White else Color.Black, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Digital Display Screen
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF),
                    border = BorderStroke(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Formula Expression with horizontal scroll
                        val scrollState = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = if (expression.isBlank()) "0" else expression,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isDark) Color(0x99FFFFFF) else Color(0xFF64748B),
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Large Evaluated Result Display
                        Text(
                            text = resultText,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (resultText == "Error") Color(0xFFEF4444) else if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            textAlign = TextAlign.End,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scientific Keypad Matrix
                val row1 = listOf("sin", "cos", "tan", "log", "ln")
                val row2 = listOf("√", "x²", "x³", "^", "1/x")
                val row3 = listOf("(", ")", "π", "e", "%")
                val row4 = listOf("7", "8", "9", "÷", "⌫")
                val row5 = listOf("4", "5", "6", "×", "AC")
                val row6 = listOf("1", "2", "3", "-", "+")
                val row7 = listOf("+/-", "0", ".", "=")

                val keypad = listOf(row1, row2, row3, row4, row5, row6)

                keypad.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowKeys.forEach { key ->
                            CalculatorKeyButton(
                                text = key,
                                onClick = { onKeyClick(key) },
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Bottom Row: [+/-] [0] [.] [=] and [USE AS ANSWER]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    row7.forEach { key ->
                        CalculatorKeyButton(
                            text = key,
                            onClick = { onKeyClick(key) },
                            isDark = isDark,
                            modifier = Modifier.weight(if (key == "=") 1.5f else 1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary One-Tap "Use as Answer" Button
                Button(
                    onClick = {
                        val finalAnswer = if (resultText != "Error" && resultText != "0" && resultText != "...") {
                            resultText
                        } else if (expression.isNotBlank() && expression.toDoubleOrNull() != null) {
                            expression
                        } else {
                            resultText
                        }
                        onUseAnswer(finalAnswer)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("calculator_use_answer_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text(
                        "🎯 Insert As Answer ($resultText)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CalculatorKeyButton(
    text: String,
    onClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val isOp = text in listOf("÷", "×", "-", "+", "=", "^", "%")
    val isSci = text in listOf("sin", "cos", "tan", "log", "ln", "√", "x²", "x³", "1/x", "π", "e", "(", ")")
    val isSpecial = text in listOf("AC", "⌫")

    val bg = when {
        text == "=" -> Color(0xFF6366F1)
        isSpecial -> if (text == "AC") Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f)
        isOp -> Color(0xFF6366F1).copy(alpha = if (isDark) 0.25f else 0.12f)
        isSci -> if (isDark) Color(0xFF1E293B) else Color(0xFFEEF2F6)
        else -> if (isDark) Color(0xFF334155) else Color.White
    }

    val textColor = when {
        text == "=" -> Color.White
        text == "AC" -> Color(0xFFEF4444)
        text == "⌫" -> Color(0xFFF59E0B)
        isOp -> Color(0xFF6366F1)
        isSci -> if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
        else -> if (isDark) Color.White else Color(0xFF0F172A)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, if (isDark) Color(0x1AFFFFFF) else Color(0xFFE2E8F0)),
        modifier = modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = if (text.length > 2) 11.sp else 13.5.sp,
                fontWeight = if (text.all { it.isDigit() } || text == "=") FontWeight.Bold else FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

/**
 * Robust zero-dependency Mathematical Expression Evaluator for CBT.
 * Evaluates arithmetic, trigonometric (deg/rad), power, sqrt, log10, ln, and constants.
 */
object MathExpressionParser {

    fun eval(expr: String, isDegreeMode: Boolean): Double {
        val sanitized = expr.replace(" ", "")
            .replace("pi", Math.PI.toString())
            .replace("e", Math.E.toString())

        return Parser(sanitized, isDegreeMode).parse()
    }

    private class Parser(val str: String, val isDegree: Boolean) {
        var pos = -1
        var ch = 0

        fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x += parseTerm()
                else if (eat('-'.code)) x -= parseTerm()
                else return x
            }
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x *= parseFactor()
                else if (eat('/'.code)) x /= parseFactor()
                else if (eat('%'.code)) x %= parseFactor()
                else return x
            }
        }

        fun parseFactor(): Double {
            if (eat('+'.code)) return +parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else if (ch >= 'a'.code && ch <= 'z'.code) {
                while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                val func = str.substring(startPos, pos)
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else {
                    x = parseFactor()
                }
                x = when (func) {
                    "sqrt" -> sqrt(x)
                    "sin" -> sin(if (isDegree) Math.toRadians(x) else x)
                    "cos" -> cos(if (isDegree) Math.toRadians(x) else x)
                    "tan" -> tan(if (isDegree) Math.toRadians(x) else x)
                    "log" -> log10(x)
                    "ln" -> ln(x)
                    "abs" -> abs(x)
                    else -> throw RuntimeException("Unknown function: $func")
                }
            } else {
                throw RuntimeException("Unexpected: " + ch.toChar())
            }

            if (eat('^'.code)) x = x.pow(parseFactor())

            return x
        }
    }
}
