package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

enum class CbtDisplayTheme(
    val id: String,
    val title: String,
    val hindiTitle: String,
    val subtitle: String,
    val icon: ImageVector,
    val badge: String
) {
    NORMAL(
        id = "normal",
        title = "Normal Screen",
        hindiTitle = "नॉर्मल स्क्रीन (Standard NTA CBT)",
        subtitle = "Original Standard Exam Interface • Clean White Paper Pattern",
        icon = Icons.Default.WbSunny,
        badge = "DEFAULT"
    ),
    NIGHT_SLATE(
        id = "night_slate",
        title = "Night Mode",
        hindiTitle = "नाइट मोड (आई-केयर)",
        subtitle = "Soft Slate Navy • Zero Glare • Calms Eye Fatigue",
        icon = Icons.Default.NightlightRound,
        badge = "EYE CARE"
    ),
    DEEP_DARK(
        id = "deep_dark",
        title = "Deep Dark",
        hindiTitle = "डीप डार्क (AMOLED ब्लैक)",
        subtitle = "AMOLED Pitch Black (#000000) • Razor-Sharp White Text",
        icon = Icons.Default.DarkMode,
        badge = "ZERO BLEED"
    ),
    WARM_PAPER(
        id = "warm_paper",
        title = "Paper Sepia",
        subtitle = "Kindle Warm Parchment • Filters Blue-Light Spectrum",
        hindiTitle = "वॉर्म पेपर (सेपिया)",
        icon = Icons.Default.MenuBook,
        badge = "WARM SHIELD"
    );

    companion object {
        val LIGHT_DAY: CbtDisplayTheme get() = NORMAL
    }
}

data class CbtThemePalette(
    val screenBackground: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val topBarBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textAccent: Color,
    val optionUnselectedBg: Color,
    val optionUnselectedBorder: Color,
    val optionSelectedBg: Color,
    val optionSelectedBorder: Color,
    val imageContainerBg: Color,
    val imageContainerBorder: Color,
    val bottomBarBg: Color,
    val isDarkEquivalent: Boolean,
    val isDeepDark: Boolean = false
)

object CbtThemeManager {
    /**
     * Hardware color matrix for inverting white exam paper image backgrounds to dark,
     * turning black ink text/circuits/graphs into clean glowing white lines.
     */
    val InvertColorMatrix = ColorMatrix(
        floatArrayOf(
            -1f,  0f,  0f, 0f, 255f,
             0f, -1f,  0f, 0f, 255f,
             0f,  0f, -1f, 0f, 255f,
             0f,  0f,  0f, 1f,   0f
        )
    )

    fun getPalette(theme: CbtDisplayTheme): CbtThemePalette {
        return when (theme) {
            CbtDisplayTheme.NIGHT_SLATE -> CbtThemePalette(
                screenBackground = Color(0xFF0F172A),      // Eye-comfort slate 900
                cardBackground = Color(0xFF1E293B),        // Slate 800
                cardBorder = Color(0xFF334155),            // Slate 700
                topBarBackground = Color(0xFF0F172A),
                textPrimary = Color(0xFFF8FAFC),           // Crystal clear crisp slate white
                textSecondary = Color(0xFF94A3B8),         // Muted slate text
                textAccent = Color(0xFF818CF8),            // Indigo 400
                optionUnselectedBg = Color(0xFF162032),
                optionUnselectedBorder = Color(0xFF334155),
                optionSelectedBg = Color(0x336366F1),
                optionSelectedBorder = Color(0xFF818CF8),
                imageContainerBg = Color(0xFF162032),
                imageContainerBorder = Color(0xFF334155),
                bottomBarBg = Color(0xFF0F172A),
                isDarkEquivalent = true,
                isDeepDark = false
            )
            CbtDisplayTheme.DEEP_DARK -> CbtThemePalette(
                screenBackground = Color(0xFF000000),      // Pure pitch black (AMOLED 0%)
                cardBackground = Color(0xFF0C0D12),        // Subtle dark charcoal
                cardBorder = Color(0xFF27272A),            // Zinc 800 border
                topBarBackground = Color(0xFF000000),
                textPrimary = Color(0xFFFFFFFF),           // 100% pure high-contrast white
                textSecondary = Color(0xFFA1A1AA),         // Zinc 400
                textAccent = Color(0xFFA5B4FC),            // Soft violet accent
                optionUnselectedBg = Color(0xFF111218),
                optionUnselectedBorder = Color(0xFF27272A),
                optionSelectedBg = Color(0x3D4F46E5),
                optionSelectedBorder = Color(0xFF6366F1),
                imageContainerBg = Color(0xFF08080C),
                imageContainerBorder = Color(0xFF27272A),
                bottomBarBg = Color(0xFF000000),
                isDarkEquivalent = true,
                isDeepDark = true
            )
            CbtDisplayTheme.WARM_PAPER -> CbtThemePalette(
                screenBackground = Color(0xFFF5F0E6),      // Kindle Paperwhite warm parchment
                cardBackground = Color(0xFFFAF7F0),        // Soft paper card
                cardBorder = Color(0xFFE5DDD0),
                topBarBackground = Color(0xFFF5F0E6),
                textPrimary = Color(0xFF292524),           // Warm espresso charcoal ink
                textSecondary = Color(0xFF57534E),         // Stone warm text
                textAccent = Color(0xFF059669),            // Forest emerald accent
                optionUnselectedBg = Color(0xFFF7F3EB),
                optionUnselectedBorder = Color(0xFFE2D9C8),
                optionSelectedBg = Color(0x2E10B981),
                optionSelectedBorder = Color(0xFF059669),
                imageContainerBg = Color(0xFFFDFCFA),
                imageContainerBorder = Color(0xFFE2D9C8),
                bottomBarBg = Color(0xFFF5F0E6),
                isDarkEquivalent = false,
                isDeepDark = false
            )
            CbtDisplayTheme.NORMAL -> CbtThemePalette(
                screenBackground = Color(0xFFF8FAFC),      // Crisp NTA daylight
                cardBackground = Color(0xFFFFFFFF),
                cardBorder = Color(0xFFE2E8F0),
                topBarBackground = Color(0xFFFFFFFF),
                textPrimary = Color(0xFF0F172A),           // Deep slate ink
                textSecondary = Color(0xFF64748B),
                textAccent = Color(0xFF4F46E5),
                optionUnselectedBg = Color(0xFFF8FAFC),
                optionUnselectedBorder = Color(0xFFE2E8F0),
                optionSelectedBg = Color(0xFFEEF2FF),
                optionSelectedBorder = Color(0xFF6366F1),
                imageContainerBg = Color(0xFFFFFFFF),
                imageContainerBorder = Color(0xFFE2E8F0),
                bottomBarBg = Color(0xFFFFFFFF),
                isDarkEquivalent = false,
                isDeepDark = false
            )
        }
    }
}

