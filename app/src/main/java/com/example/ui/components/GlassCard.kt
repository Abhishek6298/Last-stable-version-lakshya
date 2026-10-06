package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Premium Liquid Glass / Mosaic Frosted Glass Card (iOS / VisionOS aesthetics).
 *
 * Visual Highlights:
 * 1. Prismatic Fresnel Rim: 4-stop chamfered border catching directional light.
 * 2. Specular Liquid Sheen: Angled translucent gradient body letting background mosaic colors softly bleed through.
 * 3. Micro-Specular Hairline: Subtle 1.2px top inner glint replicating authentic liquid glass edge refraction.
 * 4. Ambient Colored Shadow: Violet/indigo tinted float elevation.
 * 5. Zero-lag: Pure hardware shader drawing with zero GC overhead.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.run { (red + green + blue) < 1.0f }
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    // 1. Translucent Liquid Glass Surface Fill (Allows mosaic glow bleed)
    val cardBackgroundBrush = remember(isDark) {
        if (isDark) {
            // Liquid Frosted Obsidian Glass
            Brush.linearGradient(
                colors = listOf(
                    Color(0xCC1E293B), // Translucent specular catch (top-start)
                    Color(0xD9131C2E), // Frosted body
                    Color(0xEA0F172A), // Deep liquid body
                    Color(0xF20B101E)  // Grounding shadow
                ),
                start = Offset.Zero,
                end = Offset.Infinite
            )
        } else {
            // Liquid Frosted Crystal Glass (Translucent body lets vibrant orbs bleed & refract)
            Brush.linearGradient(
                colors = listOf(
                    Color(0xD9FFFFFF), // Crystal reflection
                    Color(0xA6F8FAFC), // Translucent liquid glass body
                    Color(0xB8F0F4FF), // Cool violet-blue refraction
                    Color(0xCCFFFFFF)  // Base specular catch
                ),
                start = Offset.Zero,
                end = Offset.Infinite
            )
        }
    }

    // 2. Prismatic Fresnel Chamfered Rim Lighting (Bevel Border Gradient)
    val cardBorderBrush = remember(isDark) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0x73FFFFFF), // Crisp specular gleam (top-left)
                    Color(0x33818CF8), // Prismatic violet-indigo refraction
                    Color(0x14FFFFFF), // Soft lateral rim
                    Color(0x2E38BDF8)  // Ambient cyan bounce reflection (bottom-right)
                ),
                start = Offset.Zero,
                end = Offset.Infinite
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xF5FFFFFF), // Crisp bright white gleam
                    Color(0x596366F1), // Prismatic soft violet tint
                    Color(0x3D38BDF8), // Cyan edge shimmer
                    Color(0xB3FFFFFF)  // Lower specular highlight
                ),
                start = Offset.Zero,
                end = Offset.Infinite
            )
        }
    }

    // 3. Ambient Luminescent Shadow Colors
    val spotShadow = remember(isDark) {
        if (isDark) Color(0x336366F1) else Color(0x246366F1)
    }
    val ambientShadow = remember(isDark) {
        if (isDark) Color(0x1A090D16) else Color(0x12475569)
    }

    // 4. Specular Inner Hairline Glint Brush (Cached to eliminate per-frame allocations during scrolling)
    val innerHairlineBrush = remember(isDark) {
        val color = if (isDark) Color(0x33FFFFFF) else Color(0x99FFFFFF)
        Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                color,
                Color.Transparent
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                clip = false,
                spotColor = spotShadow,
                ambientColor = ambientShadow
            )
            .clip(shape)
            .background(cardBackgroundBrush)
            .border(borderWidth, cardBorderBrush, shape)
            .drawWithContent {
                drawContent()
                // Micro-specular hairline across top edge for authentic liquid glass edge reflection
                val cornerPx = cornerRadius.toPx()
                drawLine(
                    brush = innerHairlineBrush,
                    start = Offset(x = cornerPx * 0.45f, y = 1.2f),
                    end = Offset(x = size.width - cornerPx * 0.45f, y = 1.2f),
                    strokeWidth = 1.2f
                )
            }
    ) {
        content()
    }
}