/**
 * Eye-Care Display & Night Mode Settings Dialog
 */
@Composable
fun CbtEyeCareSettingsDialog(
    currentTheme: CbtDisplayTheme,
    onSelectTheme: (CbtDisplayTheme) -> Unit,
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    autoInvertImages: Boolean,
    onAutoInvertImagesChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = CbtThemeManager.getPalette(currentTheme)

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = palette.cardBackground,
            border = BorderStroke(1.2.dp, palette.cardBorder),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(palette.textAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = palette.textAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Eye Protection & Display",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "आँखों की सुरक्षा • 3-घंटे लगातार टेस्ट",
                                fontSize = 10.5.sp,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = palette.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    "CHOOSE TEST SCREEN THEME",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    color = palette.textAccent,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 4 Theme Selectors
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CbtDisplayTheme.values().forEach { themeItem ->
                        val isSelected = currentTheme == themeItem
                        val previewPalette = CbtThemeManager.getPalette(themeItem)

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) previewPalette.cardBackground else palette.optionUnselectedBg,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) palette.textAccent else palette.cardBorder.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectTheme(themeItem) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(previewPalette.screenBackground)
                                        .border(1.dp, previewPalette.cardBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = themeItem.icon,
                                        contentDescription = null,
                                        tint = previewPalette.textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = themeItem.title,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = palette.textAccent.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = themeItem.badge,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = palette.textAccent,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = themeItem.hindiTitle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = palette.textAccent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = themeItem.subtitle,
                                        fontSize = 10.sp,
                                        color = palette.textSecondary,
                                        lineHeight = 14.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = palette.textAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = palette.cardBorder.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // Font Size Adjuster
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Question & Math Font Size",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            "Text size scale: ${(fontScale * 100).toInt()}%",
                            fontSize = 10.5.sp,
                            color = palette.textSecondary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (fontScale > 0.85f) onFontScaleChange((fontScale - 0.10f).coerceAtLeast(0.85f))
                            },
                            enabled = fontScale > 0.85f,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Smaller", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("A-", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (fontScale < 1.35f) onFontScaleChange((fontScale + 0.10f).coerceAtMost(1.35f))
                            },
                            enabled = fontScale < 1.35f,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.textAccent),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Larger", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("A+", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto-Invert Diagram Switch (Protects against bright white cropped paper images in dark mode)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.optionUnselectedBg)
                        .clickable { onAutoInvertImagesChange(!autoInvertImages) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Anti-Glare White Paper Invert",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            "Scanned white questions/circuits dark background me automatically invert honge",
                            fontSize = 10.sp,
                            color = palette.textSecondary,
                            lineHeight = 13.sp
                        )
                    }
                    Switch(
                        checked = autoInvertImages,
                        onCheckedChange = onAutoInvertImagesChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = palette.textAccent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (currentTheme != CbtDisplayTheme.NORMAL) {
                    OutlinedButton(
                        onClick = {
                            onSelectTheme(CbtDisplayTheme.NORMAL)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, palette.cardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.textPrimary),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Switch to Normal Screen (नॉर्मल स्क्रीन)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.textAccent),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("Apply & Continue Test ✓", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
